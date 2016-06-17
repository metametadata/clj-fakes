(ns unit.fake
  #?@(:clj  [
             (:require
               [clojure.test :refer :all]
               [unit.utils :as u]
               [unit.fake-fn-contract :refer :all]
               [clj-fakes.core :as f]
               [clj-fakes.context :as fc]
               )]
      :cljs [(:require
               [cljs.test :refer-macros [is testing]]
               [unit.fake-fn-contract :refer [testing-fake-fn-contract]]
               [clj-fakes.core :as f :include-macros true]
               [clj-fakes.context :as fc :include-macros true])
             (:require-macros [unit.utils :as u])]))

(u/-deftest
  "fake contract"
  (testing-fake-fn-contract
    ; we can't pass a macro into a function so let's wrap it into a func
    (fn [config] (f/fake config))
    (fn [ctx config] (fc/fake ctx config))
    true))