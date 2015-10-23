(ns unit.were-called-in-order
  #?@(:clj  [
             (:require
               [clojure.test :refer :all]
               [clj-fakes.core :as f]
               )]
      :cljs [(:require
               [cljs.test :refer-macros [is testing]]
               [clj-fakes.core :as f :include-macros true]
               )
             ]))

(f/-deftest
  "passes when function was called twice"
  (f/with-fakes
    (let [foo (f/recorded-fake)]
      (foo)
      (foo)

      (is (f/were-called-in-order foo []))
      (is (f/were-called-in-order
            foo []
            foo [])))))

(f/-deftest
  "throws if function was not called"
  (f/with-fakes
    (let [foo (f/recorded-fake)]
      (f/-is-error-thrown
        #"^TODO"
        (f/were-called-in-order
          foo [])))))

(f/-deftest
  "throws if other function was not called"
  (f/with-fakes
    (let [foo (f/recorded-fake)
          bar (f/recorded-fake)]
      (foo)

      (f/-is-error-thrown
        #"^TODO"
        (f/were-called-in-order
          foo []
          bar [])))))

(f/-deftest
  "throws if different functions were not called in expected order"
  (f/with-fakes
    (let [foo (f/recorded-fake)
          bar (f/recorded-fake)]
      (bar)
      (foo)

      (f/-is-error-thrown
        #"^TODO"
        (f/were-called-in-order
          foo []
          bar [])))))

(f/-deftest
  "throws if function was called less times than expected"
  (f/with-fakes
    (let [foo (f/recorded-fake)]
      (foo)
      (foo)

      (f/-is-error-thrown
        #"^TODO"
        (f/were-called-in-order
          foo []
          foo []
          foo [])))))

(f/-deftest
  "throws if function was not called with specified args"
  (f/with-fakes
    (let [foo (f/recorded-fake)]
      (foo 1 2 3)

      (f/-is-error-thrown
        #"^TODO"
        (f/were-called-in-order
          foo [100 200 300])))))

(f/-deftest
  "passes when function was called once"
  (f/with-fakes
    (let [foo (f/recorded-fake)]
      (foo)
      (is (f/were-called-in-order foo [])))))

(f/-deftest
  "(integration) passes with args-matches when three functions were called"
  (f/with-fakes
    (let [foo (f/recorded-fake)
          bar (f/recorded-fake)
          baz (f/recorded-fake)]
      (foo 1 2 3)
      (foo 4)
      (bar 100 200)
      (baz 300)
      (bar 400 500 600)

      (is (f/were-called-in-order
            foo [1 2 3]
            foo [4]))

      (is (f/were-called-in-order
            foo [1 2 3]
            bar [400 500 600]))

      (is (f/were-called-in-order
            baz [300]
            bar [400 500 600]))

      (is (f/were-called-in-order
            foo [1 2 3]
            foo [(f/arg integer?)]
            bar [100 200]
            baz [300]
            bar [400 500 600])))))