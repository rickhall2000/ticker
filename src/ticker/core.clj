(ns ticker.core
  (:require [clojure.core.async
             :refer [chan <! >! timeout go]
              :as async]))

(defn make-ticker [symbol t]
  (let [c (chan)]
    (go
     (while true
       (<! (timeout t))
       (>! c {:symbol symbol})))
    c))

(defn run-sim []
  (let [ticker (make-ticker "Tick" 2000)]
    (go
     (loop [x 0]
       (when (< x 5)
         (do
           (println (<! ticker))
           (recur (inc x))))))))
