(ns frontier.net.login.client
  (:require [com.stuartsierra.component :as c]
            [clojure.tools.logging :as log]
            [clojure.core.async :as a :refer [go go-loop <! >! put! chan alts!
                                              close! sub pub unsub timeout]]
            [frontier.net.async :refer [future-chan]]
            [frontier.util.redis :as redis]
            [clojure.java.io :as io]
            [frontier.net.tcp :refer [tcp-message-codec ->TCPClient]])
  (:import (io.netty.bootstrap Bootstrap)
           (io.netty.channel ChannelOption ChannelInitializer ChannelFuture
                             ChannelPipeline ChannelHandlerAdapter
                             ChannelHandlerContext ChannelHandler
                             SimpleChannelInboundHandler Channel ChannelPromise)
           (io.netty.channel.nio NioEventLoopGroup)
           (io.netty.channel.group ChannelGroup DefaultChannelGroup)
           (io.netty.handler.logging LoggingHandler LogLevel)
           (io.netty.channel.socket DatagramPacket SocketChannel)
           (io.netty.channel.socket.nio NioDatagramChannel NioSocketChannel)
           (io.netty.handler.codec ByteToMessageCodec CorruptedFrameException)
           (io.netty.util.concurrent GlobalEventExecutor)
           (io.netty.buffer ByteBuf)
           (io.netty.handler.ssl SslContext SslHandler)
           (io.netty.handler.ssl.util SelfSignedCertificate
                                      InsecureTrustManagerFactory)
           (java.net SocketAddress InetAddress InetSocketAddress)))

(set! *warn-on-reflection* true)

(defn login-client-handler
  [^InetSocketAddress remote-address]
  (let [channels (DefaultChannelGroup. GlobalEventExecutor/INSTANCE)]
    (proxy [SimpleChannelInboundHandler] []
      (^void messageReceived [^ChannelHandlerContext ctx msg]
        (log/warn msg))
      (^void exceptionCaught [^ChannelHandlerContext ctx ^Throwable t]
        (log/error t (.getMessage t))
        (.printStackTrace t)))))

(defn login-client-pipeline
  [^InetSocketAddress remote-address]
  (proxy [ChannelInitializer] []
    (initChannel [^Channel ch]
      (let [trust-manager InsecureTrustManagerFactory/INSTANCE
            ssl-ctx (SslContext/newClientContext trust-manager)]
        (doto (.pipeline ch)
          (.addLast (into-array ChannelHandler
                                [(LoggingHandler. LogLevel/INFO)
                                 (.newHandler ssl-ctx (.alloc ch)
                                              (.getHostName remote-address)
                                              (.getPort remote-address))
                                 (tcp-message-codec)
                                 (login-client-handler remote-address)])))))))

(defn login-client
  ([] (login-client (InetSocketAddress. "192.168.1.2" 9091)))
  ([remote-address]
     (login-client remote-address (login-client-pipeline remote-address)))
  ([^InetSocketAddress remote-address handler]
     (let [group (NioEventLoopGroup.)
           bootstrap (doto (Bootstrap.)
                       (.group group)
                       (.remoteAddress remote-address)
                       (.channel NioSocketChannel)
                       (.handler handler))
           in (chan 1)
           events (pub in :op)
           id (rand-int Integer/MAX_VALUE)
           conn (atom nil)]
       (go (let [{:keys [channel] :as msg} (<! (sub events :connect (chan 1)))]
             (reset! conn channel)
             (.writeAndFlush ^Channel channel {:op :connect :id id})))
       (->TCPClient group bootstrap remote-address nil conn id in events))))

