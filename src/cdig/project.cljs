(ns cdig.project
  (:require
   [cdig.fs :as fs]
   [cdig.io :as io]))

(declare pull)

(def system-files ["bower.json" "cdig.json" "gulpfile.coffee" "package.json"])
(def generated-files [".DS_Store" "bower_components" "deploy" "node_modules" "public" "yarn.lock"])
(def new-project-files {:cd-module ["source/index.kit" "source/pages/ending.kit" "source/pages/title.kit" "source/styles/fonts.scss"]
                        :svga ["source/symbol.coffee" "source/config.coffee"]})

(defn- pull-from-origin [type files]
  (let [base-url (str "https://raw.githubusercontent.com/cdig/" (name type) "-starter/v4/dist/")]
    (dorun (map #(fs/download (str base-url %) %) files))))

(defn project-name []
  (fs/current-dirname))

(defn index-name []
  (if (fs/dir? ".deploy")
    (fs/basename (first (fs/readdir ".deploy")))))

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
  (io/print :yellow "Pushing to S3...")
  (let [project (project-name)
        index (index-name)]
    (io/exec "aws s3 sync deploy s3://lbs-cdn/v4/ --size-only --exclude \".*\" --cache-control max-age=86400,immutable")
    (println)
    (io/print :green "  Successfully pushed:")
    (io/print :blue "    v4/" project "/" index)
    (println)))

(defn watch []
  (io/exec "gulp"))
