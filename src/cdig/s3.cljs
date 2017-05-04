(ns cdig.s3
  (:require
   [cdig.io :as io]))

(def aws (js/require "aws-sdk"))
(def s3 (aws.S3.))

(def bucketName "lbs-cdn")
(def keyName "hello_world.txt")
(def params {:Bucket bucketName :Key keyName :Body "Hello World!"})

(defn handle
  [err, data]
  (println (or
            err
            (str "Successfully uploaded data to " bucketName "/" keyName))))

(defn put []
  (.putObject s3 (clj->js params) handle))
