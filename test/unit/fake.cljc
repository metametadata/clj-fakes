(ns unit.fake
  (:require
    [unit.fake-fn-contract :as c]
    [unit.utils :as u]
    [clj-fakes.core :as f]
    [clj-fakes.context :as fc]))

(u/-deftest
  "fake contract"
  (c/testing-fake-fn-contract
    ; we can't pass a macro into a function so let's wrap it into a func
    (fn [config] (f/fake config))
    (fn [ctx config] (fc/fake ctx config))
    true))