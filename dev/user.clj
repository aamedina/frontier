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
   [clojure.test :as test]
   [clojure.tools.namespace.repl :refer [refresh refresh-all]]
   [com.stuartsierra.component :as c]
   [frontier.db :refer [map->Database]]
   [frontier.net.server :refer :all]))

(def system
  "A Var containing an object representing the application under
  development."
  nil)

(def uri "datomic:cass://127.0.0.1:9042/datomic.datomic/frontier")

(defn init
  "Creates and initializes the system under development in the Var
  #'system."
  []
  (alter-var-root #'system (fnil identity (c/system-map
                                           :server (game-server)
                                           :db (map->Database {:uri uri})))))

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
