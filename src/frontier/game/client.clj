(ns frontier.game.client
  (:require [clojure.core.async :as a :refer [go-loop <! >! put! chan take!
                                              thread <!! >!!]]
            [com.stuartsierra.component :as c]
            [clojure.tools.logging :as log]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.java.io :as io]
            [frontier.net.client :refer [client-socket]]
            [clojure.edn :as edn]
            [frontier.game.ui :as ui]
            [frontier.game.theme :as t :refer [default-appearance]])
  (:import (com.googlecode.lanterna TerminalFacade)
           (com.googlecode.lanterna.terminal Terminal Terminal$Color
                                             XTerm8bitIndexedColorUtils)
           (com.googlecode.lanterna.terminal.swing SwingTerminal
                                                   TerminalAppearance)
           (com.googlecode.lanterna.screen Screen ScreenWriter
                                           ScreenCharacterStyle)
           (com.googlecode.lanterna.gui GUIScreen GUIScreen$Position)
           (io.netty.channel ChannelOption)
           (java.nio.charset Charset)
           (java.awt Color)))

(defrecord GameClient [^SwingTerminal term
                       ^GUIScreen gui
                       ^Screen screen
                       login-client]
  c/Lifecycle
  (start [this]
    (if (nil? term)
      (let [term (doto (TerminalFacade/createSwingTerminal default-appearance
                                                           120 40)
                   (t/set-foreground! (Color/decode "#DCDCCC"))
                   (t/set-background! (Color/decode "#3F3F3F")))
            gui (doto (TerminalFacade/createGUIScreen term)
                  (.setTheme (t/zenburn-theme)))
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
    (if screen
      (do (.stopScreen screen) this)
      (assoc this :term nil :gui nil :screen nil))))
