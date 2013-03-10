(ns error.case
  (:require-macros [error.macros :refer [test is]]
                   [redlobster.macros :refer [defer]])
  (:require [error.test]))

(test "tests should execute and things and stuff"
      [foo "foo"
       bar "bar"]
      (is (= "foobar" (str foo bar))
          "bindings gonna bind, str gonna concatenate")
      (done))

(test "tests should probably sometimes liek fail and stuff"
      (is (= "foobar" "gazonk")
          "these things are totally not the same")
      (done))

(test "async test should async"
      (defer 10 (done)))

(test "failing async test should fail"
      (defer 10
        (is (= "GOOBY PLS" "Goofy, please."))
        (done)))

(test {:timeout 100}
      "tests that don't call (done) should timeout at some point"
      (is (= true true)))

(test {:only :node}
      "tests with {:only :node} set should only run on Node"
      (is (string? process.versions.node))
      (done))

(test {:only :browser}
      "tests with {:only :browser} set should only run in a browser"
      (is (string? navigator.userAgent))
      (done))
