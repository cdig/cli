(ns cdig.cli
  (:require
   [cdig.svga :as svga]
   [cdig.io :as io]))

(defn get-project-type []
  (get :type (io/slurp-json "cdig.json")))

(defn new-project
  "Create a new project project folder and fill it with default files"
  [type & args]
  (case type
        nil (println "Please specify what sort of project you want to create: cdig new svga")
        "svga" (apply svga/new-project args)
        (println (str "\"" type "\" is not a valid project type."))))

(defn update-project
  "Update an existing project in the current folder"
  [& args]
  (case (get-project-type)
        :svga (apply svga/update-project args)
        (println "This doesn't appear to be a valid project folder.")))

(defn -main [task & args]
  (case (keyword task)
        nil (println "Please specify a command to run: cdig update")
        :new (apply new-project args)
        :update (apply update-project args)
        (println (str "\"" task "\" is not a valid task."))))

(set! *main-cli-fn* -main)
(enable-console-print!)
