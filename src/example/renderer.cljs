(ns example.renderer
  (:require
   [clojure.core.async :as cca]
   [frame.core :as fc]
   [hx.react :as hx]
   [punk.ui.core :as puc]
   ["net" :as n.n]
   ["react-dom" :as n.rd]
   ["readline" :as n.r]))

(defonce in-chan (cca/chan))

(defonce out-chan (cca/chan))

(defonce subscriber puc/external-handler)

(fc/reg-fx
 puc/ui-frame :emit
 (fn [v]
   (cca/put! out-chan (pr-str v))))

;; subscriber loop
(cca/go-loop []
  (let [ev (cca/<! in-chan)]
    (subscriber ev)
    (recur)))

(defonce tcp-server
  (atom nil))

(defn start
  []
  (js/console.log "renderer - start")
  (let [port 1338
        server (n.n/createServer)]
    (reset! tcp-server server)
    (.on server "error"
      (fn [err]
        (js/console.log (str "error: " err))))
    (.on server "close"
      (fn []
        (js/console.log (str "close: "))))
    (.on server "connection"
       (fn [sock]
         (let [rl (n.r/createInterface #js {"input" sock})]
           (js/console.log "connection: ")
           (cca/go-loop []
             (let [ev (cca/<! out-chan)]
               (.write sock ev)
               (recur)))
           (.on rl "line"
              (fn [a-line]
                (js/console.log (str "line: " a-line))
                (cca/put! in-chan a-line)))
           (.on sock "end"
             (fn []
               (js/console.log "end: ")
               (.end sock))))))
    (.listen server
             port (fn []
                    (js/console.log (str "listen: " port)))))
  ;;
  (n.rd/render (hx/f [puc/JustBrowser])
               (.getElementById js/document "app")))

(defn ^:export init
  []
  (js/console.log "renderer - init")
  ;; init is only called once, live reload will call stop then start
  (start))

(defn stop
  []
  (js/console.log "renderer - stop"))
