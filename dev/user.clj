(ns user
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:require
   [clojure.java.io :as io]
   [clojure.java.javadoc :refer [javadoc]]
   [clojure.pprint :refer [pprint]]
   [clojure.reflect :refer [reflect]]
   [clojure.repl :refer [apropos dir doc find-doc pst source]]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.edn :as edn]
   [clojure.test :as test]
   [clojure.tools.namespace.repl :refer [refresh refresh-all]]
   [com.stuartsierra.component :as c]
   [frontier.db :refer [map->Database]]
   [frontier.net.server :refer :all]
   [frontier.game.client :refer [map->GameClient]]
   [frontier.net.login.client :refer [login-client]]
   [frontier.net.login.server :refer [login-server]])
  (:import (java.net InetSocketAddress)))

(def system nil)

(defonce +config+
  (let [{:keys [^String remote-host ^int remote-port
                ^String login-host ^int login-port] :as config}
        (edn/read-string (slurp (io/resource "config.edn")))]
    (assoc config
      :remote-address (InetSocketAddress. remote-host remote-port)
      :login-address (InetSocketAddress. login-host login-port))))

(defn init
  "Creates and initializes the system under development in the Var
  #'system."
  []
  (alter-var-root
   #'system
   (fn [_]
     (c/system-map
      ;; :db (map->Database {:uri (:db-uri +config+)})
      ;; :login-server (c/using (login-server (:login-address +config+))
      ;;                        [:db])
      ;; :login-client (c/using (login-client (:login-address +config+))
      ;;                        [:login-server :db])
      :game-client (c/using (map->GameClient {}) []
                            ;; [:login-client :db]
                            )))))

(defn start
  "Starts the system running, updates the Var #'system."
  []
  (alter-var-root #'system c/start-system))

(defn stop
  "Stops the system if it is currently running, updates the Var
  #'system."
  []
  (alter-var-root #'system c/stop-system))

(defn go
  "Initializes and starts the system running."
  []
  (init)
  (start)
  :ready)

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (refresh :after 'user/go))
