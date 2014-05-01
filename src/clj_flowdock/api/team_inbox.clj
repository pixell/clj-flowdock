(ns clj-flowdock.api.team-inbox
  (:require [clj-flowdock.api :as api]))

(def route "v1/messages/team_inbox/")

(defn post [flow-api-token params]
  (api/http-post (str route flow-api-token) params))

(defn flow-api-token [message]
  (let [flow (flow/find "api_token" (clojure.core/get message "flow"))]
    (flow/flow->flow-api-token flow)))