(ns clj-flowdock.streaming
  (:require [clj-flowdock.api :as api]
            [clj-http.client :as client]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.tools.logging :as log])
  (:refer-clojure :exclude [read]))

(defprotocol Connection
  (user-id [this] "Return identifier for user who opened the connection")
  (read [this] "Read from connection, returning a json map")
  (close [this] "Close connection"))

(deftype FlowConnection [flow-ids user-id reader]
  Connection
  (user-id [this]
    user-id)
  (read [this]
    (json/parse-string (.readLine reader)))
  (close [this]
    (.close reader)))

(defn- encode-url-params [params]
  (let [encode #(java.net.URLEncoder/encode (str %) "UTF-8")
        coded-params (for [[k v] params] (str k "=" v))]
    (apply str (interpose "&" coded-params))))

(defn- streaming-url [private flow-ids]
  (let [url
    (str "https://stream.flowdock.com/flows/?"
      (encode-url-params
        (merge {"active" "true"}
               (when-let [_ private] {"user" "1"})
               (when-let [ids (not-empty flow-ids)] {"filter" (reduce #(str %1 "," %2) ids)}))))]
    (log/info "url:" url)
    url))

(defn open
  ([] (open true ()))
  ([private & flow-ids]
    (let [response (client/get (streaming-url private flow-ids) {:as :stream :basic-auth api/basic-auth-token})]
      (log/info "Streaming messages from:" flow-ids)
      (FlowConnection. flow-ids (get-in response [:headers "flowdock-user"]) (io/reader (:body response))))))
