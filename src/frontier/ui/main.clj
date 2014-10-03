(ns frontier.ui.main
  (:require [frontier.ui :refer :all]
            [clojure.tools.logging :as log]
            [seesaw.core :as ui :refer [config config!]]
            [clojure.core.async :as a :refer [go go-loop <! >! put! chan take!
                                              thread <!! >!! alts!]]
            [frontier.net.async :refer [future-chan]]))

(defn basic-systems-health
  [client]
  (mig-panel
   :constraints ["fill" "" ""]
   :items []
   :border (title-border "Systems Health")))

(defn primary-terminal
  [client]
  (mig-panel
   :constraints ["" "" ""]
   :items [[(basic-systems-health client) ""]]
   :border (title-border "Veckon Strife")))
