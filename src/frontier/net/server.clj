(ns frontier.net.server
  (:require [com.stuartsierra.component :as c]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.tools.logging :as log]
            [clojure.core.async :as a :refer [go go-loop <! >! put! chan alts!
                                              close! sub pub unsub timeout]]
            [frontier.net.async :refer [future-chan]]
            [frontier.util.redis :as redis]
            [clojure.java.io :as io]
            [taoensso.nippy :as nippy :refer [freeze thaw]]
            [crypto.password.scrypt :as scrypt])
  (:import (io.netty.bootstrap Bootstrap)
           (io.netty.channel ChannelOption ChannelInitializer ChannelFuture
                             ChannelPipeline ChannelHandlerAdapter
                             ChannelHandlerContext ChannelHandler
                             SimpleChannelInboundHandler Channel)
           (io.netty.channel.nio NioEventLoopGroup)
           (io.netty.channel.group ChannelGroup DefaultChannelGroup)
           (io.netty.handler.logging LoggingHandler LogLevel)
           (io.netty.channel.socket DatagramPacket SocketChannel)
           (io.netty.channel.socket.nio NioDatagramChannel)
           (io.netty.handler.codec MessageToMessageDecoder
                                   MessageToMessageEncoder)
           (io.netty.handler.codec.serialization ObjectDecoder ObjectEncoder
                                                 ClassResolver)
           (io.netty.util.concurrent GenericFutureListener GlobalEventExecutor)
           (io.netty.buffer ByteBuf Unpooled)
           (io.netty.util CharsetUtil)
           (java.net SocketAddress InetAddress InetSocketAddress)
           (java.util List)))

(set! *warn-on-reflection* true)

(defonce +server+ nil)

(defonce +sessions+ (atom {}))

(defmulti handle-client-op
  (fn [^ChannelHandlerContext ctx msg] (get-in msg [:packet :op])))

(defn sliding-pub
  [in]
  (a/pub in
         (fn [msg] (get-in msg [:packet :op]))
         (fn [op] (a/sliding-buffer 1))))

(defn ensure-ack
  ([ctx msg] (ensure-ack ctx msg 1000))
  ([ctx msg resend-ms] (ensure-ack ctx msg resend-ms 15000))
  ([ctx msg resend-ms link-dead-ms]
     (go (<! (future-chan (.write ^ChannelHandlerContext ctx msg)))
         (let [{:keys [id in pub]} (:session msg)
               ctx ^ChannelHandlerContext ctx
               op (get-in msg [:packet :op])
               ack (sub pub :ack (chan 1))
               link-dead (timeout link-dead-ms)
               ack? (loop [t (timeout resend-ms)]
                      (when-some [[msg ch] (alts! [ack link-dead t])]
                        (condp identical? ch
                          ack (if (= (get-in msg [:packet :ack]) op)
                                true
                                (recur t))
                          t (do (<! (future-chan (.write ctx msg)))
                                (recur (timeout resend-ms)))
                          link-dead false)))]
           (close! ack)
           (close! link-dead)
           ack?))))

(defmethod handle-client-op :connect
  [^ChannelHandlerContext ctx msg]
  (let [in (chan (a/sliding-buffer 1024))
        pub (sliding-pub in)
        sid (get-in msg [:session :id])
        session (atom (assoc (:session msg)
                        :status :connecting
                        :in in
                        :pub pub
                        :snapshots []
                        :context ctx))]
    (go (<! (sub pub :disconnect (chan 1)))
        (swap! +sessions+ dissoc sid))
    
    (swap! +sessions+ assoc sid session)))

(defmethod handle-client-op :login
  [^ChannelHandlerContext ctx msg]
  (let [{:keys [account password-hash] :as packet} (:packet msg)]
    )
  ;; (go (if (true? (<! (ensure-ack ctx (assoc msg :packet {:op :auth}))))
  ;;       (swap! session assoc :status :connected)
  ;;       (swap! +sessions+ dissoc sid)))
  )

(defmethod handle-client-op :ack
  [^ChannelHandlerContext ctx msg]
  (put! (get-in msg [:session :in]) msg))

(defmethod handle-client-op :disconnect
  [^ChannelHandlerContext ctx msg]
  (put! (get-in msg [:session :in]) msg))

(defmethod handle-client-op :ping
  [^ChannelHandlerContext ctx msg]
  (let [ping-future (future-chan (.write ctx (assoc msg :packet {:op :pong})))]
    ))

(defmethod handle-client-op :pong
  [^ChannelHandlerContext ctx msg]
  )

(defmethod handle-client-op :default
  [^ChannelHandlerContext ctx msg]
  (log/warn "Unimplemented op" (:packet msg)))

(defn update-session!
  [{:keys [id] :as session}]
  (get (swap! +sessions+ update-in [id] merge session) id))

(defn client-packet-handler
  []
  (proxy [SimpleChannelInboundHandler] []
    (^void channelReadComplete [^ChannelHandlerContext ctx]
      (.flush ctx))
    (^void messageReceived [^ChannelHandlerContext ctx msg]
      (if (contains? @+sessions+ (get-in msg [:session :id]))
        (let [msg (-> (assoc-in msg [:session :ctx] ctx)
                      (update-in [:session] update-session!))]
          (do (put! (get-in msg [:session :in]) msg)
              (handle-client-op ctx msg)))
        (handle-client-op ctx msg)))
    (^void exceptionCaught [^ChannelHandlerContext ctx ^Throwable t]
      (log/error t (.getMessage t))
      (.printStackTrace t))))

(defn client-datagram-decoder
  []
  (proxy [MessageToMessageDecoder] []
    (^void decode [^ChannelHandlerContext ctx ^DatagramPacket packet ^List out]
      (let [content ^ByteBuf (.content packet)
            session {:sequence (.readInt content)
                     :ack-sequence (.readInt content)
                     :id (.readInt content)
                     :address (.sender packet)}]
        (.add out {:session session
                   :packet (let [arr (byte-array (.readableBytes content))]
                             (.getBytes content 12 arr)
                             (thaw arr))})))))

(defn server-datagram-encoder
  []
  (proxy [MessageToMessageEncoder] []
    (^void encode [^ChannelHandlerContext ctx msg ^List out]
      (let [session (:session msg)
            frozen (freeze (:packet msg))
            content (doto (.buffer (.alloc ctx) (+ (alength frozen) 12))
                      (.writeInt (:sequence session))
                      (.writeInt (:ack-sequence session))
                      (.writeInt (:id session))
                      (.writeBytes frozen))]
        (.add out (DatagramPacket. content (:address session)))))))

(defn server-pipeline
  []
  (proxy [ChannelInitializer] []
    (initChannel [^Channel ch]
      (doto (.pipeline ch)
        (.addLast (into-array ChannelHandler
                              [(LoggingHandler. LogLevel/INFO)
                               (client-datagram-decoder)
                               (client-packet-handler)
                               (server-datagram-encoder)]))))))

(defrecord GameServer [^NioEventLoopGroup group
                       ^DefaultChannelGroup channel-group
                       ^Bootstrap bootstrap
                       ^InetSocketAddress address
                       ^ChannelFuture bind-future]
  c/Lifecycle
  (start [this]
    (if (nil? bind-future)
      (let [bind-future (.bind bootstrap address)
            server (assoc this
                     :bind-future bind-future)]
        (go (when-some [future (<! (future-chan bind-future))]
              (<! (future-chan (.closeFuture (.channel ^ChannelFuture future))))
              (.shutdownGracefully ^NioEventLoopGroup group)))
        (alter-var-root #'+server+ (constantly server))
        server)
      this))
  (stop [this]
    (if (and bind-future (not (or (.isShutdown group)
                                  (.isShuttingDown group))))
      (do (.shutdownGracefully group)
          (assoc this :bind-future nil))
      this)))

(defn game-server
  ([] (game-server (InetSocketAddress. 9090)))
  ([address] (game-server address (server-pipeline)))
  ([address handler]
     (let [group (NioEventLoopGroup.)
           channel-group (DefaultChannelGroup. GlobalEventExecutor/INSTANCE)
           bootstrap (doto (Bootstrap.)
                       (.group group)
                       (.channel NioDatagramChannel)
                       (.option ChannelOption/SO_BROADCAST true)
                       (.handler handler))]
       (GameServer. group channel-group bootstrap address nil))))
