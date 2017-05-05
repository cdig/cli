(ns cdig.cli
  (:require
   [cdig.auth :as auth]
   [cdig.http :as http]
   [cdig.io :as io]
   [cdig.fs :as fs]
   [cdig.lbs :as lbs]
   [cdig.s3 :as s3]
   [cdig.svga :as svga]))

(declare commands)

(defn- print-affirmation
  "Acknowledge that the CLI is working and we have an internet connection"
  []
  (if-let [resp (http/slurp "https://morbotron.com/api/random")]
    (dorun (map (comp println (partial io/color :green "  ") :Content)
                (:Subtitles (io/json->clj resp))))
    (println (io/color :red "  You are without an internet connection! How do you live?"))))

(defn cmd-auth
  "Get/set the LBS API token for this user"
  [token]
  (if (nil? token)
    (auth/get-password "api-token" (partial println "Your current token is:"))
    (do
     (auth/set-password "api-token" token)
     (println "Your API token has been saved"))))

(defn- validate-deploy []
  (let [valid (fs/path-exists? "cdig.json")]
    (if-not valid (io/print :red "This is not a valid v4 project folder."))
    valid))

(defn cmd-deploy
  "Compile the project, then deploy it to LBS"
  []
  ; (when (validate-deploy)
  (let [project-name (fs/parent-dirname ".")]
    (println project-name)))
; (io/exec "aws s3 sync public" (str "s3://lbs-cdn/v4/" name) "--size-only --exclude .*")))
; (lbs/get "http://www.lbs.dev/api/artifacts/new"
;          (fn [presigned-post-url]
;            (println presigned-post-url)))))

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
          (do
           (println (io/color :red (str "\n  \"" task "\" is not a valid task")))
           (cmd-help)))))

(def commands {:auth [cmd-auth "get/set your LBS API token"]
               :deploy [cmd-deploy "deploy the project in this folder to LBS"]
               :help [cmd-help "display this helpful information"]
               :new [cmd-new "create a new project in this folder"]
               :run [cmd-run "compile and run the project in this folder"]
               :upgrade [cmd-upgrade "update the cdig tool to the latest verion"]})

(set! *main-cli-fn* -main)
(enable-console-print!)
