(ns frontier.db
  (:require [datomic.api :as db]
            [com.stuartsierra.component :as c]))

(defonce +database+ nil)

(defrecord Database [conn uri]
  c/Lifecycle
  (start [this]
    (if (nil? conn)
      (let [conn (do (db/create-database uri)
                     (db/connect uri))
            db (assoc this :conn conn)]
        (alter-var-root #'+database+ (constantly db))
        db)
      this))
  (stop [this] this))


