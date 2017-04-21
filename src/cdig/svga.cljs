(ns cdig.svga
  (:require
   [cdig.io :as io]))

(def source-url "https://raw.githubusercontent.com/cdig/svga-starter/v3/dist/")
(def source-files ["gulpfile.coffee" "package.json" "source/config.coffee" "source/symbol.coffee"])
(def cdig-json {:type :svga
                :version 4})

(defn new-project
  [project-name]
  (cond
    (nil? project-name) (println "Please provide a project name: cdig new svga laser-cats")
    (io/path-exists? project-name) (println (str "There is already an SVGA named \"" project-name "\" here"))
    :else (do
           (println (str "Generating an SVGA named \"" project-name "\""))
           (io/mkdir project-name)
           (dorun (map #(io/download (str source-url %) (str project-name "/" %)) source-files))
           (io/mkdir (str project-name "/resources"))
           (io/spit-json (str project-name "/cdig.json") cdig-json))))

(defn update-project
  []
  (io/rm "node_modules")
  (dorun (map #(io/download (str source-url %) %) source-files)))
