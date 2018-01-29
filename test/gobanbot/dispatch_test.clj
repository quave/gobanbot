(ns gobanbot.flow-test
  (:use clojure.test
        gobanbot.flow))

(deftest entry-test
  (with-redefs-fn {#'move! (constantly "move")
                   #'find-game (constantly nil)
                   #'start-game (constantly "start-game")
                   #'update-size (constantly "update size")}
    #(are [cid uid text res] (= (entry cid uid text) res)
          1 1 "9" :ok
          1 1 "13" :ok
          1 1 "19" :ok))

  (with-redefs-fn {#'move (constantly "move")
                   #'find-game (constantly game-empty)
                   #'start-game (constantly "start-game")
                   #'update-size (constantly "update size")}
    #(are [cid uid text res] (= (entry cid uid text) res)
          1 1 "9" :ok
          1 1 "13" :ok
          1 1 "19" :ok)))
