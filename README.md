# clj-fakes
clj-fakes is an isolation framework for Clojure/ClojureScript. It makes creating mocks and stubs for unit testing much easier.

## Features
* Works in Clojure and ClojureScript
* Test runner agnostic
* All test doubles are named "fakes" to simplify terminology
* Fakes can be created for:
  * protocol instances
  * functions
* Supports "nice" protocol fakes
* Supports Arrange-Act-Assert style testing
* Monkey patching is supported to fake implicit dependencies
* Self-testing: automatically checks for unused fakes

## Status
A work in progress. Not even published to Clojars yet. Please look at the tests to see what lib can do.

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

## Examples
Fake function:

```clj
(f/with-fakes
  (let [foo (f/fake [[1 2 3] "hey!"])]
    (println (foo 1 2 3))))
```

Fake protocol instance with recorded method (aka mock object): ...

Patching implicit function dependency: ...

Self-test: fake is created but never called: ...

Self-test: recorded fake was not checked: ...

## Development notes
Run Clojure tests:
```
$ lein test-refresh
```

Run ClojureScript tests:
```
$ lein clean && lein doo phantom test once
```
(clean is needed because there's an issue: ClojureScript plugin does not seem to recompile macros)

Autorun ClojureScript tests:
```
$ fswatch -o ./src ./test | xargs -n1 -I{} sh -c 'echo wait.... && lein clean && lein doo rhino test once'
```

## License
Copyright © 2015 Yuri Govorushchenko.

Released under an MIT license.