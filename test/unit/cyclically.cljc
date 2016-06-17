(ns unit.cyclically
  #?@(:clj  [
             (:require
               [clojure.test :refer :all]
               [unit.utils :as u]
               [clj-fakes.core :as f]
               [unit.fixtures.protocols :as p :refer [AnimalProtocol]])]
      :cljs [(:require
               [cljs.test :refer-macros [is testing]]
               [clj-fakes.core :as f :include-macros true]
               [unit.fixtures.protocols :as p :refer [AnimalProtocol]])
             (:require-macros [unit.utils :as u])]))

(u/-deftest
  "returned function cycles through specified values infinitely and takes any args"
  (let [foo (f/cyclically [:first :second :third])]
    (dotimes [_ 5]
      (is (= :first (foo)))
      (is (= :second (foo :some-arg)))
      (is (= :third (foo :some-arg1 :some-arg2))))))

(u/-deftest
  "can be used for iterator-style stubbing"
  (f/with-fakes
    (let [get-weekday (f/fake [["My event"] (f/cyclically [:monday :tuesday :wednesday])])]
      (dotimes [_ 2]
        (is (= :monday (get-weekday "My event")))
        (is (= :tuesday (get-weekday "My event")))
        (is (= :wednesday (get-weekday "My event")))))))

(u/-deftest
  "(just in case) can be used for iterator-style stubbing of methods"
  (f/with-fakes
    (let [cow (f/reify-fake
                AnimalProtocol
                (speak :fake [[] (f/cyclically ["moo" "mmm" "moo-moo"])]))]
      (dotimes [_ 2]
        (is (= "moo" (p/speak cow)))
        (is (= "mmm" (p/speak cow)))
        (is (= "moo-moo" (p/speak cow)))))))