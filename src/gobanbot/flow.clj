(ns gobanbot.flow
  (:require [clojure.java.jdbc :refer :all]
            [clojure.string :as s]
            [clojure.set :as ss]))

(def db
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     "resources/storage.db" }) 

(defn get-moves [gid]
  (seq (query db (str "select * from moves where eaten=0 and gid=" gid))))
(defn moves-to-mvc [moves] (zipmap (map :mv moves) (map :color moves)))
(def get-last-color (comp :color last))
(defn get-color [moves mv]  (get (moves-to-mvc moves) mv))
(defn get-move-by-mv [moves mv] (first (filter #(= (:mv %) mv) moves)))
(defn mark-eaten [move] 
  (println "mark-eaten" move)
  (update! db :moves {:eaten 1} ["mid=?" (:mid move)]))

(defn find-game [cid]
  (if-let [game (first (query db (str "select * from games where ended=0 and cid=" cid)))]
    (assoc game :moves (get-moves (:gid game)))))

(defn get-game [gid]
  (if-let [game (first (query db (str "select * from games where gid=" gid)))]
    (assoc game :moves (get-moves (:gid game)))))

(defn end-game [gid] 
  (println "end-game" gid)
  (update! db :games {:ended 1} ["gid=?" gid]))

(defn on-board? [mv size]
  (let [min-move 97
        max-move (+ 96 size)]
    (and (-> mv count (= 2))
         (-> mv first int (>= min-move))
         (-> mv first int (<= max-move))
         (-> mv second int (>= min-move))
         (-> mv second int (<= max-move)))))

(defn guess-color [{:keys [bid wid]} uid]
  (cond
    (or (not bid) (= bid uid)) "b"
    (or (not wid) (= wid uid)) "w"))

(defn decide-move [game uid  mv]
  (let [moves (:moves game)
        color (guess-color game uid)
        bid (:bid game)
        wid (:wid game)
        bwid (cond (not bid) :bid (and (not wid) (not= bid uid)) :wid)]
    {:bwid bwid
     :color color
     :status
     (cond 
       (not game) :no-game
       (not color) :not-a-player
       (-> game :moves get-last-color (= color)) :not-your-turn
       (and (get-color moves mv) (not= mv "pass")) :ocupied
       (= (:ended game) 1) :no-game
       (#{"pass" "resign"} mv) :ok
       (on-board? mv (:size game)) :ok
       :else :not-a-move)}))

(defn should-end? [game]
  (or (->> game :moves (some #(= "resign" (:mv %))))
      (->> game 
           :moves
           (filter (comp (partial = "pass") :mv)) 
           count 
           (= 2))))

(defn show [{:keys [size moves]}]
  (let [mvc (moves-to-mvc moves)]
    (str 
      (apply str (repeat (+ 2 size) "-")) "\n"
      (apply str (for [v (range size)]
        (str "|"
           (apply str (for [h (range size)] 
                        (case (mvc (str (-> h (+ 97) char) (-> v (+ 97) char))) 
                          "b" "x" 
                          "w" "o" 
                          nil "+")))
           "|\n")))
      (apply str (repeat (+ 2 size) "-")))))

(defn char-add [c n] (-> c int (+ n) char))
(defn get-near-cells [mv size]
  (->> [(str (char-add (first mv) 1) (second mv))
        (str (char-add (first mv) -1) (second mv))
        (str (first mv) (char-add (second mv) 1))
        (str (first mv) (char-add (second mv) -1))]
       (filter #(on-board? % size))))

(defn get-group-at
  ([{:keys [size moves]} color mv]
    (get-group-at size 
                  (moves-to-mvc moves) 
                  color 
                  #{} 
                  mv))

  ([size mvc color found mv]
  (if (= (mvc mv) color)
    (conj 
      (apply ss/union (map 
        #(get-group-at
           size 
           mvc
           color 
           (conj found mv) 
           %)
        (-> mv (get-near-cells size) set (ss/difference found))))
      mv)
    found)))

(defn get-dame [{:keys [size moves]} group] 
  (let [mvc (moves-to-mvc moves)]
  (->> group
      (map #(get-near-cells % size))
      (apply concat)
      set
      (filter (comp not (partial get mvc)))
      count)))

(defn find-to-eat [{:keys [size moves] :as game} color mv] 
  (let [op-color (if (= color "b") "w" "b")]
    (->> mv
         (#(get-near-cells % size))
         (map #(get-group-at game op-color %))
         (filter seq)
         (filter #(= 0 (get-dame game %)))
         (apply ss/union)
         (map #(get-move-by-mv moves %)))))

(defn add-move [game color mv]
  (println "add-move gid" (:gid game) "color" color "mv" mv)
  (insert! db :moves {:gid (:gid game) :color color :mv mv})
  (let [gid (:gid game)
        updated (get-game gid)]
    (if (should-end? updated) 
      (end-game gid)
      (doall (map mark-eaten (find-to-eat updated color mv))))))

(defn move [game uid mv]
  (println "move gid" (:gid game) uid mv)
  (let [gid (:gid game)
        {:keys [status color bwid]} (decide-move game uid mv)]
    (if bwid (update! db :games {bwid uid} ["gid=?" gid]))
    (if (= status :ok) (add-move game color mv))
    status))

(defn start-game [cid size]
  (println "start-game" cid size)
  (->> {:cid cid :size size}
       (insert! db :games)
       first
       ((keyword "last_insert_rowid()"))
       get-game))

(defn update-size [game size]
  (println "update-size gid" (:gid game) size)
  (update! db :games {:size size} ["gid=?" (:gid game)])
  (assoc game :size size))

(defn entry [cid uid value] 
  (let [game (find-game cid)
        mv (re-find #"^\s*[a-zA-Z]+" value)
        size (read-string (or (re-find #"^\s*\d+" value) "nil"))]
    (println "entry mv" mv "size" size)
    (if mv
      ;has move coords
      (if game 
        (move game uid (-> mv s/trim s/lower-case))
        (move (start-game cid 19) uid (-> mv s/trim s/lower-case)))
      ;no move coords
      (if size 
        (cond
          (not (get #{9 13 19} size)) :not-a-size
          (not game) (do (start-game cid size) :ok)
          (-> game :moves seq not) (do (update-size game size) :ok)
          :else :not-a-move) 
        :not-a-move))))

