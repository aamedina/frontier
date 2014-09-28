(ns frontier.net.client
  (:require [frontier.net.server :as s]
            [com.stuartsierra.component :as c]
            [clojure.core.async :as a :refer [go go-loop <! >! put! chan take!]]
            [frontier.net.async :refer [future-chan]]
            [clojure.tools.logging :as log]
            [taoensso.nippy :as nippy :refer [freeze thaw]])
  (:import (java.net InetSocketAddress)
           (io.netty.bootstrap Bootstrap)
           (io.netty.channel ChannelHandlerContext ChannelOption ChannelFuture
                             Channel SimpleChannelInboundHandler ChannelHandler
                             ChannelInitializer)
           (io.netty.channel.nio NioEventLoopGroup)
           (io.netty.channel.socket DatagramPacket)
           (io.netty.handler.codec MessageToMessageDecoder
                                   MessageToMessageEncoder)
           (io.netty.channel.socket.nio NioDatagramChannel)
           (io.netty.handler.logging LoggingHandler LogLevel)
           (io.netty.handler.codec.serialization ObjectDecoder ObjectEncoder
                                                 ClassResolver)
           (io.netty.buffer ByteBuf Unpooled)
           (io.netty.util CharsetUtil)
           (java.util List)))

(set! *warn-on-reflection* true)

(defonce +client+ nil)

(defonce +session+ (atom nil))

(defmulti handle-server-op
  (fn [^ChannelHandlerContext ctx msg] (get-in msg [:packet :op])))

(defmethod handle-server-op :ping
  [^ChannelHandlerContext ctx msg]
  (.write ctx (assoc msg :packet {:op :pong})))

(defmethod handle-server-op :pong
  [^ChannelHandlerContext ctx msg]
  (log/info "PONG"))

(defmethod handle-server-op :shutdown
  [^ChannelHandlerContext ctx msg]
  (reset! +session+ nil))

(defmethod handle-server-op :default
  [^ChannelHandlerContext ctx msg]
  (log/warn "Unimplemented op" (:packet msg)))

(defn server-packet-handler
  []
  (proxy [SimpleChannelInboundHandler] []
    (^void channelReadComplete [^ChannelHandlerContext ctx]
      (.flush ctx))
    (^void messageReceived [^ChannelHandlerContext ctx msg]
      (handle-server-op ctx msg))
    (^void exceptionCaught [^ChannelHandlerContext ctx ^Throwable t]
      (log/error t (.getMessage t))
      (.printStackTrace t)
      (.close ctx))))

(defn server-datagram-decoder
  []
  (proxy [MessageToMessageDecoder] []
    (^void decode [^ChannelHandlerContext ctx ^DatagramPacket packet ^List out]
      (let [content ^ByteBuf (.content packet)
            session {:sequence (.readInt content)
                     :ack-sequence (.readInt content)
                     :id (.readInt content)}]
        (when (> (:sequence session) (:sequence @+session+))
          (.add out {:session session
                     :packet (let [arr (byte-array (.readableBytes content))]
                               (.getBytes content 12 arr)
                               (thaw arr))}))))))

(defn client-datagram-encoder
  [remote-address]
  (proxy [MessageToMessageEncoder] []
       (^void encode [^ChannelHandlerContext ctx msg ^List out]
         (let [session (:session msg)
               frozen (freeze (:packet msg))
               content (doto (.buffer (.alloc ctx) (+ (alength frozen) 12))
                         (.writeInt (:sequence session))
                         (.writeInt (:ack-sequence session))
                         (.writeInt (:id session))
                         (.writeBytes frozen))]
           (.add out (DatagramPacket. content remote-address))))))

(defn client-pipeline
  [remote-address]
  (proxy [ChannelInitializer] []
    (initChannel [^Channel ch]
      (doto (.pipeline ch)
        (.addLast (into-array ChannelHandler
                              [(LoggingHandler. LogLevel/INFO)
                               (server-datagram-decoder)
                               (server-packet-handler)
                               (client-datagram-encoder remote-address)]))))))

(defrecord ClientSocket [^NioEventLoopGroup group
                         ^Bootstrap bootstrap
                         ^InetSocketAddress local-address
                         ^InetSocketAddress remote-address
                         ^ChannelFuture bind-future]
  c/Lifecycle
  (start [this]
    (if (nil? bind-future)
      (let [bind-future (.bind bootstrap local-address)
            client (assoc this
                     :bind-future bind-future)]
        (go (when-some [ret (<! (future-chan bind-future))]
              (let [ch (.channel ^ChannelFuture ret)
                    session {:sequence -1
                             :ack-sequence -1
                             :id (rand-int Integer/MAX_VALUE)}]
                (reset! +session+ session)
                (.writeAndFlush ch {:session session
                                    :packet {:op :connect}})
                (<! (future-chan (.closeFuture ch)))
                (.shutdownGracefully ^NioEventLoopGroup group))))
        (alter-var-root #'+client+ (constantly client))
        client)
      this))
  (stop [this]
    (if (and bind-future (not (or (.isShutdown group)
                                  (.isShuttingDown group))))
      (do (.shutdownGracefully group)
          (assoc this :bind-future nil))
      this)))

(defn client-socket
  ([] (client-socket (InetSocketAddress. "192.168.1.2" 9090)))
  ([remote-address]
     (client-socket remote-address (client-pipeline remote-address)))
  ([remote-address handler]
     (let [group (NioEventLoopGroup.)
           bootstrap (doto (Bootstrap.)
                       (.group group)
                       (.channel NioDatagramChannel)
                       (.option ChannelOption/SO_BROADCAST true)
                       (.handler handler))
           local-address (InetSocketAddress. 0)]
       (ClientSocket. group bootstrap local-address remote-address nil))))

