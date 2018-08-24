(ns gobanbot.sgf
  (:require [clojure.string :as s]))

(defn moves->sgf [{moves :moves handicap :handicap :as game}]
  (reduce
    (fn [acc {color :color mv :mv}]
      (str acc ";" (s/upper-case color) "[" mv "]"))
    "" moves))

(defn game->sgf [{size :size handicap :handicap :as game}]
  (str "(;FF[4]GM[1]SZ[" size "]"
       "CA[UTF-8]SO[https://telegram.me/gobanbot]"
       "AP[https://telegram.me/gobanbot]"
       "GN[" "]" ; TODO: place chat name
       "DT[" "]" ; TODO: add game time
       "HA[" handicap "]"
       "KM[6.5]" ; komi
       "RU[Japanese]" ; ruleset
       (moves->sgf game)
       ")"))

