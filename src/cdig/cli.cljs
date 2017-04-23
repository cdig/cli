(ns cdig.cli
  (:require
   [cdig.io :as io]
   [cdig.svga :as svga]
   [clojure.string :refer [join]]))

(defn print-affirmation
  "Acknowledge that the CLI is working and we have an internet connection"
  []
  (dorun (map (comp println (partial io/color :green) :Content)
              (:Subtitles (io/slurp-json "https://morbotron.com/api/random")))))

(defn upgrade
  "Upgrade brew and all relevant global npm packages"
  []
  (io/exec "brew upgrade")
  (io/exec "npm i -g npm")
  (io/exec "npm i -g gulp-cli cdig/cli")
  (print-affirmation))

(defn new-project
  "Populate the folder with framework files and default source/config files"
  [type]
  (case (keyword type)
        :svga (svga/new-project)
        :cd-module nil
        nil (println "Please specify what sort of project you want to create - eg: cdig new svga")
        (println (str "\"" type "\" is not a valid project type"))))

(defn run
  "Refresh the framework files, fire up a server, and watch for changes"
  [type]
  (case (keyword type)
        :svga (svga/run)
        :cd-module nil
        nil (println "Please specify what sort of project you want to run - eg: cdig run svga")
        (println (str "\"" type "\" is not a valid project type"))))

(defn -main [task & args]
  (case (keyword task)
        nil (print-affirmation)
        :new (apply new-project args)
        :run (apply run args)
        :upgrade (upgrade)
        (println (str "\"" task "\" is not a valid task"))))

(set! *main-cli-fn* -main)
(enable-console-print!)
