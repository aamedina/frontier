(ns frontier.game.client
  (:require [clojure.core.async :as a :refer [go-loop <! >! put! chan take!
                                              thread <!! >!!]]
            [com.stuartsierra.component :as c]
            [clojure.tools.logging :as log]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.java.io :as io]
            [frontier.net.client :refer [client-socket]]
            [clojure.edn :as edn]
            [frontier.game.ui :as ui])
  (:import (com.googlecode.lanterna TerminalFacade)
           (com.googlecode.lanterna.terminal Terminal Terminal$Color)
           (com.googlecode.lanterna.screen Screen ScreenWriter
                                           ScreenCharacterStyle)
           (com.googlecode.lanterna.gui GUIScreen GUIScreen$Position)
           (io.netty.channel ChannelOption)
           (java.nio.charset Charset)))

(defrecord GameClient [^Terminal term
                       ^GUIScreen gui
                       ^Screen screen
                       login-socket]
  c/Lifecycle
  (start [this]
    (if (nil? term)
      (let [term (TerminalFacade/createTerminal)
            gui (TerminalFacade/createGUIScreen term)
            screen (.getScreen gui)
            game-client (assoc this
                          :term term
                          :gui gui
                          :screen screen)]
        (.startScreen screen)
        (thread
          (let [{:keys [window auth]} (ui/login-window)]
            (ui/show-window! game-client window :center)
            (println (<!! auth))
            (c/stop this)))
        game-client)
      (do (.startScreen screen) this)))
  (stop [this]
    (when screen
      (.stopScreen screen))
    this))
