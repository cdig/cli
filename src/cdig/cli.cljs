(ns cdig.cli
  (:require
   [cdig.auth :as auth]
   [cdig.http :as http]
   [cdig.io :as io]
   [cdig.fs :as fs]
   [cdig.lbs :as lbs]
   [cdig.project :as project]
   [cdig.svga :as svga])
  (:refer-clojure :exclude [update]))

(declare -project-type)
(declare commands)

; HELPERS

(defn- print-affirmation
  "Acknowledge that the CLI is working and we have an internet connection"
  []
  (if-let [resp (http/slurp "https://morbotron.com/api/random")]
    (dorun (map (comp println (partial io/color :green "  ") :Content)
                (:Subtitles (io/json->clj resp))))
    (println (io/color :red "  You are without an internet connection! How do you live?"))))

(defn- project-type!
  "Load or prompt for the project type. This function creates state as a side-effect, to avoid redundant loads/prompts."
  []
  (or -project-type
      (def -project-type
        (or (keyword (:type (io/json->clj (fs/slurp "cdig.json"))))
            (io/prompt "What type of project is this?" {:c :cd-module :s :svga})))))

; COMMANDS

(defn cmd-auth
  "Get/set the LBS API token for this user"
  [token]
  (if (nil? token)
    (auth/get-password "api-token" (partial println "Your current token is:"))
    (do
     (auth/set-password "api-token" token)
     (println "Your API token has been saved"))))

(defn cmd-build
  "Compile the project in this folder"
  []
  (case (project-type!)
        :svga (svga/build)
        :cd-module nil))

(defn cmd-clean
  "Delete system and generated files"
  []
  (fs/rm project/generated-files)
  (fs/rm project/system-files))

(defn cmd-deploy
  "Compile the project, then deploy it to LBS"
  []
  (cmd-build)
  (let [name (fs/current-dirname)]
    (io/exec "aws s3 sync public" (str "s3://lbs-cdn/v4/" name) "--size-only --exclude \".*\"")
    (println)
    (io/print :green "  Successfully deployed:")
    (io/print :blue "    https://lbs-cdn.s3.amazonaws.com/v4/" name "/" name ".min.html")
    (println)))

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
  []
  (case (project-type!)
        :svga (svga/new-project)
        :cd-module nil))

(defn cmd-run
  "Refresh the framework files, fire up a server, and watch for changes"
  []
  (case (project-type!)
        :svga (svga/run)
        :cd-module nil))

(defn cmd-update
  "Pull down system files for the project in this folder"
  []
  (case (project-type!)
        :svga (svga/update)
        :cd-module nil))

(defn cmd-upgrade
  "Upgrade brew and all relevant global npm packages"
  []
  (io/exec "brew upgrade")
  (io/exec "brew prune")
  (io/exec "brew cleanup")
  (io/exec "npm install npm -g")
  (io/exec "npm update -g")
  (io/exec "npm prune -g")
  (print-affirmation))

; MAIN

(defn -main [task & args]
  (if-let [command (first (get commands (keyword task)))]
    (apply command args)
    (case task
          nil (cmd-help)
          "--version" (println (.-version (js/require "./package.json")))
          (do
           (println (io/color :red (str "\n  \"" task "\" is not a valid task")))
           (cmd-help)))))

(def commands {:auth [cmd-auth "get/set your LBS API token"]
               :build [cmd-build "update and compile the project in this folder"]
               :clean [cmd-clean "delete the public folder and all system files generated during compilation"]
               :deploy [cmd-deploy "update and compile the project in this folder, then deploy it to LBS"]
               :help [cmd-help "display this helpful information"]
               :new [cmd-new "create a new project in this folder"]
               :run [cmd-run "update, build, serve, and watch the project in this folder"]
               :update [cmd-update "update the system files for the project in this folder"]
               :upgrade [cmd-upgrade "update the cdig tool to the latest verion"]})

(set! *main-cli-fn* -main)
(enable-console-print!)
