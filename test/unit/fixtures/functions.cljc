(ns unit.fixtures.functions)

(defn sum [x y]
  (+ x y))

(defn variadic
  ([] "[]")
  ([_] "[a]")
  ([_ _] "[a b]")
  ([_ _ & _] "[a b & c]"))

(defmulti fib int)
(defmethod fib 0 [_] 1)
(defmethod fib 1 [_] 1)
(defmethod fib :default [n] (+ (fib (- n 2)) (fib (- n 1))))