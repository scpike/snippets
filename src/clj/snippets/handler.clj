(ns snippets.handler
  (:require [compojure.core :refer [GET POST PUT DELETE defroutes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.defaults :refer [api-defaults wrap-defaults]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.util.response :refer [response]]
            [clojure.string :as str]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [include-js include-css]]
            [prone.middleware :refer [wrap-exceptions]]
            [ring.middleware.reload :refer [wrap-reload]]
            [environ.core :refer [env]]))

(def home-page
  (html
   [:html
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1"}]
     (include-js "//cdn.opalrb.org/opal/0.7.1/opal.min.js")
     (include-js "//cdn.opalrb.org/opal/0.7.1/opal-parser.min.js")
     (include-js "//cdnjs.cloudflare.com/ajax/libs/codemirror/5.8.0/codemirror.min.js")
     (include-js "//cdnjs.cloudflare.com/ajax/libs/codemirror/5.8.0/mode/ruby/ruby.js")
     (include-js "//cdnjs.cloudflare.com/ajax/libs/codemirror/5.8.0/keymap/emacs.js")
     (include-css "//cdnjs.cloudflare.com/ajax/libs/codemirror/5.8.0/codemirror.min.css")
     (include-css (if (env :dev) "css/site.css" "css/site.min.css"))]
    [:body
     [:div#nav
      [:ul
       [:li [:a {:href "#/"} "Snippets"]]]]
     [:div#app]
     [:br]
     [:div.footer [:a {:href "//scpike.com"} "scpike.com"]]
     (include-js "js/app.js")]]))

(def snippets
  (atom { 1 { :slug "echo" :name "echo" :code "(function(x) { return x })"}
         2 { :slug "sort" :name "sort" :code "
(function(x) {
  return x.split(\"\\n\").sort().join(\"\\n\");
})"}
         3 { :slug "catsqlq" :name "catsqlq" :code "
(function(x) {
  if (x.length > 0) {
    return \"('\" + x.split(\"\\n\").join(\"','\") + \"')\"
  }
})"}
         4 { :slug "uniqify" :name "uniqify" :code "
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
})"}}))

(defn snippets-index
  []
  (let [with-ids (map #(into {} (conj {:id (first %)} (second %))) @snippets)]
    (response with-ids)))

(defn find-snippet-idx
  [id]
  (first (first (filter
                 #(= (:id (last %)) 10)
                 (map-indexed vector @snippets)))))

(defn snippets-show
  [id]
  (get @snippets 4))

(defn conj-snippet
  [snippets attrs]
  (let [max-id (last (sort (keys snippets)))
        new-id (if max-id (inc max-id) 1)]
    (assoc snippets new-id attrs)))

(defn valid-snippet?
  [{:keys [slug name code] :as attrs}]
  (and
   (not (str/blank? slug))
   (not (str/blank? code))
   (not (str/blank? name))
  ))

(defn snippets-create
  [attrs]
  (println attrs)
  (if (valid-snippet? attrs)
    (do
      (swap! snippets conj-snippet attrs)
      (response "Ok"))
    {:status 401 :body "Invalid" :headers {}}
    ))

(defn snippets-update
  [{:keys [id] :as attrs}]
  (println id)
  (println attrs)
  (let [id (Integer. id)]
    (swap! snippets
           #(update-in % [id] merge attrs))))

(defn snippets-delete
  [id]
  (let [id (Integer. id)]
    (swap! snippets #(dissoc % id))))

(defroutes routes
  (GET "/" [] home-page)
;  (POST "/snippets" {params :params} (snippets-create params))
  (GET "/snippets" [] (snippets-index))
  (GET "/snippets/:id" [id] (snippets-show id))
;  (PUT "/snippets/:id" {params :params} (snippets-update params))
;  (DELETE "/snippets/:id" [id] (snippets-delete id))
  (resources "/")
  (not-found "Not Found"))

(def app
  (let [handler
        (-> #'routes
            (wrap-defaults api-defaults)
            wrap-keyword-params
            wrap-params
            wrap-json-response
            wrap-gzip)]
    (if (env :dev) (-> handler wrap-exceptions wrap-reload) handler)))
