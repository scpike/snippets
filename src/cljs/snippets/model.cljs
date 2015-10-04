(ns snippets.model
    (:require [cognitect.transit :as t]
              [reagent.core :as reagent]
              [ajax.core :refer [GET POST PUT DELETE]]))

(defonce snippets (reagent/atom {}))

(defn fetch
  []
  (GET "/snippets"
       {:handler #(do
                    (println % )
                    (reset! snippets %))
        :response-format :json :keywords? true
        :error-handler #(js/alert "Could not fetch snippets")}))

(defn find-by-slug
  [slug]
  (first (filter #(= slug (:slug %)) @snippets)))

(defn generic-warning
  []
  (js/alert "Something went wrong"))

(defn save
  [{:keys [id] :as snippet} cb]
  (PUT (str "/snippets/" id)
          {:error-handler generic-warning
           :handler cb
           :params snippet
           :format :raw}))

(defn delete
  [{:keys [id] :as snippet} cb]
  (DELETE (str "/snippets/" id)
          {:error-handler generic-warning
           :handler cb}))

(defn create
  [snippet cb]
  (POST "/snippets"
        {:error-handler #(js/alert "Saving failed")
         :handler #(do (fetch) (cb))
         :format :raw
         :params snippet}))
