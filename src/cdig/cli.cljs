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
  "Set the LBS API token for this user"
  [token]
  (if (nil? token)
    (io/get-password "api-token" (partial println "Your current token is:"))
    (do
     (io/set-password "api-token" token)
     (println "Your API token has been saved"))))

(defn cmd-deploy
  "Compile the project, then deploy it to LBS"
  []
  (io/get-password
   "api-token"
   (fn [token]
     (io/curl-post {:api_token token} "http://www.lbs.dev/api/artifacts"))))

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
    (case task
          nil (cmd-help)
          "--version" (println (.-version (js/require "./package.json")))
          (println (str "\"" task "\" is not a valid task")))))

(def commands {:auth [cmd-auth "set your LBS API token so that we can issue secure requests"]
               :deploy [cmd-deploy "deploy the project in this folder to LBS"]
               :help [cmd-help "display this helpful information"]
               :new [cmd-new "create a new project in this folder"]
               :run [cmd-run "compile and run the project in this folder"]
               :upgrade [cmd-upgrade "update the cdig tool to the latest verion"]})

(set! *main-cli-fn* -main)
(enable-console-print!)
