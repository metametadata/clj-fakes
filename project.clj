(defproject clj-fakes "0.11.0"
  :description "An isolation framework for Clojure/ClojureScript that makes creating test doubles (stubs, mocks, etc.) much easier."
  :url "https://github.com/metametadata/clj-fakes"
  :license {:name "MIT" :url "http://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]
                 [org.clojure/clojurescript "1.10.238" :scope "provided"]

                 ; For tests only
                 [org.clojure/core.async "0.4.474" :scope "provided"]]

  :profiles {:min-deps {:dependencies [[org.clojure/clojure "1.7.0"]

                                       ; Fix conflicts
                                       [org.clojure/clojurescript "1.9.229" :exclusions [org.clojure/clojure]]
                                       [org.clojure/tools.reader "1.0.0-beta4"]]}}

  :plugins [[lein-cljsbuild "1.0.6"]
            [com.jakemccrary/lein-test-refresh "0.15.0"]
            [lein-doo "0.1.10" :exclusions [org.clojure/clojure]]
            [lein-codox "0.9.5"]]

  :pedantic? :abort

  :source-paths ["src" "test"]
  :java-source-paths ["test/unit/fixtures/interop"]

  :clean-targets ^{:protect false} [:target-path "resources/public/js/" "out"]

  :test-refresh {:notify-command ["terminal-notifier" "-title" "Tests" "-message"]
                 :quiet          true}

  :repositories {"clojars" {:sign-releases false}}

  :cljsbuild
  {:builds {:test {:source-paths ["src" "test" "test/unit" "test/unit/fixtures"]
                   :compiler     {:main          unit.runner
                                  :output-to     "resources/public/js/testable.js"
                                  :optimizations :none}}}}

  :codox {:source-uri   "https://github.com/metametadata/clj-fakes/blob/master/{filepath}#L{line}"
          :source-paths ["src"]
          :namespaces   [clj-fakes.core clj-fakes.context]
          :output-path  "site/api"
          :metadata     {:doc/format :markdown}})
