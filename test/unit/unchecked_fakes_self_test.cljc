(ns unit.unchecked-fakes-self-test
  #?@(:clj  [
             (:require
               [clojure.test :refer :all]
               [clj-fakes.core :as f]
               [clj-fakes.context :as fc]
               [unit.fixtures.protocols :as p]
               )]
      :cljs [(:require
               [cljs.test :refer-macros [is testing]]
               [clj-fakes.core :as f :include-macros true]
               [clj-fakes.context :as fc :include-macros true]
               [unit.fixtures.protocols :as p]
               )
             ]))

(f/-deftest
  "user is warned if fake was never checked"
  (f/-is-error-thrown
    #?(:clj
       #"^Self-test: no check performed on:\nrecorded fake from unit/unchecked_fakes_self_test\.cljc, 25:7"
       :cljs
       #"^Self-test: no check performed on:\nrecorded fake from test/unit/unchecked_fakes_self_test\.cljc, 25:7")
    (f/with-fakes
      (f/recorded-fake [f/any? nil]))))

(f/-deftest
  "user is not warned if fake was never called if it's explicitly marked as checked"
  (f/with-fakes
    (f/mark-checked (f/recorded-fake [f/any? nil]))))

(f/-deftest
  "user is not warned if fake was never checked in case of exception inside the context"
  (f/-is-error-thrown
    #"expected"
    (f/with-fakes
      (f/recorded-fake [f/any? nil])
      (throw (ex-info "expected" {})))))

#?(:cljs
   (f/-deftest
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

(f/-deftest
  "self-test works with explicit context"
  (f/-is-error-thrown
    #?(:clj
       #"^Self-test: no check performed on:\nrecorded fake from unit/unchecked_fakes_self_test\.cljc, 61:7"
       :cljs
       #"^Self-test: no check performed on:\nrecorded fake from test/unit/unchecked_fakes_self_test\.cljc, 61:7")
    (let [ctx (fc/context)]
      (fc/recorded-fake ctx [f/any? nil])
      (fc/self-test-unchecked-fakes ctx))))

(f/-deftest
  "self-test about unchecked recorded fakes is more important than the one about unused fakes"
  (f/-is-error-thrown
    #?(:clj
       #"^Self-test: no check performed on:\nrecorded fake from unit/unchecked_fakes_self_test\.cljc, 73:7"
       :cljs
       #"^Self-test: no check performed on:\nrecorded fake from test/unit/unchecked_fakes_self_test\.cljc, 73:7")
    (f/with-fakes
      (f/fake [f/any? nil])
      (f/recorded-fake [f/any? nil]))))

(f/-deftest
  "user is warned if several fakes were not checked"
  (f/-is-error-thrown
    #?(:clj
       #"^Self-test: no check performed on:
recorded fake from unit/unchecked_fakes_self_test\.cljc, 89:7
recorded fake from unit/unchecked_fakes_self_test\.cljc, 90:7
recorded fake from unit/unchecked_fakes_self_test\.cljc, 91:7"
       :cljs
       #"^Self-test: no check performed on:
recorded fake from test/unit/unchecked_fakes_self_test.cljc, 89:7
recorded fake from test/unit/unchecked_fakes_self_test.cljc, 90:7
recorded fake from test/unit/unchecked_fakes_self_test.cljc, 91:7")
    (f/with-fakes
      (f/recorded-fake [f/any? nil])
      (f/recorded-fake [f/any? nil])
      (f/recorded-fake [f/any? nil]))))

;;;;;;;;;;;;;;;;;;;;;;;;; reify-fake
(f/-deftest
  "user is warned if reified protocol fake was never checked"
  (f/-is-error-thrown
    #?(:clj
       #"^Self-test: no check performed on:\nrecorded fake from unit/unchecked_fakes_self_test\.cljc, 102:7 \(p/AnimalProtocol, speak\)"
       :cljs
       #"^Self-test: no check performed on:\nrecorded fake from test/unit/unchecked_fakes_self_test\.cljc, 102:7 \(p/AnimalProtocol, speak\)")
    (f/with-fakes
      (f/reify-fake p/AnimalProtocol
                    (speak :recorded-fake [f/any? nil])))))

(f/-deftest
  "user is warned if reified protocol fake was never checked (explicit context)"
  (f/-is-error-thrown
    #?(:clj
       #"^Self-test: no check performed on:\nrecorded fake from unit/unchecked_fakes_self_test\.cljc, 113:7 \(p/AnimalProtocol, speak\)"
       :cljs
       #"^Self-test: no check performed on:\nrecorded fake from test/unit/unchecked_fakes_self_test\.cljc, 113:7 \(p/AnimalProtocol, speak\)")
    (let [ctx (fc/context)]
      (fc/reify-fake ctx p/AnimalProtocol
                     (speak :recorded-fake [f/any? nil]))
      (fc/self-test-unchecked-fakes ctx))))