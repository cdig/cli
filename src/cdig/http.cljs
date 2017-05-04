(ns cdig.http
  (:require
   [clojure.string :refer [starts-with?]])
  (:refer-clojure :exclude [get]))

(def request (js/require "sync-request"))

(defn url? [path]
  (or (starts-with? path "http://")
      (starts-with? path "https://")))

(defn url-encode [string]
  (some-> string str (js/encodeURIComponent) (.replace "+" "%20")))

(defn map->query [m]
  (some->> (seq m)
           (map (fn [[k v]]
                  [(url-encode (name k))
                   "="
                   (url-encode (str v))]))
           (interpose "&")
           flatten
           (apply str)))

(defn get [url & headers]
  (request "GET" url))

; (defn post [url header data cb]
;   (exec "curl --write-out '\n' --data" (map->query data) "--header" (str \' header \') url))

(defn slurp [path]
  (try
    (.toString (.getBody (get path)))
    (catch :default e
      nil)))
