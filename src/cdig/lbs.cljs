(ns cdig.lbs
  (:require
   [cdig.auth :as auth]
   [cdig.http :as http]
   [cdig.io :as io]
   [cdig.project :as project])
  (:refer-clojure :exclude [get]))

(def auth-header "X-LBS-API-TOKEN")
(def lbs-prod-domain "https://www.lunchboxsessions.com")
(def lbs-dev-domain "http://www.lbs.dev")
(def lbs-domain lbs-prod-domain)

(defn- get [url cb]
  (auth/get-password
   "api-token"
   (fn [token]
     (cb (http/slurp url {auth-header token})))))

(defn- post [url data cb]
  (auth/get-password
   "api-token"
   (fn [token]
     (cb (http/spit url data {auth-header token})))))

(defn- register-done [res]
  (if (http/url? res)
   (io/exec "open" res)
   (io/print :red res)))

(defn register []
  (let [era "v4"
        project (str (project/project-name))
        index (project/index-name)]
    (post (str lbs-domain "/api/artifacts/new")
          {:era era :name project :source index}
          register-done)))
