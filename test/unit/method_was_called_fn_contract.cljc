(ns unit.method-was-called-fn-contract
  #?@(:clj  [
             (:require
               [clojure.test :refer :all]
               [clj-fakes.core :as f]
               [clj-fakes.context :as fc]
               [unit.fixtures.protocols :as p]
               )]
      :cljs [(:require
               [cljs.test :refer-macros [is testing]]
               [clj-fakes.core :as f :include-macros true]
               [clj-fakes.context :as fc :include-macros true]
               [unit.fixtures.protocols :as p]
               )
             ]))

(defn testing-method-was-called-fn-contract
  "Parametrized test which defines a contract for method-was-called-* funcs.
  Unfortunately it will short-circuit on first uncaught exception."
  [was-called-fn expected-exc-re-on-no-call]

  (testing "passes if method was called once"
    (f/with-fakes
      (let [cow (f/reify-fake p/AnimalProtocol
                              (speak :recorded-fake))]
        (p/speak cow)
        (is (was-called-fn p/speak cow [])))))

  (testing "args matcher can be specified"
    (f/with-fakes
      (let [cow (f/reify-fake p/AnimalProtocol
                              (speak :recorded-fake))]
        (p/speak cow 2 3)
        (is (was-called-fn p/speak cow (reify fc/ArgsMatcher
                                         (args-match? [_ args]
                                           (= [2 3] args))))))))

  (testing "throws if function was not called at all"
    (f/with-fakes
      (let [cow (f/reify-fake p/AnimalProtocol
                              (speak :recorded-fake))]
        (f/-is-error-thrown
          expected-exc-re-on-no-call
          (was-called-fn p/speak cow [])))))

  (testing "throws if function was never called with specified args"
    (f/with-fakes
      (let [cow (f/reify-fake p/AnimalProtocol
                              (speak :recorded-fake [f/any? nil]))]
        (p/speak cow 2 3)
        (f/-is-error-thrown
          ; message is too complicated to assert here fully
          #"^Function was never called with the expected args\.\nArgs matcher: <this> \[2 4\]\.\nActual calls:\n.*\n"
          (was-called-fn p/speak cow [2 4])))))

  (testing "on exception args matcher with any?, function and regex arg matchers is printed in a readable form"
    (f/with-fakes
      (let [cow (f/reify-fake p/AnimalProtocol
                              (speak :recorded-fake [f/any? nil]))]
        (p/speak cow 2 3)
        (f/-is-error-thrown
          ; message is too complicated to assert here fully
          #"^Function was never called with the expected args\.\nArgs matcher: <this> \[2 4 <any\?> <string\?> <abc>\]\.\nActual calls:\n.*\n"
          (was-called-fn p/speak cow [2 4 f/any? (f/arg string?) (f/arg #"abc")])))))
  )