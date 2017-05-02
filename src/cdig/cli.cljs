(ns cdig.cli
  (:require
   [cdig.io :as io]
   [cdig.svga :as svga]
   [clojure.string :refer [join]]))

(declare commands)

(defn print-affirmation
  "Acknowledge that the CLI is working and we have an internet connection"
  []
  (dorun (map (comp println (partial io/color :green "  ") :Content)
              (:Subtitles (io/slurp-json "https://morbotron.com/api/random")))))

(defn upgrade
  "Upgrade brew and all relevant global npm packages"
  []
  (io/exec "brew upgrade")
  (io/exec "brew prune")
  (io/exec "brew cleanup")
  (io/exec "npm install npm -g")
  (io/exec "npm update -g")
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

(defn help
  "Display a list of available commands"
  []
  (println "\n  Usage: cdig [command]")
  (println "\n  Commands:")
  (dorun (map #(println (str "    "
                             (name (first %))
                             (io/color :blue " - " (second (second %)))))
              commands))
  (println)
  (print-affirmation)
  (println))

(defn -main [task & args]
  (if-let [command (first (get commands (keyword task)))]
    (apply command args)
    (if (nil? task)
      (help)
      (println (str "\"" task "\" is not a valid task")))))

(def commands {:help [help "display this helpful information"]
               :new [new-project "create a new project in this folder"]
               :run [run "compile and run the project in this folder"]
               :upgrade [upgrade "update the cdig tool to the latest verion"]})

(set! *main-cli-fn* -main)
(enable-console-print!)
