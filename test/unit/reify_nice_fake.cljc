(ns unit.reify-nice-fake
  (:require
    [clojure.test :refer [is testing]]
    [unit.utils :as u]
    [clj-fakes.core :as f]
    [clj-fakes.context :as fc]
    [unit.fixtures.protocols :as p :refer [AnimalProtocol]]))

(defprotocol LocalProtocol
  (bar [this] [this x y])
  (baz [this x])
  (qux [this x y z]))

(defn is-faked
  [method & args]
  ; strings are compared instead of values, because, presumably, 'lein test-refresh' plugin incorrectly reloads deftype
  ; and tests start failing on every change to contex.cljc
  (is (= #?(:clj  "clj_fakes.context.FakeReturnValue"
            :cljs "clj-fakes.context/FakeReturnValue")
         (pr-str (type (apply method args)))))

  (is (not= (apply method args)
            (apply method args))))

(u/-deftest
  "methods from same-namespace-protocol can be automatically faked"
  (f/with-fakes
    (let [foo (f/reify-nice-fake LocalProtocol)]
      (is-faked bar foo)
      (is-faked bar foo 1 2)
      (is-faked baz foo 100)
      (is-faked qux foo 1 2 3))))

(u/-deftest
  "method from fully-qualified protocol can be automatically faked"
  (f/with-fakes
    (let [cow (f/reify-nice-fake p/AnimalProtocol)]
      (is-faked p/speak cow))))

(u/-deftest
  "method from refered protocol can be automatically faked"
  (f/with-fakes
    (let [cow (f/reify-nice-fake AnimalProtocol)]
      (is-faked p/speak cow))))

(u/-deftest
  "works in explicit context"
  (let [ctx (fc/context)
        cow (fc/reify-nice-fake ctx AnimalProtocol)]
    (is-faked p/speak cow)))
;
;(u/-deftest
;  "method with arglists can be automatically reified even if one of arglists is faked explicitly"
;  (f/with-fakes
;    (let [foo (f/reify-nice-fake LocalProtocol
;                                 [bar :optional-fake [f/any? "bar"]])]
;      (is (= "bar" (bar foo)))
;      (is-faked bar foo 1 2))))

(u/-deftest
  "several protocols can be automatically reified"
  (f/with-fakes
    (let [cow (f/reify-nice-fake p/AnimalProtocol
                                 p/FileProtocol)]
      (is-faked p/speak cow)
      (is-faked p/speak cow 1)
      (is-faked p/speak cow 1 2)
      (is-faked p/eat cow 100 200)
      (is-faked p/sleep cow)

      (is-faked p/save cow)
      (is-faked p/scan cow))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Java interface
#?(:clj
   (u/-deftest
     "IFn cannot be automatically reified"
     (u/-is-exception-thrown
       java.lang.AbstractMethodError
       "n/a"
       #""
       (f/with-fakes
         (let [foo (f/reify-nice-fake clojure.lang.IFn)]
           (foo 1 2 3 4))))))

#?(:clj
   (u/-deftest
     "java.lang.CharSequence can be explicitly reified alongside automatically reified protocol"
     (f/with-fakes
       (let [foo (f/reify-nice-fake
                   java.lang.CharSequence
                   (charAt :fake [[100] \a])

                   p/FileProtocol)]
         (is (= \a (.charAt foo 100)))

         (is-faked p/save foo)
         (is-faked p/scan foo)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Object
#?(:cljs
   (u/-deftest
     "Object can be reified with a new optional-fake method"
     (f/with-fakes
       (let [foo (f/reify-nice-fake Object
                                    (new-method1 [] :optional-fake)
                                    (new-method2 [x y] :optional-fake [[f/any? f/any?] #(+ %2 %3)])
                                    (new-method3 [x y z] :optional-fake [f/any? "bar"]))]
         (is (instance? fc/FakeReturnValue (.new-method1 foo)))

         (is (= 5 (.new-method2 foo 2 3)))

         (is (= "bar" (.new-method3 foo)))
         (is (= "bar" (.new-method3 foo 1)))
         (is (= "bar" (.new-method3 foo 1 2)))
         (is (= "bar" (.new-method3 foo 1 2 3)))))))

#?(:cljs
   (u/-deftest
     "Object can be reified with a new fake method"
     (f/with-fakes
       (let [foo (f/reify-nice-fake Object
                                    (new-method1 [] :fake [[] "bla"])
                                    (new-method2 [x y] :fake [[f/any? f/any?] #(+ %2 %3)])
                                    (new-method3 [x y z] :fake [f/any? "bar"]))]
         (is (= "bla" (.new-method1 foo)))

         (is (= 5 (.new-method2 foo 2 3)))

         (is (= "bar" (.new-method3 foo)))
         (is (= "bar" (.new-method3 foo 1)))
         (is (= "bar" (.new-method3 foo 1 2)))
         (is (= "bar" (.new-method3 foo 1 2 3)))))))

#?(:cljs
   (u/-deftest
     "Object can be reified with a new recorded fake method"
     (f/with-fakes
       (let [foo (f/reify-nice-fake Object
                                    (new-method1 [x] :recorded-fake)
                                    (new-method2 [x y] :recorded-fake [[f/any? f/any?] #(+ %2 %3)]))]
         (is (instance? fc/FakeReturnValue (.new-method1 foo 777)))
         (is (= 5 (.new-method2 foo 2 3)))

         (is (f/method-was-called "new-method1" foo [777]))
         (is (f/method-was-called "new-method2" foo [2 3]))))))

(u/-deftest
  "Object/toString cannot be automatically reified"
  (f/with-fakes
    (let [foo (f/reify-nice-fake Object)]
      (is (not (instance? clj_fakes.context.FakeReturnValue (.toString foo)))))))

#?(:cljs
   (u/-deftest
     "Object/toString can be faked"
     (f/with-fakes
       (let [foo (f/reify-nice-fake Object
                                    (toString [] :recorded-fake [[] "bla"]))]
         (is (= "bla" (str foo)))
         (is (f/method-was-called "toString" foo []))))))

#?(:clj
   (u/-deftest
     "Object/toString can be faked"
     (f/with-fakes
       (let [foo (f/reify-nice-fake Object
                                    (toString :recorded-fake [[] "bla"]))]
         (is (= "bla" (str foo)))
         (is (f/method-was-called "toString" foo []))))))

#?(:clj
   (u/-deftest
     "java.lang.Object is also supported"
     (f/with-fakes
       (let [foo (f/reify-nice-fake java.lang.Object
                                    (toString :fake [[] "bla"]))]
         (is (= "bla" (str foo)))))))

#?(:clj
   (u/-deftest
     "Object/toString can be explicitly reified alongside automatically reified protocol"
     (f/with-fakes
       (let [foo (f/reify-nice-fake Object
                                    (toString :recorded-fake [[] "bla"])

                                    p/FileProtocol)]
         (is (= "bla" (str foo)))
         (is (f/method-was-called "toString" foo []))

         (is-faked p/save foo)
         (is-faked p/scan foo)))))

#?(:cljs
   (u/-deftest
     "Object/toString can be explicitly reified alongside automatically reified protocol"
     (f/with-fakes
       (let [foo (f/reify-nice-fake Object
                                    (toString [] :recorded-fake [[] "bla"])

                                    p/FileProtocol)]
         (is (= "bla" (str foo)))
         (is (f/method-was-called "toString" foo []))

         (is-faked p/save foo)
         (is-faked p/scan foo)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;; integration
(u/-deftest
  "several protocols can be automatically reified and be partially explicitly faked"
  (f/with-fakes
    (let [cow (f/reify-nice-fake p/AnimalProtocol
                                 (sleep :recorded-fake [[] "zzz"])

                                 p/FileProtocol
                                 (scan :recorded-fake))]

      ; AnimalProtocol
      (is-faked p/speak cow)
      (is-faked p/speak cow 1)
      (is-faked p/speak cow 2 3)
      (is-faked p/eat cow 100 200)
      (is (= "zzz" (p/sleep cow)))

      ; FileProtocol
      (is-faked p/save cow)
      (p/scan cow)

      ; AnimalProtocol
      (f/method-was-called p/sleep cow [])

      ; FileProtocol
      (f/method-was-called p/scan cow []))))