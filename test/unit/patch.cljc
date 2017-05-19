(ns unit.patch
  (:require
    [clojure.test :refer [is testing #?(:cljs async)]]
    [unit.utils :as u]
    [clj-fakes.core :as f]
    [clj-fakes.context :as fc]
    [unit.fixtures.functions :as funcs]))

(def my-var1 111)
(def my-var2 222)

(defrecord MyRecord [field])

(u/deftest+
  "var can be patched inside the context"
  (f/with-fakes
    (is (not= 200 my-var1))
    (f/patch! #'my-var1 200)
    (is (= 200 my-var1))))

(u/deftest+
  "patched var is recovered on exiting mocking context"
  (let [original-val my-var1]
    (f/with-fakes
      (f/patch! #'my-var1 200))

    (is (= original-val my-var1))))

(u/deftest+
  "fully qualified single patched var is recovered on exiting mocking context"
  (let [original-val my-var1]
    (f/with-fakes
      (f/patch! #'unit.patch/my-var1 200))

    (is (= original-val my-var1))))

(u/deftest+
  "patched var is recovered on exiting mocking context because of exception"
  (let [original-val my-var1]
    (u/is-error-thrown
      #"expected exception"
      (try
        (f/with-fakes
          (f/patch! #'my-var1 200)
          (throw (ex-info "expected exception" {})))))

    (is (= original-val my-var1))))

(u/deftest+
  "two patched vars are recovered on exiting mocking context"
  (let [original-val1 my-var1
        original-val2 my-var2]
    (f/with-fakes
      (f/patch! #'my-var1 100)
      (f/patch! #'my-var2 200))

    (is (= original-val1 my-var1))
    (is (= original-val2 my-var2))))

(u/deftest+
  "var can be patched more than once and be recovered"
  (let [original-val my-var1]
    (f/with-fakes
      (f/patch! #'my-var1 200)
      (f/patch! #'my-var1 300)

      (is (= 300 my-var1)))

    (is (= original-val my-var1))))

(u/deftest+
  "function can be patched inside the context"
  (f/with-fakes
    (is (not= 123 (funcs/sum 2 3)))
    (f/patch! #'funcs/sum (constantly 123))
    (is (= 123 (funcs/sum 2 3)))))

(u/deftest+
  "println can be patched (this test will fail in Clojure 1.8 with enabled direct linking)"
  (f/with-fakes
    (f/patch! #'println (constantly 123))
    (is (= 123 (println "YOU SHOULDN'T SEE IT")))))

(u/deftest+
  "variadic function can be patched with non-variadic function"
  (f/with-fakes
    (f/patch! #'funcs/variadic (constantly 200))

    (is (= 200 (funcs/variadic)))
    (is (= 200 (funcs/variadic 1)))
    (is (= 200 (funcs/variadic 1 2)))
    (is (= 200 (funcs/variadic 1 2 3 4 5 6 7)))))

(u/deftest+
  "variadic function can be patched with variadic function"
  (f/with-fakes
    (f/patch! #'funcs/variadic (fn my-variadic
                                 ([] 0)
                                 ([_] 1)
                                 ([_ _] 2)
                                 ([_ _ _] 3)
                                 ([_ _ _ & _] :etc)))

    (is (= 0 (funcs/variadic)))
    (is (= 1 (funcs/variadic 1)))
    (is (= 2 (funcs/variadic 1 2)))
    (is (= 3 (funcs/variadic 1 2 3)))
    (is (= :etc (funcs/variadic 1 2 3 4 5 6 7)))))

(u/deftest+
  "non-variadic function can be patched with recursive variadic function which calls original function"
  (f/with-fakes
    (let [original-sum funcs/sum]
      (f/patch! #'funcs/sum (fn my-variadic
                              ([] 0)
                              ([x] (original-sum 0 x))
                              ([x y] ((f/original-val #'funcs/sum) x y))
                              ([x y z] (->> (my-variadic x y)
                                            ((f/original-val #'funcs/sum) z)))
                              ([x y z & etc]
                               ((f/original-val #'funcs/sum)
                                 (my-variadic x y z)
                                 (apply my-variadic etc)))))

      ; alias is created to get rid of "WARNING: Wrong number of args (...) passed to unit.fixtures.functions/sum"
      (let [new-sum funcs/sum]
        (is (= 0 (new-sum)))
        (is (= 100 (new-sum 100)))
        (is (= 7 (new-sum 3 4)))
        (is (= 10 (new-sum 1 2 3 4)))
        (is (= 15 (new-sum 1 2 3 4 5)))
        (is (= 21 (new-sum 1 2 3 4 5 6)))
        (is (= 120 (new-sum 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15)))))))

; TODO: fails in CLJS - see https://github.com/metametadata/clj-fakes/issues/3
#?(:clj
   (u/deftest+
     "variadic function can be patched with non-variadic function which calls original function"
     (f/with-fakes
       (f/patch! #'funcs/variadic (fn my-sum
                                    [x]
                                    ((f/original-val #'funcs/variadic) x)))

       (is (= "[a]" (funcs/variadic 100))))))

; TODO: fails in CLJS - see https://github.com/metametadata/clj-fakes/issues/3
#?(:clj
   (u/deftest+
     "variadic function can be patched with variadic function which calls original function"
     (f/with-fakes
       (let [original-variadic funcs/variadic]
         (f/patch! #'funcs/variadic (fn my-variadic
                                      ([] (original-variadic))
                                      ([a] ((f/original-val #'funcs/variadic) a))
                                      ([_ _] 2)
                                      ([_ _ _] 3)
                                      ([_ _ _ & _] :etc)))

         (is (= "[]" (funcs/variadic)))
         (is (= "[a]" (funcs/variadic 1)))
         (is (= 2 (funcs/variadic 1 2)))
         (is (= 3 (funcs/variadic 1 2 3)))
         (is (= :etc (funcs/variadic 1 2 3 4 5 6 7)))))))

(u/deftest+
  "multimethod can be patched"
  (f/with-fakes
    (f/patch! #'funcs/fib (constantly 200))
    (is (= [200 200 200 200 200] (map funcs/fib (range 5))))))

(u/deftest+
  "var can be patched with a fake"
  (f/with-fakes
    (f/patch! #'funcs/sum (f/fake [[1 2] "foo"
                                   [3 4] "bar"]))
    (is (= "foo" (funcs/sum 1 2)))
    (is (= "bar" (funcs/sum 3 4)))))

(u/deftest+
  "var can be patched with a recorded fake"
  (f/with-fakes
    (f/patch! #'funcs/sum (f/recorded-fake [[(f/arg integer?) (f/arg integer?)] #(* %1 %2)]))

    (is (= 2 (funcs/sum 1 2)))
    (is (= 12 (funcs/sum 3 4)))

    (is (f/was-called funcs/sum [1 2]))
    (is (f/was-called funcs/sum [3 4]))))

(u/deftest+
  "record instantiation using -> can be patched"
  (f/with-fakes
    (f/patch! #'->MyRecord (constantly 123))
    (is (= 123 (->MyRecord {:field 111})))))

(u/deftest+
  "record instantiation using map-> can be patched"
  (f/with-fakes
    (f/patch! #'map->MyRecord (constantly 123))
    (is (= 123 (map->MyRecord {:field 111})))))

#?(:cljs
   (u/deftest+
     "var stays patched in setTimeout context"
     (async done
       (let [ctx (fc/context)]
         (is (not= 200 my-var1))
         (fc/patch! ctx #'my-var1 200)
         (.setTimeout js/window
                      #(do
                        ; assert
                        (is (= 200 my-var1))

                        ; tear down
                        (fc/unpatch! ctx #'my-var1)
                        (is (not= 200 my-var1))
                        (done))
                      10)))))