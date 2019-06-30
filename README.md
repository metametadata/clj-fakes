**clj-fakes** is an isolation framework for Clojure/ClojureScript that makes creating [test doubles](https://en.wikipedia.org/wiki/Test_double) much easier.

[![Clojars Project](https://img.shields.io/clojars/v/clj-fakes.svg)](https://clojars.org/clj-fakes)
[![Gitter](https://img.shields.io/gitter/room/metametadata/clj-fakes.svg?maxAge=2592000?style=plastic)](https://gitter.im/metametadata/clj-fakes)

# Features
* All test doubles are named "fakes" to simplify the terminology.
* Fakes can be created for:
    * functions
    * instances of protocols and Java interfaces
* "Nice" and "strict" protocol fakes are supported.
* Monkey patching is supported to fake implicit dependencies.
* Several functions are provided for asserting recorded calls.
* Self-testing: automatically checks for unused fakes.
* Informative error messages.
* Test runner agnostic.
* Arrange-Act-Assert style testing.

# Installation
Requirements: Clojure 1.7.0+ and/or ClojureScript 1.10.238+.

Add this to your dependencies:

```clj
[clj-fakes "0.12.0"]
```

Require framework namespace in your unit test source file:

```clj
(ns unit.example
  (:require
    [clj-fakes.core :as f]    
    ; and/or:
    [clj-fakes.context :as fc]))
```

# Cheat Sheet

### Creating Faking Context

Explicit context:

```clj
(let [ctx (fc/context)]
  ; use clj-fakes.context API here
)
```

Implicit context:

```clj
(f/with-fakes
  ; use clj-fakes.core API here 
)
; on exit block will automatically unpatch all patched vars and execute self-tests
```

All the following examples are assumed to be used inside an implicit context.

### Stubbing

#### Function Stub

```clj
(let [foo (f/fake [[1 2] "foo"
                   [3 4 5] "bar"])]
  (foo 1 2) ; => "foo"
  (foo 3 4 5) ; => "bar"
  (foo 100 200)) ; => raises "Unexpected args are passed into fake: (100 200) ..."
```

#### Method Stub

```clj
(let [cow (f/reify-fake p/AnimalProtocol
                        (sleep :fake [[] "zzz"]))]
  (p/sleep cow) ; => "zzz"
  (p/speak cow)) ; => undefined method exception
```

#### Nice Method Stub

```clj
(let [cow (f/reify-nice-fake p/AnimalProtocol)]
  (p/sleep cow) ; => FakeReturnValue
  (p/speak cow)) ; => FakeReturnValue 
```

### Mocking

#### Function Mock

```clj
(let [foo (f/recorded-fake [[(f/arg integer?) (f/arg integer?)] #(+ %1 %2)])
      bar (f/recorded-fake [[(f/arg integer?) (f/arg integer?)] #(* %1 %2)])]
  (foo 1 2)
  (bar 5 6)
  (foo 7 8)
       
  (f/calls foo)
  ; => [{:args [1 2] :return-value 3}
  ;     {:args [7 8] :return-value 15}]

  (f/calls)
  ; => [[foo {:args [1 2] :return-value 3}]
  ;     [bar {:args [5 6] :return-value 30}]
  ;     [foo {:args [7 8] :return-value 15}]]
)
```

#### Method Mock

```clj
(let [cow (f/reify-fake p/AnimalProtocol
                        (speak :recorded-fake [f/any "moo"]))]
  (p/speak cow)
    
  (f/calls (f/method cow p/speak))) ; => [{:args ..., :return-value moo}]
```

### Assertions

These functions return true or raise an exception and can be used only with recorded fakes.

#### Strictly One Call

```clj
(f/was-called-once foo [1 2])

(f/method-was-called-once p/speak cow ["Bob"])
```

#### At Least One Call

```clj
(f/was-called foo [1 2])

(f/method-was-called p/speak cow ["Bob"])
```

#### Strictly One Call Matched The Provided Args Matcher

```clj
(f/was-matched-once foo [1 2])

(f/method-was-matched-once p/speak cow ["Bob"])
```

#### No Calls

```clj
(f/was-not-called foo)

(f/method-was-not-called p/speak cow)
```

#### Calls In The Specified Order

```clj
(f/were-called-in-order
  foo [1 2 3]
  foo [(f/arg integer?)]
  bar [100 200]
  baz [300])

(f/methods-were-called-in-order
  p/speak cow []
  p/sleep cow []
  p/eat dog ["dog food" "water"]
  p/speak cow ["Bob"])
```

### Monkey Patching

**Caution**: this feature is not thread-safe.
Strongly consider avoiding it in Clojure code if you plan to someday run your tests concurrently.

#### Patch Function With Stub

```clj
(f/with-fakes
  (f/patch! #'funcs/sum (f/fake [[1 2] "foo"
                                 [3 4] "bar"]))
  (funcs/sum 1 2) ; => "foo"
  (funcs/sum 3 4)) ; => "bar"

; patching is reverted on exiting with-fakes block
(funcs/sum 1 2) ; => 3
```

#### Patch To Spy

```clj
(f/patch! #'funcs/sum (f/recorded-fake [f/any funcs/sum]))
(funcs/sum 1 2) ; => 3
(f/was-called funcs/sum [1 2]) ; => true
```

### Self-tests

```clj
(f/with-fakes
  (f/fake [f/any nil]))
; => raises "Self-test: no call detected for: non-optional fake ..."

(f/with-fakes
  (f/recorded-fake))
; => raises "Self-test: no check performed on: recorded fake ..."
```

# Documentation
More documentation can be found at [the project site](http://metametadata.github.io/clj-fakes/):

* [Quickstart](http://metametadata.github.io/clj-fakes/quickstart/)
* [User Guide](http://metametadata.github.io/clj-fakes/user-guide/)
* [API Reference](http://metametadata.github.io/clj-fakes/api/)
* [Developer Guide](http://metametadata.github.io/clj-fakes/dev-guide/)

# References
The API was mainly inspired by [jMock](http://www.jmock.org/) and
[unittest.mock](https://docs.python.org/3/library/unittest.mock.html) frameworks with
design decisions loosely based on the
["Fifteen things I look for in an Isolation framework" by Roy Osherove](http://osherove.com/blog/2013/11/23/fifteen-things-i-look-for-in-an-isolation-framework.html).

Some alternative frameworks with isolation capabilities:

* [bond](https://github.com/circleci/bond)
* [clj-mock](https://github.com/EchoTeam/clj-mock)
* [conjure](https://github.com/amitrathore/conjure)
* [Midje](https://github.com/marick/Midje)
* [shrubbery](https://github.com/bguthrie/shrubbery)
* [speclj](https://github.com/slagyr/speclj)
* [Untangled Spec](https://github.com/untangled-web/untangled-spec)

Also take at look at the article
["Isolating External Dependencies in Clojure" by Joseph Wilk](http://blog.josephwilk.net/clojure/isolating-external-dependencies-in-clojure.html).

For more detailed information about unit testing, TDD and test double patterns I'd recommend the books below:

* "Test Driven Development: By Example" by Kent Beck
* "Growing Object-Oriented Software, Guided by Tests" by Steve Freeman and Nat Pryce [[site](http://www.growing-object-oriented-software.com/)]
* "xUnit Test Patterns: Refactoring Test Code" by Gerard Meszaros [[site](http://xunitpatterns.com/)]

# License
Copyright © 2015 Yuri Govorushchenko.

Released under an MIT license.
