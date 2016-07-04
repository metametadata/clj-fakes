# Changelog

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