(ns gobanbot.flow
  (:require [clojure.java.jdbc :refer :all]
            [clojure.string :as s]))

(def db
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     "resources/storage.db" }) 

(defn get-game-where [clause]
  (seq (query db (str "select * from moves where ended=0 and (" clause ")"))))

(defn get-game [cid] (get-game-where (str "cid=" cid)))
(defn end-game [cid] (update! db :moves {:ended 1} ["cid=?" cid]))
(defn detect-bwid [game uid]
  (cond
    (not game) :bid
    (some #(-> % :bid (= uid)) game) :bid 
    (some #(-> % :wid (= uid)) game) :wid 
    :else :wid))

(defn get-move-state [game uid mv bwid]
  (cond 
    (-> game last (get bwid) (= uid)) :not-your-turn
    (and (some #(= (:mv %) mv) game) 
         (not= mv "pass")) :ocupied
    (-> game last :mv (= "resign")) :game-ended
    :else :ok))

(defn should-end? [game mv]
  (or (= mv "resign")
      (->> game 
           (filter #(-> % :mv (= "pass"))) 
           count 
           (= 2))))

(defn move [cid uid mv]
  (let [game (get-game cid)
        bwid (detect-bwid game uid)
        state (get-move-state game uid mv bwid)]
    (if (= state :ok) 
      (do (insert! db :moves {:cid cid, bwid uid, :mv mv}) 
          (if (should-end? game mv) (end-game cid))))
    state))

