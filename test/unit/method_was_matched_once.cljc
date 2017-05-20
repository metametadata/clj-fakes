(ns unit.method-was-matched-once
  (:require
    [clojure.test :refer [is testing]]
    [unit.utils :as u]
    [clj-fakes.core :as f]
    [unit.method-was-called-fn-contract :as c]
    [unit.fixtures.protocols :as p]))

(u/deftest+
  "contract"
  (c/test-method-was-called-fn-contract f/method-was-matched-once
                                        #"^Function was not called the expected number of times\. Expected: > 0\. Actual: 0\."))

(u/deftest+
  "throws if more than one call matches"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :recorded-fake [f/any nil]))]
      (p/speak cow)
      (p/speak cow 2)
      (p/speak cow 3)
      (p/speak cow 2)
      (u/is-error-thrown
        #"^More than one call satisfies the provided args matcher\.\nArgs matcher: <this> \[2\]\.\nMatched calls:\n.*"
        (f/method-was-matched-once p/speak cow [2])))))