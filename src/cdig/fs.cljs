(ns cdig.fs
  (:require
   [cdig.io :as io]))

(def fs (js/require "fs"))

(defn dir? [path]
  (.isDirectory (.lstatSync fs path)))

(defn path-exists? [path]
  (.existsSync fs path))

(defn touch [path]
  (io/exec "touch" path))

(defn mkdir [path]
  (io/exec "mkdir -p" path))

(defn rm [path]
  (try
    (if (dir? path)
      (.rmdirSync fs path)
      (.unlinkSync fs path))
    (catch js/Error e nil)))

(defn spit [path text]
  (.writeFileSync fs path text))

(defn slurp [path]
  (.toString (.readFileSync fs path)))

(defn download [url path]
  (if-not (path-exists? path)
          (io/exec "curl --create-dirs -fsSo" path url)))
