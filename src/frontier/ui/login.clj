(ns frontier.ui.login
  (:require [frontier.ui :refer :all]
            [clojure.tools.logging :as log]
            [seesaw.core :refer [config config!]]
            [clojure.core.async :as a :refer [go go-loop <! >! put! chan take!
                                              thread <!! >!!]]
            [frontier.net.async :refer [future-chan]]))

(defn handle-login
  [client username-input password-input]
  (go (let [conn (get-in client [:login-client :conn])
            id   (get-in client [:login-client :id])
            msg {:op :login
                 :id id
                 :username (config username-input :text)
                 :password (config password-input :text)}]
        (log/info (<! (future-chan (.writeAndFlush @conn msg))))
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
