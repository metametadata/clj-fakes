(ns unit.reify-fake
  (:require
    [clojure.test :refer [is testing]]
    [unit.utils :as u]
    [clj-fakes.core :as f]
    [clj-fakes.context :as fc]
    [unit.fixtures.protocols :as p :refer [AnimalProtocol]])
  #?(:clj
     (:import [interop InterfaceFixture])))

(defprotocol LocalProtocol
  (bar [this]))

;;;;;;;;;;;;;;;;;;;;;;;;;; optional-fake
(u/-deftest
  "simplest method from same-namespace-protocol can be an optional fake"
  (f/with-fakes
    (let [foo (f/reify-fake LocalProtocol
                            (bar :optional-fake [f/any "baz"]))]
      (is (= "baz" (bar foo))))))

(u/-deftest
  "simplest method from fully-qualified protocol can be an optional fake"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :optional-fake [f/any "moo"]))]
      (is (= "moo" (p/speak cow))))))

(u/-deftest
  "simplest method from refered protocol can be an optional fake"
  (f/with-fakes
    (let [cow (f/reify-fake AnimalProtocol
                            (speak :optional-fake [f/any "moo"]))]
      (is (= "moo" (p/speak cow))))))

(u/-deftest
  "method with an arg can be an optional fake"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (eat :optional-fake [f/any "om-nom"]))]
      (is (= "om-nom" (p/eat cow :grass :water))))))

(u/-deftest
  "method args are passed to a fake on call"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (eat :optional-fake [[f/any f/any] #(vector %1 %2 %3)]))]
      (is (= [cow "grass" "water"] (p/eat cow "grass" "water"))))))

(u/-deftest
  "args matchers get correct args on call"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (eat :optional-fake [["grass" "water"] "ate as usual"
                                                 ["banana" nil] "ate a banana"
                                                 ["" "juice"] "drank some juice"]))]
      (is (= "ate as usual" (p/eat cow "grass" "water")))
      (is (= "drank some juice" (p/eat cow "" "juice")))
      (is (= "ate a banana" (p/eat cow "banana" nil))))))

(u/-deftest
  "several methods can be reified"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :optional-fake [f/any "moo"])
                            (eat :optional-fake [[1 2] "ate"]))]
      (is (= "moo" (p/speak cow)))
      (is (= "ate" (p/eat cow 1 2))))))

(u/-deftest
  "multiple method arglists are supported"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :optional-fake [[] "moo"
                                                   [f/any] #(str "moo, " %2)
                                                   [f/any f/any] #(str "moo, " %2 " and " %3)]))]
      (is (= "moo, User" (p/speak cow "User")))
      (is (= "moo" (p/speak cow)))
      (is (= "moo, Bob and Alice" (p/speak cow "Bob" "Alice"))))))

(u/-deftest
  "recursion works"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :optional-fake [["you"] "moo to you!"
                                                   [f/any] (fn [this _]
                                                             (p/speak this "you"))]))]
      (is (= "moo to you!" (p/speak cow "Bob"))))))

(u/-deftest
  "calling a non-reified method throws an exception"
  (u/-is-exception-thrown
    java.lang.AbstractMethodError
    js/Error
    #""
    (f/with-fakes
      (let [cow (f/reify-fake p/AnimalProtocol
                              (speak :optional-fake [f/any nil]))]
        (p/sleep cow)))))

(u/-deftest
  "config is not required for optional fake"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :optional-fake))]
      (is (not= (p/speak cow "Bob") (p/speak cow "Bob"))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;; recorded-fake
(u/-deftest
  "simplest method can be a recorded fake"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :recorded-fake [f/any "moo"]))]
      (is (= "moo" (p/speak cow)))
      (is (f/was-called (f/method cow p/speak) [cow])))))

(u/-deftest
  "recorded fake's args matchers get correct args on call"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (eat :recorded-fake [["grass" "water"] "ate as usual"
                                                 ["banana" nil] "ate a banana"
                                                 ["" "juice"] "drank some juice"]))]
      (is (= "ate as usual" (p/eat cow "grass" "water")))
      (is (= "drank some juice" (p/eat cow "" "juice")))
      (is (= "ate a banana" (p/eat cow "banana" nil)))

      (is (f/method-was-called p/eat cow ["grass" "water"]))
      (is (f/method-was-called p/eat cow ["" "juice"]))
      (is (f/method-was-called p/eat cow ["banana" nil])))))

(u/-deftest
  "the same method can be recorded in different fake instances"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :recorded-fake [f/any #(str "moo, " %2)]))
          dog (f/reify-fake p/AnimalProtocol
                            (speak :recorded-fake [f/any #(str "woof, " %2)]))]

      (is (= "moo, Bob" (p/speak cow "Bob")))
      (is (= "woof, Alice" (p/speak dog "Alice")))

      (is (f/method-was-called p/speak cow ["Bob"]))
      (is (f/method-was-called p/speak dog ["Alice"])))))

(u/-deftest
  "several methods can be recorded in the fake instance"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :recorded-fake [f/any #(str "moo, " %2)])
                            (sleep :recorded-fake [f/any "zzz"]))]

      (is (= "moo, Bob" (p/speak cow "Bob")))
      (is (= "zzz" (p/sleep cow)))

      (is (f/method-was-called p/speak cow ["Bob"]))
      (is (f/method-was-called p/sleep cow [])))))

(u/-deftest
  "config is not required for recorded fake"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :recorded-fake))]
      (is (not= (p/speak cow "Bob") (p/speak cow "Bob")))
      (is (f/method-was-called p/speak cow ["Bob"])))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; fake
(u/-deftest
  "simplest method can be a fake"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (sleep :fake [[] "zzz"]))]
      (is (= "zzz" (p/sleep cow))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; several protocols
(u/-deftest
  "several protocols can be reified at once with optional fakes"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :optional-fake [f/any "moo"])
                            (eat :optional-fake [["grass" "water"] "om-nom"])

                            p/FileProtocol
                            (save :optional-fake [f/any "saved"]))]
      (is (= "moo" (p/speak cow)))
      (is (= "saved" (p/save cow))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Java interfaces
#?(:clj
   (u/-deftest
     "java.lang.CharSequence/length can be reified with fake"
     (f/with-fakes
       (let [foo (f/reify-fake
                   java.lang.CharSequence
                   (length :fake [[] 123]))]
         (is (= 123 (.length foo)))))))

#?(:clj
   (u/-deftest
     "java.lang.CharSequence/charAt can be reified with optional fake"
     (f/with-fakes
       (let [foo (f/reify-fake
                   java.lang.CharSequence
                   (charAt :optional-fake [[100] \a]))]
         (is (= \a (.charAt foo 100)))))))

#?(:clj
   (u/-deftest
     "java.lang.CharSequence/subSequence can be reified with optional fake"
     (f/with-fakes
       (let [foo (f/reify-fake
                   java.lang.CharSequence
                   (subSequence :optional-fake [[100 200] "bar"]))]
         (is (= "bar" (.subSequence foo 100 200)))))))

#?(:clj
   (u/-deftest
     "java.lang.Appendable/append (overloaded method) can be reified with optional fake"
     (f/with-fakes
       (let [my-char-seq "123"
             expected1 (new StringBuffer)
             expected2 (new StringBuffer)
             foo (f/reify-fake
                   java.lang.Appendable
                   (append :optional-fake [[\a] expected1
                                           [my-char-seq] expected2]))]
         (is (= expected1 (.append foo \a)))
         (is (= expected2 (.append foo my-char-seq)))))))

#?(:clj
   (u/-deftest
     "overloaded java interface method with different return types can be reified with optional fake"
     (f/with-fakes
       (let [foo (f/reify-fake
                   interop.InterfaceFixture
                   (overloadedMethodWithDifferentReturnTypes :optional-fake [[\a] 100
                                                                             [\b] 200
                                                                             [314] 300
                                                                             [3.14] 400
                                                                             ["bar"] "500"
                                                                             [] "600"
                                                                             [true] "700"
                                                                             [100 200] true]))]
         (is (= 100 (.overloadedMethodWithDifferentReturnTypes foo \a)))
         (is (= 200 (.overloadedMethodWithDifferentReturnTypes foo \b)))
         (is (= 300 (.overloadedMethodWithDifferentReturnTypes foo 314)))
         (is (= 400 (.overloadedMethodWithDifferentReturnTypes foo 3.14)))
         (is (= "500" (.overloadedMethodWithDifferentReturnTypes foo "bar")))
         (is (= "600" (.overloadedMethodWithDifferentReturnTypes foo)))
         (is (= "700" (.overloadedMethodWithDifferentReturnTypes foo true)))
         (is (= true (.overloadedMethodWithDifferentReturnTypes foo 100 200)))))))

; TODO:
;#?(:clj
;   (u/-deftest
;     "java.lang.Appendable/append (overloaded method) can be reified with recorded fake"
;     (f/with-fakes
;       (let [foo (f/reify-fake
;                   java.lang.Appendable
;                   [append :recorded-fake])]
;         (is (satisfies? fc/FakeReturnValue (.append foo \a)))))))

#?(:clj
   (u/-deftest
     "IFn/invoke can be reified with recordable fake"
     (f/with-fakes
       (let [foo (f/reify-fake
                   clojure.lang.IFn
                   (invoke :recorded-fake))]
         (foo 123)
         (foo 1 2 3)

         (is (f/method-was-called "invoke" foo [123]))
         (is (f/method-was-called "invoke" foo [1 2 3]))))))

#?(:cljs
   (u/-deftest
     "IFn/invoke can be reified with recordable fake"
     (f/with-fakes
       (let [foo (f/reify-fake
                   IFn
                   (-invoke :recorded-fake))]
         (foo 123)
         (foo 1 2 3)
         (is (f/method-was-called -invoke foo [123]))
         (is (f/method-was-called -invoke foo [1 2 3]))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Object
#?(:cljs
   (u/-deftest
     "Object can be reified with a new optional-fake method"
     (f/with-fakes
       (let [foo (f/reify-fake Object
                               (new-method1 [] :optional-fake)
                               (new-method2 [x y] :optional-fake [[f/any f/any] #(+ %2 %3)])
                               (new-method3 [x y z] :optional-fake [f/any "bar"]))]
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
       (let [foo (f/reify-fake Object
                               (new-method1 [] :fake [[] "bla"])
                               (new-method2 [x y] :fake [[f/any f/any] #(+ %2 %3)])
                               (new-method3 [x y z] :fake [f/any "bar"]))]
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
       (let [foo (f/reify-fake Object
                               (new-method1 [x] :recorded-fake)
                               (new-method2 [x y] :recorded-fake [[f/any f/any] #(+ %2 %3)]))]
         (is (instance? fc/FakeReturnValue (.new-method1 foo 777)))
         (is (= 5 (.new-method2 foo 2 3)))

         (is (f/method-was-called "new-method1" foo [777]))
         (is (f/method-was-called "new-method2" foo [2 3]))))))

#?(:cljs
   (u/-deftest
     "Object/toString can be faked"
     (f/with-fakes
       (let [foo (f/reify-fake Object
                               (toString [] :recorded-fake [[] "bla"]))]
         (is (= "bla" (str foo)))
         (is (f/method-was-called "toString" foo []))))))

#?(:clj
   (u/-deftest
     "Object/toString can be faked"
     (f/with-fakes
       (let [foo (f/reify-fake Object
                               (toString :recorded-fake [[] "bla"]))]
         (is (= "bla" (str foo)))
         (is (f/method-was-called "toString" foo []))))))

#?(:clj
   (u/-deftest
     "java.lang.Object is also supported"
     (f/with-fakes
       (let [foo (f/reify-fake java.lang.Object
                               (toString :fake [[] "bla"]))]
         (is (= "bla" (str foo)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;; integration
(u/-deftest
  "several methods can be fakes using different fake types"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :recorded-fake [f/any #(str "moo, " %2)])
                            (sleep :optional-fake [f/any "zzz"]))]

      (is (= "moo, Bob" (p/speak cow "Bob")))
      (is (= "zzz" (p/sleep cow)))
      (is (f/method-was-called p/speak cow ["Bob"])))))

(u/-deftest
  "several methods can be fakes using different fake types (using explicit context)"
  (let [ctx (fc/context)]
    (let [cow (fc/reify-fake ctx p/AnimalProtocol
                             (speak :recorded-fake [f/any #(str "moo, " %2)])
                             (sleep :optional-fake [f/any "zzz"]))]

      (is (= "moo, Bob" (p/speak cow "Bob")))
      (is (= "zzz" (p/sleep cow)))
      (is (fc/method-was-called ctx p/speak cow ["Bob"])))))

(u/-deftest
  "several protocols can be reified at once with different fake types"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :recorded-fake [f/any #(str "moo, " %2)])
                            (sleep :optional-fake [f/any "zzz"])

                            p/FileProtocol
                            (save :recorded-fake))]
      (p/save cow)
      (is (= "moo, Bob" (p/speak cow "Bob")))
      (is (= "zzz" (p/sleep cow)))
      (is (f/method-was-called p/speak cow ["Bob"]))
      (is (f/method-was-called p/save cow [])))))

#?(:clj
   (u/-deftest
     "protocol and Java interface can be reified at the same time"
     (f/with-fakes
       (let [foo (f/reify-fake
                   p/FileProtocol
                   (save :recorded-fake)

                   java.lang.CharSequence
                   (charAt :recorded-fake [f/any \a]))]
         (p/save foo)
         (.charAt foo 100)

         (is (f/method-was-called p/save foo []))
         (is (f/method-was-called "charAt" foo [100]))))))

(defprotocol WebService
  (save [this data]))

(u/-deftest
  "real example: method can be faked with cyclical return values"
  (f/with-fakes
    (let [service (f/reify-fake WebService
                                (save :fake [[:--data--]
                                             (f/cyclically [503 200])]))]
      (is (= 503 (save service :--data--)))
      (is (= 200 (save service :--data--)))
      (is (= 503 (save service :--data--))))))