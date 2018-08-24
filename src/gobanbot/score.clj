(ns gobanbot.score
  (:require [clojure.string :as s])
  (:use [clojure.java.shell :only [sh]]
        [gobanbot.sgf :only [game->sgf]]))

(defn run-score [method {:keys [cid size handicap] :as game}]
  (let [filename (str (System/getProperty "java.io.tmpdir") "/gobanbot/" cid ".sgf")
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

(def estimate (partial run-score "estimate"))
(def score (partial run-score "aftermath"))

