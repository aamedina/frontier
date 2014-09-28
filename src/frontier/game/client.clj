(ns frontier.game.client
  (:require [clojure.core.async :as a :refer [go-loop <! >! put! chan take!]]
            [com.stuartsierra.component :as c]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.java.io :as io]
            [frontier.net.client :refer [client-socket]]
            [clojure.edn :as edn])
  (:import (com.googlecode.lanterna TerminalFacade)
           (com.googlecode.lanterna.terminal Terminal)
           (com.googlecode.lanterna.gui GUIScreen)
           (com.googlecode.lanterna.screen Screen)
           (io.netty.channel ChannelOption)
           (java.nio.charset Charset)))

(defonce +game-client+ nil)

(defrecord GameClient [^Terminal term ^GUIScreen gui ^Screen screen socket]
  c/Lifecycle
  (start [this]
    (if (nil? term)
      (let [term (TerminalFacade/createTerminal (Charset/forName "UTF-8"))
            gui (TerminalFacade/createGUIScreen term)
            screen (.getScreen gui)
            game-client (assoc this
                          :term term
                          :gui gui
                          :screen screen)]
        (.startScreen screen)
        game-client)
      (do (.startScreen screen) this)))
  (stop [this]
    (when screen
      (.stopScreen screen))
    this))

