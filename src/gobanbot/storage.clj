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

(defn end-game! [gid] 
  (println "end-game" gid)
  (update! db :games {:ended 1} ["gid=?" gid]))

(defn start-game! [cid size]
  (println "start-game" cid size)
  (->> {:cid cid :size size}
       (insert! db :games)
       first
       ((keyword "last_insert_rowid()"))
       get-game))

(defn set-size! [game size]
  (println "update-size gid" (:gid game) size)
  (update! db :games {:size size} ["gid=?" (:gid game)])
  (assoc game :size size))

(defn insert-move! [game color mv]
  (println "add-move gid" (:gid game) "color" color "mv" mv)
  (insert! db :moves {:gid (:gid game) :color color :mv mv}))

(defn mark-eaten! [move] 
  (println "mark-eaten" move)
  (update! db :moves {:eaten 1} ["mid=?" (:mid move)]))

(defn set-player! [gid bwid uid]
  (update! db :games {bwid uid} ["gid=?" gid]))
