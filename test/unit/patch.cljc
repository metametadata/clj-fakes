(ns unit.patch
  #?@(:clj  [
             (:require
               [clojure.test :refer :all]
               [clj-fakes.core :as f]
               [clj-fakes.context :as fc]
               [unit.fixtures.functions :as funcs]
               )]
      :cljs [(:require
               [cljs.test :refer-macros [is testing]]
               [clj-fakes.core :as f :include-macros true]
               [clj-fakes.context :as fc :include-macros true]
               [unit.fixtures.functions :as funcs]
               )
             ]))

(def my-var1 111)
(def my-var2 222)

(defrecord MyRecord [field])

(f/-deftest
  "var can be patched inside the context"
  (f/with-fakes
    (is (not= 200 my-var1))
    (f/patch! #'my-var1 200)
    (is (= 200 my-var1))))

(f/-deftest
  "patched var is recovered on exiting mocking context"
  (let [original-val my-var1]
    (f/with-fakes
      (f/patch! #'my-var1 200))

    (is (= original-val my-var1))))

(f/-deftest
  "fully qualified single patched var is recovered on exiting mocking context"
  (let [original-val my-var1]
    (f/with-fakes
      (f/patch! #'unit.patch/my-var1 200))

    (is (= original-val my-var1))))

(f/-deftest
  "patched var is recovered on exiting mocking context because of exception"
  (let [original-val my-var1]
    (f/-is-error-thrown
      #"expected exception"
      (try
        (f/with-fakes
          (f/patch! #'my-var1 200)
          (throw (ex-info "expected exception" {})))))

    (is (= original-val my-var1))))

(f/-deftest
  "two patched vars are recovered on exiting mocking context"
  (let [original-val1 my-var1
        original-val2 my-var2]
    (f/with-fakes
      (f/patch! #'my-var1 100)
      (f/patch! #'my-var2 200))

    (is (= original-val1 my-var1))
    (is (= original-val2 my-var2))))

(f/-deftest
  "var can be patched more than once and be recovered"
  (let [original-val my-var1]
    (f/with-fakes
      (f/patch! #'my-var1 200)
      (f/patch! #'my-var1 300)

      (is (= 300 my-var1)))

    (is (= original-val my-var1))))

(f/-deftest
  "function can be patched inside the context"
  (f/with-fakes
    (is (not= 123 (funcs/sum 2 3)))
    (f/patch! #'funcs/sum (constantly 123))
    (is (= 123 (funcs/sum 2 3)))))

(f/-deftest
  "println can be patched"
  (f/with-fakes
    (f/patch! #'println (constantly 123))
    (is (= 123 (println "YOU SHOULDN'T SEE IT")))))

(f/-deftest
  "variadic function can be patched"
  (f/with-fakes
    (f/patch! #'funcs/variadic (constantly 200))

    (is (= 200 (funcs/variadic)))
    (is (= 200 (funcs/variadic 1)))
    (is (= 200 (funcs/variadic 1 2)))
    (is (= 200 (funcs/variadic 1 2 3 4 5 6 7)))))

(f/-deftest
  "multimethod can be patched"
  (f/with-fakes
    (f/patch! #'funcs/fib (constantly 200))
    (is (= [200 200 200 200 200] (map funcs/fib (range 5))))))

(f/-deftest
  "var can be patched with a fake"
  (f/with-fakes
    (f/patch! #'funcs/sum (f/fake [[1 2] "foo"
                                   [3 4] "bar"]))
    (is (= "foo" (funcs/sum 1 2)))
    (is (= "bar" (funcs/sum 3 4)))))

(f/-deftest
  "record instantiation using -> can be patched"
  (f/with-fakes
    (f/patch! #'->MyRecord (constantly 123))
    (is (= 123 (->MyRecord {:field 111})))))

(f/-deftest
  "record instantiation using map-> can be patched"
  (f/with-fakes
    (f/patch! #'map->MyRecord (constantly 123))
    (is (= 123 (map->MyRecord {:field 111})))))