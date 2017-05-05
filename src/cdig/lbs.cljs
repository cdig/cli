(ns cdig.lbs
  (:require
   [cdig.auth :as auth]
   [cdig.http :as http]
   [cdig.io :as io])
  (:refer-clojure :exclude [get]))

(def auth-header "X-LBS-API-TOKEN")

(defn get [url cb]
  (auth/get-password "api-token"
                     #(cb (:url (io/json->clj (http/slurp url {auth-header %}))))))

; (defn post [url data cb]
;   (auth/get-password "api-token" #(http/post url {auth-header %} data)))
