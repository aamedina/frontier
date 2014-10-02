(ns frontier.game.client
  (:require [clojure.core.async :as a :refer [go-loop <! >! put! chan take!
                                              thread <!! >!!]]
            [com.stuartsierra.component :as c]
            [clojure.tools.logging :as log]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [frontier.net.client :refer [client-socket]]
            [frontier.ui :refer :all]
            [frontier.ui.login :refer [login-panel]]
            [seesaw.core :as ui])
  (:import (io.netty.channel ChannelOption)
           (java.nio.charset Charset)
           (java.awt Color)))

(defn content
  [client]
  (mig-panel
   :constraints ["" "" ""]
   :items [[(login-panel client) "center, wrap, push"]
           [(label :text "Starship Executive Command Terminal")
            "bottom, right"]]
   :border (title-border "Frontier")))

(defrecord GameClient [frame login-client]
  c/Lifecycle
  (start [this]
    (if (nil? frame)
      (let [frame (ui/frame :title "Frontier"
                            :size [(* 120 7) (* 30 15)]
                            :resizable? false)
            game-client (assoc this :frame frame)]
        (ui/config! :content (content game-client))
        (doto frame
          (.setLocationRelativeTo nil)
          (ui/show!))
        game-client)
      this))
  (stop [this]
    (if frame
      (do (ui/dispose! frame) (dissoc this :frame))
      this)))
