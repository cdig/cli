(ns cdig.fs
  (:require
   [cdig.io :as io]
   [clojure.string :as str]))

(def del (js/require "del"))
(def fs (js/require "fs"))
(def os (js/require "os"))
(def path (js/require "path"))

(defn readdir [filepath]
  (.readdirSync fs filepath))

(defn dirname [filepath]
  (.dirname path filepath))

(defn basename [filepath]
  (.basename path filepath))

(defn current-dirname []
  (last (str/split (.cwd js/process) path.sep)))

(defn dir? [filepath]
  (.isDirectory (.lstatSync fs filepath)))

(defn path-exists? [filepath]
  (.existsSync fs filepath))

(defn homedir []
  (.homedir os))

(defn touch [filepath]
  (io/exec "touch" filepath))

(defn mkdir [filepath]
  (io/exec "mkdir -p" filepath))

(defn rm [globs]
  (.sync del (clj->js globs)))

(defn spit [filepath text]
  (.writeFileSync fs filepath text))

(defn slurp [filepath]
  (if (path-exists? filepath)
    (.toString (.readFileSync fs filepath))))

(defn download [url filepath]
  (if-not (path-exists? filepath)
          (io/exec "curl --create-dirs -fsSo" filepath url)))
