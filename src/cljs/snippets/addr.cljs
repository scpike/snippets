(ns snippets.addr
    (:require [reagent.core :as reagent :refer [atom]]
              [ajax.core :refer [POST]]
              [clojure.string :as string :refer [upper-case]]
              [linked.core :as linked]
              [snippets.components :as c])
    (:import goog.History))

(def addr-parser-url "//localhost:8080/parse_multi")

(defonce output (atom []))
(defonce raw-output (atom []))
(def show-raw (atom false))
(def input (atom "112 Highview Road, Milford PA 18337
319 Avenue C, Apt. 10A NEw York NY 10009"))

(defn result->map
  [result]
  (into (linked/map) (map (fn [x] [(keyword (:Label x)) (:Value x)]) result)))

(defn handle-new-addresses
  [res]
  (reset! raw-output res)
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

(defn format-state
  [s]
  (if (= 2 (count s)) (upper-case s) (capitalize-words s)))

(def formatters 
  (linked/map :house upper-case
              :house_number upper-case
              :road capitalize-words
              :suburb capitalize-words
              :city_district capitalize-words
              :city capitalize-words
              :state format-state
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

(defn result-toggler
  []
  [:a.small {:on-click #(swap! show-raw not)} (if @show-raw "view table" "view raw") ])

(defn result-table
  [output]
  (let [ks (headers (map :result output))]
  [:table.addr-results
   [:thead
    ^{:key "header"} [:tr
                      [:th.addr-input "input"]
                      (doall
                       (map-indexed (fn [n x]
                                      ^{:key n} [:th.addr-hdr (name x)])
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

(defn raw-results
  [output]
  [:pre
   (str "[\n" (string/join ",\n" (map #(.stringify js/JSON (clj->js %)) output))
        "\n]"
        )
   
   ])

(defn results
  [raw-output output]
    [:div.addr-results
     [:span "Results"] [:span " "] [result-toggler]
     (if @show-raw
       [raw-results raw-output]
       [result-table output])])

(defn show-page []
  (fn []
    [:div
     [:h2 "expand some addresses"]
     [:p [:button {:on-click send-addr} "Run"]]
     [:div
      [:div.row
       [:label "Paste your input here"
        [:textarea#data-input {:rows 30 :value @input
                               :on-change #(reset! input (-> % .-target .-value))}]]]
      [:div#output
       (if (seq @output) [results @raw-output @output])
       ]]]))
