(ns clj-fakes.macro)

(defn -cljs-env?
  "Take the &env from a macro, and tell whether we are expanding into cljs.
   Source: https://groups.google.com/d/msg/clojurescript/iBY5HaQda4A/w1lAQi9_AwsJ"
  [env]
  (boolean (:ns env)))

(defn P
  "Prints to the compiler's console at compile time. Sprinkle as needed within
  macros and their supporting functions to facilitate debugging during
  development.
  Inspired by: https://gist.github.com/michaelsbradleyjr/7509505"
  [& args]
  (binding [*out* *err*]
    (apply println args)))

(defn PP
  "The same as P but uses pprint."
  [& args]
  (binding [*out* *err*]
    (apply clojure.pprint/pprint args)))