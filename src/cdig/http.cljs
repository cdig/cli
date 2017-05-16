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

(defn get
  ([url] (get url {}))
  ([url headers] (request "GET" url (clj->js {:headers headers}))))

(defn post
  ([url data] (post url data {}))
  ([url data headers] (request "POST" url (clj->js {:json data :headers headers}))))

(defn submit [f & args]
  (.toString
   (try
     (.getBody (apply f args) "utf8")
     (catch :default e
       (if-let [body (.-body e)]
         body
         e)))))

(defn slurp
  ([url] (slurp url {}))
  ([url headers] (submit get url headers)))

(defn spit
  ([url data] (spit url data {}))
  ([url data headers] (submit post url data headers)))
