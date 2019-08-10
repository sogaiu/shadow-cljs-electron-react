(ns example.wss
  (:require
   [clojure.core.async :as cca]
   [goog.object :as go] ; XXX: for maxConnections hack
   ["ws" :as n.w]))

(defonce server
  (atom nil))

(def server-port
  9876)

(defonce sock-chan
  (cca/chan))

(defn server-start
  [in-chan]
  (let [ws-server (n.w/Server. #js {:port server-port})]
    (reset! server
      {:ws-server ws-server})
    ;; setup the websocket
    ;; XXX: hack to limit maximum connections to 1
    (set! (.-maxConnections (go/get ws-server "_server")) 1)
    (.on ws-server "connection"
      (fn connection [ws]
        (js/console.log "ws connection")
        (cca/go-loop []
          (let [ev (cca/<! sock-chan)]
            (js/console.log (str "ws sending: " ev))
            (.send ws ev)))
        (.on ws "message"
          (fn message [m]
            (js/console.log (str "ws message: " m))
            (cca/put! in-chan m)))
        (.on ws "close"
          (fn close []
            (js/console.log "ws close")
            nil))))))

(defn server-stop
  []
  ;; XXX: close or terminate or something else?
  (when @server
    (js/console.log "stopping ws-server")
    (.close (:ws-server @server))
    (reset! server nil)))

(defn handle-send
  [ev]
  (when @server
    (cca/put! sock-chan ev)))

