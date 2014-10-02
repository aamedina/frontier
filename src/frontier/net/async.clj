(ns frontier.net.async
  (:require [clojure.core.async :as a
             :refer [go go-loop <! >! put! chan close! buffer]]
            [clojure.core.async.impl.protocols :as impl]
            [datomic.api :as d]
            [clojure.core.async.impl.exec.threadpool :refer [the-executor]])
  (:import (io.netty.channel ChannelHandlerContext ChannelOption ChannelFuture)
           (java.util.concurrent TimeUnit)
           (io.netty.util.concurrent Future AbstractFuture
                                     GenericFutureListener)))

(set! *warn-on-reflection* true)

(defn future-chan
  [^Future channel-future]
  (let [out (chan 1)]
    (.addListener channel-future (reify GenericFutureListener
                                   (operationComplete [_ future]
                                     (put! out future
                                           (fn [_] (a/close! out))))))
    out))

(defn transact
  [conn tx-data]
  (let [out (chan 1)]
    (-> (if (sequential? tx-data)
          (d/transact-async conn tx-data)
          (d/transact-async conn [tx-data]))
        (d/add-listener #(put! out true (fn [_] (a/close! out))) the-executor))
    out))
