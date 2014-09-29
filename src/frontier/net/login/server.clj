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
            [crypto.password.scrypt :as scrypt])
  (:import (io.netty.bootstrap ServerBootstrap)
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
           (io.netty.handler.ssl SslContext SslHandler)
           (io.netty.handler.ssl.util SelfSignedCertificate)
           (java.net SocketAddress InetAddress InetSocketAddress)
           (java.util List)))

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
        )
      (^void exceptionCaught [^ChannelHandlerContext ctx ^Throwable t]
        (log/error t (.getMessage t))
        (.printStackTrace t)))))

(defn login-server-pipeline
  []
  (proxy [ChannelInitializer] []
    (initChannel [^Channel ch]
      (let [ssc (SelfSignedCertificate.)
            ssl-ctx (SslContext/newServerContext (.certificate ssc)
                                                 (.privateKey ssc))]
        (doto (.pipeline ch)
          (.addLast (into-array ChannelHandler
                                [(.newHandler ssl-ctx (.alloc ch))
                                 (login-server-handler)])))))))

(defrecord LoginServer [^NioEventLoopGroup bosses
                        ^NioEventLoopGroup workers
                        ^ServerBootstrap bootstrap
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
              (c/stop this)))
        (alter-var-root #'+server+ (constantly server))
        server)
      this))
  (stop [this]
    (if (and bind-future
             (not (or (.isShutdown bosses) (.isShuttingDown bosses)))
             (not (or (.isShutdown workers) (.isShuttingDown workers))))
      (do (.shutdownGracefully bosses)
          (.shutdownGracefully workers)
          (assoc this :bind-future nil))
      this)))

(defn login-server
  ([] (login-server (InetSocketAddress. 9091)))
  ([address] (login-server address (login-server-pipeline)))
  ([address handler]
     (let [bosses (NioEventLoopGroup. 1)
           workers (NioEventLoopGroup.)
           bootstrap (doto (ServerBootstrap.)
                       (.group bosses workers)
                       (.channel SocketChannel)
                       (.option ChannelOption/SO_BROADCAST true)
                       (.handler (LoggingHandler. LogLevel/INFO))
                       (.childHandler handler))]
       (LoginServer. bosses workers bootstrap address nil))))

