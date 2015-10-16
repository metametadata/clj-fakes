(ns unit.args-matcher
  #?@(:clj  [
             (:require
               [clojure.test :refer :all]
               [clj-fakes.core :as f]
               [clj-fakes.context :as fc]
               )]
      :cljs [(:require
               [cljs.test :refer-macros [is are testing]]
               [clj-fakes.core :as f :include-macros true]
               [clj-fakes.context :as fc :include-macros true]
               )
             ]))

(defn args-match?
  [matcher args]
  (is (satisfies? fc/ArgsMatcher matcher) "self test")
  (fc/args-match? matcher args))

(f/-deftest
  "f/any? args matcher matches everything"
  (are [args] (args-match? f/any? args)
              nil
              [4]
              [3 2 4]))

(f/-deftest
  "vector is args matcher"
  (are [v args] (args-match? v args)
                [11 22] [11 22]
                [] nil
                [1 nil] [1 nil]
                [nil nil] [nil nil])

  (are [v args] (not (args-match? v args))
                [2 3] [2 3 4]
                [2 3 4] [2 3]
                [1 2] nil)

  (testing "vector elements can be functional arg matchers"
    (are [v args] (args-match? v args)
                  [100 (f/arg even?)] [100 2]
                  [100 (f/arg even?)] [100 10])

    (are [v args] (not (args-match? v args))
                  [100 (f/arg odd?)] [100 2]
                  [100 (f/arg odd?)] [100 10]))

  (testing "empty vector can be matched"
    (are [v args] (args-match? v args)
                  [] []
                  [[]] [[]]
                  [(f/arg integer?) []] [123 []]))

  (testing "f/any? can be an arg matcher"
    (are [args] (args-match? [f/any? 111] args)
                [nil 111]
                [[] 111]
                [2 111]))
  )