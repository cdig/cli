(ns cdig.project
  (:require
   [cdig.io :as io]
   [cdig.fs :as fs]))

(declare pull)

(def system-files ["bower.json" "cdig.json" "gulpfile.coffee" "package.json"])
(def generated-files [".DS_Store" "bower_components" "node_modules" "public" "yarn.lock"])
(def new-project-files {:cd-module ["source/index.kit" "source/pages/ending.kit" "source/pages/title.kit" "source/styles/fonts.scss"]
                        :svga ["source/symbol.coffee" "source/config.coffee"]})

(defn- pull-from-origin [type files]
  (let [base-url (str "https://raw.githubusercontent.com/cdig/" (name type) "-starter/v4/dist/")]
    (dorun (map #(fs/download (str base-url %) %) files))))

;;;

(defn clean []
  (fs/rm generated-files)
  (fs/rm system-files))

(defn new-project [type]
  (if (fs/path-exists? "source")
    (io/print :red "This folder already contains a project")
    (let [type-files (get new-project-files type)]
      (io/print :yellow (str "Creating a new " (name type) " project..."))
      (fs/mkdir "resources")
      (pull-from-origin type type-files)
      (pull type))))

(defn pull [type]
  (io/print :yellow "Pulling fresh system files...")
  (fs/rm system-files)
  (pull-from-origin type system-files)
  (io/exec "yarn")
  (io/exec "bower update")
  (io/exec "bower prune"))

(defn push []
  (io/print :yellow "Pushing to S3...")
  (let [name (fs/current-dirname)]
    (io/exec "aws s3 sync public" (str "s3://lbs-cdn/v4/" name) "--size-only --exclude \".*\" --cache-control max-age=86400,immutable")
    (println)
    (io/print :green "  Successfully deployed:")
    (io/print :blue "    https://lbs-cdn.s3.amazonaws.com/v4/" name "/" name ".min.html")
    (println)))


;;;

(defn compile []
  (io/print :yellow "Compiling deployable build...")
  (io/exec "gulp prod"))

(defn watch []
  (io/print :yellow "Running development process... (press control-c to stop)")
  (io/exec "gulp"))
