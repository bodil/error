(ns leiningen.error
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [leiningen.core.main :as main]
            [leiningen.cljsbuild :refer [cljsbuild]]))

(defn- load-as-tempfile
  "Copy a file from the classpath into a temporary file.
  Return the path to the temporary file."
  [filename]
  (let [tempfile (java.io.File/createTempFile "error" ".js")
        resource (io/resource filename)]
    (.deleteOnExit tempfile)
    (assert resource (str "Can't find " filename " in classpath"))
    (with-open [in (io/input-stream resource)
                out (io/output-stream tempfile)]
      (io/copy in out))
    (.getAbsolutePath tempfile)))

(defn- process
  [cwd args]
  (let [proc (ProcessBuilder. args)]
    (.directory proc (io/file cwd))
    (.redirectErrorStream proc true)
    (.start proc)))

(defn- exec
  [cwd & args]
  (let [proc (process cwd args)]
    (io/copy (.getInputStream proc) (System/out))))

(defn- on-path?
  "Check if a command is available on the path."
  [cmd]
  (= 0 ((sh "which" cmd) :exit)))

(defn- phantom? [] (on-path? "phantomjs"))
(defn- node? [] (on-path? "node"))

(defn- run-node [project path]
  (let [script (load-as-tempfile "org/bodil/error/node.js")]
    (exec (project :root) "node" script path)))

(defn- run-phantom [project path]
  (let [script (load-as-tempfile "org/bodil/error/phantom.js")]
    (exec (project :root) "phantomjs" script path)))

(defn- find-test-binary [project]
  (or (-> project :cljsbuild :builds :test :compiler :output-to)
      (do
        (println "Unable to determine test build path.")
        (println "Please ensure your :test build contains an explicit :output-to option.")
        (main/abort))))

(defn error
  "Run Error tests."
  ([project & args]
     (let [{:keys [environments]} (:error project)
           environments (set (if (seq args) (set (map #(keyword (subs % 1)) args))
                                 (or environments #{:node})))
           test-binary (find-test-binary project)]
       (when (and (:phantom environments) (not (phantom?)))
         (println "PhantomJS environment requested, but \"phantomjs\" not on path.")
         (main/abort))
       (when (and (:node environments) (not (node?)))
         (println "Node environment requested, but \"node\" not on path.")
         (main/abort))
       (cljsbuild project "once" "test")
       (doseq [env environments]
         (case env
           :node (run-node project test-binary)
           :phantom (run-phantom project test-binary))))))
