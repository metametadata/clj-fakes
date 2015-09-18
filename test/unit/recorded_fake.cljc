(ns unit.recorded-fake
  #?@(:clj  [
             (:require
               [clojure.test :refer :all]
               [unit.fake-fn-contract :refer :all]
               [clj-fakes.context :as fc]
               [clj-fakes.core :as f]
               )]
      :cljs [(:require
               [cljs.test :refer-macros [is testing]]
               [unit.fake-fn-contract :refer [testing-fake-fn-contract]]
               [clj-fakes.context :as fc :include-macros true]
               [clj-fakes.core :as f :include-macros true]
               )
             ]))

(f/-deftest
  "fake contract"
  (testing-fake-fn-contract
    ; we can't pass a macro into a function so let's wrap it into a func
    (fn [config]
      (let [fake-fn (f/recorded-fake config)]
        ; supress self-test warnings
        (f/mark-checked fake-fn)
        fake-fn))
    (fn [ctx config] (fc/recorded-fake ctx config))))

(f/-deftest
  "there are no calls recorded if fake was not called"
  (f/with-fakes
    (let [foo (f/recorded-fake [[] 123])]
      (f/mark-checked foo)
      (is (= [] (f/calls foo)))
      (is (= [] (f/calls))))))

(f/-deftest
  "call args and return values are recorded on single call without args"
  (f/with-fakes
    (let [foo (f/recorded-fake [[] 123])]
      (f/mark-checked foo)

      (foo)

      (is (= [{:args nil :return-value 123}]
             (f/calls foo)))
      (is (= [[foo {:args nil :return-value 123}]]
             (f/calls))))))

(f/-deftest
  "call args and return values are recorded on single call with args"
  (f/with-fakes
    (let [foo (f/recorded-fake [f/any? 123])]
      (f/mark-checked foo)

      (foo 100 200)

      (is (= [{:args '(100 200) :return-value 123}]
             (f/calls foo)))
      (is (= [[foo {:args '(100 200) :return-value 123}]]
             (f/calls))))))

(f/-deftest
  "call args and return values are recorded on several calls"
  (f/with-fakes
    (let [foo (f/recorded-fake [f/any? #(- %1 %2)])]
      (f/mark-checked foo)

      (foo 200 100)
      (foo 500 300)
      (foo 900 500)

      (is (= [{:args '(200 100) :return-value 100}
              {:args '(500 300) :return-value 200}
              {:args '(900 500) :return-value 400}]
             (f/calls foo)))
      (is (= [[foo {:args '(200 100) :return-value 100}]
              [foo {:args '(500 300) :return-value 200}]
              [foo {:args '(900 500) :return-value 400}]]
             (f/calls))))))

(f/-deftest
  "call args and return values are recorded for several fakes"
  (f/with-fakes
    (let [foo (f/recorded-fake [f/any? #(+ %1 %2)])
          bar (f/recorded-fake [f/any? #(* %1 %2)])]
      (f/mark-checked foo)
      (f/mark-checked bar)

      (foo 1 2)
      (foo 3 4)
      (bar 5 6)
      (foo 7 8)
      (bar 9 10)

      (is (= [{:args [1 2] :return-value 3}
              {:args [3 4] :return-value 7}
              {:args [7 8] :return-value 15}]
             (f/calls foo)))

      (is (= [{:args [5 6] :return-value 30}
              {:args [9 10] :return-value 90}]
             (f/calls bar)))
      (is (= [[foo {:args [1 2] :return-value 3}]
              [foo {:args [3 4] :return-value 7}]
              [bar {:args [5 6] :return-value 30}]
              [foo {:args [7 8] :return-value 15}]
              [bar {:args [9 10] :return-value 90}]]
             (f/calls))))))

(f/-deftest
  "(just in case) calls are not recorded for other types of fakes"
  (f/with-fakes
    (let [foo (f/optional-fake [f/any? nil])
          bar (f/fake [f/any? nil])]
      (foo)
      (bar)

      (is (= [] (f/calls))))))

(f/-deftest
  "config is not required"
  (f/with-fakes
    (let [foo (f/recorded-fake)]
      (f/mark-checked foo)

      (let [result (foo 1 2 3)]
        (is (= [{:args [1 2 3] :return-value result}] (f/calls foo)))))))

(f/-deftest
  "config is not required (using explicit context)"
  (let [ctx (fc/context)
        foo (fc/recorded-fake ctx)
        result (foo 1 2 3)]
    (is (= [{:args [1 2 3] :return-value result}] (fc/calls ctx foo)))))

(f/-deftest
  "without config fake returns unique values on each call"
  (f/with-fakes
    (let [foo (f/recorded-fake)]
      (f/mark-checked foo)
      (is (not= (foo 1 2 3) (foo 1 2 3) (foo 100) (foo))))))

(f/-deftest
  "without config fake returns unique values on each call (using explicit context)"
  (let [ctx (fc/context)
        foo (fc/recorded-fake ctx)]
    (is (not= (foo 1 2 3) (foo 1 2 3) (foo 100) (foo)))))

(f/-deftest
  "without config fake returns value of sensible type"
  (f/with-fakes
    (let [foo (f/recorded-fake)]
      (f/mark-checked foo)
      (is (satisfies? fc/FakeReturnValue (foo))))))