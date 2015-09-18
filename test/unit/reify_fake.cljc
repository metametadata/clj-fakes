(ns unit.reify-fake
  #?@(:clj  [
             (:require
               [clojure.test :refer :all]
               [clojure.walk :as w]
               [clj-fakes.core :as f]
               [clj-fakes.context :as fc]
               [unit.fixtures.protocols :as p :refer [AnimalProtocol]]
               )]
      :cljs [(:require
               [cljs.test :refer-macros [is testing deftest]]
               [clj-fakes.core :as f :include-macros true]
               [clj-fakes.context :as fc :include-macros true]
               [unit.fixtures.protocols :as p :refer [AnimalProtocol]]
               )
             ]))

(defprotocol LocalProtocol
  (bar [this]))

;;;;;;;;;;;;;;;;;;;;;;;;;; optional-fake
(f/-deftest
  "simplest method from same-namespace-protocol can be an optional fake"
  (f/with-fakes
    (let [foo (f/reify-fake LocalProtocol
                            (bar :optional-fake [f/any? "baz"]))]
      (is (= "baz" (bar foo))))))

(f/-deftest
  "simplest method from fully-qualified protocol can be an optional fake"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :optional-fake [f/any? "moo"]))]
      (is (= "moo" (p/speak cow))))))

(f/-deftest
  "simplest method from refered protocol can be an optional fake"
  (f/with-fakes
    (let [cow (f/reify-fake AnimalProtocol
                            (speak :optional-fake [f/any? "moo"]))]
      (is (= "moo" (p/speak cow))))))

(f/-deftest
  "method with an arg can be an optional fake"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (eat :optional-fake [f/any? "om-nom"]))]
      (is (= "om-nom" (p/eat cow :grass :water))))))

(f/-deftest
  "method args are passed to a fake on call"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (eat :optional-fake [[f/any? f/any?] #(vector %1 %2 %3)]))]
      (is (= [cow "grass" "water"] (p/eat cow "grass" "water"))))))

(f/-deftest
  "arg matchers get correct args on call"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (eat :optional-fake [["grass" "water"] "ate as usual"
                                                 ["banana" nil] "ate a banana"
                                                 ["" "juice"] "drank some juice"]))]
      (is (= "ate as usual" (p/eat cow "grass" "water")))
      (is (= "drank some juice" (p/eat cow "" "juice")))
      (is (= "ate a banana" (p/eat cow "banana" nil))))))

(f/-deftest
  "several methods can be reified"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :optional-fake [f/any? "moo"])
                            (eat :optional-fake [[1 2] "ate"]))]
      (is (= "moo" (p/speak cow)))
      (is (= "ate" (p/eat cow 1 2))))))

(f/-deftest
  "multiple method arglists are supported"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :optional-fake [[] "moo"
                                                   [f/any?] #(str "moo, " %2)
                                                   [f/any? f/any?] #(str "moo, " %2 " and " %3)]))]
      (is (= "moo, User" (p/speak cow "User")))
      (is (= "moo" (p/speak cow)))
      (is (= "moo, Bob and Alice" (p/speak cow "Bob" "Alice"))))))

(f/-deftest
  "recursion works"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :optional-fake [["you"] "moo to you!"
                                                   [f/any?] (fn [this _]
                                                              (p/speak this "you"))]))]
      (is (= "moo to you!" (p/speak cow "Bob"))))))

(f/-deftest
  "calling a non-reified method throws an exception"
  (f/-is-exception-thrown
    java.lang.AbstractMethodError
    js/Error
    #""
    (f/with-fakes
      (let [cow (f/reify-fake p/AnimalProtocol
                              (speak :optional-fake [f/any? nil]))]
        (p/sleep cow)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;; recorded-fake
(f/-deftest
  "simplest method can be a recorded fake"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :recorded-fake [f/any? "moo"]))]
      (is (= "moo" (p/speak cow)))
      (is (f/was-called (f/method cow p/speak) [cow])))))

(f/-deftest
  "recorded fake's arg matchers get correct args on call"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (eat :recorded-fake [["grass" "water"] "ate as usual"
                                                 ["banana" nil] "ate a banana"
                                                 ["" "juice"] "drank some juice"]))]
      (is (= "ate as usual" (p/eat cow "grass" "water")))
      (is (= "drank some juice" (p/eat cow "" "juice")))
      (is (= "ate a banana" (p/eat cow "banana" nil)))

      (is (f/was-called-on cow p/eat ["grass" "water"]))
      (is (f/was-called-on cow p/eat ["" "juice"]))
      (is (f/was-called-on cow p/eat ["banana" nil])))))

(f/-deftest
  "the same method can be recorded in different fake instances"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :recorded-fake [f/any? #(str "moo, " %2)]))
          dog (f/reify-fake p/AnimalProtocol
                            (speak :recorded-fake [f/any? #(str "woof, " %2)]))]

      (is (= "moo, Bob" (p/speak cow "Bob")))
      (is (= "woof, Alice" (p/speak dog "Alice")))

      (is (f/was-called-on cow p/speak ["Bob"]))
      (is (f/was-called-on dog p/speak ["Alice"])))))

(f/-deftest
  "several methods can be recorded in the fake instance"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :recorded-fake [f/any? #(str "moo, " %2)])
                            (sleep :recorded-fake [f/any? "zzz"]))]

      (is (= "moo, Bob" (p/speak cow "Bob")))
      (is (= "zzz" (p/sleep cow)))

      (is (f/was-called-on cow p/speak ["Bob"]))
      (is (f/was-called-on cow p/sleep [])))))

(f/-deftest
  "config is not required for recorded fake"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :recorded-fake))]
      (p/speak cow "Bob")
      (is (f/was-called-on cow p/speak ["Bob"])))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; fake
(f/-deftest
  "simplest method can be a fake"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (sleep :fake [f/any? "zzz"]))]
      (is (= "zzz" (p/sleep cow))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; several protocols
(f/-deftest
  "several protocols can be reified at once with optional fakes"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :optional-fake [f/any? "moo"])
                            (eat :optional-fake [["grass" "water"] "om-nom"])

                            p/FileProtocol
                            (save :optional-fake [f/any? "saved"]))]
      (is (= "moo" (p/speak cow)))
      (is (= "saved" (p/save cow))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Java interfaces
#?(:clj
   (f/-deftest
     "java.lang.CharSequence/length can be reified with fake"
     (f/with-fakes
       (let [foo (f/reify-fake
                   java.lang.CharSequence
                   (length :fake [[] 123]))]
         (is (= 123 (.length foo)))))))

#?(:clj
   (f/-deftest
     "java.lang.CharSequence/charAt can be reified with optional fake"
     (f/with-fakes
       (let [foo (f/reify-fake
                   java.lang.CharSequence
                   (charAt :optional-fake [[100] \a]))]
         (is (= \a (.charAt foo 100)))))))

#?(:clj
   (f/-deftest
     "java.lang.CharSequence/subSequence can be reified with optional fake"
     (f/with-fakes
       (let [foo (f/reify-fake
                   java.lang.CharSequence
                   (subSequence :optional-fake [[100 200] "bar"]))]
         (is (= "bar" (.subSequence foo 100 200)))))))

#?(:clj
   (f/-deftest
     "java.lang.Appendable/append (overloaded method) can be reified with optional fake"
     (f/with-fakes
       (let [expected (new StringBuffer)
             foo (f/reify-fake
                   java.lang.Appendable
                   (append :optional-fake [[\a] expected]))]
         (is (= expected (.append foo \a)))))))

; TODO:
;#?(:clj
;   (f/-deftest
;     "java.lang.Appendable/append (overloaded method) can be reified with recorded fake"
;     (f/with-fakes
;       (let [foo (f/reify-fake
;                   java.lang.Appendable
;                   [append :recorded-fake])]
;         (is (satisfies? fc/FakeReturnValue (.append foo \a)))))))

#?(:clj
   (f/-deftest
     "IFn/invoke can be reified with recordable fake"
     (f/with-fakes
       (let [foo (f/reify-fake
                   clojure.lang.IFn
                   (invoke :recorded-fake))]
         (foo 123)
         (foo 1 2 3)
         (is (f/was-called-on foo "invoke" [123]))
         (is (f/was-called-on foo "invoke" [1 2 3]))))))

#?(:cljs
   (f/-deftest
     "IFn/invoke can be reified with recordable fake"
     (f/with-fakes
       (let [foo (f/reify-fake
                   IFn
                   (-invoke :recorded-fake))]
         (foo 123)
         (foo 1 2 3)
         (is (f/was-called-on foo -invoke [123]))
         (is (f/was-called-on foo -invoke [1 2 3]))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Object
; TODO: the same for cljs
#?(:clj
   (f/-deftest
     "Object/toString can be reified"
     (f/with-fakes
       (let [foo (f/reify-fake Object
                               (toString :recorded-fake [[] "bla"]))]
         (is (= "bla" (str foo)))
         (is (f/was-called-on foo "toString"))))))

#?(:clj
   (f/-deftest
     "java.lang.Object is also supported"
     (f/with-fakes
       (let [foo (f/reify-fake java.lang.Object
                               (toString :fake [[] "bla"]))]
         (is (= "bla" (str foo)))))))

; TODO:
;#?(:cljs
;   (f/-deftest
;     "Object can be reified with any new methods"
;     (f/with-fakes
;       (let [foo (f/reify-fake Object
;                               [bar :optional-fake [f/any? 145]
;                                #_[[1] "1"
;                                   [1 2] "1 2 !!!!!!"
;                                   [] "none"]])]
;         (is (= "1" (.bar foo 1)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;; integration
(f/-deftest
  "several methods can be fakes using different fake types"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :recorded-fake [f/any? #(str "moo, " %2)])
                            (sleep :optional-fake [f/any? "zzz"]))]

      (is (= "moo, Bob" (p/speak cow "Bob")))
      (is (= "zzz" (p/sleep cow)))
      (is (f/was-called-on cow p/speak ["Bob"])))))

(f/-deftest
  "several methods can be fakes using different fake types (using explicit context)"
  (let [ctx (fc/context)]
    (let [cow (fc/reify-fake ctx p/AnimalProtocol
                             (speak :recorded-fake [f/any? #(str "moo, " %2)])
                             (sleep :optional-fake [f/any? "zzz"]))]

      (is (= "moo, Bob" (p/speak cow "Bob")))
      (is (= "zzz" (p/sleep cow)))
      (is (fc/was-called-on ctx cow p/speak ["Bob"])))))

(f/-deftest
  "several protocols can be reified at once with different fake types"
  (f/with-fakes
    (let [cow (f/reify-fake p/AnimalProtocol
                            (speak :recorded-fake [f/any? #(str "moo, " %2)])
                            (sleep :optional-fake [f/any? "zzz"])

                            p/FileProtocol
                            (save :recorded-fake))]
      (p/save cow)
      (is (= "moo, Bob" (p/speak cow "Bob")))
      (is (= "zzz" (p/sleep cow)))
      (is (f/was-called-on cow p/speak ["Bob"]))
      (is (f/was-called-on cow p/save)))))

#?(:clj
   (f/-deftest
     "protocol and Java interface can be reified at the same time"
     (f/with-fakes
       (let [foo (f/reify-fake
                   p/FileProtocol
                   (save :recorded-fake)

                   java.lang.CharSequence
                   (charAt :recorded-fake [f/any? \a]))]
         (p/save foo)
         (.charAt foo 100)

         (is (f/was-called-on foo p/save))
         (is (f/was-called-on foo "charAt"))))))