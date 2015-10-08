(defproject clj-fakes "0.1.1-SNAPSHOT"
  :description "An isolation framework for Clojure/ClojureScript. It makes creating mocks and stubs for unit testing much easier."
  :url "https://github.com/metametadata/clj-fakes"
  :license {:name "MIT" :url "http://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.122"]]

  :plugins [[lein-cljsbuild "1.0.6"]
            [com.jakemccrary/lein-test-refresh "0.10.0"]
            [lein-doo "0.1.4"]
            [codox "0.8.13"]]

  :source-paths ["src" "test"]

  :clean-targets ^{:protect false} [:target-path "resources/public/js/"]

  :test-refresh {:notify-command ["terminal-notifier" "-title" "Tests" "-message"]
                 :quiet          true}

  :cljsbuild
  {:builds {:test {:source-paths ["src" "test" "test/unit" "test/unit/fixtures"]
                   :compiler     {:output-to     "resources/public/js/testable.js"
                                  :main          'unit.runner
                                  :optimizations :whitespace}}}}

  :codox {
          :src-dir-uri "https://github.com/metametadata/clj-fakes/blob/master/"
          :src-linenum-anchor-prefix "L"
          :sources    ["src"]
          :exclude    clj-fakes.macro
          :output-dir "site/api"
          :defaults   {:doc/format :markdown}
          :project    {:description ""}})
