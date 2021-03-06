(ns unit.runner
  (:require [cljs.test]
            [doo.runner :refer-macros [doo-tests]]
            [unit.context]
            [unit.args-matcher]
            [unit.optional-fake]
            [unit.fake]
            [unit.recorded-fake]
            [unit.unused-fakes-self-test]
            [unit.unchecked-fakes-self-test]
            [unit.was-called]
            [unit.was-called-once]
            [unit.was-matched-once]
            [unit.was-not-called]
            [unit.were-called-in-order]
            [unit.reify-fake]
            [unit.reify-nice-fake]
            [unit.positions]
            [unit.method-was-called]
            [unit.method-was-called-once]
            [unit.method-was-matched-once]
            [unit.method-was-not-called]
            [unit.methods-were-called-in-order]
            [unit.patch]
            [unit.original-val]
            [unit.unpatch]
            [unit.spy]
            [unit.cyclically]
            ))

(doo-tests
  'unit.context
  'unit.args-matcher
  'unit.optional-fake
  'unit.fake
  'unit.recorded-fake
  'unit.unused-fakes-self-test
  'unit.unchecked-fakes-self-test
  'unit.was-called
  'unit.was-called-once
  'unit.was-matched-once
  'unit.was-not-called
  'unit.were-called-in-order
  'unit.reify-fake
  'unit.reify-nice-fake
  'unit.positions
  'unit.method-was-called
  'unit.method-was-called-once
  'unit.method-was-matched-once
  'unit.method-was-not-called
  'unit.methods-were-called-in-order
  'unit.patch
  'unit.original-val
  'unit.unpatch
  'unit.spy
  'unit.cyclically
  )
