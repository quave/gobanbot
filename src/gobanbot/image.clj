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
    [:circle style (vec (map #(-> % (* 100) (+ 100)) xy)) 45]))

(defn get-stones [moves]
  (map (fn [{:keys [mv bid]}] 
         (let [color (if bid :black :white)]
           (xy-to-circle (mv-to-xy mv) color)))
       moves))

(defn get-hlines []
  (map 
    (fn [i] [:line {:stroke :black :stroke-width 5} [i 100] [i 1900]])  
    (range 100 2000 100)))

(defn get-vlines []
  (map 
    (fn [i] [:line {:stroke :black :stroke-width 5} [100 i] [1900 i]])  
    (range 100 2000 100)))

(defn get-htext []
  (map
    (fn [i] [:text {:font-family "sans-serif" :font-size 72 :x (+ 72 (* i 100)) :y 72}  
             (-> i (+ 97) char str)])
    (range 19)))

(defn get-vtext []
  (map
    (fn [i] [:text {:font-family "sans-serif" :font-size 72 :x 10 :y (+ 122 (* i 100))}  
             (-> i (+ 97) char str)])
    (range 19)))

(defn get-goban [filename moves]
  (let [f-n (str filename ".png")]
    (svg/render-png (vec (concat [:dali/page] 
                                 (get-hlines) 
                                 (get-vlines)
                                 (get-htext)
                                 (get-vtext)
                                 (get-stones moves)))
                    (str "resources/" f-n))
    f-n))

