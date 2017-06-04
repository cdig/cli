(ns cdig.cli
  (:require
   [cdig.auth :as auth]
   [cdig.http :as http]
   [cdig.io :as io]
   [cdig.fs :as fs]
   [cdig.lbs :as lbs]
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
            (if (fs/path-exists? "source/config.coffee") :svga)
            (if (fs/path-exists? "source/index.kit") :cd-module)
            (io/prompt "What type of project is this?" {:m :cd-module :s :svga})))))

(defn- is-deployable []
  (and (project/project-name) (project/index-name)))

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
  (io/exec "npm i -g npm")
  (io/exec "npm update -g")
  (io/exec "npm prune -g")
  (io/exec "exec bash -l")
  (print-affirmation))

; COMMANDS: PROJECT

(defn cmd-clean []
  (io/print :yellow "Flushing the trash off...")
  (project/clean))

(defn cmd-compile []
  (io/print :yellow "Compiling deployable build...")
  (project/compile (project-type!)))

(defn cmd-new []
  (if (fs/path-exists? "source")
    (io/print :red "This folder already contains a project")
    (let [type (project-type!)]
      (io/print :yellow (str "Creating a new " (name type) " project..."))
      (project/new-project type))))

(defn cmd-pull [fast]
  (io/print :yellow "Pulling fresh system files...")
  (project/pull (project-type!) fast))

(defn cmd-push []
  (io/print :yellow "Pushing to S3...")
  (if (is-deployable)
    (project/push)
    (io/print :red "No deploy folder - please do a production build first, or run: cdig deploy")))

(defn cmd-register []
  (io/print :yellow "Registering with LBS...")
  (if (is-deployable)
    (lbs/register)
    (io/print :red "No deploy folder - please do a production build first, or run: cdig deploy")))

(defn cmd-watch []
  (io/print :yellow "Running development process... (press control-c to stop)")
  (project/watch (project-type!)))

; COMMANDS: SHORTCUTS

(defn cmd-deploy []
  (cmd-pull true)
  (cmd-compile)
  (cmd-push)
  (cmd-register))

(defn cmd-run []
  (cmd-pull true)
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
               :auth [cmd-auth "    Get/set your LBS API token"]
               :help [cmd-help "    Display this helpful information"]
               :upgrade [cmd-upgrade " Upgrade all command line utilities"]
               
               ; Project
               :clean [cmd-clean "   Delete all the auto-generated stuff in this folder"]
               :compile [cmd-compile " Make a deployable build of the project in this folder"]
               :new [cmd-new "     Create a new project in this folder"]
               :pull [cmd-pull "    Download fresh system files for the project in this folder"]
               :push [cmd-push "    Upload items from the deploy folder to S3"]
               :register [cmd-register "Tell LBS about the project in this folder"]
               :watch [cmd-watch "   Continually make & serve a development build of the project in this folder"]
               
               ; Shortcuts
               :deploy [cmd-deploy "  Shortcut: pull + compile + push + register"]
               :run [cmd-run "     Shortcut: pull + watch"]})

(set! *main-cli-fn* -main)
(enable-console-print!)
