(ns tasks.core
  (:require [clojure.string :as string]
            [cljs.analyzer.api]))

(defn run
  "Runs the specified process."
  [command & args]
  (println "ᐅ" (str command " " (string/join " " args)))
  (.spawnSync (js/require "child_process") command (clj->js args) (clj->js {:stdio "inherit"})))

(defn cli*
  "tasks example: [{:name 'site' :fn site-fn :doc 'Lorem ipsum.'} ...]"
  [tasks]
  ; .-argv example: [/usr/local/Cellar/lumo/1.0.0/bin/lumo nexe.js ./tasks.cljc site]
  (let [task-name (get (.-argv js/process) 3)
        task-fn (->> tasks
                     (filter #(= (:name %) task-name))
                     first
                     :fn)]
    (when (nil? task-fn)
      (if (nil? task-name)
        (println "Error: no task specified")
        (println "Error: unknown task" (pr-str task-name)))

      (println)
      (println "Usage: <this script> task")
      (println "Available tasks:")
      (println)
      (println (->> tasks
                    (map #(str "\t" (:name %) "\t" (:doc %)))
                    (string/join "\n")))
      (println)

      (.exit js/process 1))

    (task-fn)))

(defmacro cli
  "Executes the task passed into argv."
  []
  (let [publics (cljs.analyzer.api/ns-publics (.-name *ns*))
        task-publics (->> publics
                          (filter #(-> % second :task)))
        tasks (->> task-publics (mapv #(-> {:name (-> % second :name name)
                                            :fn   (-> % second :name)
                                            :doc  (-> % second :doc)})))]
    `(cli* ~tasks)))