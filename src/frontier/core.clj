(ns frontier.core
  (:gen-class)
  (:require [clojure.core.async :as a :refer [go-loop <! >! put! chan take!]]
            [com.stuartsierra.component :as c]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.java.io :as io]
            [frontier.net.client :refer [client-socket]]
            [clojure.edn :as edn])
  (:import (com.googlecode.lanterna TerminalFacade)
           (com.googlecode.lanterna.terminal Terminal)
           (com.googlecode.lanterna.gui Action GUIScreen)
           (com.googlecode.lanterna.gui.dialog TextInputDialog FileDialog
                                               ActionListDialog ListSelectDialog
                                               WaitingDialog MessageBox
                                               DialogResult DialogButtons)
           (com.googlecode.lanterna.screen Screen)
           (io.netty.channel ChannelOption)
           (java.nio.charset Charset)
           (java.net InetSocketAddress)))

(set! *warn-on-reflection* true)

(defonce +game-client+ nil)

(defn text-input
  [owner title description text]
  (TextInputDialog/showTextInputBox owner title description text))

(defn file-dialog
  [owner dir title]
  (FileDialog/showOpenFileDialog owner (io/as-file dir) title))

(defn ^Action action
  [f]
  (reify Action
    (doAction [_] (f))))

(defn action-list-dialog
  [owner title description width close-before-action? & fs]
  (ActionListDialog/showActionListDialog
   owner title description width close-before-action?
   (into-array Action (map action fs))))

(defn message-box
  ([owner title message]
     (MessageBox/showMessageBox owner title message))
  ([owner title message dialog-buttons]
     (MessageBox/showMessageBox owner title message dialog-buttons)))

(defn waiting-dialog
  [title description]
  (WaitingDialog. title description))

(defn list-select-dialog
  [owner title description list-width & items]
  (ListSelectDialog/showDialog owner title description list-width
                               (into-array items)))

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
        (alter-var-root #'+game-client+ (constantly game-client))
        game-client)
      (do (.startScreen screen) this)))
  (stop [this]
    (when screen
      (.stopScreen screen))
    this))

(defn client-system
  [address]
  (c/system-map
   :socket (client-socket address)
   :client (c/using (map->GameClient {}) [:socket])))

(defn init
  "Creates and initializes the system under development in the Var
  #'system."
  []
  (let [config (edn/read-string (slurp (io/resource "config.edn")))
        address (InetSocketAddress. ^String (:remote-host config)
                                    ^int (:remote-port config))]
    (alter-var-root #'+game-client+ (fnil identity (client-system address)))))

(defn start
  []
  (alter-var-root #'+game-client+ c/start-system))

(defn stop
  []
  (alter-var-root #'+game-client+ c/stop-system))

(defn go
  []
  (init)
  (start)
  :ready)

(defn reset
  []
  (stop)
  (refresh :after 'frontier.core/go))

(defn -main
  [& args]
  (go))
