(ns snippets.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [cognitect.transit :as t]
              [ajax.core :refer [GET POST]]
              [snippets.model :as m :refer [snippets]]
              [snippets.components :as c :refer [snippet-widget]]
              [snippets.gists :as gists]
              [snippets.re-builder :as re-builder]
              [munge.core :as munge]
              [snippets.dedupe :as dedupe]
              [goog.history.EventType :as EventType])
    (:import goog.History))

(def gist-key (atom ""))
(defn home-page []
  (m/fetch)
  (fn []
    [:div [:h2 ]
     [:div.panel
      [:ul
       (for [s @m/snippets]
         ^{:key (:name s)} [:li [:a { :href (str "#/" (:slug s)) } (:name s)]])
       [:li ^{:key "dedupe"} [:a {:href "#/dedupe"} "dedupe"]]
       ]
      [:div [:label "Enter a gist url to create a snippet"
             [:input.gist-key {:name "gist-key"
                      :placeholder "https://gist.github.com/scpike/77f1c362b82750b53559"
                               :on-change #(reset! gist-key (-> % .-target .-value))}]
             (let [k @gist-key]
               (if-not (clojure.string/blank? k)
                 [:a {:href (str "#/gists/" (gists/parse-gist-key @gist-key))}
                  "Create snippet"]))]]]
                                        ;[:p [:a {:href "#/snippets/new"} "Create a snippet"]]
     ]))

(defn go-home
  []
  (set! js/window.location.hash "/#"))

(defn show-page [slug]
  (reset! c/input "")
  (fn []
    (let [snippet (m/find-by-slug slug)]
          [:div
           [:h2 (:name snippet)]
           [:div.main [snippet-widget snippet]]
           [:h3 "Code"]
           [:pre (:code snippet)]
           [:div
            [:span " "]
          ;  [:a {:on-click #(m/delete snippet go-home)} "Delete"]
            [:span " "]
;            [:a {:href (str "#/snippets/" slug "/edit")} "Edit"]
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
   [:label "Language"
    [:select {:on-change #(swap! new-snippet-state assoc :lang (-> % .-target .-value)) }
     [:option {:value "js"} "Javascript"]
     [:option {:value "ruby"} "Ruby"]]]
   [:label "Code"
    [:textarea { :rows 40 :cols 60
                :on-change #(swap! new-snippet-state assoc :code (-> % .-target .-value)) }]]
   [:div.actions
    [:button { :on-click create-snippet } "Submit"]]
   [snippet-widget @new-snippet-state]
   [:div [:a {:href "#/"} "Home page"]]
   ])

(def edit-snippet-state (atom {}))

(defn edit-snippet [slug]
  (let [snippet (m/find-by-slug slug)]
    (reset! edit-snippet-state snippet))
  (fn []
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
       [:div [:a {:href "#/"} "Home page"]]
       ])))

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page (home-page)))

(secretary/defroute "/re-builder" []
  (session/put! :current-page #'re-builder/show-page))

(secretary/defroute "/munge" []
  (session/put! :current-page #'munge/munge-home))

(secretary/defroute "/dedupe" [key]
  (session/put! :current-page #'dedupe/show-page))

(secretary/defroute "/:slug" [slug]
  (session/put! :current-page (#'show-page slug)))

(secretary/defroute "/snippets/new" [id]
  (session/put! :current-page #'new-snippet))

(secretary/defroute "/snippets/:slug/edit" [slug]
  (session/put! :current-page (edit-snippet slug)))

(secretary/defroute "/gists/:key" [key]
  (session/put! :current-page (gists/show-page key)))

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
