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
is that `core` uses an implicit context and the `context` namespace
functions require an explicit context argument.

For your convenience functions which don't rely on a context can also be sometimes found in both namespaces (e.g. `f/any?` is the same as `fc/any?`).

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
used in your own helper functions.

# Function Fakes

Fake is a function which returns canned values on matched arguments and can optionally record its calls. It 
can be used to define and assert a behavior of an explicit functional dependency of an SUT (system under test).

## Fake

A regular fake function can be created using a macro:

`(fake [ctx] config)`

[Config](#fake-configuration) is a vector which defines which values to return for different arguments:

```clj
(let [foo (f/fake [[1 2] "foo"
                   [3 4 5] "bar"])]
  (foo 1 2) ; => "foo"
  (foo 3 4 5)) ; => "bar"
```

If passed arguments cannot be [matched](#argument-matching) using specified config then the exception will be raised:

```clj
(foo 100 200) ; => raises "Unexpected args are passed into fake: (100 200)"
```

A fake is assumed to be called at least once inside the context. Otherwise [self-test](#self-tests) exception 
will be raised. In such case user should either modify a test, an SUT 
or consider using an [optional fake](#optional-fake):

```clj
(f/with-fakes
  (f/fake [[] nil])) ; => raises "Self-test: no call detected for: non-optional fake ..."
```

If your test scenario focuses on testing a behavior (e.g. "assert that foo was called by an SUT") then do not rely on self-tests, 
instead use [recorded fakes](#recorded-fake) with explicit assertions. 
Self-tests are more about checking sensibility of provided preconditions than 
about testing expected behavior.

## Optional Fake

`(optional-fake [ctx] [config])`

It works the same as a regular fake but is not expected to be always called in the context:

```clj
(f/with-fakes
  (f/optional-fake [[1 2] 3])) ; => ok, self-test will pass
```

Such fakes should be used to express the intent of the test writer, 
for example, when you have to provide a dependency to an SUT,
but this dependency is not really related to the test case:
 
```clj
(defn process-payments
  "Processor requires a logger."
  [data logger]
  {:pre [(fn? logger)]}
  ; ...
  )

(deftest good-payments-are-processed-without-error
  (f/with-fakes
    (let [; ...
          ; we are not interested in how logger is going to be used, just stub it and forget
          fake-logger (f/optional-fake)]
      (is (= :success (process-payments good-payments fake-logger))))))
```

As you may have noticed, `config` argument can be omitted. In such case fake will be created 
with `(default-fake-config)` which allows any arguments to be passed on invocation.

## Recorded Fake

-

# Fake Configuration

Fake function config should contain pairs of args-matcher & return-value. On fake invocation 
argument matchers will be tested from top to bottom and on the first match the specified value will be returned.

# Argument Matching

-

# Assertions

-

# Protocol Fakes

-

## Strict

-

## Nice

-

# Self-tests

-

## Unused Fakes

-

## Unchecked Fakes

-

# Monkey Patching

-

## Function Spy

Example:

```clj
(f/patch! #'funcs/sum (f/recorded-fake [f/any? funcs/sum]))
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