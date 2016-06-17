(ns unit.spy
  #?@(:clj  [
             (:require
               [clojure.test :refer :all]
               [unit.utils :as u]
               [clj-fakes.core :as f]
               [unit.fixtures.functions :as funcs])]
      :cljs [(:require
               [cljs.test :refer-macros [is testing]]
               [clj-fakes.core :as f :include-macros true]
               [unit.fixtures.functions :as funcs])
             (:require-macros [unit.utils :as u])]))

(u/-deftest
  "function can be spied on"
  (f/with-fakes
    (f/patch! #'funcs/sum
              (f/recorded-fake [f/any? funcs/sum]))

    (is (= 3 (funcs/sum 1 2)))
    (is (= 7 (funcs/sum 3 4)))
    (is (f/was-called funcs/sum [1 2]))
    (is (f/was-called funcs/sum [3 4]))))