(ns gobanbot.image
  (:require [dali.io :as svg]
            [clojure.string :as s]))

(def cell-size 100)
(def font-size (* cell-size 0.72))
(def margin 100)

(defn mv-to-xy [mv]
  (let [[a b] (s/lower-case mv)
        x (- (int a) 97)
        y (- (int b) 97)]
    [x y]))

(defn get-stone-center [[x y]]
  [(-> x (* cell-size) (+ (/ cell-size 2) margin)) 
   (-> y (* cell-size) (+ (/ cell-size 2) margin))])

(defn xy-to-circle [xy color]
  (let [style (if (= color "b") 
                {:fill :black} 
                {:stroke :black :fill :white :stroke-width 3})]
    [:circle style (get-stone-center xy) (* 0.45 cell-size)]))

(defn get-stones [moves]
  (map (fn [{:keys [mv color]}] 
         (xy-to-circle (mv-to-xy mv) color))
    (filter #(-> % :mv count (<= 2)) moves)))

(defn get-last-move [{:keys [mv color] :as move}]
  (if (and move (<= (count mv) 2))
    (let [xy (mv-to-xy mv)
          style (case color 
                  "b" {:stroke :white :fill :black :stroke-width 3}
                  "w" {:stroke :black :fill :white :stroke-width 3})]
      [[:circle style (get-stone-center xy) (* 0.3 cell-size)]])))

(defn get-stars [size]
  (case size
    9 [[2 2] [2 6] [6 2] [6 6] [4 4]]
    13 [[2 2] [2 6] [2 10] [6 2] [6 6] [6 10] [10 2] [10 6] [10 10]]
    19 [[3 3] [3 9] [3 15] [9 3] [9 9] [9 15] [15 3] [15 9] [15 15]]))

(defn get-letters [size]
  (let [stars (get-stars size)]
    (map (fn [[x y]] [:text 
                      {:font-family "Open Sans Condensed Light" 
                       :font-size font-size 
                       :fill (if (some (partial = [x y]) stars) "#444444" "#aaaaaa")
                       :x (+ margin (/ cell-size 4) (* x cell-size))
                       :y (+ margin font-size (* y cell-size))}
                      (str (-> x (+ 97) char) (-> y (+ 97) char))])
      (for [a (range 0 size) b (range 0 size)] [a b]))))

(defn get-svg-data [game]
  (vec (concat [:dali/page {:width (+ (* 2 margin) (* (:size game) cell-size))
                            :height (+ (* 2 margin) (* (:size game) cell-size))}] 
               (get-letters (:size game))
               (get-stones (:moves game))
               (get-last-move (-> game :moves last)))))

(defn get-goban [filename game]
  (let [f-n (str filename ".png")
        dir (str (System/getProperty "java.io.tmpdir") "/gobanbot/")]
    (.mkdir (java.io.File. dir))
    (svg/render-png 
      (get-svg-data game) 
      (str dir f-n))
    f-n))

