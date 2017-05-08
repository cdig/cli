(ns cdig.project
  (:require
   [cdig.io :as io]
   [cdig.fs :as fs])
  (:refer-clojure :exclude [update]))

(def system-files ["bower.json" "cdig.json" "gulpfile.coffee" "package.json"])
(def generated-files [".DS_Store" "bower_components" "node_modules" "public" "yarn.lock"])

(defn pull-from-origin [type files]
  (let [origin-url (str "https://raw.githubusercontent.com/cdig/" type "-starter/v4/dist/")]
    (dorun (map #(fs/download (str origin-url %) %) files))))

(defn- clear-and-pull-from-origin [type files]
  (fs/rm files)
  (pull-from-origin type files))

(defn update [type]
  (io/print :yellow "Pulling fresh system files from origin...")
  (clear-and-pull-from-origin type system-files)
  (io/exec "yarn")
  (io/exec "bower update"))

(defn new-project [type source-files]
  (if (fs/path-exists? "source")
    (io/print :yellow "This folder already contains a project")
    (do
     (io/print :yellow (str "Creating a new " type " project..."))
     (fs/mkdir "resources")
     (clear-and-pull-from-origin type source-files)
     (update type))))

(defn compile-prod []
  (io/print :yellow "Building deployable version...")
  (io/exec "gulp prod"))

(defn watch-dev []
  (io/print :yellow "Running development version...")
  (io/exec "gulp"))
