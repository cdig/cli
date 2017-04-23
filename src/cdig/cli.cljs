(ns cdig.cli
  (:require
   [cdig.io :as io]
   [cdig.project :as project]
   [cdig.svga :as svga]
   [clojure.string :refer [join]]))

(defn print-affirmation
  "Acknowledge that the CLI is working and we have an internet connection"
  []
  (dorun (map (comp println :Content)
              (:Subtitles (io/slurp-json "https://morbotron.com/api/random")))))

(defn upgrade-global
  "Upgrade brew and all relevant global npm packages"
  []
  (io/exec "brew upgrade")
  (io/exec "npm i -g npm")
  (io/exec "npm i -g gulp cdig/cdig-cli")
  (print-affirmation))

(defn upgrade
  "Upgrade the CLI or the current project"
  [target]
  (case (keyword target)
        nil (upgrade-global)
        :v3 (project/upgrade-v3)))

(defn -main [task & args]
  (case (keyword task)
        nil (print-affirmation)
        :new (apply project/new-project args)
        :refresh (apply project/refresh args)
        :upgrade (apply upgrade args)
        :work (apply project/work args)
        (println (str "\"" task "\" is not a valid task"))))

(set! *main-cli-fn* -main)
(enable-console-print!)
