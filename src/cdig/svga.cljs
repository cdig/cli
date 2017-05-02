(ns cdig.svga
  (:require
   [cdig.io :as io]))

(def origin-url "https://raw.githubusercontent.com/cdig/svga-starter/v4/dist/")
(def system-files ["bower.json" "cdig.json" "gulpfile.coffee" "package.json"])
(def source-files ["source/symbol.coffee" "source/config.coffee"])

(defn pull-from-origin
  [files]
  (dorun (map #(io/curl (str origin-url %) %) files)))

(defn new-project
  []
  (if (io/path-exists? "cdig.json")
    (println "This folder already contains an SVGA project")
    (do
     (println (str "Generating an SVGA"))
     (io/mkdir "resources")
     (pull-from-origin source-files)
     (pull-from-origin system-files)
     (io/exec "yarn")
     (io/exec "bower install"))))

(defn run
  []
  (println "Updating...")
  (pull-from-origin system-files)
  (io/exec "yarn upgrade")
  (io/exec "bower update")
  (io/exec "gulp"))
