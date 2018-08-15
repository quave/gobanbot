(ns gobanbot.web
  (:gen-class)
  (:require [gobanbot.image :as img]
            [gobanbot.dispatcher :as dispatcher]
            [gobanbot.storage :as storage]
            [compojure.api.sweet :refer :all]
            [compojure.api.exception :as ex]
            [ring.util.http-response :refer :all]
            [schema.core :as scm]
            [ring.adapter.jetty :refer :all]
            [ring.logger :as logger]
            [morse.handlers :refer :all]
            [morse.api :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.pprint :refer :all]))

(def token "162694958:AAGu9QiYPEm9ADwSYtEGEZ83G_9420ZvWok")

(defn send-answer! [chat-id status]
  (if (= status :ok)
    (send-photo token
                chat-id
                (->> chat-id
                     storage/last-game
                     (img/get-goban chat-id)
                     (str (System/getProperty "java.io.tmpdir") "/gobanbot/")
                     io/file))
    (send-text token chat-id (str "Can't move " status))))

(defn parse-cmd [cmd]
  (let [[_ cmd value] (re-find #"(?i)^\/(\w+)\s*(.*)?$" cmd)]
    {:cmd (if cmd (s/trim cmd) nil)
     :value (if value (s/trim value) nil)}))

(defn handle-cmd!
  [{{chat-id :id} :chat
    {user-id :id} :from
    cmd :text
    :as message}]
  (println "msg handler" message)
  (let [{cmd-text :cmd value :value} (parse-cmd cmd)]
    (->> (dispatcher/dispatch! chat-id user-id cmd-text value)
         (send-answer! chat-id)))
  "ok")

(defn handle-estimate!
  [{{chat-id :id} :chat :as message}]
  (println "score handler" message)
  (->> (dispatcher/dispatch! chat-id 0 "estimate" "")
       ; TODO estimate
       (send-text token chat-id))
  "ok")

(defhandler bot-handler
  (command "start" {user :user} (do (println "User" user "joined") "ok"))
  (command "go" message (handle-cmd! message))
  (command "size" message (handle-cmd! message))
  (command "handicap" message (handle-cmd! message))
  (command "estimate" message (handle-estimate! message))
  (message message (do (println "Intercepted message:" message) "ok")))

(def app
  (api
    {:formats [:json]
     :exceptions {:handlers
                  {::ex/request-parsing
                     (ex/with-logging ex/request-parsing-handler :info)
                   ::ex/response-validation
                     (ex/with-logging ex/response-validation-handler :error)
                   ::ex/default (ex/with-logging internal-server-error :error)}}}
    (context "/api" []
      :tags ["api"]
      (GET "/test" []
        :return {:result scm/Str}
        :summary "test"
        (ok {:result "test ok"}))
      (POST "/update" []
        :return scm/Str
        :body [up scm/Any]
        :summary "gets updates"
        (ok (do (println "Incoming update")
                (pprint up)
                (bot-handler up)))))
    (ANY "/*" []
      :responses {404 String}
      (not-found "These aren't the droids you're looking for."))))

(defn content-logger [handler]
  (fn [content]
    (pprint "Incoming request")
    (pprint content)
    (handler content)))

(defn -main []
  (run-jetty
    (-> app
        logger/wrap-log-response
        content-logger
        logger/wrap-log-request-start)
    {:port 8080
     :ssl-port  8443
     :join?     false
     :ssl?      true
     :keystore  "./gobanbot.jks"
     :key-password  "password"}))

