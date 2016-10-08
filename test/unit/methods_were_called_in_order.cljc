(ns unit.methods-were-called-in-order
  (:require
    [clojure.test :refer [is testing]]
    [unit.utils :as u]
    [clj-fakes.core :as f]
    [unit.fixtures.protocols :as p]))

(u/-deftest
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

(u/-deftest
  "throws if different functions were not called in expected order"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :recorded-fake))
          dog (f/reify-fake p/AnimalProtocol
                            (eat :recorded-fake))]
      (p/speak cow "Bob")
      (p/eat dog "dog food" "water")

      (u/-is-error-thrown
        (re-pattern (str "^Could not find a call satisfying step #2:\n"
                         "recorded fake from .*unit/methods_were_called_in_order\\.cljc"
                         ", 55:15 \\(p/AnimalProtocol, speak\\)\n"
                         "args matcher: <this> \\[Bob\\]$"))
        (f/methods-were-called-in-order
          p/eat dog ["dog food" "water"]
          p/speak cow ["Bob"])))))