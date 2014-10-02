(ns frontier.ui.login
  (:require [frontier.ui :refer :all]
            [clojure.tools.logging :as log]
            [seesaw.core :refer [config config!]]))

(defn handle-login
  [client username-input password-input]
  (log/info (config username-input :text)
            (config password-input :text)))

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
                                (handle-login client
                                              username-input
                                              password-input)))
              "align right"]]
     :border (title-border "Log In"))))
