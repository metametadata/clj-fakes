(ns unit.unused-fakes-self-test
  (:require
    [clojure.test :refer [is testing #?(:cljs async)]]
    [unit.utils :as u]
    [clj-fakes.core :as f]
    [clj-fakes.context :as fc]
    [unit.fixtures.protocols :as p]))

(u/-deftest
  "user is warned if fake was never called"
  (u/-is-error-thrown
    #"^Self-test: no call detected for:\nnon-optional fake from .*unit/unused_fakes_self_test\.cljc, 14:7"
    (f/with-fakes
      (f/fake [f/any nil]))))

(u/-deftest
  "user is not warned if fake was never called in case of exception inside the context"
  (u/-is-error-thrown
    #"expected"
    (f/with-fakes
      (f/fake [f/any nil])
      (throw (ex-info "expected" {})))))

(u/-deftest
  "user is not warned if fake was never called in case of assertion error inside the context"
  (u/-is-assertion-error-thrown
    #"expected"
    (f/with-fakes
      (f/fake [f/any nil])
      (assert nil "expected"))))

#?(:cljs
   (u/-deftest
     "user is not warned if fake was never called in case of non-object exception inside the context"
     (let [caught-exception (atom nil)]
       (try
         (f/with-fakes
           (f/fake [f/any nil])
           (throw "expected"))
         (catch :default e
           (reset! caught-exception e))
         (finally
           (is (= "expected" @caught-exception)))))))

(u/-deftest
  "works with explicit context"
  (u/-is-error-thrown
    #"^Self-test: no call detected for:\nnon-optional fake from .*unit/unused_fakes_self_test\.cljc, 50:7"
    (let [ctx (fc/context)]
      (fc/fake ctx [f/any nil])
      (fc/self-test-unused-fakes ctx))))

(u/-deftest
  "user is not warned if optional fake was never called"
  (f/with-fakes
    (f/optional-fake [f/any nil])))

(u/-deftest
  "user is warned if several fakes were never called"
  (u/-is-error-thrown
    #"^Self-test: no call detected for:
non-optional fake from .*unit/unused_fakes_self_test\.cljc, 66:7
non-optional fake from .*unit/unused_fakes_self_test\.cljc, 67:7
non-optional fake from .*unit/unused_fakes_self_test\.cljc, 68:7"
    (f/with-fakes
      (f/fake [f/any nil])
      (f/fake [f/any nil])
      (f/fake [f/any nil]))))

;;;;;;;;;;;;;;;;;;;;;;;;;; reify-fake
(u/-deftest
  "user is not warned if reified protocol fake was never called"
  (u/-is-error-thrown
    #"^Self-test: no call detected for:\nnon-optional fake from .*unit/unused_fakes_self_test\.cljc, 76:7 \(p/AnimalProtocol, speak\)"
    (f/with-fakes
      (f/reify-fake p/AnimalProtocol
                    (speak :fake [f/any nil])))))

(u/-deftest
  "user is not warned if reified protocol fake was never called (explicit context)"
  (u/-is-error-thrown
    #"^Self-test: no call detected for:\nnon-optional fake from .*unit/unused_fakes_self_test\.cljc, 84:7 \(p/AnimalProtocol, speak\)"
    (let [ctx (fc/context)]
      (fc/reify-fake ctx p/AnimalProtocol
                     (speak :fake [f/any nil]))
      (fc/self-test-unused-fakes ctx))))