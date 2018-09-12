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
            [clojure.tools.logging :as log]
            [morse.handlers :refer :all]
            [morse.api :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.pprint :refer :all]))

(def token "162694958:AAGu9QiYPEm9ADwSYtEGEZ83G_9420ZvWok")

(log/info "Startup")

(defn send-answer! [chat-id status]
  (log/debug "send-answer!" chat-id status)
  (when (->> "GOBANBOT_ENV"
             System/getenv
             s/lower-case
             (not= "dev"))
    (if (= status :ok)
      (send-photo token
                  chat-id
                  (->> chat-id
                       storage/last-game
                       (img/get-goban chat-id)
                       (str (System/getProperty "java.io.tmpdir") "/gobanbot/")
                       io/file))
      (send-text token chat-id (str "Can't move " status)))))

(defn parse-cmd [cmd]
  (let [[_ cmd value] (re-find #"(?i)^\/(\w+)\s*(.*)?$" cmd)]
    {:cmd (if cmd (s/trim cmd) nil)
     :value (if value (s/trim value) nil)}))

(defn handle-cmd!
  [{{chat-id :id} :chat
    {user-id :id} :from
    cmd :text
    :as message}]
  (log/debug "msg handler" message)
  (let [{cmd-text :cmd value :value} (parse-cmd cmd)]
    (log/debug "Command parsed" cmd-text value)
    (->> (dispatcher/dispatch! chat-id user-id cmd-text value)
         (send-answer! chat-id)))
  "ok")

(defn handle-estimate!
  [{{chat-id :id} :chat {user-id :id} :from :as message}]
  (log/debug "score handler" message)
  (->> (dispatcher/dispatch! chat-id user-id "estimate" "")
       (send-text token chat-id))
  "ok")

(defhandler bot-handler
  (command "start" {user :user} (do (log/debug "User" user "joined") "ok"))
  (command "go" message (handle-cmd! message))
  (command "size" message (handle-cmd! message))
  (command "handicap" message (handle-cmd! message))
  (command "estimate" message (handle-estimate! message))
  (message message (do (log/debug "Intercepted message:" message) "ok")))

(defn custom-handler [f type]
  (fn [^Exception e data request]
    (log/error e)
    (f {:message (.getMessage e), :type type})))

(def app
  (api
    {:formats [:json]
     :exceptions {:handlers
                  {::ex/request-parsing
                     (ex/with-logging ex/request-parsing-handler :info)
                   ::ex/response-validation
                     (ex/with-logging ex/response-validation-handler :error)
                   ::ex/default (custom-handler
                                  internal-server-error
                                  :error)}}}
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
        (ok (do (log/debug "Incoming update" up)
                (bot-handler up)))))
    (ANY "/*" []
      :responses {404 String}
      (not-found "These aren't the droids you're looking for."))))

(defn content-logger [handler]
  (fn [content]
    (log/debug "Incoming request" content)
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

