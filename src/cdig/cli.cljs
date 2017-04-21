(ns cdig.cli
  (:require
   [cdig.svga :as svga]
   [cdig.io :as io]))

(defn get-project-type []
  (keyword (get (io/slurp-json "cdig.json") :type)))

(defn get-v3-project-type []
  (keyword (get (io/slurp-json "package.json") :name)))

(defn new-project
  "Populate the folder with framework files and default source/config files"
  [type & args]
  (case (keyword type)
        :svga (apply svga/new-project args)
        :cd-module-project nil
        nil (println "Please specify what sort of project you want to create - eg: cdig new svga")
        (println (str "\"" type "\" is not a valid project type"))))

(defn update-project
  "Pull down the latest framework files"
  [& args]
  (case (get-project-type)
        :svga (apply svga/update-project args)
        :cd-module nil
        (println "This doesn't appear to be a v4 project folder")))

(defn upgrade-project
  "Upgrade a v3 project to v4"
  [& args]
  (case (get-v3-project-type)
        :svga-project (apply svga/v3->v4 args)
        :cd-module-project nil
        (println "This doesn't appear to be a v3 project folder")))

(defn -main [task & args]
  (case (keyword task)
        :new (apply new-project args)
        :update (apply update-project args)
        :upgrade (apply upgrade-project args)
        nil (println "Please specify a command to run - eg: cdig update")
        (println (str "\"" task "\" is not a valid task"))))

(set! *main-cli-fn* -main)
(enable-console-print!)
