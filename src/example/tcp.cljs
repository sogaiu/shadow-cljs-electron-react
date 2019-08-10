(ns example.tcp
  (:require
   [clojure.core.async :as cca]
   [example.encode :as ee]
   ["net" :as n.n]
   ["readline" :as n.r]))

(defonce server
  (atom nil))

(def server-port
  1338)

(defonce sock-chan
  (cca/chan))

(defn server-start
  [in-chan]
  (let [tcp-server (n.n/createServer)]
    (reset! server
      {:tcp-server tcp-server})
    ;; prepare tcp server
    (set! (.-maxConnections tcp-server) 1)
    (.on tcp-server "error"
      (fn [err]
        (js/console.log (str "tcp-server error: " err))))
    (.on tcp-server "close"
      (fn []
        (js/console.log (str "tcp-server close: "))))
    (.on tcp-server "connection"
       (fn [sock]
         (js/console.log "tcp-server connection")
         (cca/go-loop []
           (let [ev (cca/<! sock-chan)
                 encoded (ee/p-encode ev)]
             (js/console.log (str "socket sending: " encoded))
             (.write sock encoded)
             (.write sock "\n")))
         (let [rl (n.r/createInterface #js {"input" sock})]
           (.on rl "line"
              (fn [a-line]
                (js/console.log (str "socket (readline) line: " a-line))
                (let [partly (ee/p-decode a-line)]
                  (js/console.log (str "partly to in-chan: " partly))
                  (cca/put! in-chan partly))))
           (.on sock "end"
             (fn []
               (js/console.log "socket end: ")
               (.end sock))))))
    ;; start tcp server
    (.listen tcp-server server-port
      (fn []
        (js/console.log (str "tcp-server listen: " server-port))))))

(defn server-stop
  []
  (when @server
    (js/console.log "stopping tcp-server")
    (.close (:tcp-server @server))
    (reset! server nil)))

(defn handle-send
  [ev]
  (when @server
    (cca/put! sock-chan ev)))
