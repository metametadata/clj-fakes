# Introduction

clj-fakes is an isolation framework for Clojure/ClojureScript. It makes creating mocks and stubs for unit testing much easier.

One of the unique features of the framework is the ability to find unused fakes in order to help users write more concise test cases.

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

# Requirements

Clojure 1.7.0 or higher.

# Installation

Add this to your dependencies in project.clj:

```clj
[clj-fakes "0.1.1-SNAPSHOT"]
```