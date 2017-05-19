(ns unit.context
  (:require
    [clojure.test :refer [is testing]]
    [unit.utils :as u]
    [clj-fakes.core :as f]))

(def my-var 111)

(u/deftest+
  "context executes its body and returns last expression"
  (let [x (atom 100)
        return-val (f/with-fakes
                     (reset! x 200)
                     (inc @x))]
    (is (= return-val 201))))

(u/deftest+
  "contexts can nest"
  (f/with-fakes
    (is (= 111 my-var))
    (f/patch! #'my-var "parent context")
    (is (= "parent context" my-var))

    (f/with-fakes
      (is (= "parent context" my-var))
      (f/patch! #'my-var "child context")
      (is (= "child context" my-var)))

    (is (= "parent context" my-var))))

(u/deftest+
  "function can be used instead of a macro"
  (f/with-fakes*
    (fn [] (is (= 111 my-var))
      (f/patch! #'my-var "parent context")
      (is (= "parent context" my-var))

      (f/with-fakes*
        (fn [new-name]
          (is (= "parent context" my-var))
          (f/patch! #'my-var new-name)
          (is (= "child context" my-var)))
        "child context")

      (is (= "parent context" my-var)))))