(ns gobanbot.web
  (:require [gobanbot.image :as img]
            [compojure.api.sweet :refer :all]
            [compojure.api.exception :as ex]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [ring.adapter.jetty :refer :all]
            [ring.logger.protocols :as logger.protocols]
            [ring.logger :as logger]
            [morse.handlers :refer :all]
            [morse.api :refer :all]
            [clojure.java.io :as io]))

(def token "162694958:AAGu9QiYPEm9ADwSYtEGEZ83G_9420ZvWok")

(defn handle-message [msg]
  (let [chat-id (get-in msg [:chat :id])
        text (:text msg)]
    (send-text token chat-id (str "You said " text))
    (send-photo token chat-id (-> (img/get-goban chat-id) io/resource io/file))))

(defhandler bot-handler
    (command "start" {user :user} (println "User" user "joined"))
    (command "chroma" message (do (println "chrome handler" message) "ok"))

    (message message (do (println "Intercepted message:" message) (handle-message message) "ok")))

(def app
  (api
    {:formats [:json]
     :exceptions 
       {:handlers {::ex/request-parsing 
                   (ex/with-logging ex/request-parsing-handler :info)
                   ::ex/response-validation 
                   (ex/with-logging ex/response-validation-handler :error)}}}

    (context "/api" []
      :tags ["api"]
      (POST "/update" []
        :return s/Str
        :body [up s/Any]
        :summary "gets updates"
        (ok (bot-handler up))))))


(defn -main
  []
  (run-jetty 
    (-> app
        (logger/wrap-with-logger
          {:logger (reify logger.protocols/Logger
                     (add-extra-middleware [_ handler] handler)
                     (log [_ level throwable msg]
                       (println (name level) "-" msg)))})
        )
   {:port 8080
    :ssl-port  8443
    :join?     false
    :ssl?      true
    :keystore  "./gobanbot.jks"                    
    :key-password  "password"}))

