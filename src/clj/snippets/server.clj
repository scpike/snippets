(ns snippets.server
  (:require [snippets.handler :refer [app]]
            [environ.core :refer [env]]
            [mount.core :as mount]
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

 (defn -main [& args]
   (let [port (Integer/parseInt (or (env :port) "3000"))]
     (mount/start)
     (run-jetty app {:port port :join? false})))
