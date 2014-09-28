(ns frontier.core-test
  (:require [clojure.test :refer :all]
            [taoensso.nippy :as nippy :refer [freeze thaw]]
            [criterium.core :refer [quick-bench]]
            [com.stuartsierra.component :as c]
            [clojure.core.async :as a :refer [go go-loop <! >! put! chan
                                              thread <!! >!!]]
            [frontier.net.async :refer [future-chan]]
            [clojure.java.io :as io])
  (:import (io.netty.handler.codec MessageToMessageEncoder)
           (io.netty.bootstrap Bootstrap)
           (io.netty.channel.socket DatagramPacket)
           (io.netty.channel.socket.nio NioDatagramChannel)
           (io.netty.channel ChannelHandlerContext ChannelOption Channel
                             ChannelFuture)
           (io.netty.util CharsetUtil)
           (io.netty.channel.nio NioEventLoopGroup)
           (io.netty.buffer ByteBuf)
           (java.net InetSocketAddress SocketAddress)
           (java.io File RandomAccessFile)))

(set! *warn-on-reflection* true)

(def ^:const log-event-separator (byte \:))

(defrecord LogEvent [^InetSocketAddress source
                     ^long received
                     ^String logfile
                     ^String message])

(defn log-event-encoder
  [^InetSocketAddress remote]
  (proxy [MessageToMessageEncoder] []
    (^void encode [^ChannelHandlerContext ctx ^LogEvent evt ^java.util.List out]
      (let [msg (freeze (str (:logfile evt) \: (:message evt)))
            buf (doto (.buffer (.alloc ctx))
                  (.writeBytes msg))]
        (.add out (DatagramPacket. buf remote))))))

(defn netty-log-event-encoder
  [^InetSocketAddress remote]
  (proxy [MessageToMessageEncoder] []
    (^void encode [^ChannelHandlerContext ctx ^LogEvent evt ^java.util.List out]
      (let [buf (.buffer (.alloc ctx))]
        (doto ^ByteBuf buf
          (.writeBytes (.getBytes ^String (:logfile evt) CharsetUtil/UTF_8))
          (.writeByte log-event-separator)
          (.writeBytes (.getBytes ^String (:message evt) CharsetUtil/UTF_8)))
        (.add out (DatagramPacket. buf remote))))))

(defrecord LogEventBroadcaster [^NioEventLoopGroup group
                                ^Bootstrap bootstrap
                                ^InetSocketAddress address
                                ^java.io.File file]
  c/Lifecycle
  (start [this]
    (when true
      (thread
        (let [bind-future (future-chan (.bind ^Bootstrap bootstrap 0))
              ch (.channel ^ChannelFuture (<!! bind-future))]
          (loop [pointer 0
                 length (.length ^File file)]
            (let [new-pointer
                  (cond
                    (< length pointer) length
                    
                    (> length pointer)
                    (with-open [raf (RandomAccessFile. ^File file "r")]
                      (.seek raf pointer)
                      (loop []
                        (when-some [line (.readLine raf)]
                          (.writeAndFlush ^Channel ch
                                          (LogEvent. nil -1
                                                     (.getAbsolutePath file)
                                                     line))
                          (recur)))
                      (.getFilePointer raf))
                    
                    :else pointer)]
              (<!! (a/timeout 1000))
              (recur (long new-pointer) (.length ^File file)))))))
    this)
  (stop [this]
    (when-not (or (.isShutdown group) (.isShuttingDown group))
      (.shutdownGracefully group))
    this))

(defn log-event-broadcaster
  [address file]
  (let [group (NioEventLoopGroup.)
        bootstrap (Bootstrap.)]
    (-> (.group bootstrap group)
        (.channel NioDatagramChannel)
        (.option ChannelOption/SO_BROADCAST true)
        (.handler (log-event-encoder address)))
    (LogEventBroadcaster. group bootstrap address file)))

(def broadcaster
  (log-event-broadcaster (InetSocketAddress. "255.255.255.255" 9090)
                         (io/as-file "resources/log.edn")))
