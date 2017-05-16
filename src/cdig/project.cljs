(ns cdig.project
  (:require
   [cdig.io :as io]
   [cdig.fs :as fs]))

(declare pull)

(def system-files ["bower.json" "cdig.json" "gulpfile.coffee" "package.json"])
(def generated-files [".DS_Store" "bower_components" "deploy" "node_modules" "public" "yarn.lock"])
(def new-project-files {:cd-module ["source/index.kit" "source/pages/ending.kit" "source/pages/title.kit" "source/styles/fonts.scss"]
                        :svga ["source/symbol.coffee" "source/config.coffee"]})

(defn- pull-from-origin [type files]
  (let [base-url (str "https://raw.githubusercontent.com/cdig/" (name type) "-starter/v4/dist/")]
    (dorun (map #(fs/download (str base-url %) %) files))))

(defn clean []
  (fs/rm generated-files)
  (fs/rm system-files))

(defn compile []
  (io/exec "gulp prod"))

(defn new-project [type]
  (let [type-files (get new-project-files type)]
    (fs/mkdir "resources")
    (pull-from-origin type type-files)
    (pull type)))

(defn pull [type]
  (fs/rm system-files)
  (pull-from-origin type system-files)
  (io/exec "yarn")
  (io/exec "bower prune")
  (io/exec "bower update"))

(defn push []
  (let [name (fs/current-dirname)]
    (io/exec "aws s3 sync deploy" (str "s3://lbs-cdn/v4/" name) "--size-only --exclude \".*\" --cache-control max-age=86400,immutable")
    (println)
    (io/print :green "  Successfully deployed:")
    (io/print :blue "    https://lbs-cdn.s3.amazonaws.com/v4/" name "/" name ".min.html")
    (println)))

(defn watch []
  (io/exec "gulp"))
