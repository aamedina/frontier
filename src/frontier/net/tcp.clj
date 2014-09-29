(ns frontier.net.tcp
  (:require [taoensso.nippy :refer [freeze thaw]]
            [clojure.core.async :as a :refer [go go-loop <! >! put! chan alts!]]
            [com.stuartsierra.component :as c]
            [frontier.net.async :refer [future-chan]])
  (:import (io.netty.bootstrap Bootstrap ServerBootstrap)
           (io.netty.channel ChannelHandlerContext ChannelFuture)
           (io.netty.handler.codec ByteToMessageCodec CorruptedFrameException)
           (io.netty.channel.nio NioEventLoopGroup)
           (io.netty.buffer ByteBuf)
           (java.util List)
           (java.net InetSocketAddress)))

(set! *warn-on-reflection* true)

(defn tcp-message-codec
  []
  (proxy [ByteToMessageCodec] []
    (^void decode [^ChannelHandlerContext ctx ^ByteBuf msg ^List out]
      (let [msg (let [arr (byte-array (.readableBytes msg))]
                      (.getBytes msg 0 arr)
                      (thaw arr))]
        (if (and (map? msg) (contains? msg :op) (contains? msg :id))
          (.add out msg)
          (throw (CorruptedFrameException. "Invalid login message decoded")))))
    (^void encode [^ChannelHandlerContext ctx msg ^List out]
      (if (and (map? msg) (contains? msg :op) (contains? msg :id))
        (let [frozen (freeze msg)]
          (.add out (doto (.buffer (.alloc ctx) (alength frozen))
                      (.writeBytes frozen))))
        (throw (CorruptedFrameException. "Invalid outgoing login message"))))))

(defrecord TCPServer [^NioEventLoopGroup bosses
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

(defrecord TCPClient [^NioEventLoopGroup group
                      ^Bootstrap bootstrap
                      ^InetSocketAddress address
                      ^ChannelFuture bind-future]
  c/Lifecycle
  (start [this]
    (if (nil? bind-future)
      (let [bind-future (.bind bootstrap address)
            client (assoc this
                     :bind-future bind-future)]
        (go (when-some [future (<! (future-chan bind-future))]
              (<! (future-chan (.closeFuture (.channel ^ChannelFuture future))))
              (c/stop this)))
        client)
      this))
  (stop [this]
    (if (and bind-future
             (not (or (.isShutdown group) (.isShuttingDown group))))
      (do (.shutdownGracefully group)
          (assoc this :bind-future nil))
      this)))