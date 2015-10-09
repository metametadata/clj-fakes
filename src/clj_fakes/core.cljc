(ns clj-fakes.core
  "Simpler API for working in implicit dynamic context.
  Implements almost the same set of functions as [[clj-fakes.context]]."
  (:require [clojure.string :as string]
    #?@(:clj  [
            [clj-fakes.context :as fc]
            [clj-fakes.macro :refer :all]
               ]
        :cljs [[clj-fakes.context :as fc :include-macros true]
               [clojure.string :as string]
               ]))
  #?(:cljs
     (:require-macros [clj-fakes.macro :refer [-cljs-env?]])))

;;;;;;;;;;;;;;;;;;;;;;;; Re-exports for usage convenience
(def any? fc/any?)

;;;;;;;;;;;;;;;;;;;;;;;; Core
(def ^{:doc "You can use this atom in your code but do not alter it directly; instead, always use framework API.

Also see [[with-fakes]] macro."}
  ^:dynamic *context* nil)

;;;;;;;;;;;;;;;;;;;;;;;; Macros - with-fakes
(defn with-fakes*
  "Function which drives [[with-fakes]] macro.
  It defines an implicit dynamic [[*context*]] and
  executes function `f` with specified arguments inside the context."
  [f & args]
  (binding [*context* (fc/context)]
    (let [exception-caught? (atom false)]
      (try
        (apply f args)

        (catch #?(:clj Exception :cljs :default) e
          (reset! exception-caught? true)
          (throw e))

        (finally
          (fc/unpatch-all! *context*)

          (when-not @exception-caught?
            (fc/self-test-unchecked-fakes *context*)
            (fc/self-test-unused-fakes *context*)))))))

#?(:clj
   (defmacro with-fakes
     "Defines an implicit dynamic [[*context*]] and executes the body in it.
     Inside the body you can use the simpler [[clj-fakes.core]] API instead of [[clj-fakes.context]] API.

     The block will automatically unpatch all the patched variables and execute self-tests on exiting.
     Self-tests will not be executed if exception was raised inside the body.
     Variables are guaranteed to always be unpatched on exiting the block."
     [& body]
     `(with-fakes* (fn [] ~@body))))

;;;;;;;;;;;;;;;;;;;;;;;; Function fakes
(defn optional-fake
  ([] (fc/optional-fake *context*))
  ([config] (fc/optional-fake *context* config)))

(defn ^:no-doc -position
  [f]
  (fc/-position *context* f))

#?(:clj
   (defmacro fake*
     [form config]
     `(fc/fake* *context* ~form ~config)))

#?(:clj
   (defmacro fake
     [config]
     `(fake* ~&form ~config)))

#?(:clj
   (defmacro recorded-fake*
     ([form] `(fc/recorded-fake* *context* ~form))
     ([form config] `(fc/recorded-fake* *context* ~form ~config))))

#?(:clj
   (defmacro recorded-fake
     ([] `(recorded-fake* ~&form))
     ([config] `(recorded-fake* ~&form ~config))))

(defn calls
  ([] (fc/calls *context*))
  ([k] (fc/calls *context* k)))

(defn mark-checked
  [k]
  (fc/mark-checked *context* k))

;;;;;;;;;;;;;;;;;;;;;;;; Protocol fakes
#?(:clj
   (defmacro reify-fake*
     [form env & specs]
     `(fc/reify-fake* *context* ~form ~env ~@specs)))

#?(:clj
   (defmacro reify-fake
     [& specs]
     `(reify-fake* ~&form ~&env ~@specs)))

#?(:clj
   (defmacro reify-nice-fake*
     [form env & specs]
     `(fc/reify-nice-fake* *context* ~form ~env ~@specs)))

#?(:clj
   (defmacro reify-nice-fake
     [& specs]
     `(reify-nice-fake* ~&form ~&env ~@specs)))

(defn method
  [obj f]
  (fc/method *context* obj f))

;;;;;;;;;;;;;;;;;;;;;;;; Assertions
(defn was-called-once
  ([k] (fc/was-called-once *context* k))
  ([k args-matcher] (fc/was-called-once *context* k args-matcher)))

(defn was-called
  ([k] (fc/was-called *context* k))
  ([k args-matcher] (fc/was-called *context* k args-matcher)))

(defn was-not-called
  [k] (fc/was-not-called *context* k))

;;;;;;;;;;;;;;;;;;;;;;;; Assertions for protocol methods
(defn was-called-once-on
  ([obj f] (fc/was-called-once-on *context* obj f))
  ([obj f args-matcher] (fc/was-called-once-on *context* obj f args-matcher)))

(defn was-called-on
  ([obj f] (fc/was-called-on *context* obj f))
  ([obj f args-matcher] (fc/was-called-on *context* obj f args-matcher)))

(defn was-not-called-on
  [obj f] (fc/was-not-called-on *context* obj f))

;;;;;;;;;;;;;;;;;;;;;;;; Monkey patching
#?(:clj
   (defmacro patch!
     [var-expr val]
     `(fc/patch! *context* ~var-expr ~val)))

(defn original-val
  [a-var]
  (fc/original-val *context* a-var))

(defn unpatch!
  [a-var]
  (fc/unpatch! *context* a-var))

(defn unpatch-all!
  []
  (fc/unpatch-all! *context*))

;;;;;;;;;;;;;;;;;;;;;;;; Macros - utils for unit tests; not extracted into separate ns for convenience
#?(:clj
   (defmacro ^:no-doc -deftest
     "The same as deftest but name is defined using a string.
     Inspired by: https://gist.github.com/mybuddymichael/4425558"
     [name-string & body]
     (let [deftest (if (-cljs-env? &env) 'cljs.test/deftest
                                         'clojure.test/deftest)
           name-symbol (-> name-string
                           string/lower-case
                           (string/replace #"\W" "-")
                           (string/replace #"-+" "-")
                           (string/replace #"-$" "")
                           symbol)]
       `(~deftest ~name-symbol ~@body))))

#?(:clj
   (defmacro ^:no-doc -is-exception-thrown
     "(is (thrown-with-msg? ...)) for specified exceptions in Clojure/ClojureScript."
     [clj-exc-class cljs-exc-class re expr]
     (let [is (if (-cljs-env? &env) 'cljs.test/is
                                    'clojure.test/is)
           exc-class (if (-cljs-env? &env) cljs-exc-class
                                           clj-exc-class)]
       `(~is (~'thrown-with-msg? ~exc-class ~re ~expr)))))

#?(:clj
   (defmacro ^:no-doc -is-error-thrown
     "(is (thrown-with-msg? ...)) for general exceptions in Clojure/ClojureScript."
     [re expr]
     `(-is-exception-thrown java.lang.Exception js/Error ~re ~expr)))

#?(:clj
   (defmacro ^:no-doc -is-assertion-error-thrown
     "(is (thrown-with-msg? ...)) for assert exceptions in Clojure/ClojureScript."
     [re expr]
     `(-is-exception-thrown java.lang.AssertionError js/Error ~re ~expr)))