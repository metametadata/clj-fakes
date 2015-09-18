(ns unit.was-called-on
  #?@(:clj  [
             (:require
               [clojure.test :refer :all]
               [clj-fakes.core :as f]
               [unit.was-called-on-fn-contract :refer :all]
               [unit.fixtures.protocols :as p])]
      :cljs [(:require
               [cljs.test :refer-macros [is testing]]
               [clj-fakes.core :as f :include-macros true]
               [unit.was-called-on-fn-contract :refer [testing-was-called-on-fn-contract]]
               [unit.fixtures.protocols :as p]
               )
             ]))

(f/-deftest
  "contract"
  (testing-was-called-on-fn-contract f/was-called-on
                                     #"^Function was not called the expected number of times\. Expected: > 0\. Actual: 0\."))

(f/-deftest
  "passes if function was called several times"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :recorded-fake))]
      (p/speak cow)
      (p/speak cow 2)
      (p/speak cow 3)
      (is (f/was-called-on cow p/speak [2]))
      (is (f/was-called-on cow p/speak))
      (is (f/was-called-on cow p/speak [3])))))