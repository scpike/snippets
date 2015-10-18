(ns snippets.gists
    (:require [reagent.core :as reagent :refer [atom]]
              [ajax.core :refer [GET POST]]
              [snippets.components :as c])
    (:import goog.History))

(def gists (atom {}))

(defn parse-gist-key
  [token-or-url]
  (if-let [m (re-find #".*/(.*)" token-or-url)]
    (last m)
    token-or-url))

(defn gist-key-to-url [gist-key]
  (let [prefix "https://api.github.com"
        token (parse-gist-key gist-key)]
    (str prefix "/gists/" token)))

(defn load-gist
  [gist-key]
  (let [url (gist-key-to-url gist-key)]
    (GET url
         {:handler #(do
                      (swap! gists assoc gist-key %))
          :response-format :json :keywords? true
          :error-handler #(js/alert "Could not fetch gist")}))

  (defn gist-contents
    [{description :description url :html_url files :files {user :login} :owner}]
    (let [{:keys [filename content]} (first (vals files))]
      {:filename filename
       :description description
       :code content
       :owner user
       :url url})))

(defn show-page [gist-key]
  (load-gist gist-key)
  (fn []
    (if-let [gist (get @gists gist-key)]
      (let [{:keys [filename owner code] :as snippet} (gist-contents gist)]
        [:div
         [:h2 filename " by " owner]
         [:p
          (if-let [desc (:description snippet)] (str desc " "))
          [:a {:href (:url snippet) :target "_blank"} "Github"]]
         [:div.main [c/snippet-widget snippet]]
         [:h3 "Code"]
         [:pre code]])
      [:div "Loading " gist-key])))
