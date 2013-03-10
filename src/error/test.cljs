(ns error.test
  (:use-macros [redlobster.macros :only [waitp when-realised]])
  (:require [error.assert :as a]
            [redlobster.promise :as p]
            [clojure.string :as s]))



(def ^:private tests {})



(defn- on-node? []
  (try (string? process.versions.node)
       (catch js/Error e false)))

(defn- on-browser? []
  (try (string? navigator.userAgent)
       (catch js/Error e false)))

(defn- valid-platform? [platforms]
  (if (nil? platforms)
    true
    (let [platforms (if (keyword? platforms) #{platforms} (set platforms))]
      (cond
       (on-node?) (:node platforms)
       (on-browser?) (:browser platforms)))))



(defprotocol ITest
  (-start [this]))

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
    (test-func this)
    (p/timeout this (get options :timeout 10000))
    this))

(defn- launch-test [ns desc test options]
  (-start (Test. ns desc test options (p/promise))))

(defn- launch-suite [suite]
  (for [ns (keys suite) [desc test options] (suite ns)
        :when (valid-platform? (:only options))]
    (launch-test ns desc test options)))



(defn- succeeded? [test]
  (and (not (p/failed? test)) (= :error.test/success @test)))

(defn- failed? [test]
  (and (not (p/failed? test)) (not (= :error.test/success @test))))

(defn- errored? [test]
  (and (p/failed? test) (not= :redlobster.promise/timeout @test)))

(defn- timed-out? [test]
  (and (p/failed? test) (= :redlobster.promise/timeout @test)))



(defn- ansi [code string]
  (str \u001b "[" code string \u001b "[0m"))

(defn- pluralise [value descriptor suffix]
  (let [out (str value " " descriptor)]
    (cond
     (zero? value) nil
     (= value 1) (str out suffix)
     :else (str out "s" suffix))))

(defn- str-list [& items]
  (str (s/join ", " (remove nil? items)) "."))

(defn- header [suite context]
  (str (ansi "36;1m" "ERROR")
       (ansi "34;1m"
        (str " running "
             (pluralise (count suite) "test" "")
             " from "
             (pluralise (-> (map #(:namespace %) suite) set count) "namespace" "")
             " in environment "))
       (ansi "33;1m" context)))

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
       (print (if (timed-out? test)
                (ansi "33m" "?")
                (ansi "33m" "!")))
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
        @test)))



(defn run-tests [context]
  (let [t (launch-suite tests)]
    (println)
    (println (header t context))
    (println)

    (when-realised (cons :all (map test-result t))
      (println "\n")
      (let [reporting (remove succeeded? t)
            success (= 0 (count reporting))]
        (println (ansi (if success "32;1m" "31;1m")
                       (report-summary t)))
        (println)
        (doseq [test reporting]
          (println (report-fail test)))
        (when (seq reporting) (println))

        (cond
         (= context "node")
         (.exit js/process (if success 0 1))

         (= context "phantom")
         (window/callPhantom (js-obj "cmd" "quit" "data" (if success 0 1))))))))
