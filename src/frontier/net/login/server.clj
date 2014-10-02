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
            [frontier.net.tcp :refer [tcp-message-codec ->TCPServer]]
            [datomic.api :as db])
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
           (java.net SocketAddress InetAddress InetSocketAddress)
           (javax.net.ssl SSLEngine)))

(set! *warn-on-reflection* true)

(defn login
  [server ^ChannelHandlerContext ctx msg]
  #_(redis/update (:id msg) assoc :auth
                  {:username (:username msg)
                   :password (:password msg)})
  (log/info (:db server)))

(defn logout
  [server ^ChannelHandlerContext ctx msg]
  (redis/remove (:id msg) (dissoc msg :op))
  (.close ctx))

(defn login-server-handler
  [server]
  (let [channels (DefaultChannelGroup. GlobalEventExecutor/INSTANCE)]
    (proxy [SimpleChannelInboundHandler] []
      (^void channelActive [^ChannelHandlerContext ctx]
        (go (let [ctx ^ChannelHandlerContext ctx
                  ssl ^SslHandler (.get (.pipeline ctx) SslHandler)]
              (<! (future-chan (.handshakeFuture ssl)))
              (log/info (str "Welcome to "
                             (.getHostName (InetAddress/getLocalHost))
                             " secure chat service\n!"))
              (log/info (str "Your session is protected by"
                             (-> (.engine ^SslHandler ssl)
                                 (.getSession)
                                 (.getCipherSuite)) 
                             " cipher suite.\n"))
              (.add ^DefaultChannelGroup channels (.channel ctx)))))
      (^void channelReadComplete [^ChannelHandlerContext ctx]
        (.flush ctx))
      (^void messageReceived [^ChannelHandlerContext ctx msg]
        (log/info msg)
        (case (:op msg)
          :connect (redis/add (:id msg) (dissoc msg :op))
          :login (login server ctx msg)
          :logout (logout server ctx msg)
          (log/error msg)))
      (^void exceptionCaught [^ChannelHandlerContext ctx ^Throwable t]
        (.printStackTrace t)
        (.close ctx)))))

(defn login-server-pipeline
  [server]
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
                                 (login-server-handler server)])))))))

(defn login-server
  ([] (login-server (InetSocketAddress. 9091)))
  ([address]
     (let [bosses (NioEventLoopGroup. 1)
           workers (NioEventLoopGroup.)
           bootstrap (doto (ServerBootstrap.)
                       (.group bosses workers)
                       (.channel NioServerSocketChannel)
                       (.handler (LoggingHandler. LogLevel/INFO)))]
       (->TCPServer bosses workers bootstrap address nil
                    login-server-pipeline))))

