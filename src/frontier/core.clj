(ns frontier.core
  (:require [clojure.core.async :as a :refer [go-loop <! >! put! chan take!]]
            [com.stuartsierra.component :as c]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.java.io :as io]
            [frontier.net.client :refer [client-socket]]
            [frontier.game.client :refer [map->GameClient +game-client+]]
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

(defonce +config+ (edn/read-string (slurp (io/resource "config.edn"))))

(defn client-system
  [address]
  (c/system-map
   :socket (client-socket address)
   :client (c/using (map->GameClient {}) [:socket])))

(defn init
  "Creates and initializes the system under development in the Var
  #'system."
  []
  (let [address (InetSocketAddress. ^String (:remote-host +config+)
                                    ^int (:remote-port +config+))]
    (alter-var-root #'+game-client+ (fnil identity (client-system address)))))

(defn start [] (alter-var-root #'+game-client+ c/start-system))
(defn stop [] (alter-var-root #'+game-client+ c/stop-system))
(defn go [] (init) (start) :ready)
(defn reset [] (stop) (refresh :after 'frontier.core/go))
