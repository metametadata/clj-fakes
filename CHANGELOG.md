# Changelog

## 0.5.0

- Better formatting of args matchers in assertion errors for protocol methods.

    E.g. previously:
```
expected: (f/method-was-called p/speak cow [1 2 3])
  actual: clojure.lang.ExceptionInfo: Function was never called with the expected args.
Args matcher: first: <any?>, rest: [1 2 3].
Actual calls: ...
```

    and now:
```
expected: (f/method-was-called p/speak cow [1 2 3])
  actual: clojure.lang.ExceptionInfo: Function was never called with the expected args.
Args matcher: <this> [1 2 3].
Actual calls: ...
```

## 0.4.0

Fixed:
- Clojurescript is a hard dependency #1.

## 0.3.0

- Public release.