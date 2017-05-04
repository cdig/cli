(ns cdig.lbs
  (:require
   [cdig.auth :as auth]
   [cdig.http :as http])
  (:refer-clojure :exclude [get]))

(defn lbs-auth-header [token]
  (str "X-LBS-API-TOKEN: " token))

(defn get [url cb]
  (auth/get-password "api-token" #(http/get url (lbs-auth-header %))))

; (defn post [url data cb]
;   (auth/get-password "api-token" #(http/post url (lbs-auth-header %) data)))
