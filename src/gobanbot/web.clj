(ns gobanbot.web
  (:require [gobanbot.image :as img]
            [gobanbot.flow :as flow]
            [compojure.api.sweet :refer :all]
            [compojure.api.exception :as ex]
            [ring.util.http-response :refer :all]
            [schema.core :as scm]
            [ring.adapter.jetty :refer :all]
            [ring.logger.protocols :as logger.protocols]
            [ring.logger :as logger]
            [morse.handlers :refer :all]
            [morse.api :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as s]))

(def token "162694958:AAGu9QiYPEm9ADwSYtEGEZ83G_9420ZvWok")

(defn handle-move [{{chat-id :id} :chat {user-id :id} :from cmd :text}]
  (let [text (s/replace cmd #"\/move\s" "")
        res (flow/move chat-id user-id text)]
    (if (= res :ok) 
        (send-photo token chat-id (->> chat-id
                                      flow/get-game
                                      (img/get-goban chat-id)
                                      io/resource
                                      io/file))
        (send-text token chat-id (str "Can't move " res)))))

(defhandler bot-handler
  (command "start" {user :user} (println "User" user "joined"))
  (command "move" message (do (println "move handler" message) (handle-move message) "ok"))
  (message message (do (println "Intercepted message:" message) "ok")))

(def app
  (api
    {:formats [:json]
     :exceptions {:handlers
                  {::ex/request-parsing 
                     (ex/with-logging ex/request-parsing-handler :info)
                   ::ex/response-validation 
                     (ex/with-logging ex/response-validation-handler :error)}}}

    (context "/api" []
      :tags ["api"]
      (POST "/update" []
        :return scm/Str
        :body [up scm/Any]
        :summary "gets updates"
        (ok (bot-handler up))))))

(def full-logger
  (reify logger.protocols/Logger
    (add-extra-middleware [_ handler] handler)
    (log [_ level throwable msg] (println (name level) "-" msg))))

(defn -main []
  (run-jetty 
    (logger/wrap-with-logger {:logger full-logger} app)
    {:port 8080
     :ssl-port  8443
     :join?     false
     :ssl?      true
     :keystore  "./gobanbot.jks"
     :key-password  "password"}))

