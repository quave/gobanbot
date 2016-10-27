(ns gobanbot.image
  (:require [dali.io :as svg]
            [clojure.string :as s]))

(defn mv-to-xy [mv]
  (let [[a b] (s/lower-case mv)
        x (- (int a) 97)
        y (- (int b) 97)]
    (if (and (= (count mv) 2)
             (< x 19)
             (< y 19))
      [x y])))

(defn xy-to-circle [xy color]
  (let [style (if (= color :black) 
                {:fill :black} 
                {:stroke :black :fill :white :width 1})]
    [:circle style (vec (map #(-> % (* 10) (+ 10)) xy)) 6]))

(defn get-stones [moves]
  (map (fn [{:keys [mv bid]}] 
         (let [color (if bid :black :white)]
           (xy-to-circle (mv-to-xy mv) color)))
       moves))

(defn get-hlines []
  (map 
    (fn [i] [:line {:stroke :black :width 1} [i 10] [i 190]])  
    (range 10 200 10)))

(defn get-vlines []
  (map 
    (fn [i] [:line {:stroke :black :width 1} [10 i] [190 i]])  
    (range 10 200 10)))

(defn get-goban [filename moves]
  (let [f-n (str filename ".png")]
    (svg/render-png (vec (concat [:dali/page] 
                                 (get-hlines) 
                                 (get-vlines)
                                 (get-stones moves)))
                    (str "resources/" f-n))
    f-n))

