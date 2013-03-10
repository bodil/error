(ns error.macros
  (:refer-clojure :exclude [test]))

(defn- parse-args [args]
  (let [has-options (map? (first args))
        options (if has-options (first args) nil)
        args (if has-options (rest args) args)

        has-docstring (string? (first args))
        docstring (if has-docstring (first args) nil)
        args (if has-docstring (rest args) args)

        has-bindings (vector? (first args))
        bindings (if has-bindings (first args) nil)

        forms (if has-bindings (rest args) args)]

    [options docstring bindings forms]))

(defmacro test
  "Defines a test case. Takes three optional arguments in order: a map
of options, a string describing the test, and a vector of binding/value
pairs. The remaining arguments will constitute the body of the test.

The function `done` is defined in the scope of the test, and must be
called when the test is done, or the test will never complete and
eventually time out.

Available options are:

    {:timeout <milliseconds>}
        defines the time a test has to complete before timing out
    {:only <platforms>}
        given a set of keywords, or just one keyword, will restrict
        the test to running on the given platforms. Possible keywords
        are `:node` and `:browser`."

  [& args]
  (let [[options test-name bindings forms] (parse-args args)
        ns-name (name (-> &env :ns :name))]
    `(let [test#
           (fn [promise#]
             (let [~'done
                   #(when-not (redlobster.promise/realised? promise#)
                      (redlobster.promise/realise promise# :error.test/success))
                   ~'-error-promise promise#]
               ~(if bindings `(let ~bindings ~@forms) `(do ~@forms))))
           tests# error.test/tests
           ns-tests# (or (tests# ~ns-name) [])]
       (set! error.test/tests
             (assoc tests# ~ns-name (conj ns-tests#
                                          [~test-name test# ~options]))))))

(defmacro is [[func & args] & [message]]
  `(error.assert/is ~'-error-promise '~func ~func (list ~@args) ~message))
