(ns cdig.cd-module
  (:require
   [cdig.project :as project])
  (:refer-clojure :exclude [update]))

(def source-files ["source/index.kit" "source/pages/ending.kit" "source/pages/title.kit" "source/styles/fonts.scss"])

(defn new-project []
  (project/new-project "cd-module" source-files))

(defn update []
  (project/update "cd-module"))

(defn build []
  (project/update "cd-module")
  (project/compile-prod))

(defn run []
  (project/update "cd-module")
  (project/watch-dev))
