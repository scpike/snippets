(ns snippets.re-builder
  (:require
   [reagent.core :as reagent :refer [atom]]
   [clojure.string :as str :refer [split join]]
   )
    (:import goog.History))

(def regexp (atom "o+"))
(def input (atom "foo
bar
baz
bottle
sam
uncle"))

(defn match-by-line
  [str re-string]
  (let [lines (split str #"\n")]
    (println lines)
    (for [l lines]
      [:span l]
      )
    ))

(defn show-page []
  [:div
   [:h2 "re-builder"]
   [:div
    [:span "/"]
    [:input {:on-change #(reset! regexp (-> % .-target .-value)) :value @regexp}]
    [:span "/"]
    ]
   [:div#input {:class "preview thirds"}
    [:label "Paste your input here"
     [:textarea#data-input {:rows 40
                            :value @input
                            :on-change #(reset! input (-> % .-target .-value))}]]]
   [:div#output {:class "preview thirds"}
    [:label [:span "Matches"]
     [:div.re-matches (match-by-line @input @regexp)]]]
   [:div#output {:class "preview thirds"}
    [:label [:span "Replacement"]
     [:textarea#results-input {:rows 40 :value @input :readOnly true }]]]])
