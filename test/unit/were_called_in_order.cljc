(ns unit.were-called-in-order
  (:require
    [clojure.test :refer [is testing]]
    [unit.utils :as u]
    [clj-fakes.core :as f]))

(u/-deftest
  "passes when function was called twice"
  (f/with-fakes
    (let [foo (f/recorded-fake)]
      (foo)
      (foo)

      (is (f/were-called-in-order foo []))
      (is (f/were-called-in-order
            foo []
            foo [])))))

(u/-deftest
  "throws if function was not called"
  (f/with-fakes
    (let [foo (f/recorded-fake)]
      (u/-is-error-thrown
        #"^Could not find a call satisfying step #1"
        (f/were-called-in-order
          foo [])))))

(u/-deftest
  "throws if other function was not called"
  (f/with-fakes
    (let [foo (f/recorded-fake)
          bar (f/recorded-fake)]
      (foo)

      (u/-is-error-thrown
        #"^Could not find a call satisfying step #2"
        (f/were-called-in-order
          foo []
          bar [])))))

(u/-deftest
  "throws if different functions were not called in expected order"
  (f/with-fakes
    (let [foo (f/recorded-fake)
          bar (f/recorded-fake)]
      (bar)
      (foo)

      (u/-is-error-thrown
        #"^Could not find a call satisfying step #2"
        (f/were-called-in-order
          foo []
          bar [])))))

(u/-deftest
  "throws if function was called less times than expected"
  (f/with-fakes
    (let [foo (f/recorded-fake)]
      (foo)
      (foo)

      (u/-is-error-thrown
        #"^Could not find a call satisfying step #3"
        (f/were-called-in-order
          foo []
          foo []
          foo [])))))

(u/-deftest
  "throws if function was not called with specified args"
  (f/with-fakes
    (let [foo (f/recorded-fake)]
      (foo 1 2 3)

      (u/-is-error-thrown
        #"^Could not find a call satisfying step #1"
        (f/were-called-in-order
          foo [100 200 300])))))

(u/-deftest
  "passes when function was called once"
  (f/with-fakes
    (let [foo (f/recorded-fake)]
      (foo)
      (is (f/were-called-in-order foo [])))))

(u/-deftest
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

(u/-deftest
  "specifies step details on exception"
  (f/with-fakes
    (let [foo (f/recorded-fake)]
      (foo 1 2 3)

      (u/-is-error-thrown
        #"^Could not find a call satisfying step #1:\nrecorded fake from .*unit/were_called_in_order\.cljc, 121:15\nargs matcher: \[100 <string\?>\]"
        (f/were-called-in-order
          foo [100 (f/arg string?)])))))

(u/-deftest
  "(regression) correctly reports a step on any args matcher"
  (f/with-fakes
    (let [foo (f/recorded-fake [[1 2] 3])]
      (u/-is-error-thrown
        #"^Could not find a call satisfying step #1:\nrecorded fake from .*unit/were_called_in_order\.cljc, 132:15\nargs matcher: <any>"
        (f/were-called-in-order foo f/any)))))

(u/-deftest
  "marks all mentioned fakes checked, even on failure"
  (f/with-fakes
    (let [foo (f/recorded-fake)
          bar (f/recorded-fake)]
      (u/-is-error-thrown
        #"^Could not find a call satisfying step #1"
        (f/were-called-in-order foo f/any
                                bar f/any)))))