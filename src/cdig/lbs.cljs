(ns cdig.lbs
  (:require
   [cdig.auth :as auth]
   [cdig.http :as http])
  (:refer-clojure :exclude [get]))

(def auth-header "X-LBS-API-TOKEN")

(defn get [url cb]
  (auth/get-password
   "api-token"
   (fn [token]
     (cb (http/slurp url {auth-header token})))))
