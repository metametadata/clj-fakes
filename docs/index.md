# Introduction

clj-fakes is an isolation framework for Clojure/ClojureScript. It makes creating mocks and stubs for unit testing much easier.
One of the unique features of the framework is the ability to find unused fakes in order to help users write more concise test cases.
 
# References
The API was mainly inspired by [jMock](http://www.jmock.org/) and [unittest.mock](https://docs.python.org/3/library/unittest.mock.html) frameworks with
design decisions loosely based on the ["Fifteen things I look for in an Isolation framework" by Roy Osherove](http://osherove.com/blog/2013/11/23/fifteen-things-i-look-for-in-an-isolation-framework.html).

Some alternative frameworks with isolation capabilities:

* [clj-mock](https://github.com/EchoTeam/clj-mock)
* [Midje](https://github.com/marick/Midje)
* [speclj](https://github.com/slagyr/speclj)

Also take at look at the article ["Isolating External Dependencies in Clojure" by Joseph Wilk](http://blog.josephwilk.net/clojure/isolating-external-dependencies-in-clojure.html)
which discusses different approaches to mocking in Clojure.
 
For more detailed information about unit testing, TDD and test double patterns I'd recommend the books below:

* "Test Driven Development: By Example" by Kent Beck
* "Growing Object-Oriented Software, Guided by Tests" by Steve Freeman and Nat Pryce [[site](http://www.growing-object-oriented-software.com/)]
* "xUnit Test Patterns: Refactoring Test Code" by Gerard Meszaros [[site](http://xunitpatterns.com/)]

# Features
* All test doubles are named "fakes" to simplify terminology
* Fakes can be created for:
    * protocol instances
    * functions
* "Nice" and "strict" protocol fakes are supported
* Monkey patching is supported to fake implicit dependencies
* Self-testing: automatically checks for unused fakes
* Test runner agnostic
* Arrange-Act-Assert style testing

# Installation

Add this to your dependencies in project.clj:

```
[clj-fakes 0.1.1-SNAPSHOT]
```

Require in the namespace:

```clj
; Clojure
(ns unit.example
  (:require
    ; ...
    [clj-fakes.core :as f]))

; ClojureScript
(ns unit.example
  (:require
    ; ...
    [clj-fakes.core :as f :include-macros true]))
```

# Quickstart

# Guide


