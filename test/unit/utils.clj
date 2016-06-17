(ns unit.utils
  (:require [clj-fakes.macro :refer [-cljs-env?]]
            [clojure.string :as string]))

(defmacro ^:no-doc -deftest
  "The same as deftest but name is defined using a string.
  Inspired by: https://gist.github.com/mybuddymichael/4425558"
  [name-string & body]
  (let [deftest (if (-cljs-env? &env) 'cljs.test/deftest
                                      'clojure.test/deftest)
        name-symbol (-> name-string
                        string/lower-case
                        (string/replace #"\W" "-")
                        (string/replace #"-+" "-")
                        (string/replace #"-$" "")
                        symbol)]
    `(~deftest ~name-symbol ~@body)))

(defmacro ^:no-doc -is-exception-thrown
  "(is (thrown-with-msg? ...)) for specified exceptions in Clojure/ClojureScript."
  [clj-exc-class cljs-exc-class re expr]
  (let [is (if (-cljs-env? &env) 'cljs.test/is
                                 'clojure.test/is)
        exc-class (if (-cljs-env? &env) cljs-exc-class
                                        clj-exc-class)]
    `(~is (~'thrown-with-msg? ~exc-class ~re ~expr))))

(defmacro ^:no-doc -is-error-thrown
  "(is (thrown-with-msg? ...)) for general exceptions in Clojure/ClojureScript."
  [re expr]
  `(-is-exception-thrown Exception js/Error ~re ~expr))

(defmacro ^:no-doc -is-assertion-error-thrown
  "(is (thrown-with-msg? ...)) for assert exceptions in Clojure/ClojureScript."
  [re expr]
  `(-is-exception-thrown AssertionError js/Error ~re ~expr))