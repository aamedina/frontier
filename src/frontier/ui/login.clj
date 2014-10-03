(ns frontier.ui.login
  (:require [frontier.ui :refer :all]
            [frontier.ui.main :refer [primary-terminal]]
            [clojure.tools.logging :as log]
            [seesaw.core :as ui :refer [config config!]]
            [clojure.core.async :as a :refer [go go-loop <! >! put! chan take!
                                              thread <!! >!! alts!]]
            [frontier.net.async :refer [future-chan]]))

(declare login-panel)

(defn handle-success
  [client]
  (config! (:frame client) :content (primary-terminal client)))

(defn handle-failure
  [client]
  (config! (:frame client) :content
           (mig-panel
            :constraints ["" "" ""]
            :items [[(ui/select (:frame client) [:#login-panel])
                     "center, wrap, push"]
                    [(label :text "Authentication Failed --- This event will be recorded in the Starship's log.")
                     "bottom, right"]]
            :border (title-border "Frontier"))))

(defn handle-error
  [client]
  (config! (:frame client) :content
           (mig-panel
            :constraints ["" "" ""]
            :items [[(ui/select (:frame client) [:#login-panel])
                     "center, wrap, push"]
                    [(label :text "The Executive Command Terminal is currently unavailable while a system wide diagnostic is performed.")
                     "bottom, right"]]
            :border (title-border "Frontier"))))

(defn handle-new-account
  [client]
  (config! (:frame client) :content
           (mig-panel
            :constraints ["" "" ""]
            :items [[(login-panel client) "center, wrap, push"]
                    [(label :text "Starship Executive Command Terminal")
                     "bottom, right"]]
            :border (title-border "Frontier"))))

(defn handle-login
  [client username-input password-input]
  (go (let [conn (get-in client [:login-client :conn])
            id   (get-in client [:login-client :id])
            events (get-in client [:login-client :events])
            msg {:op :login
                 :id id
                 :username (config username-input :text)
                 :password (config password-input :text)}
            success (a/sub events :authentication-success (chan 1))
            failure (a/sub events :authentication-failure (chan 1))
            new-account (a/sub events :new-account (chan 1))]
        (<! (future-chan (.writeAndFlush @conn msg)))
        (when-some [[msg port] (alts! [success failure new-account])]
          (let [ret (case (:op msg)
                      :authentication-success (handle-success client)
                      :authentication-failure (handle-failure client)
                      :new-account (handle-new-account client)
                      (handle-error client))]
            (doall (map a/close! [success failure new-account]))
            ret)))))

(defn login-panel
  [client]
  (let [username-input (text)
        password-input (password)]
    (mig-panel
     :id :login-panel
     :constraints ["" "" ""]
     :items [[(label :text "Username") ""]
             [username-input "wrap"]
             [(label :text "Password") ""]
             [password-input "wrap 15px"]
             [(button :text "Back")
              "align left"]
             [(button :text "Next"
                      :action (fn [this]
                                (handle-login client
                                              username-input
                                              password-input)))
              "align right"]]
     :border (title-border "Log In"))))
