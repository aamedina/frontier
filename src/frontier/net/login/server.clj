(ns frontier.net.login.server
  (:require [com.stuartsierra.component :as c]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.tools.logging :as log]
            [clojure.core.async :as a :refer [go go-loop <! >! put! chan alts!
                                              close! sub pub unsub timeout]]
            [frontier.net.async :refer [future-chan]]
            [frontier.util.redis :as redis]
            [clojure.java.io :as io]
            [taoensso.nippy :as nippy :refer [freeze thaw]]
            [crypto.password.scrypt :as scrypt]
            [frontier.net.tcp :refer [tcp-message-codec ->TCPServer]])
  (:import (io.netty.bootstrap ServerBootstrap)
           (io.netty.channel ChannelOption ChannelInitializer ChannelFuture
                             ChannelPipeline ChannelHandlerAdapter
                             ChannelHandlerContext ChannelHandler
                             SimpleChannelInboundHandler Channel)
           (io.netty.channel.nio NioEventLoopGroup)
           (io.netty.channel.group ChannelGroup DefaultChannelGroup)
           (io.netty.handler.logging LoggingHandler LogLevel)
           (io.netty.channel.socket SocketChannel)
           (io.netty.channel.socket.nio NioServerSocketChannel)
           (io.netty.util.concurrent GlobalEventExecutor)
           (io.netty.buffer ByteBuf)
           (io.netty.handler.ssl SslContext SslHandler)
           (io.netty.handler.ssl.util SelfSignedCertificate)
           (java.net SocketAddress InetAddress InetSocketAddress)))

(set! *warn-on-reflection* true)

(defn login-server-handler
  []
  (let [channels (DefaultChannelGroup. GlobalEventExecutor/INSTANCE)]
    (proxy [SimpleChannelInboundHandler] []
      (^void channelActive [^ChannelHandlerContext ctx]
        (go (let [ctx ^ChannelHandlerContext ctx]
              (<! (future-chan (-> (.pipeline ctx)
                                   ^SslHandler (.get SslHandler)
                                   (.handshakeFuture))))
              (.add ^DefaultChannelGroup channels (.channel ctx)))))
      (^void channelReadComplete [^ChannelHandlerContext ctx]
        (.flush ctx))
      (^void messageReceived [^ChannelHandlerContext ctx msg]
        (case (:op msg)
          :connect )
        (log/info "hello?")
        (log/info msg))
      (^void exceptionCaught [^ChannelHandlerContext ctx ^Throwable t]
        (.printStackTrace t)
        (.close ctx)))))

(defn login-server-pipeline
  []
  (proxy [ChannelInitializer] []
    (initChannel [^Channel ch]
      (let [ssc (SelfSignedCertificate.)
            ssl-ctx (SslContext/newServerContext (.certificate ssc)
                                                 (.privateKey ssc))]
        (doto (.pipeline ch)
          (.addLast (into-array ChannelHandler
                                [(LoggingHandler. LogLevel/INFO)
                                 (.newHandler ssl-ctx (.alloc ch))
                                 (tcp-message-codec)
                                 (login-server-handler)])))))))

(defn login-server
  ([] (login-server (InetSocketAddress. 9091)))
  ([address] (login-server address (login-server-pipeline)))
  ([address handler]
     (let [bosses (NioEventLoopGroup. 1)
           workers (NioEventLoopGroup.)
           bootstrap (doto (ServerBootstrap.)
                       (.group bosses workers)
                       (.channel NioServerSocketChannel)
                       (.childHandler handler))]
       (->TCPServer bosses workers bootstrap address nil))))

