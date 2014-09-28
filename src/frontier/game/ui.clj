(ns frontier.game.ui
  (:require [clojure.java.io :as io]
            [crypto.password.scrypt :as scrypt]
            [clojure.tools.logging :as log]
            [clojure.core.async :as a :refer [go-loop <! >! put! chan take!
                                              thread <!! >!!]])
  (:import (com.googlecode.lanterna.terminal Terminal)
           (com.googlecode.lanterna.gui Action GUIScreen Window Theme
                                        Interactable$Result
                                        Interactable$FocusChangeDirection
                                        Component$Alignment GUIScreen$Position)
           (com.googlecode.lanterna.gui.dialog TextInputDialog FileDialog
                                               ActionListDialog ListSelectDialog
                                               WaitingDialog MessageBox
                                               DialogResult DialogButtons)
           (com.googlecode.lanterna.gui.component TextBox Button PasswordBox
                                                  SpinningActivityIndicator)
           (com.googlecode.lanterna.gui.layout LayoutParameter)
           (com.googlecode.lanterna.screen Screen)
           (io.netty.channel ChannelOption)
           (java.nio.charset Charset)
           (java.net InetSocketAddress)))

(defn text-input
  [owner title description text]
  (TextInputDialog/showTextInputBox owner title description text))

(defn file-dialog
  [owner dir title]
  (FileDialog/showOpenFileDialog owner (io/as-file dir) title))

(defn ^Action action
  [f]
  (reify Action
    (doAction [_] (f))))

(defn action-list-dialog
  [owner title description width close-before-action? & fs]
  (ActionListDialog/showActionListDialog
   owner title description width close-before-action?
   (into-array Action (map action fs))))

(defn message-box
  ([owner title message]
     (MessageBox/showMessageBox owner title message))
  ([owner title message dialog-buttons]
     (MessageBox/showMessageBox owner title message dialog-buttons)))

(defn waiting-dialog
  [title description]
  (WaitingDialog. title description))

(defn list-select-dialog
  [owner title description list-width & items]
  (ListSelectDialog/showDialog owner title description list-width
                               (into-array items)))

(defn button
  ([text]
     (Button. text))
  ([text f]
     (Button. text (reify Action (doAction [this] (f))))))

(defn add-component!
  ([window component]
     (.addComponent window component (into-array LayoutParameter []))))

(defn login-window
  []
  (let [window (proxy [Window] ["Login Window"])]
    (doto window
      (add-component! (button "Button with no action"))
      (add-component! (button "Button with action"
                              #(log/info "I'm a callback!")))
      (add-component! (button "Close" #(.close window))))))

(definline ->position
  [pos]
  `(case ~pos
     :center GUIScreen$Position/CENTER
     :full-screen GUIScreen$Position/FULL_SCREEN
     :overlapping GUIScreen$Position/OVERLAPPING
     :new-corner-window GUIScreen$Position/NEW_CORNER_WINDOW))

(defn show-window!
  [gui window position]
  (thread
    @(future (.showWindow gui window (->position position)))))
