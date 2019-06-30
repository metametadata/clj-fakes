(ns clj-fakes.reflection
  (:require [clj-fakes.macro :as m])
  (:import (java.lang.reflect Method)))

(defn -cljs-resolve
  "Alternative way to call cljs.analyzer/resolve-var.
  It's needed to be able to compile the library in the Clojure-only project."
  [env sym]
  ((ns-resolve 'cljs.analyzer 'resolve-var) env sym))

(defn -resolves?
  [env sym]
  (if (m/-cljs-env? env)
    ; ClojureScript
    (not (nil? (:meta (-cljs-resolve env sym))))

    ; Clojure
    (not (nil? (resolve sym)))))

(defn -resolve-protocol-with-specs
  "Returns a resolved protocol or nil if resolved object has no protocol specs."
  [env protocol-sym]
  (if (m/-cljs-env? env)
    ; ClojureScript
    (let [protocol (-cljs-resolve env protocol-sym)]
      (when-not (nil? (-> protocol :meta :protocol-info))
        protocol))

    ; Clojure
    (let [protocol (resolve protocol-sym)]
      (when (instance? clojure.lang.Var protocol)
        protocol))))

(defn -protocol-methods
  "Portable reflection helper. Returns different structures for different hosts.
  Protocol must be already resolved."
  [env protocol]
  (if (m/-cljs-env? env)
    ; ClojureScript
    (-> protocol :meta :protocol-info :methods)

    ; Clojure
    (-> protocol deref :sigs vals)))

(defn -protocol-method-name
  "Portable reflection helper."
  [env protocol-method]
  (if (m/-cljs-env? env)
    ; ClojureScript
    (first protocol-method)

    ; Clojure
    (:name protocol-method)))

(defn -protocol-method-arglist
  "Portable reflection helper."
  [env protocol-method]
  (if (m/-cljs-env? env)
    ; ClojureScript
    (second protocol-method)

    ; Clojure
    (:arglists protocol-method)))

(defn -interface-or-object-methods
  "Raises an exception if cannot reflect on specified symbol."
  [env interface-sym]
  (assert (not (m/-cljs-env? env)) "Works only in Clojure for reflection on Java interfaces and Object class.")
  ; Not using clojure.reflect because it returns types in a form cumbersome to transform into type hints.
  ; For instance, it returns "java.lang.Integer<>" but the hint should be "[Ljava.lang.Integer;".
  (let [class (try
                (if (= interface-sym 'Object)
                  Object
                  (Class/forName (str interface-sym)))

                (catch Exception e
                  (assert nil (str "Unknown protocol or interface: " interface-sym
                                   ". Underlying exception: " (pr-str e)))))]
    (for [^Method method (.getMethods class)]
      {:name            (.getName method)
       :return-type     (.getReturnType method)
       :parameter-types (.getParameterTypes method)})))