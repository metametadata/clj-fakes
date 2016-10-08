(ns unit.optional-fake
  (:require
    [clojure.test :refer [is testing]]
    [unit.utils :as u]
    [unit.fake-fn-contract :as c]
    [clj-fakes.core :as f]
    [clj-fakes.context :as fc]))

(u/-deftest
  "fake contract"
  (c/testing-fake-fn-contract f/optional-fake fc/optional-fake false))