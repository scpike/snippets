(ns snippets.components
    (:require [reagent.core :as reagent :refer [atom]]))

(def input (atom ""))

(defn compile-ruby
  [code]
  (js/Opal.compile code))

(defn compile-snippet
  [{:keys [code lang]}]
  (let [l (clojure.string.lower-case lang)]
    (if (= l "ruby")
      (compile-ruby code)
      code)))

(defn run-snippet
  [snippet s]
  (try
    (if-let [code (compile-snippet snippet)]
      (let [js-fn (js/eval code)]
        (js-fn s)))
    (catch js/Error e e)))

(defn find-and-select-output
  []
  (.select (.getElementById js/document "results-input")))

(defn find-and-select-input
  []
  (.select (.getElementById js/document "data-input")))

(defn snippet-widget
  [snippet]
  (let [output (str (run-snippet snippet @input))]
    [:div
     [:div#input {:class "preview"}
      [:label "Paste your input here"
       [:textarea#data-input {:rows 40
                   :value @input
                   :on-change #(reset! input (-> % .-target .-value))}]]
      [:a {:on-click find-and-select-input} "Select all"]
      ]
     [:div#output {:class "preview"}
      [:label [:span "Results"]
       [:textarea#results-input {:rows 40 :value output :readOnly true }]
       [:a {:on-click find-and-select-output} "Select all"]]
      ]]))
