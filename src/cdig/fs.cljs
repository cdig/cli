(ns cdig.fs
  (:require
   [cdig.io :as io]
   [clojure.string :as str]))

(def fs (js/require "fs"))
(def path (js/require "path"))

(defn dirname [filepath]
  (.dirname path filepath))

(defn basename [filepath]
  (.basename path filepath))

(defn parent-dirname [filepath]
  (last (str/split (dirname filepath) path.sep)))

(defn dir? [filepath]
  (.isDirectory (.lstatSync fs filepath)))

(defn path-exists? [filepath]
  (.existsSync fs filepath))

(defn touch [filepath]
  (io/exec "touch" filepath))

(defn mkdir [filepath]
  (io/exec "mkdir -p" filepath))

(defn rm [filepath]
  (try
    (if (dir? filepath)
      (.rmdirSync fs filepath)
      (.unlinkSync fs filepath))
    (catch js/Error e nil)))

(defn spit [filepath text]
  (.writeFileSync fs filepath text))

(defn slurp [filepath]
  (.toString (.readFileSync fs filepath)))

(defn download [url filepath]
  (if-not (path-exists? filepath)
          (io/exec "curl --create-dirs -fsSo" filepath url)))
