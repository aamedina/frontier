(ns frontier.ui.login
  (:require [frontier.ui :refer :all]
            [clojure.tools.logging :as log]
            [seesaw.core :refer [config config!]]
            [clojure.core.async :as a :refer [go go-loop <! >! put! chan take!
                                              thread <!! >!! alts!]]
            [frontier.net.async :refer [future-chan]]))

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
          (case (:op msg)
            :authentication-success (log/info msg)
            :authentication-failure (log/info msg)
            :new-account (log/info msg)
            (log/error msg)))
        (doall (map a/close! [success failure new-account]))
        true)))

(defn login-panel
  [client]
  (let [username-input (text)
        password-input (password)]
    (mig-panel
     :constraints ["" "" ""]
     :items [[(label :text "Username") ""]
             [username-input "wrap"]
             [(label :text "Password") ""]
             [password-input "wrap 15px"]
             [(button :text "Back")
              "align left"]
             [(button :text "Next"
                      :action (fn [this]
                                (go (if (<! (handle-login client
                                                          username-input
                                                          password-input))
                                      (log/info "authorized!")
                                      (log/info "unauthorized!")))))
              "align right"]]
     :border (title-border "Log In"))))
