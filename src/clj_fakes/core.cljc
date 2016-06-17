(ns clj-fakes.core
  "Simpler API for working in implicit dynamic context.
  Implements almost the same set of functions as [[clj-fakes.context]]."
  (:require #?@(:clj  [[clj-fakes.context :as fc]
                       [clj-fakes.macro :refer :all]]
                :cljs [[clj-fakes.context :as fc :include-macros true]])))

;;;;;;;;;;;;;;;;;;;;;;;; Re-exports for usage convenience
#?(:clj
   (defmacro arg [matcher]
     `(fc/arg ~matcher)))

(def any? fc/any?)
(def cyclically fc/cyclically)

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
   (defmacro ^:no-doc reify-fake-debug
     "Helper for debugging."
     [& specs]
     `(fc/-reify-fake-debug* *context* ~&form ~&env ~@specs)))

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
  [k args-matcher]
  (fc/was-called-once *context* k args-matcher))

(defn was-called
  [k args-matcher]
  (fc/was-called *context* k args-matcher))

(defn was-not-called
  [k]
  (fc/was-not-called *context* k))

(defn were-called-in-order
  [& fns-and-matchers]
  (apply fc/were-called-in-order *context* fns-and-matchers))

;;;;;;;;;;;;;;;;;;;;;;;; Assertions for protocol methods
(defn method-was-called-once
  [f obj args-matcher]
  (fc/method-was-called-once *context* f obj args-matcher))

(defn method-was-called
  [f obj args-matcher]
  (fc/method-was-called *context* f obj args-matcher))

(defn method-was-not-called
  [f obj]
  (fc/method-was-not-called *context* f obj))

(defn methods-were-called-in-order
  [& fns-objs-and-matchers]
  (apply fc/methods-were-called-in-order *context* fns-objs-and-matchers))

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