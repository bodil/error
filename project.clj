(defproject org.bodil/error "0.1.3"
  :description "Async testing toolkit for ClojureScript"
  :url "https://github.com/bodil/error"
  :license {:name "Apache License, version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :plugins [[lein-cljsbuild "1.0.2"]]
  :dependencies [[org.clojure/clojurescript "0.0-2156"]
                 [org.bodil/redlobster "0.2.1"]]
  :profiles
  {:dev
   {:dependencies [[org.bodil/cljs-noderepl "0.1.10"]]
    :plugins [[org.bodil/lein-noderepl "0.1.10"]
              [org.bodil/lein-error "0.1.3"]]
    :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}
  :cljsbuild
  {:builds {:test {:source-paths ["src" "test"]
                   :compiler
                   {:output-to "js/test.js"
                    :optimizations :simple
                    :pretty-print true}}}})
