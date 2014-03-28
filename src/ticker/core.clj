(ns ticker.core
  (:require [clojure.core.async
             :refer [chan <! >! timeout go]
              :as async]))

(defn adjust-price [old-price]
  (let  [numerator (- (rand-int 30) 15)
         adjustment (* numerator 0.01M)]
    (+ old-price adjustment)))

(defn new-transaction [symbol price]
  {:symbol symbol
;;   :time (java.util.Date.) I will want this later
   :price price})

(defn random-time [t]
  (* t (+ 1 (rand-int 5))))

(defn make-ticker [symbol t start-price]
  (let [c (chan)]
    (go
     (loop [price start-price]
       (let [new-price (adjust-price price)]
         (do
           (<! (timeout (random-time t)))
           (>! c (new-transaction symbol new-price))
           (recur new-price)))))
    c))

(def stocks [ ;; symbol min-interval starting-price
             ["AAPL" 1400 537 ]
             ["AMZN" 4200 345]
             ["CSCO" 400  22]
             ["EBAY" 1200 55]
             ["GOOG" 8200 1127]
             ["IBM" 2200  192]
             ["MSFT" 500 40]
             ["ORCL" 1000 39]
             ["RHT" 10200  53]
             ["T" 600 35]])

(defn run-sim []
  (let [ticker (async/merge (map #(apply make-ticker %) stocks)) ]
    (go
     (loop [x 0]
       (when (< x 1000)
         (do (println (str x "-" (<! ticker)))
             (recur (inc x))))))))
