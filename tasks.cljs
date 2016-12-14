#!/usr/bin/env lumo
(ns core.tasks
  (:require
    [tasks.core :include-macros true :as t]))

(def fs (js/require "fs-extra"))
(def path (js/require "path"))

(defn with-temp-copy
  "Copies file from source to dest, then calls the specified function.
  Finally, source file is returned to its initial state."
  [source-path dest-path f]
  (let [dest-els (.parse path dest-path)
        backup-path (.join path
                           (.-dir dest-els)
                           (str (.-name dest-els)
                                (.-ext dest-els)
                                "_backup"))]
    (try
      (print "copy" dest-path "to" backup-path)
      (.copySync fs dest-path backup-path)
      (print "copy" source-path "to" dest-path)
      (.copySync fs source-path dest-path)
      (f)

      (finally
        (print "recover" dest-path "from" backup-path)
        (.copySync fs backup-path dest-path)

        (print "remove temp file" backup-path)
        (.removeSync fs backup-path)))))

(defn ^:task api
  "Compile API reference into site folder."
  []
  (t/run "lein" "codox"))

(defn ^:task mkdocs
  "Only build site pages using MkDocs."
  []
  ; use project's readme file for rendering the index page
  (with-temp-copy "README.md" (.join path "docs" "index.md")
                  #(t/run "mkdocs" "build" "--clean")))

(defn ^:task site
  "Build project site (including API docs)."
  []
  (mkdocs)
  (api))

; catch exceptions explicitly because Lumo doesn't yet return error code and print stacktraces
(try
  (t/cli)

  (catch :default e
    (println "Exiting because of exception:" e)
    (println "Exception:" (.-stack e))
    (.exit js/process 1)))