(ns snippets.components
    (:require [reagent.core :as reagent :refer [atom]]))

(def input (atom ""))

(defn run-snippet
  [snippet s]
  (try
    (let [code (:code snippet)
          js-fn (js/eval code)]
      (js-fn s))
    (catch js/Error e e)))

(defn find-and-select-output
  []
  (.select (.getElementById js/document "results-input")))

(defn snippet-widget
  [snippet]
  (let [output (run-snippet snippet @input)]
    [:div
     [:div#input {:class "preview"}
      [:label "Paste your input here"
       [:textarea {:rows 40
                   :value @input
                   :on-change #(reset! input (-> % .-target .-value))}]]]
     [:div#output {:class "preview"}
      [:label [:span "Results"]
       [:textarea#results-input {:rows 40 :value output :readOnly true }]
       [:a {:on-click find-and-select-output} "Select all"]]
      ]]))
