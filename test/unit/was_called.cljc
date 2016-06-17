(ns unit.was-called
  #?@(:clj  [
             (:require
               [clojure.test :refer :all]
               [unit.utils :as u]
               [clj-fakes.core :as f]
               [unit.was-called-fn-contract :refer :all]
               )]
      :cljs [(:require
               [cljs.test :refer-macros [is testing]]
               [clj-fakes.core :as f :include-macros true]
               [unit.was-called-fn-contract :refer [testing-was-called-fn-contract]])
             (:require-macros [unit.utils :as u])]))

(u/-deftest
  "contract"
  (testing-was-called-fn-contract f/was-called
                                  #"^Function was not called the expected number of times\. Expected: > 0\. Actual: 0\."))

(u/-deftest
  "passes if function was called several times"
  (f/with-fakes
    (let [foo (f/recorded-fake)]
      (foo)
      (foo 2)
      (foo 3)
      (is (f/was-called foo [2]))
      (is (f/was-called foo []))
      (is (f/was-called foo [3])))))