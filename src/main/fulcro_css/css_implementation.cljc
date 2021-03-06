(ns fulcro-css.css-implementation
  "Implementation details for co-located CSS. Do not use these directly."
  ;; IMPORTANT: DO NOT INCLUDE GARDEN HERE!!!!
  (:require [cljs.tagged-literals]
            #?(:clj [garden.selectors :as gs])
            [fulcro-css.css-protocols :as cp :refer [local-rules include-children global-rules]]
            [clojure.string :as str]))

;; from core
(defn cssify
  "Replaces slashes and dots with underscore."
  [str] (when str (str/replace str #"[./]" "_")))

(defn fq-component [comp-class]
  #?(:clj  (if (nil? (meta comp-class))
             (str/replace (.getName comp-class) #"[_]" "-")
             (str (:component-ns (meta comp-class)) "/" (:component-name (meta comp-class))))
     :cljs (if-let [nm (.. comp-class -displayName)]
             nm
             "unknown/unknown")))

(defn local-class
  "Generates a string name of a localized CSS class. This function combines the fully-qualified name of the given class
     with the (optional) specified name."
  ([comp-class]
   (str (cssify (fq-component comp-class))))
  ([comp-class nm]
   (str (cssify (fq-component comp-class)) "__" (name nm))))

(defn set-classname
  [m subclasses]
  #?(:clj  (-> m
             (assoc :className subclasses)
             (dissoc :class))
     :cljs (cljs.core/clj->js (-> m
                                (assoc :className subclasses)
                                (dissoc :class)))))

;; css
#?(:clj (defn implements-protocol?
          [x protocol protocol-key]
          (if (fn? x)
            (some? (-> x meta protocol-key))
            (extends? protocol (class x)))))

(defn CSS?
  "Returns true if the given component has css"
  [x]
  #?(:clj  (implements-protocol? x cp/CSS :local-rules)
     :cljs (implements? cp/CSS x)))

(defn Global?
  "Returns true if the component has global rules"
  [x]
  #?(:clj  (implements-protocol? x cp/Global :global-rules)
     :cljs (implements? cp/Global x)))

(defn get-global-rules
  "Get the *raw* value from the global-rules of a component."
  [component]
  (if (Global? component)
    #?(:clj  ((:global-rules (meta component)) component)
       :cljs (global-rules component))
    []))

(defn get-local-rules
  "Get the *raw* value from the local-rules of a component."
  [component]
  (if (CSS? component)
    #?(:clj  ((:local-rules (meta component)) component)
       :cljs (local-rules component))
    []))

(defn prefixed-name?
  "Returns true if the given string starts with one of [. $ &$ &.]"
  [nm]
  (some? (re-matches #"(\.|\$|&\.|&\$).*" nm)))

(defn get-prefix
  "Returns the prefix of a string. [. $ &$ &.]"
  [nm]
  (let [[_ prefix] (re-matches #"(\.|\$|&\.|&\$).*" nm)]
    prefix))

(defn prefixed-keyword?
  "Returns true if the given keyword starts with one of [. $ &$ &.]"
  [kw]
  (and (keyword? kw)
    (prefixed-name? (name kw))))

(defn remove-prefix
  "Removes the prefix of a string."
  [nm]
  (subs nm (count (get-prefix nm))))

(defn remove-prefix-kw
  "Removes the prefix of a keyword."
  [kw]
  (keyword (remove-prefix (name kw))))

(defn get-includes
  "Returns the list of components from the include-children method of a component"
  [component]
  (if (CSS? component)
    #?(:clj  ((:include-children (meta component)) component)
       :cljs (include-children component))
    []))

(defn get-nested-includes
  "Recursively finds all includes starting at the given component."
  [component]
  (let [direct-children (get-includes component)]
    (if (empty? direct-children)
      []
      (concat direct-children (reduce #(concat %1 (get-nested-includes %2)) [] direct-children)))))

(defn localize-name
  [nm comp]
  (let [no-prefix (remove-prefix nm)
        prefix    (get-prefix nm)]
    (case prefix
      ("." "&.") (str prefix (local-class comp (keyword no-prefix)))
      "$" (str "." no-prefix)
      "&$" (str "&." no-prefix))))

(defn localize-kw
  [kw comp]
  (keyword (localize-name (name kw) comp)))

(defn kw->localized-classname
  "Gives the localized classname for the given keyword."
  [comp kw]
  (let [nm        (name kw)
        prefix    (get-prefix nm)
        no-prefix (subs nm (count prefix))]
    (case prefix
      ("$" "&$") no-prefix
      ("." "&.") (local-class comp no-prefix))))

(defn selector?
  [x]
  (try
    #?(:clj  (= garden.selectors.CSSSelector (type x))
       :cljs (= js/garden.selectors.CSSSelector (type x)))
    (catch #?(:cljs :default :clj Throwable) e
      false)))

(defn get-selector-keywords
  "Gets all the keywords that are present in a selector"
  [selector]
  (let [val        (:selector selector)
        classnames (filter #(re-matches #"[.$].*" %) (str/split val #" "))]
    (map keyword classnames)))

(defn get-class-keys
  "Gets all used classnames in from the given rules as keywords"
  [rules]
  (let [flattened-rules (flatten rules)
        selectors       (filter selector? flattened-rules)
        prefixed-kws    (filter prefixed-keyword? flattened-rules)]
    (distinct (concat (flatten (map get-selector-keywords selectors)) prefixed-kws))))

(defn get-classnames
  "Returns a map from user-given CSS rule names to localized names of the given component."
  [comp]
  (let [local-class-keys  (get-class-keys (get-local-rules comp))
        global-class-keys (map remove-prefix-kw (get-class-keys (get-global-rules comp)))
        local-classnames  (zipmap (map remove-prefix-kw local-class-keys) (map #(kw->localized-classname comp %) local-class-keys))
        global-classnames (zipmap global-class-keys (map name global-class-keys))]
    (merge local-classnames global-classnames)))
