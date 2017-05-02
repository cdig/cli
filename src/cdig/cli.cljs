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

(defn cmd-auth
  "Set the LBS deploy token for this user"
  [token]
  (if (nil? token)
    (io/get-password "deploy-token" (partial println "Your current token is:"))
    (do
     (io/set-password "deploy-token" token)
     (println "Your new token is:" token))))

(defn cmd-help
  "Display a list of available commands"
  []
  (println "\n  Usage: cdig [command]")
  (println "\n  Commands:")
  (dorun (map #(println "   "
                        (name (first %))
                        (io/color :blue "- " (second (second %))))
              commands))
  (println)
  (print-affirmation)
  (println))

(defn cmd-new
  "Populate the folder with framework files and default source/config files"
  [type]
  (case (keyword type)
        :svga (svga/new-project)
        :cd-module nil
        nil (println "Please specify what sort of project you want to create - eg: cdig new svga")
        (println (str "\"" type "\" is not a valid project type"))))

(defn cmd-run
  "Refresh the framework files, fire up a server, and watch for changes"
  [type]
  (case (keyword type)
        :svga (svga/run)
        :cd-module nil
        nil (println "Please specify what sort of project you want to run - eg: cdig run svga")
        (println (str "\"" type "\" is not a valid project type"))))

(defn cmd-upgrade
  "Upgrade brew and all relevant global npm packages"
  []
  (io/exec "brew upgrade")
  (io/exec "brew prune")
  (io/exec "brew cleanup")
  (io/exec "npm install npm -g")
  (io/exec "npm update -g")
  (print-affirmation))

(defn -main [task & args]
  (if-let [command (first (get commands (keyword task)))]
    (apply command args)
    (if (nil? task)
      (cmd-help)
      (println (str "\"" task "\" is not a valid task")))))

(def commands {:auth [cmd-auth "set your LBS deploy token"]
               :help [cmd-help "display this helpful information"]
               :new [cmd-new "create a new project in this folder"]
               :run [cmd-run "compile and run the project in this folder"]
               :upgrade [cmd-upgrade "update the cdig tool to the latest verion"]})

(set! *main-cli-fn* -main)
(enable-console-print!)
