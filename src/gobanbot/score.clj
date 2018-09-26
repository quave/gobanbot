(ns gobanbot.score
  (:require [clojure.string :as s]
            [clojure.set :as ss])
  (:use [clojure.java.shell :only [sh]]
        [gobanbot.sgf :only [game->sgf]]))

(defn int->char [i] (char (+ 97 i)))

(defn xy->int [moves x y]
  (let [mv (str (int->char x) (int->char y))
        move (first (filter #(-> % :mv (= mv)) moves))]
    (cond
      (not move) " 0"
      (= 1 (:eaten move)) " 0"
      (= "b" (:color move)) " 1"
      :else "-1")))

(defn game->body [{:keys [size moves bid]}]
  (s/join "\n" (map (fn [x]
         (s/join " "
           (map #(xy->int moves x %)
                (range 0 size))))
       (range 0 size))))

(defn game->estimator [{:keys [size moves] :as game}]
  (let [next-move (-> moves last :color (= "w") (if 1 -1))
        header (str "# 1=black -1=white 0=open\n"
                    "height " size "\n"
                    "width " size "\n"
                    "player_to_move " next-move "\n")]
    (str header (game->body game))))

(defn run-score [{:keys [cid size handicap] :as game}]
  (let [filename (str (System/getProperty "java.io.tmpdir")
                      "/gobanbot/" cid ".game")
        _ (clojure.java.io/make-parents filename)
        _ (->> game game->estimator (spit filename))
        res (sh "./score-estimator/estimator" filename)]
    (:out res)))

(defn run-score-gnugo [method {:keys [cid size handicap] :as game}]
  (let [filename (str (System/getProperty "java.io.tmpdir") "/gobanbot/" cid ".sgf")
        _ (clojure.java.io/make-parents filename)
        _ (->> game game->sgf (spit filename))
        res (sh "gnugo"
                "--score" method
                "--boardsize" (str size)
                "--handicap" (str handicap)
                "--komi" "6.5"
                "-l" filename)]
    (-> res
        :out
        s/split-lines
        last)))

(def estimate run-score)
(def score (partial run-score "aftermath"))

