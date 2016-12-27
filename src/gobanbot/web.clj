(ns gobanbot.web
  (:gen-class)
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
  (let [text (s/replace cmd #"\/go\s" "")
        res (flow/entry chat-id user-id text)]
    (if (= res :ok) 
        (send-photo token chat-id (->> chat-id
                                      flow/last-game
                                      (img/get-goban chat-id)
                                      io/resource
                                      io/file))
        (send-text token chat-id (str "Can't move " res)))))

(defhandler bot-handler
  (command "start" {user :user} (do (println "User" user "joined") "ok"))
  (command "go" message (do (println "move handler" message) (handle-move message) "ok"))
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
        (ok (do (println "Incoming update" up) 
                (bot-handler up)))))))

(defn content-logger [handler]
  (fn [content]
    (println "Incoming request" content)
    (handler content)))

(defn -main []
  (run-jetty 
    (-> app
        (logger/wrap-with-logger
          {:logger (reify logger.protocols/Logger
                     (add-extra-middleware [_ handler] handler)
                     (log [_ level throwable msg]
                       (println (name level) "-" msg)))})
        content-logger)
    {:port 8080
     :ssl-port  8443
     :join?     false
     :ssl?      true
     :keystore  "./gobanbot.jks"
     :key-password  "password"}))

