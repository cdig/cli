(ns cdig.auth)

(def keytar (js/require "keytar"))

(defn get-password [name cb]
  (-> (.getPassword keytar "com.lunchboxsessions.cli" name)
      (.then cb)))

(defn set-password [name password]
  (.setPassword keytar "com.lunchboxsessions.cli" name password))
