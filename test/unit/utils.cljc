(ns unit.utils
  (:require [clojure.string :as string]
    #?(:clj
            [clj-fakes.macro :as m]))

  ; declare macros for export
  #?(:cljs (:require-macros
             [unit.utils :refer
              [deftest+
               is-exception-thrown
               is-error-thrown
               is-assertion-error-thrown]])))

#?(:clj
   (defmacro deftest+
     "The same as deftest but name is defined using a string.
     Inspired by: https://gist.github.com/mybuddymichael/4425558"
     [name-string & body]
     (let [deftest (if (m/-cljs-env? &env) 'cljs.test/deftest
                                           'clojure.test/deftest)
           name-symbol (-> name-string
                           string/lower-case
                           (string/replace #"\W" "-")
                           (string/replace #"-+" "-")
                           (string/replace #"-$" "")
                           symbol)]
       `(~deftest ~name-symbol ~@body))))

#?(:clj
   (defmacro is-exception-thrown
     "(is (thrown-with-msg? ...)) for specified exceptions in Clojure/ClojureScript."
     [clj-exc-class cljs-exc-class re expr]
     (let [is (if (m/-cljs-env? &env) 'cljs.test/is
                                      'clojure.test/is)
           exc-class (if (m/-cljs-env? &env) cljs-exc-class
                                             clj-exc-class)]
       `(~is (~'thrown-with-msg? ~exc-class ~re ~expr)))))

#?(:clj
   (defmacro is-error-thrown
     "(is (thrown-with-msg? ...)) for general exceptions in Clojure/ClojureScript."
     [re expr]
     `(is-exception-thrown Exception js/Error ~re ~expr)))

#?(:clj
   (defmacro is-assertion-error-thrown
     "(is (thrown-with-msg? ...)) for assert exceptions in Clojure/ClojureScript."
     [re expr]
     `(is-exception-thrown AssertionError js/Error ~re ~expr)))