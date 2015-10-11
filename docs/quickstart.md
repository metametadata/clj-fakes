# Quickstart

1) I assume your Clojure/ClojureScript project is automated using [Leiningen](http://leiningen.org/) and 
already has unit tests implemented with some unit testing framework. 

I will use a built-in
[clojure.test/cljs.test](http://clojure.github.io/clojure/clojure.test-api.html) 
framework in this documentation. To learn how to use it:
 
* in Clojure: see [Leiningen tutorial](https://github.com/technomancy/leiningen/blob/master/doc/TUTORIAL.md#tests) and
 [lein-test-refresh](https://github.com/jakemcc/lein-test-refresh) plugin;
* in ClojureScript: see [wiki](https://github.com/clojure/clojurescript/wiki/Testing) and 
[doo](https://github.com/bensu/doo) plugin for running unit tests.

2) Add framework dependency into `project.clj`:

```clj
:dependencies [...
               [clj-fakes "<insert here the latest version from Clojars>"]]
```

3) Require framework namespace in your unit test source file:

```clj
; Clojure
(ns unit.example
  (:require
    ; ...
    [clojure.test :refer :all]
    [clj-fakes.core :as f]))

; ClojureScript
(ns unit.example
  (:require
    ; ...
    [cljs.test :refer-macros [is deftest]]
    [clj-fakes.core :as f :include-macros true]))
```

4) Now you can write a simple unit test which creates and calls a fake function:

```clj
(deftest fakes-work
  (f/with-fakes
    (let [hello (f/fake [[] "hello, world"])]
      (is (= "hello, world" (hello))))))
```