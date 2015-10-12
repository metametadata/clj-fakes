(ns unit.method-was-called
  #?@(:clj  [
             (:require
               [clojure.test :refer :all]
               [clj-fakes.core :as f]
               [unit.method-was-called-fn-contract :refer :all]
               [unit.fixtures.protocols :as p])]
      :cljs [(:require
               [cljs.test :refer-macros [is testing]]
               [clj-fakes.core :as f :include-macros true]
               [unit.method-was-called-fn-contract :refer [testing-method-was-called-fn-contract]]
               [unit.fixtures.protocols :as p]
               )
             ]))

(f/-deftest
  "contract"
  (testing-method-was-called-fn-contract f/method-was-called
                                         #"^Function was not called the expected number of times\. Expected: > 0\. Actual: 0\."))

(f/-deftest
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