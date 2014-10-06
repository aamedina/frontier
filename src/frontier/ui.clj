(ns frontier.ui
  (:use seesaw.dev)
  (:require [clojure.core.async :as a :refer [<! >! go go-loop chan]]
            [clojure.java.io :as io]
            [frontier.ui.theme :as t]
            [com.stuartsierra.component :refer [Lifecycle start stop]]
            [clojure.tools.namespace.repl :refer [refresh]]
            [seesaw.core :as ui :refer [config config!]]
            [seesaw.border :as border]
            [seesaw.cursor :as cursor]
            [seesaw.color :as color]
            [seesaw.mig :as mig])
  (:import (java.awt Font FontMetrics Color GraphicsEnvironment Insets Graphics
                     Dimension Graphics2D)
           (java.awt.font TextAttribute)
           (java.awt.image BufferedImage)
           (javax.swing UIManager JScrollPane JScrollBar JButton)
           (javax.swing.plaf.basic BasicScrollBarUI)))

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
   :bold-italic (create-font "fonts/UbuntuMono-BI.ttf")
   :osaka (.deriveFont (seesaw.font/font "Osaka") (float 14.0))})

(def dimensionless-button
  (proxy [JButton] []
    (getPreferredSize []
      (Dimension. 0 0))))

(defn text-image
  [color font-metrics]
  (let [img (BufferedImage. 32 32 BufferedImage/TYPE_INT_RGB)]
    (doto (.createGraphics img)
      (.setPaint color)
      (.fillRect 0 0 32 32)
      (.dispose))
    img))

(defn scrollbar-ui
  []
  (proxy [BasicScrollBarUI] []
    (paintThumb [graphics component rect]
      (doto ^Graphics2D graphics
        (.drawImage (text-image (color/color "#F0DFAF")
                                (.getFontMetrics graphics))
                    (.-x rect) (.-y rect) (.-width rect) (.-height rect) nil)))
    (paintTrack [graphics component rect]
      (doto ^Graphics2D graphics
        (.drawImage (text-image (color/color "#696969")
                                (.getFontMetrics graphics))
                    (.-x rect) (.-y rect) (.-width rect) (.-height rect) nil)))
    (createDecreaseButton [orientation] dimensionless-button)
    (createIncreaseButton [orientation] dimensionless-button)))

(defn scrollable
  [target & opts]
  (let [scroll-pane (apply ui/scrollable target
                           :border 0
                           :hscroll :never
                           :vscroll :always opts)]
    (.setUI (.getVerticalScrollBar scroll-pane) (scrollbar-ui))
    scroll-pane))

(defn label
  [& opts]
  (apply ui/label
         :font (:bold fonts)
         :foreground "#F0DFAF"
         :background "#3F3F3F" opts))

(defn derive-font
  [font new-size]
  (.deriveFont font (float new-size)))

(def h1 (comp #(ui/config! % :font (derive-font (:bold fonts) 36.0)) label))
(def h2 (comp #(ui/config! % :font (derive-font (:bold fonts) 28.0)) label))
(def h3 (comp #(ui/config! % :font (derive-font (:bold fonts) 24.0)) label))
(def h4 (comp #(ui/config! % :font (derive-font (:bold fonts) 20.0)) label))
(def h5 (comp #(ui/config! % :font (derive-font (:bold fonts) 16.0)) label))

(defn tab-border
  []
  (border/to-border (border/line-border :color "#DCDCDC"
                                        :top 2 :left 2 :right 0 :bottom 0)
                    (border/line-border :color "#949494"
                                        :top 0 :left 0 :right 2 :bottom 0)))

(defn button-border
  []
  (border/to-border (border/line-border :color "#DCDCDC"
                                        :top 2 :left 2 :right 0 :bottom 0)
                    (border/line-border :color "#949494"
                                        :top 0 :left 0 :right 2 :bottom 2)))

(defn focused-border
  []
  (border/to-border (border/line-border :color "#949494"
                                        :top 2 :left 2 :right 0 :bottom 0)
                    (border/line-border :color "#DCDCDC"
                                        :top 0 :left 0 :right 2 :bottom 2)))

(defn tab-focused-border
  []
  (border/to-border (border/line-border :color "#949494"
                                        :top 2 :left 2 :right 0 :bottom 0)
                    (border/line-border :color "#DCDCDC"
                                        :top 0 :left 0 :right 2 :bottom 0)))

(defn button
  [& opts]
  (apply ui/button
         :cursor (cursor/cursor :hand)
         :border (button-border)
         :font (:bold fonts)
         :listen [:mouse-entered #(when (config % :enabled?)
                                    (ui/config! % :background "#E5E5E5"))
                  :mouse-exited #(when (config % :enabled?)
                                   (ui/config! % :background "#D3D3D3"))
                  :mouse-pressed #(when (config % :enabled?)
                                    (ui/config! % :border (focused-border)))
                  :mouse-released #(when (config % :enabled?)
                                     (ui/config! % :border (button-border)))
                  :focus-gained #(when (config % :enabled?)
                                   (ui/config! % :background "#E5E5E5"))
                  :focus-lost #(when (config % :enabled?)
                                 (ui/config! % :background "#D3D3D3"))]
         :background "#D3D3D3"
         :foreground (t/zenburn-colors "zenburn-bg")
         opts))

(defn tab
  [& opts]
  (apply ui/button
         :cursor (cursor/cursor :hand)
         :border (button-border)
         :font (:bold fonts)
         :listen [:mouse-entered #(when (config % :enabled?)
                                    (ui/config! % :background "#E5E5E5"))
                  :mouse-exited #(when (config % :enabled?)
                                   (ui/config! % :background "#D3D3D3"))
                  :mouse-pressed #(when (config % :enabled?)
                                    (ui/config! % :border (tab-focused-border)))
                  :mouse-released #(when (config % :enabled?)
                                     (ui/config! % :border (tab-border)))
                  :focus-gained #(when (config % :enabled?)
                                   (ui/config! % :background "#E5E5E5"))
                  :focus-lost #(when (config % :enabled?)
                                 (ui/config! % :background "#D3D3D3"))]
         :background "#D3D3D3"
         :foreground (t/zenburn-colors "zenburn-bg")
         opts))

(defn text
  [& opts]
  (apply ui/text
         :caret-color (t/zenburn-colors "zenburn-fg")
         :caret-position 0
         :columns 20
         :cursor :text
         :border 0
         :font (:regular fonts)
         :margin 0
         :selected-text-color (t/zenburn-colors "zenburn-fg")
         :selection-color "#202020"
         :foreground (t/zenburn-colors "zenburn-fg")
         :background "#696969" opts))

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

(defn vertical-panel
  [& opts]
  (apply ui/vertical-panel
         :font (:bold fonts)
         :foreground (t/zenburn-colors "zenburn-fg")
         :background (t/zenburn-colors "zenburn-bg") opts))

(defn horizontal-panel
  [& opts]
  (apply ui/horizontal-panel
         :font (:bold fonts)
         :foreground (t/zenburn-colors "zenburn-fg")
         :background (t/zenburn-colors "zenburn-bg") opts))

(defn title-border
  [text]
  (doto (border/to-border text)
    (.setTitleFont (:bold fonts))
    (.setTitleColor (seesaw.color/to-color "#F0DFAF"))
    (.setBorder (border/line-border :color "#686868" :thickness 1))))

(defn basic-border
  []
  (border/line-border :color "#686868" :thickness 1))

(defn flow-panel
  [& opts]
  (apply ui/flow-panel
         :font (:regular fonts)
         :foreground (t/zenburn-colors "zenburn-fg")
         :background (t/zenburn-colors "zenburn-bg") opts))

(defn mig-panel
  [& opts]
  (apply mig/mig-panel
         :font (:regular fonts)
         :foreground (t/zenburn-colors "zenburn-fg")
         :background (t/zenburn-colors "zenburn-bg") opts))

