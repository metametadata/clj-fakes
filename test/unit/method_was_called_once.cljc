(ns unit.method-was-called-once
  (:require
    [clojure.test :refer [is testing]]
    [unit.utils :as u]
    [clj-fakes.core :as f]
    [unit.method-was-called-fn-contract :as c]
    [unit.fixtures.protocols :as p]))

(u/deftest+
  "contract"
  (c/test-method-was-called-fn-contract f/method-was-called-once
                                        #"^Function was not called the expected number of times\. Expected: 1\. Actual: 0\."))

(u/deftest+
  "throws if function was called more than once"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :recorded-fake))]
      (p/speak cow)
      (p/speak cow 2)
      (u/is-error-thrown
        #"^Function was not called the expected number of times\. Expected: 1\. Actual: 2\."
        (f/method-was-called-once p/speak cow [2])))))