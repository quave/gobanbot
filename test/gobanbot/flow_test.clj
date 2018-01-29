(ns gobanbot.flow-test
  (:use clojure.test
        gobanbot.flow))

(def uid1 10)
(def uid2 11)
(def uid3 12)
(def game-empty {:gid 1 :bid nil :wid nil :size 9 :ended 0 :moves nil})
(def game-empty-handicap
  {:gid 1 :bid nil :wid nil :size 9 :handicap 4 :ended 0 :moves nil})
(def game-1move {:gid 1 :bid uid1 :wid nil :size 9 :ended 0
                 :moves [{:gid 1 :mid 1 :mv "cc" :color "b" :eaten 0}]})
(def game-many-moves {:gid 1 :bid uid1 :wid uid2 :size 9 :ended 0
                      :moves [{:gid 1 :mid 1 :mv "cc" :color "b" :eaten 0}
                              {:gid 1 :mid 2 :mv "dd" :color "w" :eaten 0}
                              {:gid 1 :mid 3 :mv "ee" :color "b" :eaten 0}
                              {:gid 1 :mid 4 :mv "ff" :color "w" :eaten 0}]})

(deftest get-color-test
  (testing "game empty"
    (is (= nil (get-color (:moves game-empty) "aa"))))
  (testing "game has black"
    (is (= "b" (get-color (:moves game-many-moves) "cc"))))
  (testing "game has white"
    (is (= "w" (get-color (:moves game-many-moves) "dd")))))

(deftest get-last-color-test
  (testing "game empty"
    (is (= nil (get-last-color (:moves game-empty)))))
  (testing "game ends black"
    (is (= "b" (get-last-color (:moves game-1move)))))
  (testing "game ends white"
    (is (= "w" (get-last-color (:moves game-many-moves))))))

(deftest next-bwid-test
  (testing "game empty"
    (is (= :bid (next-bwid game-empty uid1))))
  (testing "game empty handicap"
    (is (= :wid (next-bwid game-empty-handicap uid1))))
  (testing "game 1move"
    (is (= :wid (next-bwid game-1move uid2))))
  (testing "game many moves bid"
    (is (= :bid (next-bwid game-many-moves uid1))))
  (testing "game many moves wid"
    (is (= :wid (next-bwid game-many-moves uid2))))
  (let [game-empty-bid (assoc game-empty :bid uid1)
        game-empty-handicap-wid (assoc game-empty-handicap :wid uid2 )]
    (testing "game empty with bid only setted"
      (is (= :wid (next-bwid game-empty-bid uid2))))
    (testing "game empty handicap with wid only setted"
      (is (= :bid (next-bwid game-empty-handicap-wid uid1))))))

(deftest decide-move-test
  (testing "no game"
    (is (= :no-game (:status (decide-move nil nil "whatever")))))
  (testing "empty game"
    (is (= (decide-move game-empty uid1 "cc")
           {:color "b" :status :ok :bwid :bid})))

   (testing "game with 1 move"
     (are [game uid mv res] (= (decide-move game uid mv) res)
          game-1move uid1 "cc" {:color "b" :status :not-your-turn :bwid :bid}
          game-1move uid2 "cc" {:color "w" :status :ocupied :bwid :wid}
          game-1move uid2 "cd" {:color "w" :status :ok :bwid :wid}
          game-1move uid2 "pass" {:color "w" :status :ok :bwid :wid}
          game-1move uid2 "resign" {:color "w" :status :ok :bwid :wid}))

   (testing "game with many moves"
     (are [game uid mv res] (= (decide-move game uid mv) res)
          game-many-moves 55 "gygy" {:color nil :status :not-a-player :bwid nil}
          game-many-moves uid2 "gygy"
            {:color "w" :status :not-your-turn :bwid :wid})))

