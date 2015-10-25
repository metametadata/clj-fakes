(ns unit.was-called-fn-contract
  #?@(:clj  [
             (:require
               [clojure.test :refer :all]
               [clj-fakes.core :as f]
               [clj-fakes.context :as fc]
               )]
      :cljs [(:require
               [cljs.test :refer-macros [is testing]]
               [clj-fakes.core :as f :include-macros true]
               [clj-fakes.context :as fc :include-macros true]
               )
             ]))

(defn testing-was-called-fn-contract
  "Parametrized test which defines a contract for was-called-* funcs.
  Unfortunately it will short-circuit on first uncaught exception."
  [was-called-fn expected-exc-re-on-no-call]

  (testing "passes if function was called once"
    (f/with-fakes
      (let [foo (f/recorded-fake)]
        (foo)
        (is (was-called-fn foo [])))))

  (testing "throws if function was not called at all"
    (f/with-fakes
      (let [foo (f/recorded-fake)]
        (f/-is-error-thrown
          expected-exc-re-on-no-call
          (was-called-fn foo [])))))

  (testing "args matcher can be specified"
    (f/with-fakes
      (let [foo (f/recorded-fake)]
        (foo 2 3)
        (is (was-called-fn foo (reify fc/ArgsMatcher
                                (args-match? [_ args]
                                  (= [2 3] args))))))))

  (testing "throws if function was never called with specified args"
    (f/with-fakes
      (let [foo (f/recorded-fake [f/any? nil])]
        (foo 2 3)
        (f/-is-error-thrown
          #"^Function was never called with the expected args\.\nArgs matcher: \[2 4\]\.\nActual calls:\n\[\{:args \(2 3\), :return-value nil\}\]\n"
          (was-called-fn foo [2 4])))))

  (testing "(regression) check with matcher should not pass when function was called with no args"
    (f/with-fakes
      (let [foo (f/recorded-fake [f/any? nil])]
        (foo)
        (f/-is-error-thrown
          #"^Function was never called with the expected args\.\nArgs matcher: \[1 2\]\.\nActual calls:\n\[\{:args nil, :return-value nil\}\]\n"
          (was-called-fn foo [1 2])))))

  (testing "on exception args matcher with any?, function and regex arg matchers is printed in a readable form"
    (f/with-fakes
      (let [foo (f/recorded-fake [f/any? nil])]
        (foo 2 3)
        (f/-is-error-thrown
          #"^Function was never called with the expected args\.\nArgs matcher: \[2 4 <any\?> <string\?> <abc>\]\.\nActual calls:\n\[\{:args \(2 3\), :return-value nil\}\]\n"
          (was-called-fn foo [2 4 f/any? (f/arg string?) (f/arg #"abc")])))))
)