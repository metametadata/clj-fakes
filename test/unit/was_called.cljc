(ns unit.was-called
  (:require
    [clojure.test :refer [is testing]]
    [unit.utils :as u]
    [clj-fakes.core :as f]
    [unit.was-called-fn-contract :as c]))

(u/deftest+
  "contract"
  (c/test-was-called-fn-contract f/was-called
                                 #"^Function was not called the expected number of times\. Expected: > 0\. Actual: 0\."))

(u/deftest+
  "passes if function was called several times"
  (f/with-fakes
    (let [foo (f/recorded-fake)]
      (foo)
      (foo 2)
      (foo 3)
      (is (f/was-called foo [f/any]))
      (is (f/was-called foo []))
      (is (f/was-called foo [3])))))