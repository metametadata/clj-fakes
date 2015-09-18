(ns unit.fixtures.macros
  (:require
    [clj-fakes.core :as f]
    [clj-fakes.context :as fc]
    ))

(defmacro my-fake
  [config]
  `(f/fake* ~&form ~config))

(defmacro ctx-my-fake
  [ctx config]
  `(fc/fake* ~ctx ~&form ~config))

(defmacro my-recorded-fake
  ([] `(f/recorded-fake* ~&form))
  ([config] `(f/recorded-fake* ~&form ~config)))

(defmacro ctx-my-recorded-fake
  ([ctx] `(fc/recorded-fake* ~ctx ~&form))
  ([ctx config] `(fc/recorded-fake* ~ctx ~&form ~config)))

(defmacro my-reify-fake
  [& specs]
  `(f/reify-fake* ~&form ~&env ~@specs))

(defmacro ctx-my-reify-fake
  [ctx & specs]
  `(fc/reify-fake* ~ctx ~&form ~&env ~@specs))