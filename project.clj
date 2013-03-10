(defproject org.bodil/error "0.1.1"
  :description "Async testing toolkit for ClojureScript"
  :url "https://github.com/bodil/error"
  :license {:name "Apache License, version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :plugins [[lein-cljsbuild "0.3.0"]]
  :dependencies [[org.bodil/redlobster "0.2.0"]]
  :profiles
  {:dev
   {:dependencies [[org.bodil/cljs-noderepl "0.1.8"]]
    :plugins [[org.bodil/lein-noderepl "0.1.8"]
              [org.bodil/lein-error "0.1.0"]]
    :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}
  :cljsbuild
  {:builds {:test {:source-paths ["src" "test"]
                   :compiler
                   {:output-to "js/test.js"
                    :optimizations :simple
                    :pretty-print true}}}})
