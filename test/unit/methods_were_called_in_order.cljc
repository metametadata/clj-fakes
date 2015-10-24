(ns unit.methods-were-called-in-order
  #?@(:clj  [
             (:require
               [clojure.test :refer :all]
               [clj-fakes.core :as f]
               [unit.fixtures.protocols :as p]
               )]
      :cljs [(:require
               [cljs.test :refer-macros [is testing]]
               [clj-fakes.core :as f :include-macros true]
               [unit.fixtures.protocols :as p]
               )
             ]))

(f/-deftest
  "big integration test"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (eat :recorded-fake)
                            (speak :recorded-fake)
                            (sleep :recorded-fake))
          dog (f/reify-fake p/AnimalProtocol
                            (eat :recorded-fake))]
      (p/speak cow)
      (p/sleep cow)
      (p/eat dog "dog food" "water")
      (p/speak cow "Bob")
      (p/eat cow "grass" "cola")
      (p/speak cow "John" "Alice")

      (is (f/methods-were-called-in-order
            p/speak cow []))

      (is (f/methods-were-called-in-order
            p/speak cow []
            p/sleep cow []))

      (is (f/methods-were-called-in-order
            p/speak cow []
            p/speak cow ["John" "Alice"]))

      (is (f/methods-were-called-in-order
            p/eat cow ["grass" "cola"]
            p/speak cow ["John" "Alice"]))

      (is (f/methods-were-called-in-order
            p/speak cow [(f/arg string?)]
            p/eat cow [(f/arg string?) (f/arg string?)]
            p/speak cow [(f/arg string?) (f/arg string?)]))

      (is (f/methods-were-called-in-order
            p/speak cow []
            p/sleep cow []
            p/eat dog ["dog food" "water"]
            p/speak cow ["Bob"]
            p/eat cow ["grass" "cola"]
            p/speak cow ["John" "Alice"])))))

(f/-deftest
  "throws if different functions were not called in expected order"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :recorded-fake))
          dog (f/reify-fake p/AnimalProtocol
                            (eat :recorded-fake))]
      (p/speak cow "Bob")
      (p/eat dog "dog food" "water")

      (f/-is-error-thrown
        #"^Could not find a call satisfying step #2"
        (f/methods-were-called-in-order
          p/eat dog ["dog food" "water"]
          p/speak cow ["Bob"])))))