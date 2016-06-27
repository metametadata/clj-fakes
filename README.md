# clj-fakes
clj-fakes is an isolation framework for Clojure/ClojureScript. It makes creating mocks and stubs for unit testing much easier.

[![Clojars Project](https://img.shields.io/clojars/v/clj-fakes.svg)](https://clojars.org/clj-fakes)
[![Gitter](https://img.shields.io/gitter/room/metametadata/clj-fakes.svg?maxAge=2592000?style=plastic)](https://gitter.im/metametadata/clj-fakes)

## Features
* All test doubles are named "fakes" to simplify terminology.
* Fakes can be created for:
    * protocol and Java interface instances
    * functions
* "Nice" and "strict" protocol fakes are supported.
* Monkey patching is supported to fake implicit dependencies.
* Functions for asserting recorded calls.
* Self-testing: automatically checks for unused fakes.
* Informative error messages.
* Test runner agnostic.
* Arrange-Act-Assert style testing.

## Requirements

Clojure 1.7.0 and/or ClojureScript 1.7.28 or higher.

## Installation
Add this to your dependencies in project.clj:

```clj
[clj-fakes "0.5.0"]
```

Require framework namespace in your unit test source file:

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

## Examples

### Fake protocol instance with recorded method (aka mock object)

```clj
(defprotocol AnimalProtocol
  (speak [this name]))
; ...

(deftest example
  (f/with-fakes
    ; create fake instance of specified protocol
    (let [cow (f/reify-fake p/AnimalProtocol
                            ; ask framework to record method calls
                            (speak :recorded-fake))]
      ; call method on fake object
      (p/speak cow "Bob")
      
      ; assert that method was called with specified args
      (is (f/method-was-called p/speak cow ["Bob"])))))
```

### Patch a function

```clj
(deftest example
  (f/with-fakes
    (f/patch! #'funcs/sum (f/fake [[1 2] "foo"
                                   [3 4] "bar"]))
    (is (= "foo" (funcs/sum 1 2)))
    (is (= "bar" (funcs/sum 3 4))))
  
  ; patching is reverted on exiting with-fakes block
  (is (= 3 (funcs/sum 1 2))))
```

### Self-tests

```clj
(f/with-fakes
  (f/fake [f/any? nil]))
; will raise "Self-test: no call detected for: non-optional fake ..."
```

```clj
(f/with-fakes
  (f/recorded-fake))
; will raise "Self-test: no check performed on: recorded fake ..."
```

## Documentation
More documentation can be found at [the project site](http://metametadata.github.io/clj-fakes/):

* [Quickstart](http://metametadata.github.io/clj-fakes/quickstart/)
* [User Guide](http://metametadata.github.io/clj-fakes/user-guide/)
* [API Reference](http://metametadata.github.io/clj-fakes/api/)
* [Developer Guide](http://metametadata.github.io/clj-fakes/dev-guide/)

## License
Copyright © 2015 Yuri Govorushchenko.

Released under an MIT license.
