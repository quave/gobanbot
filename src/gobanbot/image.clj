(ns gobanbot.image
  (:require [dali.io :as svg]
            [clojure.string :as s]))

(def cell-size 100)
(def font-size (* cell-size 0.72))

(defn mv-to-xy [mv]
  (let [[a b] (s/lower-case mv)
        x (- (int a) 97)
        y (- (int b) 97)]
    [x y]))

(defn xy-to-circle [xy color]
  (let [style (if (= color "b") 
                {:fill :black} 
                {:stroke :black :fill :white :stroke-width 3})]
    [:circle style (vec (map #(-> % (* cell-size) (+ cell-size)) xy)) (* 0.45 cell-size)]))

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
      [[:circle style 
               (vec (map #(-> % (* cell-size) (+ cell-size)) xy)) 
               (* 0.3 cell-size)]])))

(defn get-hlines [size]
  (map 
    (fn [i] [:line {:stroke :black :stroke-width 5} [i cell-size] [i (* cell-size size)]])  
    (range cell-size (-> size (+ 1) (* cell-size)) cell-size)))

(defn get-vlines [size]
  (map 
    (fn [i] [:line {:stroke :black :stroke-width 5} [cell-size i] [(* cell-size size) i]])  
    (range cell-size (-> size (+ 1) (* cell-size)) cell-size)))

(defn get-htext [size]
  (map
    (fn [i] [:text {:font-family "sans-serif" 
                    :font-size font-size 
                    :x (+ font-size (* i cell-size)) 
                    :y font-size}
             (-> i (+ 97) char str)])
    (range size)))

(defn get-stars [size]
  (map (fn [xy] [:circle {:fill :black} 
         (vec (map #(-> % (* cell-size) (+ cell-size)) xy)) 
         (* 0.1 cell-size)])
    (case size
      9 [[2 2] [2 6] [6 2] [6 6] [4 4]]
      13 [[2 2] [2 6] [2 10] [6 2] [6 6] [6 10] [10 2] [10 6] [10 10]]
      19 [[3 3] [3 9] [3 15] [9 3] [9 9] [9 15] [15 3] [15 9] [15 15]])))

(defn get-vtext [size]
  (map
    (fn [i] [:text {:font-family "sans-serif" :font-size 72 :x 10 :y (+ 122 (* i cell-size))}  
             (-> i (+ 97) char str)])
    (range size)))

(defn get-goban [filename game]
  (let [f-n (str filename ".png")]
    (svg/render-png (vec (concat [:dali/page] 
                                 (get-hlines (:size game)) 
                                 (get-vlines (:size game))
                                 (get-stars (:size game))
                                 (get-htext (:size game))
                                 (get-vtext (:size game))
                                 (get-stones (:moves game))
                                 (get-last-move (-> game :moves last))))
                    (str "resources/" f-n))
    f-n))

