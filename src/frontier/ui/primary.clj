(ns frontier.ui.primary
  (:require [frontier.ui :refer :all]
            [frontier.ui.kanji :as kanji :refer [icon]]
            [clojure.tools.logging :as log]
            [seesaw.core :as ui :refer [config config!]]
            [seesaw.dev :refer :all]
            [clojure.core.async :as a :refer [go go-loop <! >! put! chan take!
                                              thread <!! >!! alts!]]
            [frontier.net.async :refer [future-chan]]))

(defn toolbar
  [client]
  (mig-panel
   :constraints ["ins 0, gapy 0" "" ""]
   :items [[(icon (:attack kanji/commands)) ""]
           [(icon (:attack kanji/commands)) ""]
           [(icon (:attack kanji/commands)) ""]
           [(icon (:attack kanji/commands)) ""]

           [(icon (:attack kanji/commands)) ""]
           [(icon (:attack kanji/commands)) ""]
           [(icon (:attack kanji/commands)) ""]
           [(icon (:attack kanji/commands)) ""]

           [(icon (:attack kanji/commands)) ""]
           [(icon (:attack kanji/commands)) ""]
           [(icon (:attack kanji/commands)) ""]
           [(icon (:attack kanji/commands)) "wrap"]
           
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

(defn primary-icons
  [client]
  (mig-panel
   :constraints ["ins 0, gapy 0" "" ""]
   :items [[(icon (:attack kanji/commands)) ""]
           [(icon (:attack kanji/commands)) ""]
           [(icon (:attack kanji/commands)) ""]
           [(icon (:attack kanji/commands)) ""]

           [(icon (:attack kanji/commands)) ""]
           [(icon (:attack kanji/commands)) ""]
           [(icon (:attack kanji/commands)) ""]
           [(icon (:attack kanji/commands)) ""]

           [(icon (:attack kanji/commands)) ""]
           [(icon (:attack kanji/commands)) ""]
           [(icon (:attack kanji/commands)) ""]
           [(icon (:attack kanji/commands)) "wrap"]]))


(defn radar
  [client]
  (mig-panel
   :constraints ["ins 0" "" ""]
   :items [["" "grow"]]
   :border (title-border "Radar")))

(defn chat-window
  [client]
  (mig-panel
   :constraints ["fill, ins 0" "" ""]
   :items [[(scrollable (text :multi-line? true
                              :editable? false
                              :rows 5
                              :text "hello\nthere\nmulti\nline\ntext\n!"))
            "grow, top, center, wrap"]
           [(text) "grow, bottom, center"]]
   :border (title-border "Chat")))

(defn primary-panel
  [client]
  (mig-panel
   :constraints ["ins 0" "" ""]
   :items [[(basic-systems-health client) "top, left"]
           [(toolbar client) "top, wrap, push"]
           [(radar client) "bottom, left"]
           [(chat-window client) "grow, bottom, center"]
           [(primary-icons client) "bottom, right"]]
   :border (title-border "Veckon Strife")))
