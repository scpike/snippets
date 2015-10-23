(ns munge.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
              [cljsjs.react :as react])
    (:import goog.History))

;; -------------------------
;; Views

(def map-code (atom "def mapfn(x)
  x.downcase.gsub(/[^\\w\\s]/, '').gsub(' ', '_')
end"))

(def filter-code (atom "def filterfn(x)
  x =~ /.*/
end"))

(def input (atom "New York, New York
Los Angeles, California
Chicago, Illinois
Houston, Texas
Philadelphia, Pennsylvania
Phoenix, Arizona
San Antonio, Texas
San Diego, California
Dallas, Texas
San Jose, California
Austin, Texas
Indianapolis, Indiana
Jacksonville, Florida
San Francisco, California
Columbus, Ohio
Charlotte, North Carolina
Fort Worth, Texas
Detroit, Michigan
El Paso, Texas
Memphis, Tennessee
Seattle, Washington
Denver, Colorado
Washington, District of Columbia
Boston, Massachusetts
Nashville, Tennessee
Baltimore, Maryland
Oklahoma City, Oklahoma
Louisville, Kentucky
Portland, Oregon"))

(def result (atom ""))

(defn codify-map
  [s]
  (js/eval (js/Opal.compile s)))

(defn codify-filter
  [s]
  (js/eval (js/Opal.compile s)))

(defn filter-with-nil
  [f coll]
  (filter #(and (f %)
                (not= js/Opal.NilClass (.-_klass (f %))))
          coll))

(defn do-munge
  [& e]
  (let [f-js (codify-map @map-code)
        filter-js (codify-filter @filter-code)
        mapfn #(js/Opal.Object.$mapfn %)
        filterfn #(js/Opal.Object.$filterfn %)
        input (clojure.string/split @input #"\n" )]
    (reset! result (->> input
                        (filter-with-nil filterfn)
                        (map mapfn)
                        (remove clojure.string/blank?)
                        (clojure.string/join  "\n")))))
; (munge)

(defn make-codemirror
  [atm text-area-node]
  ; Have to use js-obj, passing a map as opts doesn't work
  (let [opts (js-obj "mode" "ruby" "lineNumbers" true "keyMap" "emacs")
        cm (js/CodeMirror.fromTextArea text-area-node opts )]
    (.on cm "change" #(reset! atm (-> % .getValue)))))

(defn codemirror
  [atm]
  (with-meta identity
    {:component-did-mount #(make-codemirror atm (reagent/dom-node %))}))

(defn code-input [atm]
  [(codemirror atm)
   [:textarea {:rows 20
               :cols 40
               :defaultValue (deref atm) }]])

(def map-code-input
  (code-input map-code))

(def filter-code-input
  (code-input filter-code))

(defn munge-home []
  [:div#munge-it
   [:h2 "Munge yourself some data"]
   [:div
    [:div
     [:h3 "Filter"]
     [:p "Return true to allow a row through."]
     filter-code-input]
    [:div
     [:h3 "Map"]
     [:p "Write some ruby which defines a function `mapfn`."]
     map-code-input]]
   [:div
    [:div [:input {:type "button" :value "Munge away!"
                   :class "munge-btn"
                   :on-click do-munge}]]]
   [:div
    [:div#input {:class "preview"}
     [:label "Input"
      [:textarea {:rows 40 :value @input
                  :on-change #(reset! input (-> % .-target .-value))}]]]
    [:div#input {:class "preview"}
     [:label "Output"
      [:textarea {:rows 40  :disabled true :value @result
                  :on-change #(reset! result (-> % .-target .-value))}]]]]
   ])

(enable-console-print!)
