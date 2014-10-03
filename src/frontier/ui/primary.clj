(ns frontier.ui.primary
  (:require [frontier.ui :refer :all]
            [clojure.tools.logging :as log]
            [seesaw.core :as ui :refer [config config!]]
            [seesaw.dev :refer :all]
            [clojure.core.async :as a :refer [go go-loop <! >! put! chan take!
                                              thread <!! >!! alts!]]
            [frontier.net.async :refer [future-chan]]))

(defn toolbar
  [client]
  (mig-panel
   :constraints ["ins 0" "" ""]
   :items [[(label :text "敏" :font (:osaka fonts)) ""]
           [(label :text "F1" :font (derive-font (:bold fonts) 12.0)) ""]
           [(label :text "F2" :font (derive-font (:bold fonts) 12.0)) ""]
           [(label :text "F3" :font (derive-font (:bold fonts) 12.0)) ""]
           [(label :text "F4" :font (derive-font (:bold fonts) 12.0)) ""]

           [(label :text "F5" :font (derive-font (:bold fonts) 12.0)) ""]
           [(label :text "F6" :font (derive-font (:bold fonts) 12.0)) ""]
           [(label :text "F7" :font (derive-font (:bold fonts) 12.0)) ""]
           [(label :text "F8" :font (derive-font (:bold fonts) 12.0)) ""]

           [(label :text "F9" :font (derive-font (:bold fonts) 12.0)) ""]
           [(label :text "F10" :font (derive-font (:bold fonts) 12.0)) ""]
           [(label :text "F11" :font (derive-font (:bold fonts) 12.0)) ""]
           [(label :text "F12" :font (derive-font (:bold fonts) 12.0)) ""]]))

(defn vital-bar
  [client color]
  (text :editable? false
        :cursor :default
        :font (derive-font (:regular fonts) 7.0)
        :columns 40
        :background color))

(defn basic-systems-health
  [client]
  (mig-panel
   :constraints ["ins 0" "" ""]
   :items [[(vital-bar client "#CC9393") "wrap"]
           [(vital-bar client "#BFEBBF") "wrap"]
           [(vital-bar client "#7CB8BB") ""]]))

(defn primary-panel
  [client]
  (mig-panel
   :constraints ["ins 0" "" ""]
   :items [[(basic-systems-health client) "top, left"]
           [(toolbar client) "top"]]
   :border (title-border "Veckon Strife")))
