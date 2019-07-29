(ns example.main
  (:require
   ["electron" :as n.e]
   ["path" :as n.p]
   ["url" :as n.u]))

(defn start []
  (js/console.log "main - start"))

(defn stop []
  (js/console.log "main - stop"))

(defonce win-ref (atom nil))

(defn create-window []
  (let [win
        (n.e/BrowserWindow.
         #js {:width 800
              :height 600
              :webPreferences
              #js {:nodeIntegration true}})
        url
        (n.u/format #js {:pathname (n.p/join js/__dirname "index.html")
                         :protocol "file:"
                         :slashes true})]
    (.loadURL win url)
    (.. win -webContents (openDevTools))
    (reset! win-ref win)
    (.on win "closed"
      (fn [_]
        (reset! win-ref nil)))))

(defn maybe-quit []
  (when (not= js/process.platform "darwin")
    (.quit n.e/app)))

(defn maybe-create-window []
  (when-not @win-ref
    (create-window)))

(defn main []
  (.on n.e/app "ready" create-window)
  (.on n.e/app "activate" maybe-create-window)
  (.on n.e/app "window-all-closed" maybe-quit))
