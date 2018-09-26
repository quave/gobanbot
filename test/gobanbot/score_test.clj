(ns gobanbot.score-test
  (:use clojure.test
        gobanbot.score))

(def uid1 10)
(def uid2 11)
(def game-many-moves {:gid 1 :bid uid1 :wid uid2 :size 9 :ended 0
                      :moves [{:gid 1 :mid 1 :mv "cc" :color "b" :eaten 0}
                              {:gid 1 :mid 2 :mv "dd" :color "w" :eaten 0}
                              {:gid 1 :mid 3 :mv "ee" :color "b" :eaten 0}
                              {:gid 1 :mid 4 :mv "ff" :color "w" :eaten 0}]})

(deftest score-test
  (testing "game->body"
    (let [res (game->body game-many-moves)]
      (is (> (count res) 0))))
  (testing "game->estimator"
    (let [res (game->estimator game-many-moves)]
      (is (> (count res) 0))))
  (testing "run-score"
    (let [res (run-score game-many-moves)]
      (println res)
      (is (> (count res) 0)))))

