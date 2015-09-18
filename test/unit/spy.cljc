(ns unit.spy
  #?@(:clj  [
             (:require
               [clojure.test :refer :all]
               [clj-fakes.core :as f]
               [clj-fakes.context :as fc]
               [unit.fixtures.functions :as funcs]
               )]
      :cljs [(:require
               [cljs.test :refer-macros [is testing]]
               [clj-fakes.core :as f :include-macros true]
               [clj-fakes.context :as fc :include-macros true]
               [unit.fixtures.functions :as funcs]
               )
             ]))

(f/-deftest
  "function can be spied on"
  (f/with-fakes
    (f/patch! #'funcs/sum
              (f/recorded-fake [f/any? funcs/sum]))

    (is (= 3 (funcs/sum 1 2)))
    (is (= 7 (funcs/sum 3 4)))
    (is (f/was-called funcs/sum [1 2]))
    (is (f/was-called funcs/sum [3 4]))))