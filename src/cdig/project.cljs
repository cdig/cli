(ns cdig.project
  (:require
   [cdig.fs :as fs]))

(def system-files ["bower.json" "cdig.json" "gulpfile.coffee" "package.json"])
(def generated-files ["bower_components" "node_modules" "public" "yarn.lock"])

(defn pull-from-origin [origin-url files]
  (dorun (map #(fs/download (str origin-url %) %) files)))
