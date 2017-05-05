(ns cdig.svga
  (:require
   [cdig.io :as io]
   [cdig.fs :as fs]
   [cdig.project :as project])
  (:refer-clojure :exclude [update]))

(def origin-url "https://raw.githubusercontent.com/cdig/svga-starter/v4/dist/")
(def source-files ["source/symbol.coffee" "source/config.coffee"])

(defn- pull-from-origin [files]
  (fs/rm files)
  (project/pull-from-origin origin-url files))

(defn new-project []
  (if (fs/path-exists? "source")
    (io/print :yellow "This folder already contains an SVGA project")
    (do
     (io/print :yellow "Generating an SVGA...")
     (fs/mkdir "resources")
     (pull-from-origin source-files)
     (pull-from-origin project/system-files)
     (io/exec "yarn")
     (io/exec "bower install"))))

(defn update []
  (io/print :yellow "Pulling fresh system files from origin...")
  (pull-from-origin project/system-files)
  (io/exec "yarn install")
  (io/exec "bower update"))

(defn build []
  (update)
  (io/print :yellow "Building deployable version...")
  (io/exec "gulp prod"))

(defn run []
  (update)
  (io/print :yellow "Running development version...")
  (io/exec "gulp"))
