(ns ticker.core
  (:require [clojure.core.async
             :refer [chan <! >! timeout go]
             :as async]
            [compojure.core :refer [defroutes GET]]
            [org.httpkit.server :refer [with-channel run-server on-close send!]]
            [compojure.handler :as handler]
            [ring.middleware.cors :refer [wrap-cors]]
            ))

(defn adjust-price [old-price]
  (let  [numerator (- (rand-int 30) 15)
         adjustment (* numerator 0.01M)]
    (+ old-price adjustment)))

(defn random-time [t]
  (* t (+ 1 (rand-int 5))))

(defn new-transaction [symbol price]
  {:symbol symbol
   :time (java.util.Date.)
   :price price})

(defn make-ticker [symbol t start-price]
  (let [c (chan)]
    (go
     (loop [price start-price]
       (let [new-price (adjust-price price)]
         (<! (timeout (random-time t)))
         (>! c (new-transaction symbol new-price))
         (recur new-price))))
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


(def clients (atom {}))

(defn ws
  [req]
  (with-channel req con
    (swap! clients assoc con true)
    (println con " connected")
    (on-close con (fn [status]
                    (swap! clients dissoc con)
                    (println con "disconnected. status" status)))))

(defroutes routes
  (GET "/ticker" [] ws))

(def application (-> (handler/site routes)
                     (wrap-cors
                      :access-control-allow-origin #".+")))

(defn send-update [msg]
  (doseq [client @clients]
    (send! (key client)
           (pr-str msg) false)))

(defn run-sim []
  (let [ticker (async/merge
                (map #(apply make-ticker %) stocks))]
    (go
     (loop [x 0]
       (send-update (<! ticker))
       (recur (inc x))))))


(defn -main [& args]
  (let [port (Integer/parseInt
              (or (System/getenv "PORT") "8080"))]
    (println "starting server on port: " port)
    (run-sim)
    (run-server application {:port port :join? false })))
