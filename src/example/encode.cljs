(ns example.encode
  (:require
   [cljs.tagged-literals]
   [cljs.tools.reader.edn :as ctre]
   [goog.crypt.base64 :as gcb]))

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

(defn encoded-vector?
  [ev]
  ;; XXX: the call to read-string needs to stay in sync with other parts
  ;;      of the code (e.g. puc/external-handler aka subscriber), but that's
  ;;      in some other repository...
  (let [decoded (ctre/read-string
                  {:default tagged-literal}
                  ev)
        tested (vector? decoded)]
    (js/console.log (str "decoded: " decoded))
    (js/console.log (str "vector?: " tested))
    tested))
