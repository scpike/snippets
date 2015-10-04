(ns snippets.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [cognitect.transit :as t]
              [ajax.core :refer [GET POST]]
              [snippets.model :as m :refer [snippets]]
              [goog.history.EventType :as EventType])
    (:import goog.History))

;; -------------------------
;; Views

;; TODO put these in a data store
;;

(defn home-page []
  [:div [:h2 "Welcome to snippets"]
   [:p [:a {:on-click #(secretary/dispatch! "/snippets/new")}  "New snippet"]]
   [:ul
   (for [s @m/snippets]
     ^{:key (:name s)} [:li [:a { :href (str "#/" (:slug s)) } (:name s)]])]
   ])

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

(defn go-home
  []
  (m/fetch)
  (secretary/dispatch! "/"))

(def edit-snippet-state (atom {}))
(defn go-to-edit
  [{:keys [slug] :as snippet}]
  (reset! edit-snippet-state snippet)
  (secretary/dispatch! (str "/snippets/" slug "/edit")))

(defn show-page [slug]
  (fn []
    (let [snippet (m/find-by-slug slug)]
          [:div
           [:h2 (:name snippet)]
           [:div.main [snippet-widget snippet]]
           [:pre (:code snippet)]
           [:div
            [:a {:on-click go-home :href "#/" }"go to the home page"]
            [:span " "]
            [:a {:on-click #(m/delete snippet go-home)} "Delete"]
            [:span " "]
            [:a {:on-click #(go-to-edit snippet)} "Edit"]
            ]])))

(def new-snippet-state (atom {}))

(defn create-snippet
  []
  (m/create @new-snippet-state #(secretary/dispatch! "/")))

(defn new-snippet []
  [:div.new-snippet
   [:h2 "New snippet"]
   [:label "Name"
    [:input { :on-change #(swap! new-snippet-state assoc :name (-> % .-target .-value)) }]]
   [:label "Slug"
    [:input { :on-change #(swap! new-snippet-state assoc :slug (-> % .-target .-value)) }]]
   [:label "Code"
    [:textarea { :rows 40 :cols 60
                :on-change #(swap! new-snippet-state assoc :code (-> % .-target .-value)) }]]
   [:div.actions
    [:button { :on-click create-snippet } "Submit"]]

   [:div.preview
    [snippet-widget @new-snippet-state]
    ]
   [:div [:a {:on-click go-home :href "#/"} "go to the home page"]]
   ])

(defn edit-snippet []
  (let [snippet @edit-snippet-state]
    [:div.edit-snippet
     [:h2 (str "Edit snippet " (:name snippet))]
     [:label "Name"
      [:input {:on-change #(swap! edit-snippet-state assoc :name (-> % .-target .-value))
               :value (:name snippet)}]]
     [:label "Slug"
      [:input {:on-change #(swap! edit-snippet-state assoc :slug (-> % .-target .-value))
               :value (:slug snippet)}]]
     [:label "Code"
      [:textarea {:rows 40 :cols 60
                  :on-change #(swap! edit-snippet-state assoc :code (-> % .-target .-value))
                  :value (:code snippet)}]]
     [:div.actions
      [:button { :on-click #(m/save @edit-snippet-state go-home) } "Submit"]]

     [:div.preview
      [snippet-widget @edit-snippet-state]
      ]
     [:div [:a {:on-click go-home :href "#/"} "go to the home page"]]
     ]))

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/:slug" [slug]
  (session/put! :current-page (#'show-page slug)))

(secretary/defroute "/snippets/new" [id]
  (session/put! :current-page #'new-snippet))

(secretary/defroute "/snippets/:slug/edit" [slug]
  (session/put! :current-page #'edit-snippet))

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
  (mount-root)
  (m/fetch))

;; Quick and dirty history configuration.
(let [h (History.)]
  (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
  (doto h (.setEnabled true)))
