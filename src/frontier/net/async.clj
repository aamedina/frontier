(ns frontier.net.async
  (:require [clojure.core.async :as a :refer [go go-loop <! >! put! chan]])
  (:import (io.netty.channel ChannelHandlerContext ChannelOption ChannelFuture)
           (io.netty.util.concurrent GenericFutureListener)))

(set! *warn-on-reflection* true)

(defn future-chan
  [^ChannelFuture channel-future]
  (let [out (chan 1)]
    (.addListener channel-future (reify GenericFutureListener
                                   (operationComplete [_ future]
                                     (put! out future
                                           (fn [_] (a/close! out))))))
    out))
