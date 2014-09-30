(ns frontier.core
  (:require [clojure.core.async :as a :refer [go-loop <! >! put! chan take!]]
            [com.stuartsierra.component :as c]
            [clojure.tools.namespace.repl :refer [refresh-all]]
            [clojure.java.io :as io]
            [frontier.net.client :refer [client-socket]]
            [frontier.net.login.client :refer [login-client]]
            [frontier.net.login.server :refer [login-server]]
            [frontier.game.client :refer [map->GameClient]]
            [frontier.util.redis :as redis]
            [taoensso.carmine :as car]
            [clojure.edn :as edn])
  (:import (java.net InetSocketAddress)))

(set! *warn-on-reflection* true)
