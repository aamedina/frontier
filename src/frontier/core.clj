(ns frontier.core
  (:require [clojure.core.async :as a :refer [go-loop <! >! put! chan take!]]
            [com.stuartsierra.component :as c]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.java.io :as io]
            [frontier.net.client :refer [client-socket]]
            [frontier.game.client :refer [map->GameClient +game-client+]]
            [clojure.edn :as edn])
  (:import (java.net InetSocketAddress)))

(set! *warn-on-reflection* true)

(defonce +config+
  (let [{:keys [^String remote-host ^int remote-port] :as config}
        (edn/read-string (slurp (io/resource "config.edn")))]
    (assoc config
      :remote-address (InetSocketAddress. remote-host remote-port))))

(defn client-system
  [address]
  (c/system-map
   :socket (client-socket address)
   :client (c/using (map->GameClient {}) [:socket])))

(defn init
  []
  (->> (fnil identity (client-system (:remote-address +config+)))
       (alter-var-root #'+game-client+)))

(defn start [] (alter-var-root #'+game-client+ c/start-system))
(defn stop [] (alter-var-root #'+game-client+ c/stop-system))
(defn go [] (init) (start) :ready)
(defn reset [] (stop) (refresh :after 'frontier.core/go))
