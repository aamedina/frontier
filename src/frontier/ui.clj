(ns frontier.ui
  (:use seesaw.dev)
  (:require [clojure.core.async :as a :refer [<! >! go go-loop chan]]
            [clojure.java.io :as io]
            [frontier.ui.theme :as t]
            [com.stuartsierra.component :refer [Lifecycle start stop]]
            [clojure.tools.namespace.repl :refer [refresh]]
            [seesaw.core :as ui]
            [seesaw.border :as border]
            [seesaw.cursor :as cursor])
  (:import (java.awt Font FontMetrics Color GraphicsEnvironment Insets)
           (java.awt.font TextAttribute)           
           (javax.swing UIManager)))

(defn create-font
  [font-file]
  (let [font (Font/createFont Font/TRUETYPE_FONT
                              (io/file (io/resource font-file)))]
    (.registerFont (GraphicsEnvironment/getLocalGraphicsEnvironment) font)
    (.deriveFont font 14.0)))

(def fonts
  {:regular (create-font "fonts/UbuntuMono-R.ttf")
   :regular-italic (create-font "fonts/UbuntuMono-RI.ttf")
   :bold (create-font "fonts/UbuntuMono-B.ttf")
   :bold-italic (create-font "fonts/UbuntuMono-BI.ttf")})

(defn label
  [text & opts]
  (apply ui/label
         :text text
         :font (:regular fonts)
         :foreground (t/zenburn-colors "zenburn-fg")
         opts))

(defn button
  ([text] (button text :left))
  ([text align] (button text align nil))
  ([text align action]
     (ui/button :text (str " " text " ")
                :halign align
                :cursor (cursor/cursor :hand)
                :border (border/line-border
                         :color "#949494"
                         :top 0 :left 0 :right 2 :bottom 2)
                :font (:regular fonts)
                :listen [:mouse-entered #(ui/config! % :background "#E5E5E5")
                         :mouse-exited #(ui/config! % :background "#D3D3D3")
                         :mouse-pressed #(ui/config! % :background "#898989")
                         :mouse-released #(ui/config! % :background "#D3D3D3")
                         :mouse-clicked (fn [this] (when action (action)))]
                :background "#D3D3D3"
                :foreground (t/zenburn-colors "zenburn-bg"))))

(defn vertical-panel
  [& items]
  (ui/vertical-panel :items (into [] items)
                     :foreground (t/zenburn-colors "zenburn-fg")
                     :background (t/zenburn-colors "zenburn-bg")))

(defn horizontal-panel
  [& items]
  (ui/horizontal-panel :items (into [] items)
                       :foreground (t/zenburn-colors "zenburn-fg")
                       :background (t/zenburn-colors "zenburn-bg")))

(defn text
  [& {:keys [text columns rows] :as opts}]
  (ui/text :text text
           :caret-color (t/zenburn-colors "zenburn-fg")
           :caret-position 0
           :columns (or columns 20)
           :cursor :text
           :border 0
           :font (:regular fonts)
           :margin 0
           :selected-text-color (t/zenburn-colors "zenburn-fg")
           :selection-color "#202020"
           :foreground (t/zenburn-colors "zenburn-fg")
           :background "#696969"))

(defn password
  [& {:keys [text columns rows] :as opts}]
  (ui/password :text text
               :caret-color (t/zenburn-colors "zenburn-fg")
               :caret-position 0
               :columns (or columns 20)
               :cursor :text
               :border 0
               :font (:regular fonts)
               :margin 0
               :echo-char \*
               :selected-text-color (t/zenburn-colors "zenburn-fg")
               :selection-color "#202020"
               :foreground (t/zenburn-colors "zenburn-fg")
               :background "#696969"))

(def content
  (vertical-panel
   (label "Log In" :halign :left)
   (horizontal-panel (label "Username") (text))
   (horizontal-panel (label "Password") (password))
   (horizontal-panel (button "Back" :left) (button "Next" :right))))

(def main-frame
  (ui/pack! (ui/frame :title "Frontier"
                      :content content)))
