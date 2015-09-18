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
            [unit.was-called-once]
            [unit.was-called]
            [unit.was-not-called]
            [unit.reify-fake]
            [unit.reify-nice-fake]
            [unit.positions]
            [unit.was-called-on]
            [unit.was-called-once-on]
            [unit.was-not-called-on]
            [unit.patch]
            [unit.original-val]
            [unit.unpatch]
            [unit.spy]
            ))

(doo-tests
  'unit.context
  'unit.args-matcher
  'unit.optional-fake
  'unit.fake
  'unit.recorded-fake
  'unit.unused-fakes-self-test
  'unit.unchecked-fakes-self-test
  'unit.was-called-once
  'unit.was-called
  'unit.was-not-called
  'unit.reify-fake
  'unit.reify-nice-fake
  'unit.positions
  'unit.was-called-on
  'unit.was-called-once-on
  'unit.was-not-called-on
  'unit.patch
  'unit.original-val
  'unit.unpatch
  'unit.spy
  )
