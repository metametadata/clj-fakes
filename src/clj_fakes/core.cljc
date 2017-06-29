(ns clj-fakes.core
  "Simpler API for working in implicit dynamic context.
  Implements almost the same set of functions as [[clj-fakes.context]]."
  (:require [clj-fakes.context :as fc]
    #?@(:clj [
            [clj-fakes.macro :as m]]))

  ; declare macros for export
  #?(:cljs (:require-macros
             [clj-fakes.core :refer
              [arg
               with-fakes

               fake*
               fake
               recorded-fake*
               recorded-fake

               reify-fake*
               reify-nice-fake*
               reify-fake
               reify-nice-fake
               reify-fake-debug

               patch!]])))

;;;;;;;;;;;;;;;;;;;;;;;; Re-exports for usage convenience
#?(:clj
   (defmacro arg [matcher]
     `(fc/arg ~matcher)))

(def any fc/any)
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

        (catch #?(:clj Throwable :cljs :default) e
          (reset! exception-caught? true)
          (throw e))

        (finally
          (fc/unpatch-all! *context*)

          (when-not @exception-caught?
            (fc/self-test *context*)))))))

#?(:clj
   (defmacro with-fakes
     "Defines an implicit dynamic [[*context*]] and executes the body in it.
     Inside the body you can use the simpler [[clj-fakes.core]] API instead of [[clj-fakes.context]] API.

     The block will automatically unpatch all the patched variables and execute self-tests on exiting.
     Self-tests will not be executed if exception was raised inside the body.
     Variables are guaranteed to always be unpatched on exiting the block."
     [& body]
     (let [exception-class (if (m/-cljs-env? &env)
                             ; ClojureScript
                             :default

                             ; Clojure
                             Throwable)]
       `(binding [*context* (fc/context)]
          (let [exception-caught?# (atom false)]
            (try
              ~@body

              (catch ~exception-class e#
                (reset! exception-caught?# true)
                (throw e#))

              (finally
                (fc/unpatch-all! *context*)

                (when-not @exception-caught?#
                  (fc/self-test *context*)))))))))

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
  ([f] (fc/calls *context* f)))

(defn mark-checked
  [f]
  (fc/mark-checked *context* f))

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
  [f args-matcher]
  (fc/was-called-once *context* f args-matcher))

(defn was-called
  [f args-matcher]
  (fc/was-called *context* f args-matcher))

(defn was-matched-once
  [f args-matcher]
  (fc/was-matched-once *context* f args-matcher))

(defn was-not-called
  [f]
  (fc/was-not-called *context* f))

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

(defn method-was-matched-once
  [f obj args-matcher]
  (fc/method-was-matched-once *context* f obj args-matcher))

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