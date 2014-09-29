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
                                        Component$Alignment GUIScreen$Position
                                        Border$Bevel Border$Invisible
                                        Border$Standard)
           (com.googlecode.lanterna.gui.dialog TextInputDialog FileDialog
                                               ActionListDialog ListSelectDialog
                                               WaitingDialog MessageBox
                                               DialogResult DialogButtons)
           (com.googlecode.lanterna.gui.component TextBox Button PasswordBox
                                                  SpinningActivityIndicator
                                                  Panel EmptySpace Label
                                                  Panel$Orientation)
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

(defn text-box
  []
  (TextBox.))

(defn password-box
  []
  (PasswordBox.))

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
  (let [window (proxy [Window] ["LOG IN"])
        login-panel (Panel. (Border$Invisible.) Panel$Orientation/VERTICAL)
        edit-panel (Panel. (Border$Invisible.) Panel$Orientation/HORISONTAL)
        button-panel (Panel. (Border$Invisible.) Panel$Orientation/HORISONTAL)
        account-input (TextBox. "" 20)
        password-input (PasswordBox. "" 20)
        auth (chan 1)]
    
    (doto edit-panel
      (add-component! (Label. "USERNAME: "))
      (add-component! account-input)
      (add-component! (Label. "PASSWORD: "))
      (add-component! password-input))
    
    (doto login-panel
      (add-component! edit-panel))
    
    (doto button-panel
      (add-component! (button "BACK" (fn []
                                       (a/close! auth)
                                       (-> (.getOwner window)
                                           .getScreen
                                           .stopScreen)
                                       (.close window))))
      (add-component! (button "NEXT" (fn []
                                       (put! auth {:account-name
                                                   (.getText account-input)
                                                   :password
                                                   (-> (.getText password-input)
                                                       (scrypt/encrypt))}
                                             (fn [_] (a/close! auth)))
                                       (.close window)))))
    
    (doto window
      (add-component! login-panel)
      (add-component! button-panel))
    
    {:window window :auth auth}))

(definline ->position
  [pos]
  `(case ~pos
     :center GUIScreen$Position/CENTER
     :full-screen GUIScreen$Position/FULL_SCREEN
     :overlapping GUIScreen$Position/OVERLAPPING
     :new-corner-window GUIScreen$Position/NEW_CORNER_WINDOW))

(defn show-window!
  [{:keys [gui screen]} window position]
  (thread
    (.showWindow gui window (->position position))
    (doto screen
      (.clear)
      (.refresh))))
