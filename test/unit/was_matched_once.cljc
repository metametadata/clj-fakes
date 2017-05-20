(ns unit.was-matched-once
  (:require
    [clojure.test :refer [is testing]]
    [unit.utils :as u]
    [clj-fakes.core :as f]
    [unit.was-called-fn-contract :as c]))

(u/deftest+
  "contract"
  (c/test-was-called-fn-contract f/was-matched-once
                                 #"^Function was not called the expected number of times\. Expected: > 0\. Actual: 0\."))

(u/deftest+
  "throws if more than one call matches"
  (f/with-fakes
    (let [foo (f/recorded-fake [f/any nil])]
      (foo)
      (foo 2)
      (foo 3)
      (foo 2)
      (u/is-error-thrown
        #"^More than one call satisfies the provided args matcher\.\nArgs matcher: \[2\]\.\nMatched calls:\n\[\{:args \(2\), :return-value nil\} \{:args \(2\), :return-value nil\}\]"
        (f/was-matched-once foo [2])))))