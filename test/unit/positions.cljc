; positioning is also partially tested implicitly in tests about self-tests
(ns unit.positions
  (:require
    [clojure.test :refer [is testing]]
    [unit.utils :as u]
    [clj-fakes.core :as f]
    [clj-fakes.context :as fc]
    [unit.fixtures.macros :as m]))

(def this-file-re #".*unit/positions.cljc$")

(defn is-pos
  [pos expected-line expected-column]
  (is (not (nil? (re-find this-file-re (:file pos)))))
  (is (= expected-line (:line pos)))
  (is (= expected-column (:column pos))))

(defn is-fake-has-position
  [f expected-line expected-column]
  (is-pos (f/-position f) expected-line expected-column))

(defn is-fake-has-position-in-context
  [ctx f expected-line expected-column]
  (is-pos (fc/-position ctx f) expected-line expected-column))

(defn test-fake-fn-position-detection
  [fake-fn expected-line expected-column
   ctx-fake-fn ctx-expected-line ctx-expected-column]
  (testing "fake position can be determined in implicit context"
    (f/with-fakes
      (let [foo (fake-fn [f/any nil])]
        (foo)                                               ; call to suppress an exception from self-test
        (is-fake-has-position foo expected-line expected-column))))

  (testing "fake position can be determined in explicit context"
    (let [ctx (fc/context)
          foo (ctx-fake-fn ctx [f/any nil])]
      (is-fake-has-position-in-context ctx foo ctx-expected-line ctx-expected-column))))

(u/deftest+
  "position detection for fake"
  (test-fake-fn-position-detection
    ; we can't pass a macro into a function so let's wrap it into a func
    (fn [config] (f/fake config))
    44 18
    (fn [ctx config] (fc/fake ctx config))
    46 22))

(u/deftest+
  "position detection for recorded fake with config"
  (test-fake-fn-position-detection
    ; we can't pass a macro into a function so let's wrap it into a func
    (fn [config]
      (let [fake-fn (f/recorded-fake config)]
        (f/mark-checked fake-fn)
        fake-fn))
    54 21
    (fn [ctx config] (fc/recorded-fake ctx config))
    58 22))

(u/deftest+
  "position detection for recorded fake without config"
  (test-fake-fn-position-detection
    ; we can't pass a macro into a function so let's wrap it into a func
    (fn [_config_]
      (let [fake-fn (f/recorded-fake)]
        ; supress self-test warnings
        (f/mark-checked fake-fn)
        fake-fn))
    66 21
    (fn [ctx _config_] (fc/recorded-fake ctx))
    71 24))

(u/deftest+
  "fake can be reused in a custom macro without losing ability to detect position"
  (test-fake-fn-position-detection
    ; we can't pass a macro into a function so let's wrap it into a func
    (fn [config] (m/my-fake config))
    78 18
    (fn [ctx config] (m/ctx-my-fake ctx config))
    80 22))

(u/deftest+
  "recorded-fake with config can be reused in a custom macro without losing ability to detect position"
  (test-fake-fn-position-detection
    ; we can't pass a macro into a function so let's wrap it into a func
    (fn [config]
      (let [fake-fn (m/my-recorded-fake config)]
        (f/mark-checked fake-fn)
        fake-fn))
    88 21
    (fn [ctx config] (m/ctx-my-recorded-fake ctx config))
    92 22))

(u/deftest+
  "recorded-fake without config can be reused in a custom macro without losing ability to detect position"
  (test-fake-fn-position-detection
    ; we can't pass a macro into a function so let's wrap it into a func
    (fn [_config_]
      (let [fake-fn (m/my-recorded-fake)]
        (f/mark-checked fake-fn)
        fake-fn))
    100 21
    (fn [ctx _config_] (m/ctx-my-recorded-fake ctx))
    104 24))

(defprotocol LocalProtocol
  (bar [_]))

(u/deftest+
  "position can be determined in reify-fake"
  (testing "recorded fake with config, implicit context"
    (f/with-fakes
      (let [foo (f/reify-fake LocalProtocol
                              (bar :recorded-fake [f/any nil]))]
        ; call to suppress self-test warning
        (f/mark-checked (f/method foo bar))
        (is-fake-has-position (f/method foo bar) 114 17))))

  (testing "recorded fake without config, implicit context"
    (f/with-fakes
      (let [foo (f/reify-fake LocalProtocol
                              (bar :recorded-fake))]
        ; call to suppress self-test warning
        (f/mark-checked (f/method foo bar))
        (is-fake-has-position (f/method foo bar) 122 17))))

  (testing "recorded fake with config, explicit context"
    (f/with-fakes
      (let [ctx (fc/context)
            foo (fc/reify-fake ctx LocalProtocol
                               (bar :recorded-fake [f/any nil]))]
        (is-fake-has-position-in-context ctx (fc/method ctx foo bar) 131 17))))

  (testing "recorded fake without config, explicit context"
    (f/with-fakes
      (let [ctx (fc/context)
            foo (fc/reify-fake ctx LocalProtocol
                               (bar :recorded-fake))]
        (is-fake-has-position-in-context ctx (fc/method ctx foo bar) 138 17)))))

(u/deftest+
  "position can be determined in reify-fake if it's used from a custom macro"
  (testing "recorded fake with config, implicit context"
    (f/with-fakes
      (let [foo (m/my-reify-fake LocalProtocol
                                 (bar :recorded-fake [f/any nil]))]
        ; call to suppress self-test warning
        (f/mark-checked (f/method foo bar))
        (is-fake-has-position (f/method foo bar) 146 17))))

  (testing "recorded fake without config, implicit context"
    (f/with-fakes
      (let [foo (m/my-reify-fake LocalProtocol
                                 (bar :recorded-fake))]
        ; call to suppress self-test warning
        (f/mark-checked (f/method foo bar))
        (is-fake-has-position (f/method foo bar) 154 17))))

  (testing "recorded fake with config, explicit context"
    (f/with-fakes
      (let [ctx (fc/context)
            foo (m/ctx-my-reify-fake ctx LocalProtocol
                                     (bar :recorded-fake [f/any nil]))]
        (is-fake-has-position-in-context ctx (fc/method ctx foo bar) 163 17))))

  (testing "recorded fake without config, explicit context"
    (f/with-fakes
      (let [ctx (fc/context)
            foo (m/ctx-my-reify-fake ctx LocalProtocol
                                     (bar :recorded-fake))]
        (is-fake-has-position-in-context ctx (fc/method ctx foo bar) 170 17)))))