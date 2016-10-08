(ns unit.method-was-called
  (:require
    [clojure.test :refer [is testing]]
    [unit.utils :as u]
    [clj-fakes.core :as f]
    [unit.method-was-called-fn-contract :as c]
    [unit.fixtures.protocols :as p]))

(u/-deftest
  "contract"
  (c/testing-method-was-called-fn-contract f/method-was-called
                                         #"^Function was not called the expected number of times\. Expected: > 0\. Actual: 0\."))

(u/-deftest
  "passes if function was called several times"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :recorded-fake))]
      (p/speak cow)
      (p/speak cow 2)
      (p/speak cow 3)

      (is (f/method-was-called p/speak cow [2]))
      (is (f/method-was-called p/speak cow []))
      (is (f/method-was-called p/speak cow [3])))))