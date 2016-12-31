(ns gobanbot.flow
  (:require [clojure.string :as s]
            [clojure.set :as ss]
            [gobanbot.storage :as storage]))

(defn moves-to-mvc [moves] (zipmap (map :mv moves) (map :color moves)))
(defn get-color [moves mv]  (get (moves-to-mvc moves) mv))
(defn get-move-by-mv [moves mv] (first (filter #(= (:mv %) mv) moves)))
(def get-last-color (comp :color last))

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

(defn add-move! [game color mv]
  (storage/insert-move! game color mv)
  (let [gid (:gid game)
        updated (storage/get-game gid)]
    (if (should-end? updated) 
      (storage/end-game! gid)
      (doall (map storage/mark-eaten! (find-to-eat updated color mv))))))

(defn move! [game uid mv]
  (println "move gid" (:gid game) uid mv)
  (let [gid (:gid game)
        {:keys [status color bwid]} (decide-move game uid mv)]
    (if bwid (storage/set-player! gid bwid uid))
    (if (= status :ok) (add-move! game color mv))
    status))

