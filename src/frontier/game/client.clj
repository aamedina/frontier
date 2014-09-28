(ns frontier.game.client
  (:require [clojure.core.async :as a :refer [go-loop <! >! put! chan take!]]
            [com.stuartsierra.component :as c]
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

(defonce +game-client+ nil)

(defrecord GameClient [^Terminal term
                       ^GUIScreen gui
                       ^Screen screen
                       socket]
  c/Lifecycle
  (start [this]
    (if (nil? screen)
      (let [term (TerminalFacade/createUnixTerminal)
            gui (TerminalFacade/createGUIScreen term)
            screen (.getScreen gui)
            game-client (assoc this
                          :term term
                          :gui gui
                          :screen screen)]
        (.startScreen screen)
        (alter-var-root #'+game-client+ (constantly game-client))
        (ui/show-window! gui (ui/login-window) :center)
        game-client)
      (do (.startScreen screen) this)))
  (stop [this]
    (when screen
      (.stopScreen screen))
    this))

(defn init
  []
  (alter-var-root #'+game-client+ (fn [_] (c/start (map->GameClient {})))))

(defn reset
  []
  (c/stop +game-client+)
  (refresh :after 'frontier.game.client/init))
