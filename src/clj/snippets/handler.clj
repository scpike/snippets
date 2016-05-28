(ns snippets.handler
  (:require [compojure.core :refer [GET POST PUT DELETE defroutes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.defaults :refer [api-defaults wrap-defaults]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.util.response :refer [response]]
            [clojure.string :as str]
            [clojure.java.shell :refer [sh]]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [include-js include-css]]
            [prone.middleware :refer [wrap-exceptions]]
            [ring.middleware.reload :refer [wrap-reload]]
            [environ.core :refer [env]]
            [snippets.db :as db]
            ))

(def irrust (or (env :irrust-path)
                "/home/steve/dev/information-retrieval/irrust/target/release/irrust"))

(def home-page
  (html
   [:html
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1"}]
     [:script {:src "js-libs/opal.min.js" :type "text/javascript"}]
     [:script {:src "js-libs/native.min.js" :type "text/javascript"}]
     [:script {:src "js-libs/opal-parser.min.js" :type "text/javascript"}]
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

(def snippets (atom {})) ; in DB now

(defn snippets-index
  []
  (let [snippets (db/get-all-snippets)]
    (response snippets)))

(defn snippets-show
  [id]
  (let [id (Integer. id)
        s (db/get-snippet {:id id})]
    (response (first s))))

(defn valid-snippet?
  [{:keys [slug name code] :as attrs}]
  (and
   (not (str/blank? slug))
   (not (str/blank? code))
   (not (str/blank? name))))

(defn snippets-create
  [attrs]
  (if (valid-snippet? attrs)
    (do
      ; (db/insert-snippet attrs)
      (response "Ok"))
    {:status 401 :body "Invalid" :headers {}}))

(defn snippets-update
  [{:keys [id] :as attrs}]
  (let [id (Integer. id)
        attrs (assoc attrs :id id)]
    ; (db/update-snippet attrs)
    (response "Ok")))

(defn snippets-delete
  [id]
  (let [id (Integer. id)]
    ; (db/delete-snippet {:id id})
    (response "Ok")))

(defn sanitize-lines
  [xs]
  (str/join "\n" (take 10000 (str/split (str  xs) #"\n"))))

(defn run-deduper
  [xs]
  (let [lines (sanitize-lines xs)
        scores (:out (sh irrust :in lines))]
    (str/join "\n" (reverse (sort (str/split scores #"\n"))))))

(defn dedupe-handler
  [lines]
  (response {:results (run-deduper lines)}))

(defroutes routes
  (GET "/" [] home-page)
  (POST "/snippets" {params :params} (snippets-create params))
  (GET "/snippets" [] (snippets-index))
  (GET "/snippets/:id" [id] (snippets-show id))
  (PUT "/dedupe" request (dedupe-handler (get-in request [:body :lines])))
  (PUT "/snippets/:id" {params :params} (snippets-update params))
  (DELETE "/snippets/:id" [id] (snippets-delete id))
  (resources "/")
  (not-found "Not Found"))

(def app
  (let [handler
        (-> #'routes
            (wrap-defaults api-defaults)
            wrap-keyword-params
            wrap-params
            wrap-json-response
            (wrap-json-body {:keywords? true})
            wrap-gzip)]
    (if (env :dev) (-> handler wrap-exceptions wrap-reload) handler)))
