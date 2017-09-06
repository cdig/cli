(ns cdig.io
  (:require
   [clojure.string :refer [join]])
  (:refer-clojure :exclude [print]))

(def syncprompt (js/require "syncprompt"))

; EXEC

(def exec-sync (.-execSync (js/require "child_process")))

(defn exec [& pieces]
  (exec-sync (join " " pieces) (clj->js {:stdio "inherit"})))

(defn exec-quietly [& pieces]
  (exec-sync (join " " pieces) (clj->js {:stdio "ignore"})))

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
  (print :yellow question)
  (dorun (map (fn [[k v]] (println (str "Enter \"" (name k) "\" for " (name v)))) answers))
  (loop [reply (keyword (syncprompt "Answer: "))]
    (or (get answers reply)
        (do (print :red "You suck!")
            (recur (keyword (syncprompt "Better answer: ")))))))
