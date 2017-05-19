(ns unit.was-not-called
  (:require
    [clojure.test :refer [is testing]]
    [unit.utils :as u]
    [clj-fakes.core :as f]))

(u/deftest+
  "passes if function was never called"
  (f/with-fakes
    (let [foo (f/recorded-fake)]
      (is (f/was-not-called foo)))))

(u/deftest+
  "throws if function was called once"
  (f/with-fakes
    (let [foo (f/recorded-fake [f/any nil])]
      (foo 5)
      (u/is-error-thrown
        #"^Function is expected to be never called\. Actual calls:\n\[\{:args \(5\), :return-value nil\}\]\."
        (f/was-not-called foo)))))

(u/deftest+
  "throws if function was called more than once"
  (f/with-fakes
    (let [foo (f/recorded-fake [f/any nil])]
      (foo)
      (foo 2)
      (u/is-error-thrown
        #"^Function is expected to be never called\. Actual calls:\n\[\{:args nil, :return-value nil\} \{:args \(2\), :return-value nil\}\]\."
        (f/was-not-called foo)))))