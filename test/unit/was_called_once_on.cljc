(ns unit.was-called-once-on
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
  (testing-was-called-on-fn-contract f/was-called-once-on
                                     #"^Function was not called the expected number of times\. Expected: 1\. Actual: 0\."))

(f/-deftest
  "throws if function was called more than once"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :recorded-fake))]
      (p/speak cow)
      (p/speak cow 2)
      (f/-is-error-thrown
        #"^Function was not called the expected number of times\. Expected: 1\. Actual: 2\."
        (f/was-called-once-on cow p/speak [2])))))