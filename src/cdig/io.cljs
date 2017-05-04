(ns cdig.io
  (:require
   [clojure.string :refer [join]]))

; EXEC

(def exec-sync (.-execSync (js/require "child_process")))

(defn exec [& pieces]
  (exec-sync (join " " pieces) (clj->js {:stdio "inherit"})))

; COLOR

(def colors (js/require "colors/safe"))

(defn color [col & texts]
  (let [f (aget colors (name col))]
    (f (apply str texts))))

; JSON

(defn json->clj [text]
  (-> (js/JSON.parse text)
      (js->clj :keywordize-keys true)))

(defn clj->json [text]
  (js/JSON.stringify (clj->js text)))
