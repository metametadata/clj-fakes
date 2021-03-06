# Changelog

## 0.12.0

- (Clojure) Add support for Java variadic arguments, array arguments and array return values
into `reify-fake` and `reify-nice-fake` (#4).

## 0.11.0
- BREAKING CHANGE: `with-fakes` macro now asserts if dynamic bindings work correctly.
Specifically, this prevents its usage in `core.async/go` blocks under ClojureScript because there's an unresolved issue
https://dev.clojure.org/jira/browse/CLJS-884 which led to not resetting `*context*`
back to `nil` on exiting the faking block in previous clj-fakes versions.
This was a source of subtle bugs in async ClojureScript tests.

## 0.10.0
- `with-fakes` macro now doesn't generate an anonymous function
so that it now can be used inside `core.async/go` blocks.

## 0.9.0
- Added new assertions: `was-matched-once` and `method-was-matched-once`.

## 0.8.0
- BREAKING CHANGE: matcher `any?` is renamed to `any` to fix v1.9 compiler warnings.
- (ClojureScript) BREAKING CHANGE: required ClojureScript version updated from 1.7.28 to 1.9.229.
- (ClojureScript) it's now possible to require library namespaces in the same way as you do it in Clojure:

    ```clj
    (ns unit.example
      (:require
        [clj-fakes.core :as f]
        [clj-fakes.context :as fc]))
    ```
- `were-called-in-order` now always marks all passed fakes as checked.
So that the self-test about unchecked fakes is not also triggered if some steps are not reached
because of an exception in `were-called-in-order`.
- Fixed: `were-called-in-order` with `any` args matcher in a failing step threw an incorrect exception.

## 0.7.0

- (Clojure) fixed bug: `with-fakes` should catch all `Throwable`s, not only `Exception`s,
otherwise original errors are not reported in case of self-test fail.

## 0.6.0

- (ClojureScript) it's now possible to implement any new methods under `Object` in `reify-fake`/`reify-nice-fake`. E.g.:

    ```clj
    (let [calculator (f/reify-fake Object
                                   (sum [x y] :fake [[f/any? f/any?] #(+ %2 %3)]))]
      (is (= 5 (.sum calculator 2 3))))
    ```

## 0.5.0

- Better formatting of args matchers in assertion errors for protocol methods. E.g. before and after:

    ```
    expected: (f/method-was-called p/speak cow [1 2 3])
      actual: clojure.lang.ExceptionInfo: Function was never called with the expected args.
    Args matcher: first: <any?>, rest: [1 2 3].
    Actual calls: ...
    ```
    
    ```
    expected: (f/method-was-called p/speak cow [1 2 3])
      actual: clojure.lang.ExceptionInfo: Function was never called with the expected args.
    Args matcher: <this> [1 2 3].
    Actual calls: ...
    ```

- New function `clj-fakes.context/self-test` will run all available self-tests.
- Better arg naming in API.

## 0.4.0

Fixed:
- ClojureScript is a hard dependency (#1).

## 0.3.0

- Public release.