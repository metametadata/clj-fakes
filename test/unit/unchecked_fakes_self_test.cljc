(ns unit.unchecked-fakes-self-test
  (:require
    [clojure.test :refer [is testing]]
    [unit.utils :as u]
    [clj-fakes.core :as f]
    [clj-fakes.context :as fc]
    [unit.fixtures.protocols :as p]))

(u/-deftest
  "user is warned if fake was never checked"
  (u/-is-error-thrown
    #"^Self-test: no check performed on:\nrecorded fake from .*unit/unchecked_fakes_self_test\.cljc, 14:7"
    (f/with-fakes
      (f/recorded-fake [f/any nil]))))

(u/-deftest
  "user is not warned if fake was never called if it's explicitly marked as checked"
  (f/with-fakes
    (f/mark-checked (f/recorded-fake [f/any nil]))))

(u/-deftest
  "user is not warned if fake was never checked in case of exception inside the context"
  (u/-is-error-thrown
    #"expected"
    (f/with-fakes
      (f/recorded-fake [f/any nil])
      (throw (ex-info "expected" {})))))

(u/-deftest
  "user is not warned if fake was never checked in case of assertion error inside the context"
  (u/-is-assertion-error-thrown
    #"expected"
    (f/with-fakes
      (f/recorded-fake [f/any nil])
      (assert nil "expected"))))

#?(:cljs
   (u/-deftest
     "user is not warned if fake was never checked in case of non-object exception inside the context"
     (let [caught-exception (atom nil)]
       (try
         (f/with-fakes
           (f/recorded-fake [f/any nil])
           (throw "expected"))
         (catch :default e
           (reset! caught-exception e))
         (finally
           (is (= "expected" @caught-exception)))))))

(u/-deftest
  "self-test works with explicit context"
  (u/-is-error-thrown
    #"^Self-test: no check performed on:\nrecorded fake from .*unit/unchecked_fakes_self_test\.cljc, 55:7"
    (let [ctx (fc/context)]
      (fc/recorded-fake ctx [f/any nil])
      (fc/self-test-unchecked-fakes ctx))))

(u/-deftest
  "self-test about unchecked recorded fakes is more important than the one about unused fakes"
  (u/-is-error-thrown
    #"^Self-test: no check performed on:\nrecorded fake from .*unit/unchecked_fakes_self_test\.cljc, 64:7"
    (f/with-fakes
      (f/fake [f/any nil])
      (f/recorded-fake [f/any nil]))))

(u/-deftest
  "user is warned if several fakes were not checked"
  (u/-is-error-thrown
    #"^Self-test: no check performed on:\nrecorded fake from .*unit/unchecked_fakes_self_test\.cljc, 71:7\nrecorded fake from .*unit/unchecked_fakes_self_test\.cljc, 72:7\nrecorded fake from .*unit/unchecked_fakes_self_test\.cljc, 73:7$"
    (f/with-fakes
      (f/recorded-fake [f/any nil])
      (f/recorded-fake [f/any nil])
      (f/recorded-fake [f/any nil]))))

;;;;;;;;;;;;;;;;;;;;;;;;;; reify-fake
(u/-deftest
  "user is warned if reified protocol fake was never checked"
  (u/-is-error-thrown
    #"^Self-test: no check performed on:\nrecorded fake from .*unit/unchecked_fakes_self_test\.cljc, 81:7 \(p/AnimalProtocol, speak\)"
    (f/with-fakes
      (f/reify-fake p/AnimalProtocol
                    (speak :recorded-fake [f/any nil])))))

(u/-deftest
  "user is warned if reified protocol fake was never checked (explicit context)"
  (u/-is-error-thrown
    #"^Self-test: no check performed on:\nrecorded fake from .*unit/unchecked_fakes_self_test\.cljc, 89:7 \(p/AnimalProtocol, speak\)"
    (let [ctx (fc/context)]
      (fc/reify-fake ctx p/AnimalProtocol
                     (speak :recorded-fake [f/any nil]))
      (fc/self-test-unchecked-fakes ctx))))