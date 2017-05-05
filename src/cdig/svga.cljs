(ns cdig.svga
  (:require
   [cdig.io :as io]
   [cdig.fs :as fs]
   [cdig.project :as project]))

(def origin-url "https://raw.githubusercontent.com/cdig/svga-starter/v4/dist/")
(def source-files ["source/symbol.coffee" "source/config.coffee"])

(defn- pull-from-origin
  [files]
  (dorun (map #(fs/download (str origin-url %) %) files)))

(defn build
  []
  (io/exec "gulp prod"))

(defn new-project
  []
  (if (fs/path-exists? "cdig.json")
    (println "This folder already contains an SVGA project")
    (do
     (println (str "Generating an SVGA"))
     (fs/mkdir "resources")
     (pull-from-origin source-files)
     (pull-from-origin project/system-files)
     (io/exec "yarn")
     (io/exec "bower install"))))

(defn run
  []
  (println "Updating...")
  (pull-from-origin project/system-files)
  (io/exec "yarn upgrade")
  (io/exec "bower update")
  (io/exec "gulp"))
