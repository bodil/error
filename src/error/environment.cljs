(ns error.environment)

(def ^:private in-repl true)

(defn on-node? []
  (try (string? process.versions.node)
       (catch js/Error e false)))

(defn on-phantom? []
  (or (try (fn? window.callPhantom)
           (catch js/Error e false))
      (try (fn? phantom.injectJs)
           (catch js/Error e false))))

(defn on-browser? []
  (try (string? navigator.userAgent)
       (catch js/Error e false)))

(defn in-repl? []
  in-repl)

(defn valid-platform? [platforms]
  (if (nil? platforms)
    true
    (let [platforms (if (keyword? platforms) #{platforms} (set platforms))]
      (cond
       (on-node?) (:node platforms)
       (on-browser?) (:browser platforms)))))

(defn detect-environment []
  (cond
   (on-node?) "node"
   (on-phantom?) "phantom"
   (on-browser?) "browser"))
