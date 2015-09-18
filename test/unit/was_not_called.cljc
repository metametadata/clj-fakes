(ns unit.was-not-called
  #?@(:clj  [
             (:require
               [clojure.test :refer :all]
               [clj-fakes.core :as f]
               [unit.was-called-fn-contract :refer :all]
               )]
      :cljs [(:require
               [cljs.test :refer-macros [is testing]]
               [clj-fakes.core :as f :include-macros true]
               [unit.was-called-fn-contract :refer [testing-was-called-fn-contract]]
               )
             ]))

(f/-deftest
  "passes if function was never called"
  (f/with-fakes
    (let [foo (f/recorded-fake [f/any? nil])]
      (is (f/was-not-called foo)))))

(f/-deftest
  "throws if function was called once"
  (f/with-fakes
    (let [foo (f/recorded-fake [f/any? nil])]
      (foo 5)
      (f/-is-error-thrown
        #"^Function is expected to be never called\. Actual calls: \[\{:args \(5\), :return-value nil\}\]\."
        (f/was-not-called foo)))))

(f/-deftest
  "throws if function was called more than once"
  (f/with-fakes
    (let [foo (f/recorded-fake [f/any? nil])]
      (foo)
      (foo 2)
      (f/-is-error-thrown
        #"^Function is expected to be never called\. Actual calls: \[\{:args nil, :return-value nil\} \{:args \(2\), :return-value nil\}\]\."
        (f/was-not-called foo)))))