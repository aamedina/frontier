(ns frontier.core
  (:require [clojure.core.async :as a :refer [go-loop <! >! put! chan take!]]
            [com.stuartsierra.component :as c]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.java.io :as io]
            [frontier.net.client :refer [client-socket]]
            [frontier.net.login.client :refer [login-client]]
            [frontier.game.client :refer [map->GameClient]]
            [clojure.edn :as edn])
  (:import (java.net InetSocketAddress)))

(set! *warn-on-reflection* true)

(defonce system nil)

(defonce +config+
  (let [{:keys [^String remote-host ^int remote-port] :as config}
        (edn/read-string (slurp (io/resource "config.edn")))]
    (assoc config
      :remote-address (InetSocketAddress. remote-host remote-port))))

(defn client-system
  []
  (c/system-map
   :login-socket (login-client)
   :game-client (c/using (map->GameClient {}) [:login-socket])))

(defn init [] (alter-var-root #'system (fn [_] (client-system))))
(defn start [] (alter-var-root #'system c/start-system))
(defn stop [] (alter-var-root #'system c/stop-system))
(defn go [] (init) (start) :ready)
(defn reset [] (stop) (refresh :after 'frontier.core/go))
