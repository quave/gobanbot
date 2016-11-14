(ns gobanbot.flow-test
  (:use clojure.test
        gobanbot.flow))

(def game-empty {:gid 1 :bid nil :wid nil :size 9 :ended 0 :moves nil})
(def game-1move {:gid 1 :bid 10 :wid nil :size 9 :ended 0 
                 :moves [{:gid 1 :mid 1 :mv "cc" :color "b" :eaten 0}]})
(def game-many-moves {:gid 1 :bid 10 :wid 11 :size 9 :ended 0 
                      :moves [{:gid 1 :mid 1 :mv "cc" :color "b" :eaten 0}
                              {:gid 1 :mid 2 :mv "dd" :color "w" :eaten 0}
                              {:gid 1 :mid 3 :mv "ee" :color "b" :eaten 0} 
                              {:gid 1 :mid 4 :mv "ff" :color "w" :eaten 0}]})

(testing "decide-move"
  (testing "no game"
    (is (= :no-game (:status (decide-move nil nil "whatever")))))
  (testing "empty game" 
    (is (= (decide-move game-empty 10 "cc") 
           {:color "b" :status :ok :bwid :bid})))

   (testing "game with 1 move" 
     (are [game uid mv res] (= (decide-move game uid mv) res)
          game-1move 10 "cc" {:color "b" :status :not-your-turn :bwid nil}
          game-1move 11 "cc" {:color "w" :status :ocupied :bwid :wid}
          game-1move 11 "cd" {:color "w" :status :ok :bwid :wid}
          game-1move 11 "pass" {:color "w" :status :ok :bwid :wid}
          game-1move 11 "resign" {:color "w" :status :ok :bwid :wid}
          game-1move 11 "jj" {:color "w" :status :not-a-move :bwid :wid}))
   
   (testing "game with many moves" 
     (are [game uid mv res] (= (decide-move game uid mv) res)
          game-many-moves 55 "gygy" {:color nil :status :not-a-player :bwid nil}
          game-many-moves 11 "gygy" {:color "w" :status :not-your-turn :bwid nil})))

