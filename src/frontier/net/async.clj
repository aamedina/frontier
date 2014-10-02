(ns frontier.net.async
  (:require [clojure.core.async :as a
             :refer [go go-loop <! >! put! chan close! buffer]]
            [clojure.core.async.impl.protocols :as impl])
  (:import (io.netty.channel ChannelHandlerContext ChannelOption ChannelFuture)
           (java.util.concurrent TimeUnit)
           (io.netty.util.concurrent Future AbstractFuture
                                     GenericFutureListener)))

(set! *warn-on-reflection* true)

(defn future-chan
  [^Future channel-future]
  (let [out (chan 1)]
    (if (and (not (instance? io.netty.util.concurrent.Future channel-future))
             (instance? java.util.concurrent.Future channel-future))
      (future-chan (proxy [AbstractFuture] []
                     (get
                       ([] @channel-future)
                       ([timeout unit]
                          (deref channel-future
                                 (.convert TimeUnit/MILLISECONDS timeout unit)
                                 nil)))))
      (.addListener channel-future (reify GenericFutureListener
                                     (operationComplete [_ future]
                                       (put! out future
                                             (fn [_] (a/close! out)))))))
    out))

(defn double-buffered-chan
  [n]
  (let [front-buffer (buffer n)
        back-buffer (buffer n)
        front (chan front-buffer)
        back (chan back-buffer)
        in (chan n)]
    (go (loop [front-buffer front-buffer
               back-buffer back-buffer]
          (when-some [val (<! in)]
            (cond
              (impl/full? back-buffer)
              (recur back-buffer front-buffer)
              (impl/full? front-buffer)
              (recur front-buffer back-buffer)
              :else (recur front-buffer back-buffer))))
        (close! front-buffer)
        (close! back-buffer))
    in))

(defn triple-buffered-chan
  [n]
  (let [front-buffer (buffer n)
        back-buffer-a (buffer n)
        back-buffer-b (buffer n)
        front (chan front-buffer)
        back-a (chan back-buffer-a)
        back-b (chan back-buffer-b)
        in (chan n)]
    (go (loop [front-buffer front-buffer
               back-buffer-a back-buffer-a
               back-buffer-b back-buffer-b]
          (when-some [val (<! in)]
            (cond
              (impl/full? back-buffer-a)
              (recur front-buffer back-buffer-a back-buffer-b)
              (impl/full? back-buffer-b)
              (recur front-buffer back-buffer-a back-buffer-b)
              (impl/full? front-buffer)
              (recur front-buffer back-buffer-a back-buffer-b)
              :else (recur front-buffer back-buffer-a back-buffer-b))))
        (close! front-buffer)
        (close! back-buffer-a)
        (close! back-buffer-b))
    in))
