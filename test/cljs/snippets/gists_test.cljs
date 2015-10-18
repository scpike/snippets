(ns snippets.gists-test
  (:require [cemerick.cljs.test :refer-macros [is are deftest testing use-fixtures done]]
            [snippets.gists :as gists]))

(def sort-url "https://gist.github.com/scpike/77f1c362b82750b53559")
(def token "77f1c362b82750b53559")
(def api-url "https://api.github.com/gists/77f1c362b82750b53559")

(deftest parses-token
  (is (= token (gists/parse-gist-key token)))
  (is (= token (gists/parse-gist-key sort-url))))

(deftest generates-url
  (is (= api-url (gists/gist-key-to-url sort-url)))
  (is (= api-url (gists/gist-key-to-url token))))
