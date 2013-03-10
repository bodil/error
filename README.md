# ERROR

A testing toolkit for ClojureScript, designed for testing asynchronous
code.

![I AM ERROR](https://raw.github.com/bodil/error/master/iamerror.jpg)

## Setup

Add Error as a development dependency in `project.clj`:

```clojure
:profiles {:dev {:plugins [[org.bodil/lein-error "0.1.0"]]
                 :dependencies [[org.bodil/error "0.1.0"]]}}
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

Tests can be run through Leiningen by executing `lein error`. You can
specify which environments to run the test suite in by giving keyword
arguments to `lein error`, eg. `lein error :node :phantom`.

## Writing Tests

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
```

# License

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
