# clj-fakes
clj-fakes is an isolation framework for Clojure/ClojureScript. It makes creating mocks and stubs for unit testing much easier.

[![Clojars Project](http://clojars.org/clj-fakes/latest-version.svg)](http://clojars.org/clj-fakes)

## Features
* All test doubles are named "fakes" to simplify terminology
* Fakes can be created for:
  * protocol instances
  * functions
* "Nice" and "strict" protocol fakes are supported
* Monkey patching is supported to fake implicit dependencies
* Self-testing: automatically checks for unused fakes
* Test runner agnostic
* Arrange-Act-Assert style testing

## Status
Core functionality is ready. Documentation and more features are on the way.

## Examples
### Fake function

```clj
(f/with-fakes
  (let [foo (f/fake [[1 2 3] "hey!"])]
    (println (foo 1 2 3))))
```

### Fake protocol instance with recorded method (aka mock object)

```clj
(defprotocol AnimalProtocol
  (speak [this name]))

(f/with-fakes
  ; create fake instance of specified protocol
  (let [cow (f/reify-fake p/AnimalProtocol
                          ; ask framework to record method calls
                          (speak :recorded-fake))]
    ; call method on fake object
    (p/speak cow "Bob")
    
    ; assert that method was called with specified args
    (is (f/was-called-on cow p/speak ["Bob"]))))
```

### Patch a function

```clj
(f/with-fakes
  (f/patch! #'funcs/sum (f/fake [[1 2] "foo"
                                 [3 4] "bar"]))
  (is (= "foo" (funcs/sum 1 2)))
  (is (= "bar" (funcs/sum 3 4))))

; patching is reverted on exiting with-fakes block
(is (= 3 (funcs/sum 1 2)))
```

### Self-test for unused fake

```clj
(f/with-fakes
  (f/fake [f/any? nil]))
; will raise "Self-test: no call detected for: non-optional fake ..."
```

### Self-test for unchecked fake

```clj
(f/with-fakes
  (f/recorded-fake [f/any? nil]))
; will raise "Self-test: no check performed on: recorded fake ..."
```

## Installation
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

## Documentation
More documentation can be found at [the project site](http://metametadata.github.io/clj-fakes/).

## License
Copyright © 2015 Yuri Govorushchenko.

Released under an MIT license.
