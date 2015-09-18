(ns unit.fixtures.functions)

(defn sum [x y]
  (+ x y))

(defn variadic
  ([] (println "[]"))
  ([_] (println "[a]"))
  ([_ _] (println "[a b]"))
  ([_ _ & _] (println "[a b & c]")))

(defmulti fib int)
(defmethod fib 0 [_] 1)
(defmethod fib 1 [_] 1)
(defmethod fib :default [n] (+ (fib (- n 2)) (fib (- n 1))))