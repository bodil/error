(ns error.test
  (:use-macros [redlobster.macros :only [waitp when-realised]])
  (:require [error.assert :as a]
            [error.environment :as env]
            [redlobster.promise :as p]
            [clojure.string :as s]))



(def ^:private tests {})
(def ^:private environment (env/detect-environment))



(defprotocol ITest
  (-start [this])
  (-success? [this]))

(defrecord Test [namespace description test-func options promise]
  IDeref
  (-deref [this] (deref promise))

  p/IPromise
  (realised? [this] (p/realised? promise))
  (failed? [this] (p/failed? promise))
  (realise [this value] (p/realise promise value))
  (realise-error [this value] (p/realise-error promise value))
  (on-realised [this succ err] (p/on-realised promise succ err))

  ITest
  (-start [this]
    (try
     (test-func this)
     (catch :default e
         (p/realise-error promise e)))
    (p/timeout this (get options :timeout 10000))
    this)
  (-success? [this]
    ;; (case (:expect options)
    ;;   nil (and (not (p/failed? this)) (= :error.test/success @this))
    ;;   :fail (and (not (p/failed? this)) (not (= :error.test/success @this)))
    ;;   :timeout (and (p/failed? this) (= :redlobster.promise/timeout @this))
    ;;   :error (and (p/failed? this) (not= :redlobster.promise/timeout @this))
    ;;   (and (p/failed? this) (satisfies? (:expect options) @this)))
    (cond
     (nil? (:expect options))
     (and (not (p/failed? this)) (= :error.test/success @this))

     (= :fail (:expect options))
     (and (not (p/failed? this)) (not (= :error.test/success @this)))

     (= :timeout (:expect options))
     (and (p/failed? this) (= :redlobster.promise/timeout @this))

     (= :error (:expect options))
     (and (p/failed? this) (not= :redlobster.promise/timeout @this))

     :else
     (and (p/failed? this) (instance? (:expect options) @this)))))

(defn- active-test? [ns desc test options]
  (and (env/valid-platform? (:only options))
       (not (:ignore options))
       (not (= ";" (subs desc 0 1)))))

(defn- launch-test [ns desc test options]
  (-start (Test. ns desc test options (p/promise))))

(defn- launch-suite [suite]
  (for [ns (keys suite) [desc test options] (suite ns)
        :when (active-test? ns desc test options)]
    (launch-test ns desc test options)))



(defn- succeeded? [test]
  (-success? test))

(defn- failed? [test]
  (and (not (-success? test))
       (not (p/failed? test))))

(defn- errored? [test]
  (and (not (-success? test))
       (p/failed? test)
       (not= :redlobster.promise/timeout @test)))

(defn- timed-out? [test]
  (and (not (-success? test))
       (p/failed? test)
       (= :redlobster.promise/timeout @test)))



(defn- ansi [code string]
  (if (env/in-repl?) string
      (str \u001b "[" code string \u001b "[0m")))

(defn- pluralise [value descriptor suffix]
  (let [out (str value " " descriptor)]
    (cond
     (zero? value) nil
     (= value 1) (str out suffix)
     :else (str out "s" suffix))))

(defn- str-list [& items]
  (str (s/join ", " (remove nil? items)) "."))

(defn- header [suite]
  (str (ansi "34;1m"
             (str "Running "
                  (pluralise (count suite) "test" "")
                  " from "
                  (pluralise (-> (map #(:namespace %) suite) set count) "namespace" "")
                  " in environment "))
       (ansi "33;1m" environment)))

(defn- test-result [test]
  (waitp test
         #(do
            (cond
             (succeeded? test)
             (print (ansi "32m" "."))

             (failed? test)
             (print (ansi "31;1m" "!")))

            (realise %))
         #(do
            (cond
             (succeeded? test)
             (print (ansi "32m" "."))

             (timed-out? test)
             (print (ansi "33m" "?"))

             :else
             (print (ansi "33m" "!")))
            (realise-error %))))

(defn- report-summary [suite]
  (let [succeeded (filter succeeded? suite)
        failed (filter failed? suite)
        timed-out (filter timed-out? suite)
        errored (filter errored? suite)]
    (str-list (pluralise (count succeeded) "test" " succeeded")
              (pluralise (count failed) "test" " failed")
              (pluralise (count timed-out) "test" " timed out")
              (pluralise (count errored) "test" " threw errors"))))

(defn- report-fail [test]
  (cond
   (failed? test)
   (str (ansi "31m" (str "FAILED "))
        (ansi "36m" (str (:namespace test) ": " (:description test)))
        "\n"
        (error.assert/pprint @test))

   (timed-out? test)
   (str (ansi "33m" (str "TIMEOUT "))
        (ansi "36m" (str (:namespace test) ": " (:description test)))
        "\n    Test never called (done)")

   (errored? test)
   (str (ansi "33m" (str "ERROR "))
        (ansi "36m" (str (:namespace test) ": " (:description test)))
        "\n    "
        @test)

   :else (.inspect (js/require "util") @test)))



(defn run-tests []
  (if (empty? tests)
    (println (ansi "31;1m" "ERROR") (ansi "34;1m" "no tests defined."))

    (let [t (launch-suite tests)]
      (println)
      (println (header t))
      (println)

      (when-realised (cons :all (map (if (env/in-repl?) identity test-result) t))
        (println "\n")
        (let [reporting (remove succeeded? t)
              success (= 0 (count reporting))]
          (println (ansi (if success "32;1m" "31;1m")
                         (report-summary t)))
          (println)
          (doseq [test reporting]
            (println (report-fail test)))
          (when (seq reporting) (println))
          (when-not (env/in-repl?)
            (cond
             (= environment "node")
             (.exit js/process (if success 0 1))

             (= environment "phantom")
             (window/callPhantom
              (js-obj "cmd" "quit" "data" (if success 0 1)))))))
      nil)))

(defn clear []
  (set! tests {}))
