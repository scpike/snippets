(ns snippets.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType])
    (:import goog.History))


;; -------------------------
;; Views

;; TODO put these in a data store
;;
(def snippets
  (atom [{ :id "echo" :name "echo" :code "(function(x) { return x })"}
         { :id "sort" :name "sort" :code "
(function(x) {
  return x.split(\"\\n\").sort().join(\"\\n\");
})"}
         { :id "catsqlq" :name "catsqlq" :code "
(function(x) {
  if (x.length > 0) {
    return \"('\" + x.split(\"\\n\").join(\"','\") + \"')\"
  }
})"}
         { :id "uniqify" :name "uniqify" :code "
(function(x) {
  var set = new Set();
  var res = [];
  x.split(\"\\n\").forEach(function(s) {
    if (!set.has(s)) {
      set.add(s);
      res.push(s);
    }
  })
  return res.sort().join(\"\\n\");
})"}]))

(defn find-snippet
  [id]
  (first (filter #(= id (:id %)) @snippets)))

(defn home-page []
  [:div [:h2 "Welcome to snippets"]
   [:p [:a { :href "#/snippets/new" } "New snippet"]]
   [:ul
   (for [s @snippets]
     ^{:key (:name s)} [:li [:a { :href (str "#/" (:id s)) } (:name s)]])]
   [:div [:a {:href "#/about"} "go to about page"]]])

(defn about-page []
  [:div [:h2 "About snippets"]
   [:div [:a {:href "#/"} "go to the home page"]]])

(defn run-snippet
  [snippet s]
  (try
    (let [code (:code snippet)
          js-fn (js/eval code)]
      (js-fn s))
    (catch js/Error e e)))

(def input (atom ""))

(defn snippet-widget
  [snippet]
  (let [output (run-snippet snippet @input)]
    [:div
     [:textarea {:rows 40 :cols 60
                 :value @input
                 :on-change #(reset! input (-> % .-target .-value))}]
     [:span.buffer]
     [:textarea {:rows 40 :cols 60 :value output :readOnly true }]
     ]))

(defn show-page [id]
  (fn []
    (let [snippet (find-snippet id)]
          [:div
           [:h2 (:name snippet)]
           [:div.main [snippet-widget snippet]]
           [:pre (:code snippet)]
           [:div [:a {:href "#/"} "go to the home page"]]])))

(def new-snippet-state (atom {}))

(defn create-snippet
  []
  (println @snippets)
  (swap! snippets conj @new-snippet-state)
  (println @snippets)
  (reset! new-snippet-state (atom {}))
  (session/put! :current-page #'home-page))

(defn new-snippet []
  [:div.new-snippet
   [:h2 "New snippet"]
   [:label "Name"
    [:input { :on-change #(swap! new-snippet-state assoc :name (-> % .-target .-value)) }]]
   [:label "Slug"
    [:input { :on-change #(swap! new-snippet-state assoc :id (-> % .-target .-value)) }]]
   [:label "Code"
    [:textarea { :rows 40 :cols 60
                :on-change #(swap! new-snippet-state assoc :code (-> % .-target .-value)) }]]
   [:div.actions
    [:button { :on-click create-snippet } "Submit"]]

   [:div.preview
    [snippet-widget @new-snippet-state]
    ]
   [:div [:a {:href "#/"} "go to the home page"]]
   ])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

(secretary/defroute "/:id" [id]
  (session/put! :current-page (#'show-page id)))

(secretary/defroute "/snippets/new" [id]
  (session/put! :current-page #'new-snippet))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (hook-browser-navigation!)
  (mount-root))
