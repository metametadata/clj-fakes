(ns unit.was-called-once
  (:require
    [clojure.test :refer [is testing]]
    [unit.utils :as u]
    [clj-fakes.core :as f]
    [unit.was-called-fn-contract :as c]))

(u/deftest+
  "contract"
  (c/test-was-called-fn-contract f/was-called-once
                                 #"^Function was not called the expected number of times\. Expected: 1\. Actual: 0\."))

(u/deftest+
  "throws if function was called more than once"
  (f/with-fakes
    (let [foo (f/recorded-fake)]
      (foo)
      (foo 2)
      (u/is-error-thrown
        #"^Function was not called the expected number of times\. Expected: 1\. Actual: 2\."
        (f/was-called-once foo [2])))))