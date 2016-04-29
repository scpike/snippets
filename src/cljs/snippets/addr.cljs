(ns snippets.addr
    (:require [reagent.core :as reagent :refer [atom]]
              [ajax.core :refer [POST]]
              [clojure.string :as string :refer [upper-case]]
              [linked.core :as linked]
              [snippets.components :as c])
    (:import goog.History))

(def addr-parser-url "//localhost:8080/parse_multi")

(defonce output (atom []))
(def input (atom "112 Highview Road, Milford PA 18337
319 Avenue C, Apt. 10A NEw York NY 10009"))

(defn result->map
  [result]
  (into (linked/map) (map (fn [x] [(keyword (:Label x)) (:Value x)]) result)))

(defn handle-new-addresses
  [res]
  (reset! output
          (map (fn [x] {:input (:Input x)
                        :result (result->map (:Result x))})
               res)))

(defn send-addr
  []
  (POST addr-parser-url
       {:handler handle-new-addresses
        :error-handler #(js/alert "Could not reach server")
        :response-format :json
        :keywords? true
        :format :json
        :params {:addresses (clojure.string/split @input #"\n")}}))

(send-addr)

(defn find-and-select-input
  []
  (.select (.getElementById js/document "data-input")))

(defn find-and-select-output
  []
  (.select (.getElementById js/document "results-input")))

(defn capitalize-words 
  "Capitalize every word in a string"
  [s]
  (->> (string/split (str s) #"\b") 
       (map string/capitalize)
       string/join))

(def formatters 
  (linked/map :house upper-case
              :house_number upper-case
              :road capitalize-words
              :suburb capitalize-words
              :city_district capitalize-words
              :city capitalize-words
              :state upper-case
              :state_district capitalize-words
              :postcode str
              :country capitalize-words))

(def ordered-header-options (keys formatters)) 

(defn format-part
  [part val]
  (let [f (get formatters part str)]
    (f (str val))))

(defn format-result
  [x]
  (into (linked/map) (map (fn [[k v]] [k (format-part k v)]) x)))

(defn headers
  [results]
  (let [all-keys (into (linked/set) (flatten (map keys results)))]
    (filter #(contains? all-keys %) ordered-header-options)))

(defn result-table
  [output]
  (let [ks (headers (map :result output))]
    (println ks)
    [:table.addr-results
     [:thead
      ^{:key "header"} [:tr
                        [:th.addr-input "Input"]
                        (doall
                         (map-indexed (fn [n x]
                                        ^{:key n} [:th.addr-hdr (str x)])
                                      ks
                                      ))]]
     [:tbody
      (doall 
       (map-indexed (fn [n {:keys [input result]}]
                      ^{:key n}
                      [:tr
                       [:td input]
                       (doall
                        (map-indexed (fn [j k]
                                       ^{:key j} [:td.addr-hdr (get (format-result result) k)])
                                     ks
                                     ))
                       ])
                    output
                    ))]]))

(defn show-page []
  (fn []
    [:div
     [:h2 "expand some addresses"]
     [:p [:button {:on-click send-addr} "Run"]]
     [:div
      [:div 
       [:label "Paste your input here"
        [:textarea#data-input {:rows 40 :value @input
                               :on-change #(reset! input (-> % .-target .-value))}]]]
      [:div#output
       [:br]
       [:label [:span "Results"]]
       [result-table @output]
       ]]]))
