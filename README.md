# ERROR

A testing toolkit for ClojureScript, designed for testing asynchronous
code.

![I AM ERROR](https://raw.github.com/bodil/error/master/iamerror.jpg)

## Setup

Add Error as a development dependency in `project.clj`:

```clojure
:profiles {:dev {:plugins [[org.bodil/lein-error "0.1.1"]]
                 :dependencies [[org.bodil/error "0.1.1"]]}}
```

You also need to set up a cljsbuild for compiling your tests. It needs
to be called `:test`, must have `:optimizations` set to at least
`:simple`, and must have `:output-to` set. Even if you plan on running
the tests on Node, you should not set `:target :nodejs`.

```clojure
:builds {:test {:source-paths ["src" "test"]
                :compiler {:output-to "js/test.js"
                           :optimizations :simple}}}
```

You can define default environments for Error in `project.clj`; if you
don't, it will default to running only on Node. Available environments
are `:node`, which runs the test suite on Node, and `:phantom`, which
runs it on PhantomJS.

```clojure
:error {:environments #{:node :phantom}}
```

## Usage

### Leiningen

Tests can be run through Leiningen by executing `lein error`. You can
specify which environments to run the test suite in by giving keyword
arguments to `lein error`, eg. `lein error :node :phantom`.

### REPL

To run the test suite from the REPL, simply ensure that the tests you
wish to run are loaded into the REPL, and run
`(error.test/run-tests)`.

For instance, to run Error's own test suite, start a REPL in Error's
project directory and run the following:

```clojure
$ lein trampoline noderepl
"Type: " :cljs/quit " to quit"
ClojureScript:cljs.user> (load-file "error/case.cljs")
ClojureScript:cljs.user> (error.test/run-tests)

Running 8 tests from 1 namespace in environment node

8 tests succeeded.
```

## Writing Tests

Tests are defined using the `test` macro. Error is intended for
testing asynchronous code, so a function `done` is provided inside the
scope of the test, which must be called when the test is finished. If
you forget to call `done`, the test will eventually time out and fail.
Default timeout is set to 10 seconds, but you can specify a custom
timeout in a test's options map, as seen below.

```clojure
(ns i.can.has.tests
  (:require-macros [error.macros :refer [test is]])
  (:require [error.test]))

(test "str should concatenate strings together"
  (is (= "Pinkie Pie" (str "Pinkie" " Pie")))
  (done))

(test "should asynchronously complete a test using async code"
  (js/setTimeout #(done) 100))

(test "str should still concatenate with some test local bindings"
  [pinkie "Pinkie "
   pie "Pie"]
  (is (= "Pinkie Pie" (str pinkie pie)))
  (done))

(test {:only :node} "node specific test should only run on node"
  (process/nextTick #(done)))

(test {:timeout 15000} "should succeed in 14 seconds"
  (js/setTimeout #(done) 14000))

(test {:expect :fail} "a failing test expected to fail should succeed"
  (is (= 13 37))
  (done))
```

### Test Options

The following options are supported by the `test` macro:

* `:timeout <number>` defines the number of milliseconds a test has to
  complete before failing with a timeout. Defaults to 10 seconds.
* `:only <environment>` takes a set of keywords, or just one keyword,
  describing which environments the test is valid for. If the test
  suite is running in a different environment, the test will be
  skipped. Valid environments are `:node` and `:browser`.
* `:ignore true` will cause the test to be ignored.
* `:expect <result>` redefines a test's success condition. The
  argument can be either of the following:
  * `:fail` expects the test to fail.
  * `:timeout` expects the test to time out.
  * `:error` expects the test to result in an exception.
  * Any other value expects the test to result in an exception which
    satisfies `(instance? <value> <exception>)`. For example, `:expect
    js/TypeError` expects the test to throw a TypeError exception.

Additionally, if the test's description string starts with the comment
character `;`, it will be ignored just as if `:ignore true` were set.

## TODO

* Nashorn test runner, perhaps degrading to Rhino on JDK < 8.
* Watch mode for `lein error`.

## License

Copyright 2013 Bodil Stokke

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at
[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0).

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License.
