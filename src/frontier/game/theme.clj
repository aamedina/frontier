(ns frontier.game.theme
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import (com.googlecode.lanterna.terminal.swing TerminalAppearance
                                                   TerminalPalette
                                                   SwingTerminal)
           (com.googlecode.lanterna.gui Theme Theme$Category Theme$Definition)
           (com.googlecode.lanterna.terminal XTerm8bitIndexedColorUtils
                                             Terminal Terminal$Color)
           (java.awt Font Color GraphicsEnvironment)))

(defonce graphics-environment
  (GraphicsEnvironment/getLocalGraphicsEnvironment))

(defonce graphics-device
  (.getDefaultScreenDevice graphics-environment))

(def ^{:doc "Zenburn colors sourced from bbatsov's awesome theme for Emacs: 
             https://github.com/bbatsov/zenburn-emacs"}
  zenburn-colors
  {"zenburn-fg+1"      "#FFFFEF"
   "zenburn-fg"        "#DCDCCC"
   "zenburn-fg-1"      "#656555"
   "zenburn-bg-2"      "#000000"
   "zenburn-bg-1"      "#2B2B2B"
   "zenburn-bg-05"     "#383838"
   "zenburn-bg"        "#3F3F3F"
   "zenburn-bg+05"     "#494949"
   "zenburn-bg+1"      "#4F4F4F"
   "zenburn-bg+2"      "#5F5F5F"
   "zenburn-bg+3"      "#6F6F6F"
   "zenburn-red+1"     "#DCA3A3"
   "zenburn-red"       "#CC9393"
   "zenburn-red-1"     "#BC8383"
   "zenburn-red-2"     "#AC7373"
   "zenburn-red-3"     "#9C6363"
   "zenburn-red-4"     "#8C5353"
   "zenburn-orange"    "#DFAF8F"
   "zenburn-yellow"    "#F0DFAF"
   "zenburn-yellow-1"  "#E0CF9F"
   "zenburn-yellow-2"  "#D0BF8F"
   "zenburn-green-1"   "#5F7F5F"
   "zenburn-green"     "#7F9F7F"
   "zenburn-green+1"   "#8FB28F"
   "zenburn-green+2"   "#9FC59F"
   "zenburn-green+3"   "#AFD8AF"
   "zenburn-green+4"   "#BFEBBF"
   "zenburn-cyan"      "#93E0E3"
   "zenburn-blue+1"    "#94BFF3"
   "zenburn-blue"      "#8CD0D3"
   "zenburn-blue-1"    "#7CB8BB"
   "zenburn-blue-2"    "#6CA0A3"
   "zenburn-blue-3"    "#5C888B"
   "zenburn-blue-4"    "#4C7073"
   "zenburn-blue-5"    "#366060"
   "zenburn-magenta"   "#DC8CC3"})

(def zenburn-palette
  (TerminalPalette. (Color/decode "#DCDCCC")
                    (Color/decode "#FFFFEF")
                    (Color/decode "#3F3F3F")
                    (Color/decode "#5F5F5F")
                    (Color/decode "#CC9393")
                    (Color/decode "#DCA3A3")
                    (Color/decode "#7F9F7F")
                    (Color/decode "#8FB28F")
                    (Color/decode "#F0DFAF")
                    (Color/decode "#F0DFAF")
                    (Color/decode "#8CD0D3")
                    (Color/decode "#94BFF3")
                    (Color/decode "#DC8CC3")
                    (Color/decode "#DC8CC3")
                    (Color/decode "#93E0E3")
                    (Color/decode "#93E0E3")
                    (Color/decode "#DCDCCC")
                    (Color/decode "#FFFFEF")))

(defn term-color
  [color-key]
  (case color-key
    :black Terminal$Color/BLACK
    :blue Terminal$Color/BLUE
    :default Terminal$Color/DEFAULT
    :green Terminal$Color/GREEN
    :magenta Terminal$Color/MAGENTA
    :red Terminal$Color/RED
    :white Terminal$Color/WHITE
    :yellow Terminal$Color/YELLOW))

(defn theme
  [foreground background highlighted? underlined?]
  (Theme$Definition. (term-color foreground) (term-color background)
                     highlighted? underlined?))

(defn ^Theme zenburn-theme
  []
  (proxy [Theme] []
    (^Theme$Definition getDefinition [^Theme$Category category]
      (case (.name category)
        "BORDER" (theme :black :white true false)
        "BUTTON_ACTIVE" (theme :white :black true false)
        "BUTTON_INACTIVE" (theme :black :white true false)
        "BUTTON_LABEL_ACTIVE" (theme :yellow :black true false)
        "BUTTON_LABEL_INACTIVE" (theme :black :white true false)
        "CHECKBOX" (theme :black :white false false)
        "CHECKBOX_SELECTED" (theme :white :black true false)
        "DIALOG_AREA" (theme :white :black false false)
        "LIST_ITEM" (theme :black :white false false)
        "LIST_ITEM_SELECTED" (theme :white :blue true false)
        "PROGRESS_BAR_COMPLETED" (theme :green :black false false)
        "PROGRESS_BAR_REMAINING" (theme :red :black false false)
        "RAISED_BORDER" (theme :white :white true false)
        "SCREEN_BACKGROUND" (theme :white :black true false)
        "SHADOW" (theme :black :black true false)
        "TEXTBOX" (theme :white :white false false)
        "TEXTBOX_FOCUSED" (theme :white :white true false)))))

(defn category
  [category]
  (case category
    :border Theme$Category/BORDER
    :button-active Theme$Category/BUTTON_ACTIVE
    :button-inactive Theme$Category/BUTTON_INACTIVE
    :button-label-active Theme$Category/BUTTON_LABEL_ACTIVE
    :button-label-inactive Theme$Category/BUTTON_LABEL_INACTIVE
    :checkbox Theme$Category/CHECKBOX
    :checkbox-selected Theme$Category/CHECKBOX_SELECTED
    :dialog-area Theme$Category/DIALOG_AREA
    :list-item Theme$Category/LIST_ITEM
    :list-item-selected Theme$Category/LIST_ITEM_SELECTED
    :progress-bar-completed Theme$Category/PROGRESS_BAR_COMPLETED
    :progress-bar-remaining Theme$Category/PROGRESS_BAR_REMAINING
    :raised-border Theme$Category/RAISED_BORDER
    :screen-background Theme$Category/SCREEN_BACKGROUND
    :shadow Theme$Category/SHADOW
    :textbox Theme$Category/TEXTBOX
    :textbox-focused Theme$Category/TEXTBOX_FOCUSED))

(defn get-theme
  [graphics category-key]
  (.getDefinition (.getTheme graphics) (category category-key)))

(defn create-font
  [font-file]
  (let [font (Font/createFont Font/TRUETYPE_FONT
                              (io/file (io/resource font-file)))]
    (.registerFont (GraphicsEnvironment/getLocalGraphicsEnvironment) font)
    (.deriveFont font 14.0)))

(defonce fonts
  {:ubuntu-mono-regular (create-font "fonts/UbuntuMono-R.ttf")
   :ubuntu-mono-regular-italic (create-font "fonts/UbuntuMono-RI.ttf")
   :ubuntu-mono-bold (create-font "fonts/UbuntuMono-B.ttf")
   :ubuntu-mono-bold-italic (create-font "fonts/UbuntuMono-BI.ttf")})

(defn palette
  [color-map]
  ;; (TerminalPalette. (:default color-map) (:default-bright color-map)
  ;;                   )
  )

(def default-appearance
  (TerminalAppearance. (:ubuntu-mono-regular fonts)
                       (:ubuntu-mono-bold fonts)
                       zenburn-palette
                       true))

(defn xterm-color
  [color]
  (XTerm8bitIndexedColorUtils/getClosestColor (.getRed color)
                                              (.getGreen color)
                                              (.getBlue color)))

(defn set-foreground!
  [terminal color]
  (.applyForegroundColor terminal
                         (.getRed color)
                         (.getGreen color)
                         (.getBlue color)))

(defn set-background!
  [terminal color]
  (.applyBackgroundColor terminal
                         (.getRed color)
                         (.getGreen color)
                         (.getBlue color)))

