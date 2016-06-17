(ns unit.unchecked-fakes-self-test
  #?@(:clj  [
             (:require
               [clojure.test :refer :all]
               [unit.utils :as u]
               [clj-fakes.core :as f]
               [clj-fakes.context :as fc]
               [unit.fixtures.protocols :as p])]
      :cljs [(:require [cljs.test :refer-macros [is testing]]
               [clj-fakes.core :as f :include-macros true]
               [clj-fakes.context :as fc :include-macros true]
               [unit.fixtures.protocols :as p])
             (:require-macros [unit.utils :as u])]))

(u/-deftest
  "user is warned if fake was never checked"
  (u/-is-error-thrown
    #?(:clj
       #"^Self-test: no check performed on:\nrecorded fake from unit/unchecked_fakes_self_test\.cljc, 23:7"
       :cljs
       #"^Self-test: no check performed on:\nrecorded fake from test/unit/unchecked_fakes_self_test\.cljc, 23:7")
    (f/with-fakes
      (f/recorded-fake [f/any? nil]))))

(u/-deftest
  "user is not warned if fake was never called if it's explicitly marked as checked"
  (f/with-fakes
    (f/mark-checked (f/recorded-fake [f/any? nil]))))

(u/-deftest
  "user is not warned if fake was never checked in case of exception inside the context"
  (u/-is-error-thrown
    #"expected"
    (f/with-fakes
      (f/recorded-fake [f/any? nil])
      (throw (ex-info "expected" {})))))

#?(:cljs
   (u/-deftest
     "user is not warned if fake was never checked in case of non-object exception inside the context"
     (let [caught-exception (atom nil)]
       (try
         (f/with-fakes
           (f/recorded-fake [f/any? nil])
           (throw "expected"))
         (catch :default e
           (reset! caught-exception e))
         (finally
           (is (= "expected" @caught-exception)))))))

(u/-deftest
  "self-test works with explicit context"
  (u/-is-error-thrown
    #?(:clj
       #"^Self-test: no check performed on:\nrecorded fake from unit/unchecked_fakes_self_test\.cljc, 59:7"
       :cljs
       #"^Self-test: no check performed on:\nrecorded fake from test/unit/unchecked_fakes_self_test\.cljc, 59:7")
    (let [ctx (fc/context)]
      (fc/recorded-fake ctx [f/any? nil])
      (fc/self-test-unchecked-fakes ctx))))

(u/-deftest
  "self-test about unchecked recorded fakes is more important than the one about unused fakes"
  (u/-is-error-thrown
    #?(:clj
       #"^Self-test: no check performed on:\nrecorded fake from unit/unchecked_fakes_self_test\.cljc, 71:7"
       :cljs
       #"^Self-test: no check performed on:\nrecorded fake from test/unit/unchecked_fakes_self_test\.cljc, 71:7")
    (f/with-fakes
      (f/fake [f/any? nil])
      (f/recorded-fake [f/any? nil]))))

(u/-deftest
  "user is warned if several fakes were not checked"
  (u/-is-error-thrown
    #?(:clj
       #"^Self-test: no check performed on:
recorded fake from unit/unchecked_fakes_self_test\.cljc, 87:7
recorded fake from unit/unchecked_fakes_self_test\.cljc, 88:7
recorded fake from unit/unchecked_fakes_self_test\.cljc, 89:7"
       :cljs
       #"^Self-test: no check performed on:
recorded fake from test/unit/unchecked_fakes_self_test.cljc, 87:7
recorded fake from test/unit/unchecked_fakes_self_test.cljc, 88:7
recorded fake from test/unit/unchecked_fakes_self_test.cljc, 87:7")
    (f/with-fakes
      (f/recorded-fake [f/any? nil])
      (f/recorded-fake [f/any? nil])
      (f/recorded-fake [f/any? nil]))))

;;;;;;;;;;;;;;;;;;;;;;;;; reify-fake
(u/-deftest
  "user is warned if reified protocol fake was never checked"
  (u/-is-error-thrown
    #?(:clj
       #"^Self-test: no check performed on:\nrecorded fake from unit/unchecked_fakes_self_test\.cljc, 100:7 \(p/AnimalProtocol, speak\)"
       :cljs
       #"^Self-test: no check performed on:\nrecorded fake from test/unit/unchecked_fakes_self_test\.cljc, 100:7 \(p/AnimalProtocol, speak\)")
    (f/with-fakes
      (f/reify-fake p/AnimalProtocol
                    (speak :recorded-fake [f/any? nil])))))

(u/-deftest
  "user is warned if reified protocol fake was never checked (explicit context)"
  (u/-is-error-thrown
    #?(:clj
       #"^Self-test: no check performed on:\nrecorded fake from unit/unchecked_fakes_self_test\.cljc, 111:7 \(p/AnimalProtocol, speak\)"
       :cljs
       #"^Self-test: no check performed on:\nrecorded fake from test/unit/unchecked_fakes_self_test\.cljc, 111:7 \(p/AnimalProtocol, speak\)")
    (let [ctx (fc/context)]
      (fc/reify-fake ctx p/AnimalProtocol
                     (speak :recorded-fake [f/any? nil]))
      (fc/self-test-unchecked-fakes ctx))))