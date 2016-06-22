(ns unit.original-val
  #?@(:clj  [
             (:require
               [clojure.test :refer :all]
               [unit.utils :as u]
               [clj-fakes.core :as f]
               [clj-fakes.context :as fc]
               [unit.fixtures.functions :as funcs])]
      :cljs [(:require
               [cljs.test :refer-macros [is testing]]
               [clj-fakes.core :as f :include-macros true]
               [clj-fakes.context :as fc :include-macros true]
               [unit.fixtures.functions :as funcs])
             (:require-macros [unit.utils :as u])]))

(def my-var 111)
(def my-nil-var nil)

(u/-deftest
  "original var val can be found"
  (f/with-fakes
    (f/patch! #'my-var 1000)
    (is (= 111 (f/original-val #'my-var)))))

(u/-deftest
  "original function val can be found"
  (f/with-fakes
    (is (not= 123 (funcs/sum 2 3)))
    (f/patch! #'funcs/sum (constantly 123))

    (is (= 123 (funcs/sum 2 3)))
    (is (= 10 ((f/original-val #'funcs/sum) 7 3)))))

(u/-deftest
  "original function can be called by patched var"
  (f/with-fakes
    (f/patch! #'funcs/sum
              #((f/original-val #'funcs/sum) (* %1 %2) %2))
    (is (= 16 (funcs/sum 3 4)))))

(u/-deftest
  "raises if key is not found"
  (f/with-fakes
    (u/-is-assertion-error-thrown
      #"^Assert failed: Specified var is not patched\n"
      (f/original-val #'funcs/sum))))

(u/-deftest
  "original value can be nil"
  (is (nil? my-nil-var) "self-test")
  (f/with-fakes
    (f/patch! #'my-nil-var 123)
    (is (nil? (f/original-val #'my-nil-var)))))

(testing "works in explicit context"
  (let [ctx (fc/context)
        original-val my-var]
    (fc/patch! ctx #'my-var 200)
    (is (not= original-val my-var) "self test")

    (is (= original-val (fc/original-val ctx #'my-var)))
    (fc/unpatch-all! ctx)))