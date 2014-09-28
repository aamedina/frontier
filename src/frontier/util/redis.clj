(ns frontier.util.redis
  (:refer-clojure :exclude [remove get update])
  (:use clojure.repl)
  (:require [taoensso.carmine :as car :refer [wcar]]))

(def conn {:pool {} :spec {}})

(defmacro with-redis
  [& body]
  `(wcar conn ~@body))

(defn add
  [k v]
  (if (= (with-redis (car/set k v)) "OK")
    [k v]
    (throw (ex-info "Could not set key-value pair" {:k k :v v}))))

(defn remove
  [k]
  (when-not (zero? (with-redis (car/del k)))
    (throw (ex-info "Could not remove key" {:k k}))))

(defn get
  ([k] (get k nil))
  ([k not-found]
     (if-let [v (with-redis (car/get k))]
       v
       not-found)))

(defn update
  [k f & args]
  (add k (apply f (get k) args)))

