(ns cdig.project
  (:require
   [cdig.svga :as svga]
   [cdig.io :as io]))

(defn get-v4-type []
  (keyword (get (io/slurp-json "cdig.json") :type)))

(defn get-v3-type []
  (keyword (get (io/slurp-json "package.json") :name)))

(defn new-project
  "Populate the folder with framework files and default source/config files"
  [type]
  (case (keyword type)
        :svga (svga/new-project)
        :cd-module nil
        nil (println "Please specify what sort of project you want to create - eg: cdig new svga")
        (println (str "\"" type "\" is not a valid project type"))))

(defn pull
  "Pull down the latest framework files"
  []
  (case (get-v4-type)
        :svga (svga/pull)
        :cd-module nil
        (println "This doesn't appear to be a v4 project folder")))

(defn work
  "Fire up watchers and servers"
  []
  (case (get-v4-type)
        :svga (svga/work)
        :cd-module nil
        (println "This doesn't appear to be a v4 project folder")))

(defn upgrade-v3
  "Upgrade a v3 project to v4"
  []
  (case (get-v3-type)
        :svga (svga/v3->v4)
        :cd-module nil
        (println "This doesn't appear to be a v3 project folder")))
