(ns cdig.io)

(def fs (js/require "fs"))
(def exec-sync (.-execSync (js/require "child_process")))

(defn dir? [path]
  (.isDirectory (.lstatSync fs path)))

(defn path-exists? [path]
  (.existsSync fs path))

(defn rm [path]
  (try
    (if (dir? path)
      (.rmdirSync fs path)
      (.unlinkSync fs path))
    (catch js/Error e nil)))

(defn spit [path text]
  (.writeFileSync fs path text))

(defn spit-json [path text]
  (spit path (js/JSON.stringify (clj->js text))))

(defn slurp [path]
  (.toString (.readFileSync fs path)))

(defn slurp-json [path]
  (when-let [text (slurp path)]
    (-> (js/JSON.parse text)
        (js->clj :keywordize-keys true))))

(defn download [url path]
  (if-not (path-exists? path)
          (exec-sync (str "curl --create-dirs -fsSo " path " " url))))

(defn touch [path]
  (exec-sync (str "touch " path)))

(defn mkdir [path]
  (exec-sync (str "mkdir -p " path)))
