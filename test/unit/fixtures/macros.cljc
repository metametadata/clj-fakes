(ns unit.fixtures.macros
  (:require
    [clj-fakes.core :as f]
    [clj-fakes.context :as fc])

  ; declare macros for export
  #?(:cljs (:require-macros
             [unit.fixtures.macros :refer
              [my-fake
               ctx-my-fake
               my-recorded-fake
               ctx-my-recorded-fake
               my-reify-fake
               ctx-my-reify-fake]])))

#?(:clj
   (defmacro my-fake
     [config]
     `(f/fake* ~&form ~config)))

#?(:clj
   (defmacro ctx-my-fake
     [ctx config]
     `(fc/fake* ~ctx ~&form ~config)))

#?(:clj
   (defmacro my-recorded-fake
     ([] `(f/recorded-fake* ~&form))
     ([config] `(f/recorded-fake* ~&form ~config))))

#?(:clj
   (defmacro ctx-my-recorded-fake
     ([ctx] `(fc/recorded-fake* ~ctx ~&form))
     ([ctx config] `(fc/recorded-fake* ~ctx ~&form ~config))))

#?(:clj
   (defmacro my-reify-fake
     [& specs]
     `(f/reify-fake* ~&form ~&env ~@specs)))

#?(:clj
   (defmacro ctx-my-reify-fake
     [ctx & specs]
     `(fc/reify-fake* ~ctx ~&form ~&env ~@specs)))