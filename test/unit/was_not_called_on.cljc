(ns unit.was-not-called-on
  #?@(:clj  [
             (:require
               [clojure.test :refer :all]
               [clj-fakes.core :as f]
               [unit.was-called-fn-contract :refer :all]
               [unit.fixtures.protocols :as p]
               )]
      :cljs [(:require
               [cljs.test :refer-macros [is testing]]
               [clj-fakes.core :as f :include-macros true]
               [unit.was-called-fn-contract :refer [testing-was-called-fn-contract]]
               [unit.fixtures.protocols :as p]
               )
             ]))

(f/-deftest
  "passes if function was never called"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :recorded-fake))]
      (is (f/was-not-called-on cow p/speak)))))

(f/-deftest
  "throws if function was called once"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :recorded-fake))]
      (p/speak cow 5)
      (f/-is-error-thrown
        ; message is too complicated to assert here fully
        #"^Function is expected to be never called\. .*\."
        (f/was-not-called-on cow p/speak)))))

(f/-deftest
  "throws if function was called more than once"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :recorded-fake))]
      (p/speak cow)
      (p/speak cow 2)
      (f/-is-error-thrown
        #"^Function is expected to be never called\. .*\."
        (f/was-not-called-on cow p/speak)))))