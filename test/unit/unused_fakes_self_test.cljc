(ns unit.unused-fakes-self-test
  #?@(:clj  [
             (:require
               [clojure.test :refer :all]
               [unit.utils :as u]
               [clj-fakes.core :as f]
               [clj-fakes.context :as fc]
               [unit.fixtures.protocols :as p])]
      :cljs [(:require
               [cljs.test :refer-macros [async is testing]]
               [clj-fakes.core :as f :include-macros true]
               [clj-fakes.context :as fc :include-macros true]
               [unit.fixtures.protocols :as p])
             (:require-macros [unit.utils :as u])]))

(u/-deftest
  "user is warned if fake was never called"
  (u/-is-error-thrown
    #?(:clj
       #"^Self-test: no call detected for:\nnon-optional fake from unit/unused_fakes_self_test\.cljc, 24:7"
       :cljs
       #"^Self-test: no call detected for:\nnon-optional fake from test/unit/unused_fakes_self_test\.cljc, 24:7")
    (f/with-fakes
      (f/fake [f/any? nil]))))

(u/-deftest
  "user is not warned if fake was never called in case of exception inside the context"
  (u/-is-error-thrown
    #"expected"
    (f/with-fakes
      (f/fake [f/any? nil])
      (throw (ex-info "expected" {})))))

#?(:cljs
   (u/-deftest
     "user is not warned if fake was never called in case of non-object exception inside the context"
     (let [caught-exception (atom nil)]
       (try
         (f/with-fakes
           (f/fake [f/any? nil])
           (throw "expected"))
         (catch :default e
           (reset! caught-exception e))
         (finally
           (is (= "expected" @caught-exception)))))))

(u/-deftest
  "works with explicit context"
  (u/-is-error-thrown
    #?(:clj
       #"^Self-test: no call detected for:\nnon-optional fake from unit/unused_fakes_self_test\.cljc, 55:7"
       :cljs
       #"^Self-test: no call detected for:\nnon-optional fake from test/unit/unused_fakes_self_test\.cljc, 55:7")
    (let [ctx (fc/context)]
      (fc/fake ctx [f/any? nil])
      (fc/self-test-unused-fakes ctx))))

(u/-deftest
  "user is not warned if optional fake was never called"
  (f/with-fakes
    (f/optional-fake [f/any? nil])))

(u/-deftest
  "user is warned if several fakes were never called"
  (u/-is-error-thrown
    #?(:clj
       #"^Self-test: no call detected for:
non-optional fake from unit/unused_fakes_self_test\.cljc, 77:7
non-optional fake from unit/unused_fakes_self_test\.cljc, 78:7
non-optional fake from unit/unused_fakes_self_test\.cljc, 79:7"
       :cljs
       #"^Self-test: no call detected for:
non-optional fake from test/unit/unused_fakes_self_test\.cljc, 77:7
non-optional fake from test/unit/unused_fakes_self_test\.cljc, 78:7
non-optional fake from test/unit/unused_fakes_self_test\.cljc, 79:7")
    (f/with-fakes
      (f/fake [f/any? nil])
      (f/fake [f/any? nil])
      (f/fake [f/any? nil]))))

;;;;;;;;;;;;;;;;;;;;;;;;; reify-fake
(u/-deftest
  "user is not warned if reified protocol fake was never called"
  (u/-is-error-thrown
    #?(:clj
       #"^Self-test: no call detected for:\nnon-optional fake from unit/unused_fakes_self_test\.cljc, 90:7 \(p/AnimalProtocol, speak\)"
       :cljs
       #"^Self-test: no call detected for:\nnon-optional fake from test/unit/unused_fakes_self_test\.cljc, 90:7 \(p/AnimalProtocol, speak\)")
    (f/with-fakes
      (f/reify-fake p/AnimalProtocol
                    (speak :fake [f/any? nil])))))

(u/-deftest
  "user is not warned if reified protocol fake was never called (explicit context)"
  (u/-is-error-thrown
    #?(:clj
       #"^Self-test: no call detected for:\nnon-optional fake from unit/unused_fakes_self_test\.cljc, 101:7 \(p/AnimalProtocol, speak\)"
       :cljs
       #"^Self-test: no call detected for:\nnon-optional fake from test/unit/unused_fakes_self_test\.cljc, 101:7 \(p/AnimalProtocol, speak\)")
    (let [ctx (fc/context)]
      (fc/reify-fake ctx p/AnimalProtocol
                     (speak :fake [f/any? nil]))
      (fc/self-test-unused-fakes ctx))))

;TODO:
;#?(:cljs
;   (u/-deftest
;     "(async) user is warned if fake was never called"
;     (async done
;       (u/-is-error-thrown
;         #"^Self-test: 1no call detected for:\nnon-optional fake from test/unit/unused_fakes_self_test\.cljc, 87:12"
;         (f/with-fakes
;           (f/fake [f/any? nil])))
;       (done))))