(ns frontier.ui.primary
  (:require [frontier.ui :refer :all]
            [frontier.ui.kanji :as kanji :refer [icon]]
            [clojure.tools.logging :as log]
            [seesaw.core :as ui :refer [config config!]]
            [seesaw.dev :refer :all]
            [seesaw.graphics :refer :all]
            [clojure.core.async :as a :refer [go go-loop <! >! put! chan take!
                                              thread <!! >!! alts!]]
            [frontier.net.async :refer [future-chan]]))

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

(defn toolbar
  [client]
  (mig-panel
   :constraints ["ins 0, gapy 0" "" ""]
   :items [[(icon (:attack kanji/commands) "Attack" "F1"
                  (fn [this] (println this))) ""]
           [(icon (:attack kanji/commands) "Attack" "F2"
                  (fn [this] (println this))) ""]
           [(icon (:attack kanji/commands) "Attack" "F3"
                  (fn [this] (println this))) ""]
           [(icon (:attack kanji/commands) "Attack" "F4"
                  (fn [this] (println this))) ""]

           [(icon (:attack kanji/commands) "Attack" "F5"
                  (fn [this] (println this))) ""]
           [(icon (:attack kanji/commands) "Attack" "F6"
                  (fn [this] (println this))) ""]
           [(icon (:attack kanji/commands) "Attack" "F7"
                  (fn [this] (println this))) ""]
           [(icon (:attack kanji/commands) "Attack" "F8"
                  (fn [this] (println this))) ""]

           [(icon (:mail kanji/kanji) "Mail" "F9"
                  (fn [this] (println this))) ""]
           [(icon (:attack kanji/commands) "Help" "F10"
                  (fn [this] (println this))) ""]
           [(icon (:attack kanji/commands) "Settings" "F11"
                  (fn [this] (println this))) ""]
           [(icon (:attack kanji/commands) "Game Menu" "F12"
                  (fn [this] (println this))) "wrap"]
           
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

(defn primary-icons
  [client]
  (mig-panel
   :constraints ["ins 0, gapy 0" "grow" ""]
   :items [[(icon (:navigation kanji/data-screens) "Navigation" "ctrl N"
                  (fn [this] (println this))) ""]
           [(icon (:inventory kanji/data-screens) "Inventory" "ctrl I"
                  (fn [this] (println this))) ""]
           [(icon (:databank kanji/data-screens) "Databank" "ctrl D"
                  (fn [this] (println this))) ""]
           [(icon (:ship kanji/data-screens) "Ship" "ctrl V"
                  (fn [this] (println this))) ""]
           [(icon (:character kanji/data-screens) "Character" "ctrl C"
                  (fn [this] (println this))) ""]
           [(icon (:skills kanji/data-screens) "Skills" "ctrl S"
                  (fn [this] (println this))) ""]
           [(icon (:commands kanji/data-screens) "Commands" "ctrl A"
                  (fn [this] (println this))) ""]
           [(icon (:mail kanji/data-screens) "Mail" "ctrl M"
                  (fn [this] (println this))) ""]
           [(icon (:options kanji/data-screens) "Options" "ctrl O"
                  (fn [this] (println this))) ""]
           [(icon (:menu kanji/data-screens) "Game Menu" "ESCAPE"
                  (fn [this] (println this))) "wrap"]
           
           [(label :text "C-n" :font (derive-font (:bold fonts) 12.0)) ""]
           [(label :text "C-i" :font (derive-font (:bold fonts) 12.0)) ""]
           [(label :text "C-d" :font (derive-font (:bold fonts) 12.0)) ""]
           [(label :text "C-v" :font (derive-font (:bold fonts) 12.0)) ""]
           [(label :text "C-c" :font (derive-font (:bold fonts) 12.0)) ""]
           [(label :text "C-s" :font (derive-font (:bold fonts) 12.0)) ""]
           [(label :text "C-a" :font (derive-font (:bold fonts) 12.0)) ""]
           [(label :text "C-m" :font (derive-font (:bold fonts) 12.0)) ""]
           [(label :text "C-o" :font (derive-font (:bold fonts) 12.0)) ""]
           [(label :text "ESC" :font (derive-font (:bold fonts) 12.0)) ""]]))


(defn radar
  [client]
  (mig-panel
   :constraints ["fill, ins 0" "" ""]
   :items []
   :border (title-border "Proximity Sensor")))

(defn chat-window
  [client]
  (mig-panel
   :constraints ["fill, ins 0" "" ""]
   :items [[(scrollable (text :multi-line? true
                              :editable? false
                              :rows 5))
            "grow, wrap"]
           [(text) "grow"]]
   :border (title-border "Chat")))

(defn focused-frame
  [client]
  (mig-panel
   :constraints ["fill" "" ""]
   :items []
   :border (basic-border)))

(defn mail-screen
  [client]
  (mig-panel
   :constraints ["nogrid, fill, ins 0, gap 0" "" ""]
   :items [[(scrollable
             (ui/table
              :fills-viewport-height? true
              :model
              [:columns [:status :sender :subject :date]
               :rows [{:status "R"
                       :sender "Veckon Strife"
                       :subject "Hello!"
                       :date (java.util.Date. (System/currentTimeMillis))}
                      {:status "R"
                       :sender "Veckon Strife"
                       :subject "There!"
                       :date (java.util.Date. (System/currentTimeMillis))}]]
              :font (:bold fonts)
              :foreground "#DCDCCC"
              :background "#696969"
              :drag-enabled? false))
            "width 100%, height 50%, wrap, gapbottom 5"]
           [(scrollable (text :multi-line? true
                              :editable? true
                              :rows 5))
            "grow, wrap, gapbottom 5"]
           [(button :text "Reply") "width 25%"]
           [(button :text "Delete") "width 25%"]
           [(button :text "Forward") "width 25%"]
           [(button :text "New") "width 25%"]]
   :border (title-border "Mail")))

(defn primary-panel
  [client]
  (mig-panel
   :constraints ["ins 0" "[fill, grow][fill]" "[][fill, grow][]"]
   :items [[(basic-systems-health client) "top, left"]
           [(toolbar client) "top, left, wrap"]
           [(focused-frame client) "wrap"]
           ;; [(focused-frame client) "wrap"]
           [(chat-window client) "bottom, left"]
           [(primary-icons client) "bottom, right"]]
   :border (title-border "Veckon Strife")))
