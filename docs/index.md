# Introduction

clj-fakes is an isolation framework for Clojure/ClojureScript. It makes creating mocks and stubs for unit testing much easier.

# Features
* All test doubles are named "fakes" to simplify terminology
* Fakes can be created for:
  * protocol and Java interface instances
  * functions
* "Nice" and "strict" protocol fakes are supported
* Monkey patching is supported to fake implicit dependencies
* Self-testing: automatically checks for unused fakes
* Test runner agnostic
* Arrange-Act-Assert style testing

# Requirements

Clojure 1.7.0 and/or ClojureScript 1.7.28 or higher.