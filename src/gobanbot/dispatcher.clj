(ns gobanbot.dispatcher
  (:require [clojure.string :as s]
            [gobanbot.flow :as flow]
            [gobanbot.storage :as storage]))

(defn is-move? [size value]
  (let [max-letter (-> size (+ 96) char str)]
  (if-not (empty? value)
    (re-find (re-pattern 
               (str 
                 "^[a-" 
                 max-letter 
                 "]{2}$|^pass$|^resigni$")) 
             value))))

(defn find-or-create-game [cid]
  (if-let [game (storage/find-game cid)]
    game
    (storage/start-game! cid)))

(defn is-size? [value] (#{"9" "13" "19"} value))

(defn dispatch-go! [game uid value]
  (if-not (empty? value)
    (if (is-move? (:size game) value)
      (do (flow/move! game uid value) :ok)
      :not-a-move)
    :ok))

(defn dispatch! [cid uid cmd value]
  (let [game (find-or-create-game cid)]
    (case cmd
      "go" (dispatch-go! game uid value)
      "size" (if (is-size? value) 
               (do (storage/set-size! game value) :ok)
               :not-a-size)
      "handicap" :ok)))

