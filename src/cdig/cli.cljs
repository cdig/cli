(ns cdig.cli
  (:require
   [cdig.auth :as auth]
   [cdig.http :as http]
   [cdig.io :as io]
   [cdig.fs :as fs]
   [cdig.project :as project]))

(declare !project-type)
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
  "Get or prompt for the project type. This function is stateful to avoid redundant prompts."
  []
  (or !project-type
      (def !project-type
        (or (keyword (:type (io/json->clj (fs/slurp "cdig.json"))))
            (io/prompt "What type of project is this?" {:m :cd-module :s :svga})))))

; COMMANDS: TOOL

(defn cmd-auth
  "Get/set the LBS API token for this user"
  [token]
  (if (nil? token)
    (auth/get-password "api-token" (partial println "Your current token is:"))
    (do
     (auth/set-password "api-token" token)
     (io/print :green "Your API token has been saved"))))

(defn cmd-help
  "Display a list of available commands"
  []
  (println "\n  Usage: cdig [command]")
  (println "\n  Commands:")
  (dorun (map (fn [[key [_ description]]]
                (println "   " (name key) (io/color :blue "  " description)))
              (sort commands)))
  (println)
  (print-affirmation)
  (println))

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


; COMMANDS: PROJECT

(defn cmd-clean []
  (project/clean))

(defn cmd-compile []
  (project/compile))

(defn cmd-new []
  (project/new-project (project-type!)))

(defn cmd-pull []
  (project/pull (project-type!)))

(defn cmd-push []
  (project/push))

(defn cmd-watch []
  (project/watch))

; COMMANDS: SHORTCUTS

(defn cmd-deploy []
  (cmd-pull)
  (cmd-compile)
  (cmd-push))

(defn cmd-run []
  (cmd-pull)
  (cmd-watch))

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

(def commands {
               ; Tool
               :auth [cmd-auth "   Get/set your LBS API token"]
               :help [cmd-help "   Display this helpful information"]
               :upgrade [cmd-upgrade "Upgrade all command line utilities"]
               
               ; Project
               :clean [cmd-clean "  Delete the public folder and all system files generated during compilation"]
               :compile [cmd-compile "Make a deployable build of the project in this folder"]
               :new [cmd-new "    Create a new project in this folder"]
               :pull [cmd-pull "   Download fresh system files for the project in this folder"]
               :push [cmd-push "   Upload items from the public folder to S3 (NB: the current folder name is used as the project slug)"]
               :watch [cmd-watch "  Continually make & serve a development build of the project in this folder"]
               
               ; Shortcuts
               :deploy [cmd-deploy " Shortcut: pull + compile + push"]
               :run [cmd-run "    Shortcut: pull + watch"]})

(set! *main-cli-fn* -main)
(enable-console-print!)
