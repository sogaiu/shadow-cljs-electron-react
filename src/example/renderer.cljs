(ns example.renderer
  (:require
   [clojure.core.async :as cca]
   [example.encode :as ee]
   [example.wss :as ew]
   [example.tcp :as et]
   [frame.core :as fc]
   [hx.react :as hx]
   [punk.ui.core :as puc]
   ["react-dom" :as n.rd]))

(defonce in-chan
  (cca/chan))

(defonce out-chan
  (cca/chan))

(defonce subscriber
  puc/external-handler)

;; subscriber loop -- handling info from network
(cca/go-loop []
  (let [ev (cca/<! in-chan)]
    (js/console.log (str "in-chan -> subscriber: " ev))
    ;; XXX: checking that ev represents a vector
    (ee/encoded-vector? ev)
    (subscriber ev)
    (recur)))

;; send to network via out-chan
(fc/reg-fx
  puc/ui-frame :emit
  (fn [v]
    (let [partly (pr-str v)]
      (js/console.log (str "fx :emit: " partly))
      (cca/put! out-chan partly))))

;; writing back out to the network
(cca/go-loop []
  (let [ev (cca/<! out-chan)]
    (js/console.log (str "ev from out-chan: " ev))
    (et/handle-send ev)
    (ew/handle-send ev)
    (recur)))

(defn start
  []
  (js/console.log "renderer - start")
  ;; start various listeners
  (et/server-start in-chan)
  (ew/server-start in-chan)
  ;; present punk's ui
  (n.rd/render (hx/f [puc/JustBrowser])
    (.getElementById js/document "app")))

(defn stop
  []
  (js/console.log "renderer - stop")
  (when @et/server
    (et/server-stop))
  (when @ew/server
    (ew/server-stop)))

(defn ^:export init
  []
  (js/console.log "renderer - init")
  ;; init is only called once, live reload will call stop then start
  (start))
