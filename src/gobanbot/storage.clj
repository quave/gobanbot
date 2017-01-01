(ns gobanbot.storage
  (:require [clojure.java.jdbc :refer :all]
            [clojure.string :as s]))

(def db
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     "resources/storage.db" }) 

(defn get-moves [gid]
  (seq (query db (str "select * from moves where eaten=0 and gid=" gid))))

(defn get-game-where [clause]
  (if-let [game (first (query db (str "select * from games where " clause)))]
    (assoc game :moves (get-moves (:gid game)))))

(defn find-game [cid]
  (get-game-where (str "ended=0 and cid=" cid)))

(defn last-game [cid]
  (get-game-where (str "cid=" cid " order by gid desc limit 1")))

(defn get-game [gid]
  (get-game-where (str "gid=" gid)))

(defn end-game! [{gid :gid :as game}] 
  (println "end-game!" gid)
  (update! db :games {:ended 1} ["gid=?" gid])
  (assoc game :ended 1))

(defn start-game! [{gid :gid :as game}] 
  (println "start-game!" gid)
  (update! db :games {:started 1} ["gid=?" gid])
  (assoc game :started 1))

(defn create-game! [cid]
  (println "create-game!" cid)
  (->> {:cid cid :size 19}
       (insert! db :games)
       first
       ((keyword "last_insert_rowid()"))
       get-game))

(defn set-size! [game size]
  (println "set-size! gid" (:gid game) size)
  (update! db :games {:size size} ["gid=?" (:gid game)])
  (assoc game :size size))

(defn set-handicap! [{gid :gid :as game} value]
  (println "set-handicap! gid" gid value)
  (update! db :games {:handicap value} ["gid=?" gid])
  (assoc game :handicap value))

(defn clear-moves! [{gid :gid}]
  (println "clear-moves! gid" gid)
  (delete! db :moves ["gid=?" gid])
  (get-game gid))

(defn insert-move! [game color mv]
  (println "add-move gid" (:gid game) "color" color "mv" mv)
  (insert! db :moves {:gid (:gid game) :color color :mv mv}))

(defn mark-eaten! [move] 
  (println "mark-eaten" move)
  (update! db :moves {:eaten 1} ["mid=?" (:mid move)]))

(defn set-player! [gid bwid uid]
  (update! db :games {bwid uid} ["gid=?" gid]))

