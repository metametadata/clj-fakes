(defproject clj-fakes "0.4.0"
  :description "An isolation framework for Clojure/ClojureScript. It makes creating mocks and stubs for unit testing much easier."
  :url "https://github.com/metametadata/clj-fakes"
  :license {:name "MIT" :url "http://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/clojure "1.7.0" :scope "provided"]
                 [org.clojure/clojurescript "1.7.28" :scope "provided"]]

  :plugins [[lein-cljsbuild "1.0.6"]
            [com.jakemccrary/lein-test-refresh "0.11.0"]
            [lein-doo "0.1.6"]
            [lein-codox "0.9.4"]]

  :source-paths ["src" "test"]
  :java-source-paths ["test/unit/fixtures/interop"]

  :clean-targets ^{:protect false} [:target-path "resources/public/js/" "out"]

  :test-refresh {:notify-command ["terminal-notifier" "-title" "Tests" "-message"]
                 :quiet          true}

  :cljsbuild
  {:builds {:test {:source-paths ["src" "test" "test/unit" "test/unit/fixtures"]
                   :compiler     {:output-to     "resources/public/js/testable.js"
                                  :main          'unit.runner
                                  :optimizations :whitespace}}}}

  :codox {:source-uri   "https://github.com/metametadata/clj-fakes/blob/master/{filepath}#L{line}"
          :source-paths ["src"]
          :namespaces   [clj-fakes.core clj-fakes.context]
          :output-path  "site/api"
          :metadata     {:doc/format :markdown}})
