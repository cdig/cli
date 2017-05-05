(ns cdig.io
  (:require
   [clojure.string :refer [join]])
  (:refer-clojure :exclude [print]))

(def syncprompt (js/require "syncprompt"))

; EXEC

(def exec-sync (.-execSync (js/require "child_process")))

(defn exec [& pieces]
  (exec-sync (join " " pieces) (clj->js {:stdio "inherit"})))

; COLOR

(def colors (js/require "colors/safe"))

(defn color [col & texts]
  (let [f (aget colors (name col))]
    (f (apply str texts))))

; PRINTING

(defn print [c & texts]
  (println (apply color c texts)))

; JSON

(defn json->clj [text]
  (-> (js/JSON.parse text)
      (js->clj :keywordize-keys true)))

(defn clj->json [text]
  (js/JSON.stringify (clj->js text)))

; PROMPT

(defn prompt [question answers]
  (loop [reply (keyword (syncprompt question))]
    (if-let [result (get reply answers)]
      result
      (recur (keyword (syncprompt question))))))
      
