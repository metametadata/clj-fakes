(ns unit.fake-fn-contract
  (:require
    [clojure.test :refer [is testing]]
    [unit.utils :as u]
    [clj-fakes.core :as f]
    [clj-fakes.context :as fc]))

(defn test-fake-fn-contract
  "Parametrized test which defines a contract for all function fakes.
  Unfortunately it will short-circuit on first uncaught exception."
  [fake-fn ctx-fake-fn config-required?]

  (testing "fake can call a function for the simplest matcher"
    (f/with-fakes
      (let [foo (fake-fn [(reify fc/ArgsMatcher
                            (args-match? [_ _] true))
                          #(* %1 %2)])]
        (is (= 242 (foo 11 22))))))

  (testing "matcher recieves fake's call args"
    (f/with-fakes
      (let [foo (fake-fn [(reify fc/ArgsMatcher
                            (args-match? [_ args] (= [5 6] args))
                            (args-matcher->str [_] "<custom matcher>"))
                          #(* %1 %2)])]
        (is (= 30 (foo 5 6))))))

  (testing "works in explicit context"
    (let [ctx (fc/context)
          foo (ctx-fake-fn ctx [(reify fc/ArgsMatcher
                                  (args-match? [_ args] (= [5 6] args))
                                  (args-matcher->str [_] "<custom matcher>"))
                                #(* %1 %2)])]
      (is (= 30 (foo 5 6)))))

  (testing "matcher recieves nil on no call args"
    (f/with-fakes
      (let [foo (fake-fn [(reify fc/ArgsMatcher
                            (args-match? [_ args] (nil? args))
                            (args-matcher->str [_] "<custom matcher>"))
                          500])]
        (is (= 500 (foo))))))

  (testing "calling fake on unsupported args raises an exception"
    (f/with-fakes
      (let [foo (fake-fn [(reify fc/ArgsMatcher
                            (args-match? [_ _] false)
                            (args-matcher->str [_] "<custom matcher>"))
                          (fn [_ _])])]
        (u/is-error-thrown
          #"^Unexpected args are passed into fake: \(2 3\).\nSupported args matchers:\n<custom matcher>"
          (foo 2 3)))))

  (testing "fake can return a fixed value instead of calling a function"
    (f/with-fakes
      (let [foo (fake-fn [[11 22] 111])]
        (is (= 111 (foo 11 22))))))

  (testing "fake can call different functions for different arg combinations"
    (f/with-fakes
      (let [foo (fake-fn [[11 22] (fn [x y] (* x y))
                          [8 4] (fn [x y] (- x y))
                          [100 5] (fn [x y] (/ x y))])]
        (is (= 242 (foo 11 22)))
        (is (= 4 (foo 8 4)))
        (is (= 20 (foo 100 5))))))

  (testing "fake can return fixed values for different arg combinations"
    (f/with-fakes
      (let [foo (fake-fn [[11 22] "wow"
                          [8 4] "hey"
                          [100 5] 123])]
        (is (= "wow" (foo 11 22)))
        (is (= "hey" (foo 8 4)))
        (is (= 123 (foo 100 5))))))

  (testing "fake with several matchers throws an exception on unmatched args"
    (f/with-fakes
      (let [foo (fake-fn [[11 22] 1
                          [8 4] 2])]
        (u/is-error-thrown
          #"^Unexpected args are passed into fake: \(100 100\).\nSupported args matchers:\n\[11 22\]\n\[8 4\]"
          (foo 100 100)))))

  (testing "first matching rule wins"
    (f/with-fakes
      (let [foo (fake-fn [f/any 1
                          [2] 2
                          [3 4] 3])]
        (is (= 1 (foo)))
        (is (= 1 (foo 2)))
        (is (= 1 (foo 3 4))))))

  (when-not config-required?
    (testing "without config fake returns unique values on each call"
      (f/with-fakes
        (let [foo (fake-fn)]
          (is (not= (foo 1 2 3) (foo 1 2 3) (foo 100) (foo))))))

    (testing "without config fake returns unique values on each call (using explicit context)"
      (let [ctx (fc/context)
            foo (ctx-fake-fn ctx)]
        (is (not= (foo 1 2 3) (foo 1 2 3) (foo 100) (foo)))))
    )
  )