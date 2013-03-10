(ns error.assert
  (:require [redlobster.promise :as p]))

(defprotocol IAssertionError
  (pprint [this]))

(deftype AssertionError [expected actual message]
  IAssertionError
  (pprint [this]
    (let [comparison
          (str "     Expected: " (pr-str expected)
               "\n       Actual: " (pr-str actual))]
      (if message
        (str "    Assertion: " message "\n" comparison)
        comparison))))

(defmulti reporting-assert (fn [sym _ _ _] sym))

(defmethod reporting-assert '= [_ func args msg]
  (let [expected (first args)
        actual (second args)]
    (when-not (= expected actual)
      (throw (AssertionError. expected actual msg)))))

(defmethod reporting-assert :default [_ func args msg]
  (let [actual (apply func args)]
    (when-not actual
      (throw (AssertionError. true actual msg)))))

(defn is [promise sym func args msg]
  (try
    (reporting-assert sym func args msg)
    (catch AssertionError e
      (when-not (p/realised? promise)
        (p/realise promise e)))))
