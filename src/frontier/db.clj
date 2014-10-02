(ns frontier.db
  (:require [datomic.api :as d]
            [com.stuartsierra.component :as c]
            [clojure.java.io :as io])
  (:import datomic.Util))

(defrecord Database [conn db uri]
  c/Lifecycle
  (start [this]
    (if (nil? conn)
      (let [conn (do (d/create-database uri)
                     (d/connect uri))]
        (doseq [tx (Util/readAll (io/reader (io/resource "schema.edn")))]
          (d/transact conn tx))        
        (assoc this :conn conn :db (d/db conn)))
      this))
  (stop [this] this))


