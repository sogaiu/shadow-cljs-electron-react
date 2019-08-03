(ns example.renderer
  (:require
   [cljs.tools.reader.edn :as ctre]
   [clojure.core.async :as cca]
   [frame.core :as fc]
   [goog.crypt.base64 :as gcb]
   [hx.react :as hx]
   [punk.ui.core :as puc]
   ["net" :as n.n]
   ["react-dom" :as n.rd]
   ["readline" :as n.r]))

(defonce in-chan (cca/chan))

(defonce out-chan (cca/chan))

(defonce subscriber puc/external-handler)

;; p-encode and p-decode are for encoding / decoding to avoid sending
;; data with explicit newlines in them (so newlines can be used as framing
;; delimiters)

(defn p-encode
  [partly]
  (let [encoded (gcb/encodeString partly)]
    ;; XXX
    (js/console.log (str "fully encoded: " encoded))
    encoded))

(defn p-decode
  [encoded]
  (let [partly (gcb/decodeString encoded)]
    ;; XXX
    (js/console.log (str "partly decoded: " partly))
    partly))

(fc/reg-fx
  puc/ui-frame :emit
  (fn [v]
    (js/console.log "fx :emit")
    (let [partly (pr-str v)
          encoded (p-encode partly)]
      (cca/put! out-chan partly))))

;; subscriber loop
(cca/go-loop []
  (let [ev (cca/<! in-chan)]
    ;; XXX
    (js/console.log (str "in-chan -> subscriber: " ev))
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
    ;; prepare tcp server
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
             (let [ev (cca/<! out-chan)
                   ;; XXX
                   _ (js/console.log (str "ev from out-chan: " ev))
                   encoded (p-encode ev)]
               (js/console.log (str "sending: " encoded))
               (.write sock encoded)
               (.write sock "\n")
               (recur)))
           (.on rl "line"
              (fn [a-line]
                ;; XXX
                (js/console.log (str "received: " a-line))
                (let [partly (p-decode a-line)
                      ;; XXX: remove this
                      decoded (ctre/read-string {:default tagged-literal}
                                                partly)
                      _ (js/console.log (str "decoded: " decoded))
                      _ (js/console.log (str "vector?: " (vector? decoded)))]
                  ;; XXX
                  (js/console.log (str "partly to in-chan: " partly))
                  (cca/put! in-chan partly))))
           (.on sock "end"
             (fn []
               (js/console.log "end: ")
               (.end sock))))))
    ;; start tcp server
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
