(ns cdig.svga
  (:require
   [cdig.io :as io]))

(def source-url "https://raw.githubusercontent.com/cdig/svga-starter/v4/dist/")
(def build-files ["cdig.json" "gulpfile.coffee" "package.json" "source/config.coffee"])
(def starter-files ["source/symbol.coffee"])

(defn pull-files-from-source [files]
  (dorun (map #(io/curl (str source-url %) %) files))
  (io/exec "yarn"))

(defn new-project
  []
  (if (io/path-exists? "cdig.json")
    (println "This folder already contains an SVGA project")
    (do
     (println (str "Generating an SVGA"))
     (io/mkdir "resources")
     (pull-files-from-source starter-files)
     (pull-files-from-source build-files))))

(defn run
  []
  (println "Updating...")
  (io/rm "node_modules")
  (pull-files-from-source build-files)
  (io/exec "gulp"))
