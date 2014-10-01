(ns frontier.game.client
  (:require [clojure.core.async :as a :refer [go-loop <! >! put! chan take!
                                              thread <!! >!!]]
            [com.stuartsierra.component :as c]
            [clojure.tools.logging :as log]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [frontier.net.client :refer [client-socket]]
            [frontier.game.ui :as ui]
            [frontier.game.theme :as t])
  (:import (io.netty.channel ChannelOption)
           (java.nio.charset Charset)
           (java.awt Color)))

(defrecord GameClient [frame login-client]
  c/Lifecycle
  (start [this]
    (if (nil? frame)
      this
      this))
  (stop [this]
    (if frame
      this
      this)))
