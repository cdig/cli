(ns cdig.io
  (:require
   [clojure.string :refer [join]]
   [clojure.string :refer [starts-with?]]))

(def colors (js/require "colors/safe"))
(def exec-sync (.-execSync (js/require "child_process")))
(def fs (js/require "fs"))
(def keytar (js/require "keytar"))
(def request (js/require "sync-request"))

(defn url? [path]
  (or (starts-with? path "http://")
      (starts-with? path "https://")))

(defn dir? [path]
  (.isDirectory (.lstatSync fs path)))

(defn path-exists? [path]
  (.existsSync fs path))

(defn exec [& cmds]
  (exec-sync (join " " cmds) (clj->js {:stdio "inherit"})))

(defn touch [path]
  (exec "touch" path))

(defn mkdir [path]
  (exec "mkdir -p" path))

(defn rm [path]
  (try
    (if (dir? path)
      (.rmdirSync fs path)
      (.unlinkSync fs path))
    (catch js/Error e nil)))

(defn spit [path text]
  (.writeFileSync fs path text))

(defn spit-json [path text]
  (spit path (js/JSON.stringify (clj->js text))))

(defn slurp [path]
  (if (url? path)
    (.toString (.getBody (request "GET" path)))
    (.toString (.readFileSync fs path))))

(defn slurp-json [path]
  (when-let [text (slurp path)]
    (-> (js/JSON.parse text)
        (js->clj :keywordize-keys true))))

(defn curl [url path]
  (if-not (path-exists? path)
          (exec "curl --create-dirs -fsSo" path url)))

(defn color [col & texts]
  (let [f (aget colors (name col))]
    (f (apply str texts))))

(defn get-password [name cb]
  (-> (.getPassword keytar "com.lunchboxsessions.cli" name)
      (.then cb)))

(defn set-password [name password]
  (.setPassword keytar "com.lunchboxsessions.cli" name password))
