; Prettify Clojure test reports
(ns unit.reporter
  (:require
    [clojure.test :refer [report with-test-out]]))

; Change the report multimethod to ignore namespaces that don't contain any tests.
; taken from: http://blog.jayfields.com/2010/08/clojuretest-introduction.html
(defmethod report :begin-test-ns [m]
  (with-test-out
    (when (some #(:test (meta %)) (vals (ns-interns (:ns m))))
      (println "\n-------====== Testing" (ns-name (:ns m)) "======-------"))))

(def ansi-reset "\u001B[0m")
(def ansi-bold "\u001B[1m")
(def ansi-red "\u001B[31m")

; Summary reporting with color
(defmethod report :summary [m]
  (try
    (print ansi-bold)
    (when (not (every? zero? [(:fail m) (:error m)]))
      (print ansi-red))

    (with-test-out
      (println "\nRan" (:test m) "tests containing"
               (+ (:pass m) (:fail m) (:error m)) "assertions.")
      (println (:fail m) "failures," (:error m) "errors."))

    (finally
      (print ansi-reset))))