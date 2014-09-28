(ns frontier.game.ui
  (:require [clojure.java.io :as io])
  (:import (com.googlecode.lanterna TerminalFacade)
           (com.googlecode.lanterna.terminal Terminal)
           (com.googlecode.lanterna.gui Action GUIScreen)
           (com.googlecode.lanterna.gui.dialog TextInputDialog FileDialog
                                               ActionListDialog ListSelectDialog
                                               WaitingDialog MessageBox
                                               DialogResult DialogButtons)
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
