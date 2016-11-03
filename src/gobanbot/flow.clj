(ns gobanbot.flow
  (:require [clojure.java.jdbc :refer :all]
            [clojure.string :as s]
            [clojure.set :as ss]))

(def db
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     "resources/storage.db" }) 

(defn get-game-where [clause]
  (seq (query db (str "select rowid, * from moves where ended=0 and (" clause ")"))))

(defn get-game-full [cid] (get-game-where (str "cid=" cid)))
(defn get-game [cid] (get-game-where (str "eaten=" 0 " and cid=" cid)))
(defn end-game [cid] (update! db :moves {:ended 1} ["cid=?" cid]))
(defn mark-eaten [move] (update! db :moves {:eaten 1} ["rowid=?" (:rowid move)]))

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
    (#{"pass" "resign"} mv) :ok
    (and (-> mv count (= 2))
         (-> mv first int (>= (int \a)))
         (-> mv first int (<= (int \s)))
         (-> mv second int (>= (int \a)))
         (-> mv second int (<= (int \s)))) :ok
    :else :not-a-move))

(defn should-end? [game mv]
  (or (= mv "resign")
      (->> game 
           (filter (comp (partial = "pass") :mv)) 
           count 
           (= 2))))

(def mv-to-xy (partial map #(- (int %) 97)))
(def xy-to-mv
  (comp (partial apply str)
        (partial map (comp char 
                           (partial + 97)))))
(defn move-to-xyc
  [{:keys [mv bid]}]
  [(mv-to-xy mv) (if bid :black :white)])

(defn game-to-grid [game] (->> game (map move-to-xyc) (into {})))

(defn show [grid]
  (str 
    "----------------------\n"
    (apply str (for [x (range 19)]
      (str "|"
           (apply str (for [y (range 19)] (case (grid [x y]) :black "x" :white "o" nil "+")))
           "|\n")))
    "----------------------"))

(defn get-near-cells [[x y]]
  (->> [[(- x 1) y] [(+ x 1) y]
        [x (- y 1)] [x (+ y 1)]]
       (filter #(and (>= (first %) 0) (< (first %) 19)
                     (>= (second %) 0) (< (second %) 19)))))

(defn get-group-at
  ([grid color xy]
  (get-group-at grid color #{} xy))

  ([grid color found xy]
  (if (= (grid xy) color)
    (conj 
      (apply ss/union (map 
        #(get-group-at 
           grid 
           color 
           (conj found xy) 
           %) 
        (-> xy get-near-cells set (ss/difference found))))
      xy)
    found)))

(defn get-dame [grid group] 
  (->> group
      (map get-near-cells)
      (apply concat)
      set
      (filter (comp not (partial get grid)))
      count))

(defn get-move-by-mv [game mv]
  (first (filter #(= (:mv %) mv) game)))

(defn find-to-eat [game bwid mv] 
  (let [grid (game-to-grid game)
        op-color (if (= bwid :bid) :white :black)]
    (->> mv
         mv-to-xy
         get-near-cells
         (map #(get-group-at grid op-color %))
         (filter seq)
         (filter #(= 0 (get-dame grid %)))
         (apply ss/union)
         (map (comp (partial get-move-by-mv game) xy-to-mv)))))

(defn move [cid uid mv]
  (let [game (get-game cid)
        bwid (detect-bwid game uid)
        state (get-move-state game uid mv bwid)]
    (if (= state :ok) 
      (do (insert! db :moves {:cid cid, bwid uid, :mv mv}) 
          (if (should-end? game mv) (end-game cid))
          (doall (map mark-eaten (find-to-eat (get-game cid) bwid mv)))))
    state))

