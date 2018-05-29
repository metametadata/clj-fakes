(ns unit.context
  (:require
    [clojure.test :refer [is testing #?(:cljs async)]]
    [unit.utils :as u]
    [clojure.core.async :as a]
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

(u/deftest+
  "with-fakes macro resets the binding on exiting the block"
  (is (nil? f/*context*) "self-test: context is nil by default")
  (f/with-fakes
    (is (some? f/*context*) "self-test: context is bound inside the block"))

  (is (nil? f/*context*) "context is unbound on exiting the block"))

(u/deftest+
  "with-fakes* function resets the binding on exiting the block"
  (is (nil? f/*context*) "self-test: context is nil by default")
  (f/with-fakes*
    #(is (some? f/*context*) "self-test: context is bound inside the block"))

  (is (nil? f/*context*) "context is unbound on exiting the block"))

#?(:cljs
   (u/deftest+
     "with-fakes cannot be used inside a go-block in ClojureScript"
     (async done
       (a/go
         (try
           (u/is-assertion-error-thrown
             #"with-fakes cannot be used here"
             (f/with-fakes))

           (finally
             (done)))))))

#?(:clj
   (u/deftest+
     "with-fakes can be used inside a go-block in Clojure"
     (a/<!!
       (a/go
         (f/with-fakes)))))

#?(:cljs
   (u/deftest+
     "with-fakes* function can be correctly used inside a go-block"
     (async done
       (a/go
         (try
           (is (nil? f/*context*) "self-test: context is nil by default")
           (f/with-fakes*
             #(is (some? f/*context*) "context is bound inside with-fakes* block"))

           (is (nil? f/*context*) "context is unbound on exiting with-fakes* block")

           (finally
             (done)))))))