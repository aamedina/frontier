(ns frontier.net.server
  (:require [com.stuartsierra.component :as c]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.tools.logging :as log]
            [clojure.core.async :as a :refer [go go-loop <! >! put! chan alts!]]
            [frontier.net.async :refer [future-chan]]
            [frontier.util.redis :as redis]
            [clojure.java.io :as io]
            [taoensso.nippy :as nippy :refer [freeze thaw]])
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

(defmethod handle-client-op :connect
  [^ChannelHandlerContext ctx msg]
  (let [in (chan (a/sliding-buffer 1024))
        out (sliding-pub in)
        session (assoc (:session msg)
                  :status :connected
                  :in in
                  :out out)]
    (swap! +sessions+ assoc (:id session) session)))

(defn disconnect
  [sender]
  (swap! sessions dissoc sender))

(defmethod handle-client-op :disconnect
  [^ChannelHandlerContext ctx msg]
  (disconnect (:sender msg)))

(defmethod handle-client-op :ping
  [^ChannelHandlerContext ctx msg]
  (let [ping-future (future-chan (.write ctx {:op :pong}))]
    (go (<! ping-future)
        (let [ping (:ping (get-session (:sender msg)))
              t (a/timeout 2000)]
          (when-some [[val ch] (alts! [ping t])]
            (condp identical? ch
              ping (log/info "PING SUCCESSFUL")
              t (disconnect (:sender msg))))))))

(defmethod handle-client-op :pong
  [^ChannelHandlerContext ctx msg]
  (put! (:ping (get-session (:sender msg))) true))

(defmethod handle-client-op :default
  [^ChannelHandlerContext ctx msg]
  (log/warn "Unimplemented op" (:packet msg)))

(defn update-session
  [{:keys [id] :as session}]
  (get (swap! +sessions+ update-in [id] merge session) id))

(defn client-packet-handler
  []
  (proxy [SimpleChannelInboundHandler] []
    (^void channelReadComplete [^ChannelHandlerContext ctx]
      (.flush ctx))
    (^void messageReceived [^ChannelHandlerContext ctx msg]
      (if (contains? @+sessions+ (get-in msg [:session :id]))
        (handle-client-op ctx (update-in msg [:session] update-session))
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
