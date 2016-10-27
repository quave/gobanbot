(ns gobanbot.image
  (:require [dali.io :as svg]))

(defn get-goban [filename]
  (let [lv (map 
             (fn [i] [:line {:stroke :black :width 1} [i 10] [i 190]])  
             (range 10 200 10))
        lh (map 
             (fn [i] [:line {:stroke :black :width 1} [10 i] [190 i]])  
             (range 10 200 10))
        f-n (str filename ".png")]
    (svg/render-png (vec (concat [:dali/page] lv lh))
                    (str "resources/" f-n))
    f-n))

