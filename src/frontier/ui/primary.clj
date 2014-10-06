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
  [client & opts]
  (apply mig-panel
         :constraints ["ins 0" "" ""]
         :items [[(vital-bar client "#CC9393") "wrap"]
                 [(vital-bar client "#BFEBBF") "wrap"]
                 [(vital-bar client "#7CB8BB") ""]]
         opts))

(defn toolbar
  [client]
  (mig-panel
   :constraints ["ins 0, gapy 0" "" ""]
   :items [[(icon (:attack kanji/commands) "" "F1"
                  (fn [this] (println this))) ""]
           [(icon (:attack kanji/commands) "" "F2"
                  (fn [this] (println this))) ""]
           [(icon (:attack kanji/commands) "" "F3"
                  (fn [this] (println this))) ""]
           [(icon (:attack kanji/commands) "" "F4"
                  (fn [this] (println this))) "gapafter 15"]

           [(icon (:attack kanji/commands) "" "F5"
                  (fn [this] (println this))) ""]
           [(icon (:attack kanji/commands) "" "F6"
                  (fn [this] (println this))) ""]
           [(icon (:attack kanji/commands) "" "F7"
                  (fn [this] (println this))) ""]
           [(icon (:attack kanji/commands) "" "F8"
                  (fn [this] (println this))) "gapafter 15"]

           [(icon (:mail kanji/kanji) "" "F9"
                  (fn [this] (println this))) ""]
           [(icon (:attack kanji/commands) "" "F10"
                  (fn [this] (println this))) ""]
           [(icon (:attack kanji/commands) "" "F11"
                  (fn [this] (println this))) ""]
           [(icon (:attack kanji/commands) "" "F12"
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
                  (fn [e]
                    (doto (ui/select (:frame client) [:#focused])
                      (config! :border (title-border "Navigation"))))) ""]
           [(icon (:inventory kanji/data-screens) "Inventory" "ctrl I"
                  (fn [e]
                    (doto (ui/select (:frame client) [:#focused])
                      (config! :border (title-border "Inventory"))))) ""]
           [(icon (:databank kanji/data-screens) "Databank" "ctrl D"
                  (fn [e]
                    (doto (ui/select (:frame client) [:#focused])
                      (config! :border (title-border "Databank"))))) ""]
           [(icon (:ship kanji/data-screens) "Ship" "ctrl V"
                  (fn [e]
                    (doto (ui/select (:frame client) [:#focused])
                      (config! :border (title-border "Ship"))))) ""]
           [(icon (:character kanji/data-screens) "Character" "ctrl C"
                  (fn [e]
                    (doto (ui/select (:frame client) [:#focused])
                      (config! :border (title-border "Character"))))) ""]
           [(icon (:skills kanji/data-screens) "Skills" "ctrl S"
                  (fn [e]
                    (doto (ui/select (:frame client) [:#focused])
                      (config! :border (title-border "Skills"))))) ""]
           [(icon (:commands kanji/data-screens) "Commands" "ctrl A"
                  (fn [e]
                    (doto (ui/select (:frame client) [:#focused])
                      (config! :border (title-border "Commands"))))) ""]
           [(icon (:mail kanji/data-screens) "Mail" "ctrl M"
                  (fn [e]
                    (doto (ui/select (:frame client) [:#focused])
                      (config! :border (title-border "Mail"))))) ""]
           [(icon (:options kanji/data-screens) "Options" "ctrl O"
                  (fn [e]
                    (doto (ui/select (:frame client) [:#focused])
                      (config! :border (title-border "Options"))))) ""]
           [(icon (:menu kanji/data-screens) "Game Menu" "ESCAPE"
                  (fn [e]
                    (doto (ui/select (:frame client) [:#focused])
                      (config! :border (title-border "Game Menu"))))) "wrap"]
           
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


(defn button-group
  [& buttons]
  (let [bg (ui/button-group)
        items (mapv (fn [[btn constraints]]
                      [(-> (config! btn :group bg)
                           (config! :border (tab-border)))
                       constraints]) buttons)]
    (ui/listen bg :selection
               (fn [e]
                 (if-let [s (ui/selection e)]
                   (if (ui/config s :selected?)
                     (ui/config! s :background "#E5E5E5")
                     (ui/config! s :background "#D3D3D3")))))
    (mig-panel
     :constraints ["gap 0, ins 0" "" ""]
     :items items)))

(defn chat-window
  [client]
  (mig-panel
   :constraints ["gap 0, fill, ins 0" "" ""]
   :items [[(button-group [(tab :text " Local "
                                :selected? true) ""]
                          [(tab :text " Sector ") ""]
                          [(tab :text " Faction ") ""]
                          [(tab :text " Group " :enabled? false) ""]
                          [(tab :text " Guild " :enabled? false) ""])
            "wrap"]
           [(scrollable (text :multi-line? true
                              :editable? false
                              :rows 7))
            "grow, wrap"]
           [(text) "gapy 5, grow"]]
   :border (title-border "Chat")))

(defn focused-frame
  [client title]
  (mig-panel
   :id :focused
   :constraints ["fill" "" ""]
   :items []
   :border (title-border title)))

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
            "wrap"]
           [(scrollable (text :multi-line? true
                              :editable? true
                              :rows 7))
            "wrap, gapbottom 5"]
           [(button :text "Reply") "width 25%"]
           [(button :text "Delete") "width 25%"]
           [(button :text "Forward") "width 25%"]
           [(button :text "New") "width 25%"]]
   :border (title-border "Mail")))

(defn top-row
  [client]
  (mig-panel
   :constraints ["fill, ins 0" "" ""]
   :items [[(basic-systems-health client) "gapleft 3, top, left"]
           [(toolbar client) "top, center"]
           [(basic-systems-health client) "gapright 3, top, right"]]))

(defn middle-row
  [client]
  (mig-panel
   :constraints ["fill, ins 0" "" ""]
   :items [[(focused-frame client "Navigation") "height 100%, width 70%"]
           [(focused-frame client "Sensor Log") "height 100%, width 30%"]]))

(defn render-proximity
  [client]
  (text :background "#000"
        :multi-line? true
        :editable? false
        :cursor :crosshair))

(defn radar
  [client]
  (mig-panel
   :constraints ["fill, ins 0" "" ""]
   :items [[(mig-panel
             :id :focused
             :constraints ["ins 2" "" ""]
             :items [[(render-proximity client) "width 100%, height 100%"]]
             :border (title-border "Proximity Sensor"))
            "top, wrap, grow, height 100%"]
           [(primary-icons client) "bottom, width 100%"]]))

(defn bottom-row
  [client]
  (mig-panel
   :constraints ["fill, ins 0" "" ""]
   :items [[(chat-window client) "bottom, grow, width 70%"]
           [(radar client) "height 100%, bottom, width 30%"]]))

(defn primary-panel
  [client]
  (mig-panel
   :constraints ["ins 0" "" ""]
   :items [[(top-row client) "height 10%, width 100%, wrap"]
           [(middle-row client) "height 55%, width 100%, wrap"]
           [(bottom-row client) "height 35%, width 100%, bottom"]]
   :border (title-border "Veckon Strife")))
