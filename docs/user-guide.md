# Namespaces

The public API is split into two namespaces:

* `clj-fakes.core`
* `clj-fakes.context`

This is how you could require them in Clojure and ClojureScript:

```clj
; Clojure
(ns unit.example
  (:require
    [clj-fakes.core :as f]
    [clj-fakes.context :as fc]))

; ClojureScript
(ns unit.example
  (:require
    [clj-fakes.core :as f :include-macros true]
    [clj-fakes.context :as fc :include-macros true]))
```

These namespaces contain almost the same set of members. The difference 
is that `core` uses the implicit context and the `context` namespace
functions require explicit context argument.

The private/internal API uses a `-` prefix and should not be used (e.g. `-this-is-some-private-thing`).

# Context

Context is an object which stores all the information about
created fakes (recorded calls, positions in code, etc.).
All fakes have to be created inside some context.

To create a new context use `clj-fakes.context/context`:

```clj
; explicit context
(let [ctx (fc/context)
      foo (fc/recorded-fake ctx)]
  ; ...
)
```

Alternatively a new context can be created with 
`clj-fakes.core/with-fakes` macro:

```clj
; implicit context
(f/with-fakes
  ; note that now fake is created using a macro from core ns
  (let [foo (f/recorded-fake)]
    ; ...
))
```

This approach is preferable since it requires less typing, automatically 
unpatches all patched vars and executes self-tests.

Internally `with-fakes` relies on a public dynamic var `*context*` which can be 
used to in your own helper functions.

# Function Fakes

-

## `fake`

-

## `optional-fake`

-

## `recorded-fake`

-

# Argument Matching

-

# Self-tests

-

# Asserts

-

# Protocol Fakes

-

# Monkey Patching

-

## Function Spy

Example:

```clj
(f/patch! #'funcs/sum
              (f/recorded-fake [f/any? funcs/sum]))
```

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