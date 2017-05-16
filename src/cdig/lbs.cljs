(ns cdig.lbs
  (:require
   [cdig.auth :as auth]
   [cdig.http :as http]
   [cdig.io :as io]
   [cdig.project :as project])
  (:refer-clojure :exclude [get]))

(def auth-header "X-LBS-API-TOKEN")

(defn get [url cb]
  (auth/get-password
   "api-token"
   (fn [token]
     (cb (http/slurp url {auth-header token})))))

(defn post [url data cb]
  (auth/get-password
   "api-token"
   (fn [token]
     (cb (http/spit url data {auth-header token})))))

(defn register [cb]
  (io/print :yellow "Registering with LBS...")
  (let [project (str (project/project-name))
        index (project/index-name)
        source (str "v4/" project "/" index)]
    (post "https://www.lunchboxsessions.com/api/artifacts/new" {:name project :source source} cb)))
