(ns snippets.dedupe
    (:require [reagent.core :as reagent :refer [atom]]
              [ajax.core :refer [PUT]]
              [snippets.components :as c])
    (:import goog.History))

(def input (atom ""))
(def output (atom ""))
(defn send-dedupe
  []
  (reset! output "Running...")
  (PUT "/dedupe"
       {:handler #(reset! output (:results %))
        :error-handler #(js/alert "Could not reach server")
        :response-format :json
        :keywords? true
        :format :json
        :params {:lines (str (.-value (.getElementById js/document "data-input")))}})
  )

(defn find-and-select-input
  []
  (.select (.getElementById js/document "data-input")))

(defn find-and-select-output
  []
  (.select (.getElementById js/document "results-input")))

(defn show-page []
  (fn []
    [:div
     [:h2 "deduplicate some names"]
     [:p [:button {:on-click send-dedupe} "Run"]]
     [:div
      [:div#input {:class "preview"}
       [:label "Paste your input here"
        [:textarea#data-input {:rows 40}]]
       [:a {:on-click find-and-select-input} "Select all"]
       ]
      [:div#output {:class "preview"}
       [:label [:span "Results"]
        [:textarea#results-input {:rows 40 :value @output :readOnly true }]
        [:a {:on-click find-and-select-output} "Select all"]]]]])
  )
