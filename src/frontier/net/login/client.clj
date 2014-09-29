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
                             SimpleChannelInboundHandler Channel)
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
  []
  (let [channels (DefaultChannelGroup. GlobalEventExecutor/INSTANCE)]
    (proxy [SimpleChannelInboundHandler] []
      (^void channelReadComplete [^ChannelHandlerContext ctx]
        (.flush ctx))
      (^void messageReceived [^ChannelHandlerContext ctx msg]
        (log/warn msg))
      (^void exceptionCaught [^ChannelHandlerContext ctx ^Throwable t]
        (log/error t (.getMessage t))
        (.printStackTrace t)))))

(defn login-client-pipeline
  [^InetSocketAddress address]
  (proxy [ChannelInitializer] []
    (initChannel [^Channel ch]
      (let [trust-manager InsecureTrustManagerFactory/INSTANCE
            ssl-ctx (SslContext/newClientContext trust-manager)]
        (doto (.pipeline ch)
          (.addLast (into-array ChannelHandler
                                [(.newHandler ssl-ctx (.alloc ch)
                                              (.getHostName address)
                                              (.getPort address))
                                 (tcp-message-codec)
                                 (login-client-handler)])))))))

(defn login-client
  ([] (login-client (InetSocketAddress. 10091)))
  ([address] (login-client address (login-client-pipeline address)))
  ([address handler]
     (let [group (NioEventLoopGroup.)
           bootstrap (doto (Bootstrap.)
                       (.group group)
                       (.channel NioSocketChannel)
                       (.handler handler))]
       (->TCPClient group bootstrap address nil))))

