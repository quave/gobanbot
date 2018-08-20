(ns gobanbot.dispatcher
  (:require [clojure.string :as s]
            [gobanbot.flow :as flow]
            [gobanbot.score :as score]
            [gobanbot.storage :as storage]))

(defn find-or-create-game [cid]
  (if-let [game (storage/find-game cid)]
    game
    (storage/create-game! cid)))

(defn is-size? [value] (#{"9" "13" "19"} value))
(defn is-handicap? [size value]
  (case size
    9 (re-find #"^[02-5]$" value)
    13 (re-find #"^[02-9]$" value)
    19 (re-find #"^[02-9]$" value)))

(defn dispatch-go! [{:keys [size started] :as game} uid value]
  (if-not (empty? value)
    (if (flow/is-move? size value)
      (do
        (if (= 0 started)
          (-> game
              storage/start-game!
              (flow/move! uid value))
          (flow/move! game uid value))
        :ok)
      :not-a-move)
    :ok))

(defn insert-moves! [game color mvs]
  (doall (map #(storage/insert-move! game color %) mvs)))

(defn put-handicap! [{:keys [handicap size] :as game}]
  (println "put-handicap! size" (type size)  size "handicap" (type handicap) handicap)
  (storage/clear-moves! game)
  (case size
    9 (case handicap
        2 (insert-moves! game "b" ["cc" "gg"])
        3 (insert-moves! game "b" ["cc" "gg" "gc"])
        4 (insert-moves! game "b" ["cc" "gg" "gc" "cg"])
        5 (insert-moves! game "b" ["cc" "gg" "gc" "cg" "ee"])
        (storage/set-handicap! game 0))
    13 (case handicap
         2 (insert-moves! game "b" ["cc" "kk"])
         3 (insert-moves! game "b" ["cc" "kk" "kc"])
         4 (insert-moves! game "b" ["cc" "kk" "kc" "ck"])
         5 (insert-moves! game "b" ["cc" "kk" "kc" "ck" "gg"])
         6 (insert-moves! game "b" ["cc" "kk" "kc" "ck" "cg" "kg"])
         7 (insert-moves! game "b" ["cc" "kk" "kc" "ck" "cg" "kg" "gg"])
         8 (insert-moves! game "b" ["cc" "kk" "kc" "ck" "cg" "kg" "gc" "gk"])
         9 (insert-moves! game "b" ["cc" "kk" "kc" "ck" "cg" "kg" "gc" "gk" "gg"])
         (storage/set-handicap! game 0))
    19 (case handicap
         2 (insert-moves! game "b" ["dd" "pp"])
         3 (insert-moves! game "b" ["dd" "pp" "pd"])
         4 (insert-moves! game "b" ["dd" "pp" "pd" "dp"])
         5 (insert-moves! game "b" ["dd" "pp" "pd" "dp" "jj"])
         6 (insert-moves! game "b" ["dd" "pp" "pd" "dp" "dj" "pj"])
         7 (insert-moves! game "b" ["dd" "pp" "pd" "dp" "dj" "pj" "jj"])
         8 (insert-moves! game "b" ["dd" "pp" "pd" "dp" "dj" "pj" "jd" "jp"])
         9 (insert-moves! game "b" ["dd" "pp" "pd" "dp" "dj" "pj" "jd" "jp" "jj"])
         (storage/set-handicap! game 0))
    (storage/set-handicap! game 0)))

(defn dispatch! [cid uid cmd value]
  (let [{:keys [started ended size] :as game}
        (find-or-create-game cid)]
    (if (= ended 0)
      (case cmd
        "go" (dispatch-go! game uid value)
        "size" (if (= started 0)
                 (if (is-size? value)
                   (do (-> game
                           (storage/set-size! (read-string value))
                           put-handicap!)
                       :ok)
                   :not-a-size)
                 :already-started)
        "handicap" (if (= started 0)
                     (if (is-handicap? size value)
                       (do (-> game
                               (storage/set-handicap! (read-string value))
                               put-handicap!)
                           :ok)
                       :not-a-handicap)
                     :already-started)
        "estimate" (score/estimate game))
      :ended)))

