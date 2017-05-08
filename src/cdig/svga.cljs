(ns cdig.svga
  (:require
   [cdig.project :as project])
  (:refer-clojure :exclude [update]))

(def source-files ["source/symbol.coffee" "source/config.coffee"])

(defn new-project []
  (project/new-project "svga" source-files))

(defn update []
  (project/update "svga"))

(defn build []
  (project/update "svga")
  (project/compile-prod))

(defn run []
  (project/update "svga")
  (project/watch-dev))
