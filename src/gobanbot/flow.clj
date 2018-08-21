(ns gobanbot.flow
  (:require [clojure.string :as s]
            [clojure.set :as ss]
            [clojure.tools.logging :as log]
            [gobanbot.storage :as storage]))

(defn moves-to-mvc [moves] (zipmap (map :mv moves) (map :color moves)))
(defn get-color [moves mv]  (get (moves-to-mvc moves) mv))
(defn get-move-by-mv [moves mv] (first (filter #(= (:mv %) mv) moves)))
(def get-last-color (comp :color last))

(defn is-move? [size value]
  (let [max-letter (-> size (+ 96) char str)]
  (if-not (empty? value)
    (re-find (re-pattern
               (str
                 "^[a-"
                 max-letter
                 "]{2}$|^pass$|^resigni$"))
             value))))

(defn next-bwid [{:keys [bid wid handicap]} uid]
  (cond
    (= bid uid) :bid ; already in game
    (= wid uid) :wid ; already in game
    :else (if (or (nil? handicap) (zero? handicap))
            (cond (not bid) :bid
                  (not wid) :wid)
            (cond (not wid) :wid
                  (not bid) :bid))))

(defn decide-move
  [{:keys [moves bid wid ended started] :as game} uid mv]
  (let [bwid (next-bwid game uid)
        color (cond (= bwid :bid) "b" (= bwid :wid) "w")]
    {:bwid bwid
     :color color
     :status
     (cond
       (not game) :no-game
       (not color) :not-a-player
       (= started 0) :not-started
       (-> moves get-last-color (= color)) :not-your-turn
       (and (get-color moves mv) (not= mv "pass")) :ocupied
       (= ended 1) :no-game
       :else :ok)}))

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
       (filter #(is-move? size %))))

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
  (log/debug "move gid" (:gid game) uid mv)
  (let [gid (:gid game)
        {:keys [status color bwid] :as stat} (decide-move game uid mv)]
    (log/debug "move status" stat)
    (if bwid (storage/set-player! gid bwid uid))
    (if (= status :ok) (add-move! game color mv))
    status))

