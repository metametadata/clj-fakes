(ns unit.spy
  (:require
    [clojure.test :refer [is testing]]
    [unit.utils :as u]
    [clj-fakes.core :as f]
    [unit.fixtures.functions :as funcs]))

(u/-deftest
  "function can be spied on"
  (f/with-fakes
    (f/patch! #'funcs/sum
              (f/recorded-fake [f/any funcs/sum]))

    (is (= 3 (funcs/sum 1 2)))
    (is (= 7 (funcs/sum 3 4)))
    (is (f/was-called funcs/sum [1 2]))
    (is (f/was-called funcs/sum [3 4]))))