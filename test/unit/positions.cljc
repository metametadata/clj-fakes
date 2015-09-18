; positioning is also partially tested implicitly in tests about self-tests
(ns unit.positions
  #?@(:clj  [
             (:require
               [clojure.test :refer :all]
               [clj-fakes.core :as f]
               [clj-fakes.context :as fc]
               [unit.fixtures.macros :as m]
               )]
      :cljs [(:require
               [cljs.test :refer-macros [is testing]]
               [clj-fakes.core :as f :include-macros true]
               [clj-fakes.context :as fc :include-macros true])
             (:require-macros [unit.fixtures.macros :as m])
             ]))

(def this-file #?(:clj "unit/positions.cljc" :cljs "test/unit/positions.cljc"))

(defn testing-fake-fn-position-detection
  [fake-fn expected-line expected-column
   ctx-fake-fn ctx-expected-line ctx-expected-column]
  (testing "fake position can be determined in implicit context"
    (f/with-fakes
      (let [foo (fake-fn [f/any? nil])]
        ; call it to suppress an exception from self-test
        (foo)
        (is (= {:file   this-file
                :line   expected-line
                :column expected-column}
               (f/-position foo))))))

  (testing "fake position can be determined in explicit context"
    (let [ctx (fc/context)
          foo (ctx-fake-fn ctx [f/any? nil])]
      (is (= {:file this-file :line ctx-expected-line :column ctx-expected-column} (fc/-position ctx foo))))))

(f/-deftest
  "position detection for fake"
  (testing-fake-fn-position-detection
    ; we can't pass a macro into a function so let's wrap it into a func
    (fn [config] (f/fake config))
    41 18
    (fn [ctx config] (fc/fake ctx config))
    43 22))

(f/-deftest
  "position detection for recorded fake with config"
  (testing-fake-fn-position-detection
    ; we can't pass a macro into a function so let's wrap it into a func
    (fn [config]
      (let [fake-fn (f/recorded-fake config)]
        (f/mark-checked fake-fn)
        fake-fn))
    51 21
    (fn [ctx config] (fc/recorded-fake ctx config))
    55 22))

(f/-deftest
  "position detection for recorded fake without config"
  (testing-fake-fn-position-detection
    ; we can't pass a macro into a function so let's wrap it into a func
    (fn [_config_]
      (let [fake-fn (f/recorded-fake)]
        ; supress self-test warnings
        (f/mark-checked fake-fn)
        fake-fn))
    63 21
    (fn [ctx _config_] (fc/recorded-fake ctx))
    68 24))

(f/-deftest
  "fake can be reused in a custom macro without losing ability to detect position"
  (testing-fake-fn-position-detection
    ; we can't pass a macro into a function so let's wrap it into a func
    (fn [config] (m/my-fake config))
    75 18
    (fn [ctx config] (m/ctx-my-fake ctx config))
    77 22))

(f/-deftest
  "recorded-fake with config can be reused in a custom macro without losing ability to detect position"
  (testing-fake-fn-position-detection
    ; we can't pass a macro into a function so let's wrap it into a func
    (fn [config]
      (let [fake-fn (m/my-recorded-fake config)]
        (f/mark-checked fake-fn)
        fake-fn))
    85 21
    (fn [ctx config] (m/ctx-my-recorded-fake ctx config))
    89 22))

(f/-deftest
  "recorded-fake without config can be reused in a custom macro without losing ability to detect position"
  (testing-fake-fn-position-detection
    ; we can't pass a macro into a function so let's wrap it into a func
    (fn [_config_]
      (let [fake-fn (m/my-recorded-fake)]
        (f/mark-checked fake-fn)
        fake-fn))
    97 21
    (fn [ctx _config_] (m/ctx-my-recorded-fake ctx))
    101 24))

(defprotocol LocalProtocol
  (bar [_]))

(f/-deftest
  "position can be determined in reify-fake"
  (testing "recorded fake with config, implicit context"
    (f/with-fakes
      (let [foo (f/reify-fake LocalProtocol
                              (bar :recorded-fake [f/any? nil]))]
        ; call to suppress self-test warning
        (f/mark-checked (f/method foo bar))
        (is (= {:file this-file :line 111 :column 17}
               (f/-position (f/method foo bar)))))))

  (testing "recorded fake without config, implicit context"
    (f/with-fakes
      (let [foo (f/reify-fake LocalProtocol
                              (bar :recorded-fake))]
        ; call to suppress self-test warning
        (f/mark-checked (f/method foo bar))
        (is (= {:file this-file :line 120 :column 17}
               (f/-position (f/method foo bar)))))))

  (testing "recorded fake with config, explicit context"
    (f/with-fakes
      (let [ctx (fc/context)
            foo (fc/reify-fake ctx LocalProtocol
                               (bar :recorded-fake [f/any? nil]))]
        (is (= {:file this-file :line 130 :column 17}
               (fc/-position ctx (fc/method ctx foo bar)))))))

  (testing "recorded fake without config, explicit context"
    (f/with-fakes
      (let [ctx (fc/context)
            foo (fc/reify-fake ctx LocalProtocol
                               (bar :recorded-fake))]
        (is (= {:file this-file :line 138 :column 17}
               (fc/-position ctx (fc/method ctx foo bar))))))))


(f/-deftest
  "position can be determined in reify-fake if it's used from a custom macro"
  (testing "recorded fake with config, implicit context"
    (f/with-fakes
      (let [foo (m/my-reify-fake LocalProtocol
                                 (bar :recorded-fake [f/any? nil]))]
        ; call to suppress self-test warning
        (f/mark-checked (f/method foo bar))
        (is (= {:file this-file :line 148 :column 17}
               (f/-position (f/method foo bar)))))))

  (testing "recorded fake without config, implicit context"
    (f/with-fakes
      (let [foo (m/my-reify-fake LocalProtocol
                                 (bar :recorded-fake))]
        ; call to suppress self-test warning
        (f/mark-checked (f/method foo bar))
        (is (= {:file this-file :line 157 :column 17}
               (f/-position (f/method foo bar)))))))

  (testing "recorded fake with config, explicit context"
    (f/with-fakes
      (let [ctx (fc/context)
            foo (m/ctx-my-reify-fake ctx LocalProtocol
                                     (bar :recorded-fake [f/any? nil]))]
        (is (= {:file this-file :line 167 :column 17}
               (fc/-position ctx (fc/method ctx foo bar)))))))

  (testing "recorded fake without config, explicit context"
    (f/with-fakes
      (let [ctx (fc/context)
            foo (m/ctx-my-reify-fake ctx LocalProtocol
                                     (bar :recorded-fake))]
        (is (= {:file this-file :line 175 :column 17}
               (fc/-position ctx (fc/method ctx foo bar))))))))