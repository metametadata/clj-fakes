(ns clj-fakes.context
  "API for working in explicit context."
  (:require [clojure.string :as string]
    #?@(:clj  [
            [clj-fakes.reflection :as r]
            [clojure.pprint :as pprint]
            [cljs.analyzer :as a]
            [clj-fakes.macro :refer :all]
               ]
        :cljs [[cljs.pprint :as pprint]
               ]))
  #?(:cljs
     (:require-macros [clj-fakes.macro :refer [-cljs-env? P PP]])))

(defn context
  "Creates a new context atom.

  Do not alter the atom manually, context fields should be considered a private API.

  Also see: [[clj-fakes.core/with-fakes]]."
  []
  (atom {; Contains pairs: [key call]
         :calls           []

         ; Set of recorded fakes
         :recorded-fakes  #{}

         ; f -> position
         :positions       {}

         ; Vector of fakes
         :unused-fakes    []

         ; Vector of fakes
         :unchecked-fakes []

         ; f -> description; will be used for debugging
         :fake-descs      {}

         ; object -> id
         :object-ids      {}

         ; var -> original-var-val
         :original-vals   {}

         ; var -> f; f - function which should be called to restore original value
         :unpatches       {}}))

;;;;;;;;;;;;;;;;;;;;;;;; Args matching
(defprotocol ArgsMatcher
  "Protocol for multiple args matching."
  (args-match? [this args] "Should return true or false.")
  (args-matcher->str [this] "Should return a string for debug messages."))

(defprotocol ImplicitArgMatcher
  "Most likely you shouldn't use this protocol.
  Consider creating explicit custom matchers by implementing [[ArgMatcher]] protocol and using [[arg]] macro."
  (arg-matches-implicitly? [this arg] "Should return true or false.")
  (arg-matcher->str [this] "Should return a string for debug messages."))

(defprotocol ArgMatcher
  "Protocol for explicit arg matchers.

  Also see: [[arg]]."
  (arg-matches? [this arg] "Should return true or false."))

(def ^{:doc "Matcher which matches any value. Implements both [[ArgsMatcher]] and [[ImplicitArgMatcher]]."}
any?
  (reify
    ArgsMatcher
    (args-match? [_ _args_] true)

    ImplicitArgMatcher
    (arg-matches-implicitly? [_ _arg_] true)
    (arg-matcher->str [this] "<any?>")))

(defn ^:no-doc -arg-matches?
  [matcher arg]
  (if (satisfies? ImplicitArgMatcher matcher)
    (arg-matches-implicitly? matcher arg)
    (= arg matcher)))

(extend-type #?(:clj  clojure.lang.PersistentVector
                :cljs cljs.core.PersistentVector)
  ArgsMatcher
  (args-match?
    [this args]
    (or (and (empty? this) (empty? args))
        (and (= (count this) (count args))
             (every? true? (map -arg-matches? this args)))))

  (args-matcher->str
    [this]
    (str "["
         (clojure.string/join " " (map #(if (satisfies? ImplicitArgMatcher %)
                                         (arg-matcher->str %)
                                         (str %))
                                       this))
         "]")))

(defn arg*
  "The same as [[arg]] macro, but string for printing the matcher must be specified explicitly."
  [matcher matcher-str]
  {:pre [(satisfies? ArgMatcher matcher) (string? matcher-str)]}
  (reify ImplicitArgMatcher
    (arg-matches-implicitly? [_ arg] (arg-matches? matcher arg))
    (arg-matcher->str [_] matcher-str)))

#?(:clj
   (defmacro arg
     "Creates an [[ImplicitArgMatcher]] from the specified [[ArgMatcher]] to be used in vector args matcher."
     [matcher]
     `(arg* ~matcher ~(str "<" matcher ">"))))

; functional arg matcher
(extend-type #?(:clj  clojure.lang.Fn
                :cljs function)
  ArgMatcher
  (arg-matches? [this arg] (this arg)))

; regex arg matcher
(extend-type #?(:clj  java.util.regex.Pattern
                :cljs js/RegExp)
  ArgMatcher
  (arg-matches? [this arg] (not (nil? (re-find this arg)))))

(defn ^:no-doc -with-any-first-arg
  "Args matcher decorator which allows any first arg. The rest of the args will be checked by specified matcher.
  Returns a new matcher."
  [rest-args-matcher]
  {:pre [(satisfies? ArgsMatcher rest-args-matcher)]}
  (reify ArgsMatcher
    (args-match?
      [_ args]
      (args-match? rest-args-matcher (rest args)))

    (args-matcher->str
      [_]
      (str "first: " (arg-matcher->str any?)
           ", rest: " (args-matcher->str rest-args-matcher)))))

;;;;;;;;;;;;;;;;;;;;;;;; Utils
(defn ^:no-doc -find-first
  "Returns nil if element wasn't found."
  [pred coll]
  (first (filter pred coll)))

(defn ^:no-doc -find-and-take-after
  "Returns the first found element and the seq of elements after it.
  Returns [nil _] if element was not found."
  [pred coll]
  (let [s (drop-while (complement pred) coll)]
    [(first s) (rest s)]))

(defn ^:no-doc -take-nth-from-group
  "Partitions collection by group-len and returns a lazy seq of every nth element from each partition."
  [n group-len coll]
  (take-nth group-len (drop (dec n) coll)))

;;;;;;;;;;;;;;;;;;;;;;;; Fakes - core
(defn ^:no-doc -config->f
  "Constructs a function from a config vector:
  [args-matcher1 fn-or-value1
   args-matcher2 fn-or-value2 ...]"
  [config]
  {:pre  [(vector? config)
          (seq config)
          (even? (count config))
          (->> (apply hash-map config) (keys) (every? #(satisfies? ArgsMatcher %)))]
   :post [(fn? %)]}
  (fn [& args]
    (let [config-pairs (partition 2 config)
          matched-rule (-find-first #(args-match? (first %1) args)
                                    config-pairs)
          return-value (second matched-rule)]
      (if (nil? matched-rule)
        (throw (ex-info (str "Unexpected args are passed into fake: " args
                             ".\nSupported args matchers:\n"
                             (clojure.string/join "\n" (map args-matcher->str (-take-nth-from-group 1 2 config))))
                        {}))
        (if (fn? return-value)
          (apply return-value args)
          return-value)))))

(defn ^:no-doc -optional-fake
  "Extracted for code readability."
  [ctx f]
  {:pre [ctx (fn? f)]}
  f)

(defn ^:no-doc -mark-used
  "Self-test will not warn about specified fake after using this function."
  [ctx f]
  {:pre [ctx (fn? f)]}
  (swap! ctx update-in [:unused-fakes] #(remove #{f} %)))

(defn ^:no-doc -required
  [ctx position f]
  {:pre [ctx position (fn? f)]}
  (letfn [(wrapper [& args]
                   (-mark-used ctx wrapper)
                   (apply f args))]
    (swap! ctx assoc-in [:positions wrapper] position)
    (swap! ctx update-in [:unused-fakes] conj wrapper)
    wrapper))

(defn ^:no-doc -fake
  [ctx position f]
  (-required ctx position (-optional-fake ctx f)))

(defn ^:no-doc -record-call
  [ctx k call]
  (swap! ctx update-in [:calls] conj [k call]))

(defn ^:no-doc -recorded-as
  "Decorates the specified function in order to record its calls.
  Calls will be recorded by the specified key k.
  If k is nil - calls will be recorded by the decorated function (in this case use -recorded).
  Returns decorated function."
  [ctx position k f]
  {:pre  [ctx position (fn? f)]
   :post [(fn? %)]}
  (letfn [(wrapper [& args]
                   (let [return-value (apply f args)]
                     (-record-call ctx (or k wrapper) {:args         args
                                                       :return-value return-value})
                     return-value))]
    (swap! ctx update-in [:recorded-fakes] conj (or k wrapper))
    (swap! ctx assoc-in [:positions (or k wrapper)] position)
    (swap! ctx update-in [:unchecked-fakes] conj (or k wrapper))
    wrapper))

(defn ^:no-doc -recorded
  "Decorates the specified function in order to record its calls.
  Calls will be recorded by the returned decorated function."
  [ctx position f]
  (-recorded-as ctx position nil f))

(defn ^:no-doc -recorded?
  [ctx k]
  (contains? (:recorded-fakes @ctx) k))

(defn mark-checked
  "After using this function [[self-test-unchecked-fakes]] will not warn about the specified recorded fake."
  [ctx k]
  {:pre [ctx (-recorded? ctx k)]}
  (swap! ctx update-in [:unchecked-fakes] #(remove #{k} %)))

(defn ^:no-doc -set-desc
  "Saves a description for the specified fake. It can be later used for debugging."
  [ctx k desc]
  {:pre [ctx k]}
  (swap! ctx assoc-in [:fake-descs k] desc))

; Type with meaningful name for creating unique return values
(deftype FakeReturnValue []
  Object
  ; make debug output less noisy
  (toString [_] ""))

(alter-meta! #'->FakeReturnValue assoc :no-doc true)

;;;;;;;;;;;;;;;;;;;;;;;; Fakes - API
(defn ^:no-doc -position
  "Returns a position of the specified fake. Does not yet work for optional fakes."
  [ctx f]
  {:pre [ctx f]}
  (get (:positions @ctx) f))

(def default-fake-config
  "With this config fake will return a new `FakeReturnValue` type instance for any combination of args."
  [any? (fn [& _] (FakeReturnValue.))])

(defn optional-fake
  "Creates an optional fake function which will not be checked by [[self-test-unused-fakes]],
  i.e. created fake is allowed to be never called.

  Config is a vector of pairs: `[args-matcher1 fn-or-value1 args-matcher2 fn-or-value2 ...]`

  If `config` is not specified then fake will be created with [[default-fake-config]]."
  ([ctx] (optional-fake ctx default-fake-config))
  ([ctx config] {:pre [ctx]} (-optional-fake ctx (-config->f config))))

#?(:clj
   (defn ^:no-doc -emit-fake-fn-call-with-position
     "Emits code with call to specified fake-fn with correct position arg.
     Line and column are retrieved from macro's &form var which must be passed explicitly.
     Filepath is retrieved from a dummy var metadata.
     Works in Clojure and ClojureScript, but can produce slightly different filepaths."
     [fake-fn ctx form & args]
     (assert (meta form)
             (str "Meta must be defined in order to detect fake position. "
                  "Passed form: " form
                  "\nHINT: Meta can be undefined when fake macro is invoked from another macro, "
                  "in such case use a variation of invoked macro with a 'form' param. "
                  "E.g. call (fake* &form...) instead of (fake ...)."))
     (let [{:keys [line column]} (meta form)
           filepath-catcher-sym (gensym "position-catcher")]
       `(do
          (def ~filepath-catcher-sym nil)
          (let [position# {:file   (:file (meta #'~filepath-catcher-sym))
                           :line   ~line
                           :column ~column}]
            (~fake-fn ~ctx position# ~@args))))))

(defn ^:no-doc -fake**
  "This function is the same as fake macro, but position must be passed explicitly."
  [ctx position config]
  (-fake ctx position (-config->f config)))

#?(:clj
   (defmacro fake*
     "The same as [[fake]] macro but for reuse in other macros.

     `form` must be passed from the parent macro in order to correctly detect position."
     [ctx form config]
     (-emit-fake-fn-call-with-position `-fake** ctx form config)))

#?(:clj
   (defmacro fake
     "Creates a fake function. The created function must be called; otherwise, [[self-test-unused-fakes]] will fail.

     Config is a vector of pairs: `[args-matcher1 fn-or-value1 args-matcher2 fn-or-value2 ...]`

     Use [[fake*]] in case this macro is called from another macro."
     [ctx config]
     `(fake* ~ctx ~&form ~config)))

(defn ^:no-doc -recorded-fake**
  "This function is the same as recorded-fake macro, but position must be passed explicitly."
  ([ctx position] (-recorded-fake** ctx position default-fake-config))
  ([ctx position config] (-recorded ctx position (-config->f config))))

#?(:clj
   (defmacro recorded-fake*
     "This is the same as [[recorded-fake]] macro but for reuse in other macros.

     `form` must be passed from the parent macro in order to correctly detect position."
     ([ctx form] (-emit-fake-fn-call-with-position `-recorded-fake** ctx form))
     ([ctx form config] (-emit-fake-fn-call-with-position `-recorded-fake** ctx form config))))

#?(:clj
   (defmacro recorded-fake
     "Creates a fake function whose calls will be recorded.

     Config is a vector of pairs: `[args-matcher1 fn-or-value1 args-matcher2 fn-or-value2 ...]`

     Ultimatelly, the created function must be marked checked (see [[mark-checked]]); otherwise, [[self-test-unchecked-fakes]] will fail.

     Use [[recorded-fake*]] in case this macro is called from another macro."
     ([ctx] `(recorded-fake* ~ctx ~&form))
     ([ctx config] `(recorded-fake* ~ctx ~&form ~config))))

(defn ^:no-doc -recorded-fake-as*
  [ctx position k config]
  {:pre [ctx]}
  (-recorded-as ctx position k (-config->f config)))

#?(:clj
   (defmacro ^:no-doc -recorded-fake-as
     "The same as [[recorded-fake]], but fake will be stored into context by specified key instead of its value.

     E.g. it is used in [[reify-fake]] in order to emulate recording calls on the protocol method.
     `form` must be passed if this macro is called from another macro in order to correctly determine position."
     ([ctx form k] (-emit-fake-fn-call-with-position `-recorded-fake-as* ctx form k `default-fake-config))
     ([ctx form k config] (-emit-fake-fn-call-with-position `-recorded-fake-as* ctx form k config))))

(defn calls
  "For the specified recorded fake returns a vector of its calls.
  Returned vector is ordered by call time: the earliest call will be at the head.

  A call is a map with keys: `:args`, `:return-value`.

  If fake is not specified then it will return all the calls recorded in the context:

  ```clj
  [[recorded-fake1 call1]
   [recorded-fake2 call2]
   ...]
  ```"
  ([ctx] (:calls @ctx))
  ([ctx k]
   {:pre [ctx (-recorded? ctx k)]}
   (->> (:calls @ctx)
        (filter #(= k (first %)))
        (mapv #(second %)))))

;;;;;;;;;;;;;;;;;;;;;;;; Protocol fakes
(defn ^:no-doc -register-obj-id
  [ctx obj id]
  {:pre [ctx obj id]}
  (swap! ctx assoc-in [:object-ids obj] id))

(defn ^:no-doc -obj->id
  "Finds id for reified protocol instance. Returns nil if not found."
  [ctx obj]
  {:pre [ctx obj]}
  (get (:object-ids @ctx) obj))

(defn ^:no-doc -method-hash
  "Generates a unique hash from specified protocol instance id and function."
  [id f]
  {:pre [id f]}
  [id f])

(defn method
  "Returns a hash-key by which calls are recorded for the specified fake protocol instance method.
  Can be used in functions which take recorded fake as an input."
  [ctx obj f]
  {:pre [ctx obj (or (fn? f) (string? f))]}
  (-method-hash (-obj->id ctx obj) f))

(defn ^:no-doc -with-any-first-arg-config
  "Applies -with-any-first-arg to all args-matchers in config. Returns a new config."
  [config]
  {:pre  [(vector? config)]
   :post [#(vector? %)]}
  (into []
        (map-indexed (fn [i x]
                       (if (even? i)
                         (-with-any-first-arg x)
                         x))
                     config)))

#?(:clj
   (defn ^:no-doc -add-any-first-arg-into-matchers
     "In config map kinda adds an additional first matcher to match any 'this' param.
     Before:
     [matcher1 fn1
      matcher2 fn1]

     After:
     [(fn [args] (apply matcher1 (rest args))) fn1
      ...]

     Nil config is bypassed."
     [config]
     (when-not (nil? config)
       `(-with-any-first-arg-config ~config))))

#?(:clj
   (defn ^:no-doc -looks-like-method-spec?
     [x]
     (list? x)))

#?(:clj
   (defn ^:no-doc -parse-specs
     "Returns a map: protocol -> method-specs"
     [specs]
     (loop [ret {}
            specs specs]
       (if (seq specs)
         (recur (assoc ret (first specs) (take-while -looks-like-method-spec? (next specs)))
                (drop-while -looks-like-method-spec? (next specs)))
         ret))))

#?(:clj
   (defn ^:no-doc -emit-method-full-sym
     "Infers fully qualified method symbol."
     [env protocol-sym method-sym]
     (if (-cljs-env? env)
       ; ClojureScript
       (let [protocol-var (a/resolve-var env protocol-sym)]
         (symbol (namespace (:name protocol-var)) (name method-sym)))

       ; Clojure
       (let [protocol-var (resolve protocol-sym)
             ns (if (var? protocol-var)
                  (str (:ns (meta protocol-var))))]
         (symbol ns (name method-sym))))))

#?(:clj
   (defn ^:no-doc -emit-imp-value
     [form env ctx obj-id-sym protocol-sym
      [method-sym fake-type config :as _method-spec_]]
     (let [method-full-sym (-emit-method-full-sym env protocol-sym method-sym)
           method-hash `(-method-hash
                          ~obj-id-sym
                          ; if compiler cannot resolve a symbol then hash by its string repr instead of the value
                          ; if resolved symbol is really not a protocol/class it will fail later on reify macro expansion
                          ~(if (r/-resolves? env method-full-sym)
                             method-full-sym
                             (str method-sym)))
           config (-add-any-first-arg-into-matchers config)]
       (condp = fake-type
         :optional-fake
         (if config
           `(optional-fake ~ctx ~config)
           `(optional-fake ~ctx))

         :fake
         `(let [fake# (fake* ~ctx ~form ~config)]
            (-set-desc ~ctx fake# ~(str protocol-sym ", " method-sym))
            fake#)

         :recorded-fake
         `(do
            (-set-desc ~ctx ~method-hash ~(str protocol-sym ", " method-sym))
            ~(if config
               `(-recorded-fake-as ~ctx ~form ~method-hash ~config)
               `(-recorded-fake-as ~ctx ~form ~method-hash)))

         :-nice-fake
         `(optional-fake ~ctx default-fake-config)

         (assert nil (str "Unknown fake type specified: " fake-type))))))

#?(:clj
   (defn ^:no-doc -remove-meta
     [x]
     (with-meta x nil)))

#?(:clj
   (defn ^:no-doc -with-hint
     [hint sym]
     (with-meta sym {:tag hint})))

#?(:clj
   (defn ^:no-doc -arg
     "Constructs arg symbol from the specified name."
     [name]
     (-> name
         ; sanitize in case Java name is passed in (e.g. java.lang.CharSequence)
         (string/replace #"\." "-")
         (gensym))))

#?(:clj
   (defn ^:no-doc -fix-arglist
     "Passed arglist can have duplicated args and not allowed symbols.
     This function generates a correct arglist for code emitting.
     No type hints will be specified."
     [arglist]
     (mapv -arg arglist)))

#?(:clj
   (defn ^:no-doc -fix-arglist-with-hints
     "The same as -fix-arglist and also adds type hints:
       for the first arg: protocol-sym will be used as a hint;
       for the rest: arg value itself will be used as a hint."
     [protocol-sym arglist]
     (into [(-with-hint protocol-sym (-arg (first arglist)))]
           (map #(-with-hint % (-arg %)) (rest arglist)))))

#?(:clj
   (defn ^:no-doc -method-arglists
     "Given protocol and method returns all allowed method arglists:
     [[...] ...]

     If protocol is a Java Object or interface then args will be hinted which is needed
     to support overloaded methods."
     [env protocol-sym method-sym]
     (if-let [protocol (r/-resolve-protocol-with-specs env protocol-sym)]
       ; protocol
       (let [methods (r/-protocol-methods env protocol)
             method (-find-first #(= method-sym (r/-protocol-method-name env %)) methods)]
         (map -fix-arglist (r/-protocol-method-argslist env method)))

       ; not a protocol
       (if (-cljs-env? env)
         ; ClojureScript - error (note that Object is not supported yet)
         (assert nil (str "Unknown protocol: " protocol-sym))

         ; Clojure - Object, Java interface or error
         (let [interface (r/-reflect-interface-or-object env protocol-sym)
               members (filter #(and (= method-sym (:name %))
                                     (not (contains? (:flags %) :varargs)))
                               (:members interface))]
           (mapv #(-fix-arglist-with-hints protocol-sym
                                           (into ['this] (:parameter-types %)))
                 members))))))

#?(:clj
   (defn ^:no-doc -hinted-method-sym
     [env protocol-sym method-sym]
     ; no hint is needed in ClojureScript or for Clojure protocol methods
     (if (or (-cljs-env? env)
             (r/-resolve-protocol-with-specs env protocol-sym))
       method-sym

       ; Object, Java interface or error
       (let [interface (r/-reflect-interface-or-object env protocol-sym)
             members (:members interface)
             member (-find-first #(= method-sym (:name %)) members)]
         (-with-hint (:return-type member) method-sym)))))

#?(:clj
   (defn ^:no-doc -method-imp
     "Generates an implementation for the specified method.
     {:imp-sym ...
      :imp-value ...
      :method-sym ...
      :arglists ...}

      If protocol is a Java interface then method will be have type hints which is needed
      to support overloaded methods."
     [form env ctx obj-id-sym protocol-sym
      [method-sym _fake-type_ _config_ :as method-spec]]
     (let [imp-sym (gensym "imp")
           imp-value (-emit-imp-value
                       form env ctx
                       obj-id-sym
                       protocol-sym
                       method-spec)
           arglists (-method-arglists env protocol-sym method-sym)]
       (assert (seq arglists) (str "Unknown method: " protocol-sym "/" method-sym))

       {:imp-sym    imp-sym
        :imp-value  imp-value
        :method-sym (-hinted-method-sym env protocol-sym method-sym)
        :arglists   arglists})))

#?(:clj
   (defn ^:no-doc -method-imps-for-protocol
     "Returns a list of maps:
      ({:imp-sym ...
       :imp-value ...
       :method-sym ...
       :arglists ...} ...)"
     [form env ctx obj-id-sym protocol-sym method-specs]
     (map (partial -method-imp form env ctx obj-id-sym protocol-sym) method-specs)))

#?(:clj
   (defn ^:no-doc -generate-protocol-method-imps
     "Produces a helper map:
     {protocol-sym -> [{:imp-sym ...
                        :imp-value ...
                        :method-sym ...
                        :arglists [[...] ...]}
                       ...]
      ...}"
     [form env ctx obj-id-sym protocol-method-specs]
     (into {}
           (map (fn [[p s]]
                  (vector p (-method-imps-for-protocol form env ctx obj-id-sym p s)))
                protocol-method-specs))))

#?(:clj
   (defn ^:no-doc -emit-imp-bindings
     "Emits a list of let bindings with implementations for reified methods:
     [imp-sym1 imp-value1
     imp-sym2 imp-value2 ...]"
     [protocol-method-imps]
     (mapcat #(vector (:imp-sym %) (:imp-value %))
             (flatten (vals protocol-method-imps)))))

#?(:clj
   (defn ^:no-doc -emit-method-specs
     [{:keys [method-sym arglists imp-sym] :as _method-imp_}]
     (map (fn [arglist]
            `(~method-sym ~arglist
               (~imp-sym ~@(map -remove-meta arglist))))
          arglists)))

#?(:clj
   (defn ^:no-doc -emit-protocol-specs
     [[protocol-sym method-imps]]
     (into [protocol-sym]
           (mapcat -emit-method-specs method-imps))))

#?(:clj
   (defn ^:no-doc -emit-specs
     "Emits final specs for reify macro."
     [protocol-method-imps]
     (mapcat -emit-protocol-specs protocol-method-imps)))

#?(:clj
   (defn ^:no-doc -nice-specs
     "Generates nice specs for all methods from the specified protocol.
     Does nothing for Java interfaces and unresolved symbols."
     [env protocol-sym]
     (when-let [protocol (r/-resolve-protocol-with-specs env protocol-sym)]
       (let [methods (r/-protocol-methods env protocol)
             nice-specs (map #(vector (r/-protocol-method-name env %) :-nice-fake) methods)]
         nice-specs))))

#?(:clj
   (defn ^:no-doc -not-yet-in-specs [specs [nice-method-sym :as _spec_]]
     (nil?
       (-find-first (fn [[method-sym :as _spec_]]
                      (= method-sym nice-method-sym))
                    specs))))

#?(:clj
   (defn ^:no-doc -add-nice-specs-for-protocol [env [protocol-sym parsed-specs :as _parsed_spec_]]
     (let [nice-specs (-nice-specs env protocol-sym)
           auto-specs (filter (partial -not-yet-in-specs parsed-specs) nice-specs)
           new-specs (concat parsed-specs auto-specs)]
       [protocol-sym new-specs])))

#?(:clj
   (defn ^:no-doc -add-nice-specs
     "For all methods which are not explicitly defined adds a 'nice' default spec."
     [env parsed-specs]
     (map (partial -add-nice-specs-for-protocol env) parsed-specs)))

#?(:clj
   (defmacro ^:no-doc -reify-fake*
     [ctx form env nice? & specs]
     (let [obj-id-sym (gensym "obj-id")
           obj-sym (gensym "obj")
           parsed-specs (-parse-specs specs)
           protocol-method-specs (if nice?
                                   (-add-nice-specs env parsed-specs)
                                   parsed-specs)
           protocol-method-imps (-generate-protocol-method-imps form env ctx obj-id-sym protocol-method-specs)
           r `(let [~obj-id-sym (gensym "obj-id")
                    ~@(-emit-imp-bindings protocol-method-imps)
                    ~obj-sym (reify ~@(-emit-specs protocol-method-imps))]
                (-register-obj-id ~ctx ~obj-sym ~obj-id-sym)
                ~obj-sym)]
       ;(set! *print-meta* true)
       ;(PP r) (PP "--")
       r)))

#?(:clj
   (defmacro reify-fake*
     "The same as [[reify-fake]] but to be used inside macros.

     `form` is needed to correctly determine fake positions, `env` is needed to determine target language."
     [ctx form env & specs]
     `(-reify-fake* ~ctx ~form ~env false ~@specs)))

#?(:clj
   (defmacro reify-nice-fake*
     "The same as [[reify-nice-fake]] but to be used inside macros.

     `form` is needed to correctly determine fake positions, `env` is needed to determine target language."
     [ctx form env & specs]
     `(-reify-fake* ~ctx ~form ~env true ~@specs)))

#?(:clj
   (defmacro reify-fake
     "Works similarly to `reify` macro, but implements methods in terms of fake functions.
     Created instance will raise an exception on calling protocol method which is not defined.

     Supported fake types: `:optional-fake`, `:fake`, `:recorded-fake`.

     Syntax example:

     ```clj
     (reify-fake my-context
       protocol-or-interface-or-Object
       [method1 :optional-fake [any? 123]]
       protocol-or-interface-or-Object
       [method2 :recorded-fake])
     ```"
     [ctx & specs]
     `(reify-fake* ~ctx ~&form ~&env ~@specs)))

#?(:clj
   (defmacro reify-nice-fake
     "Works similarly to [[reify-fake]], but automatically generates `:optional-fake`
     implementations for methods which are not explicitly defined by user.

     It cannot yet automatically fake Java interface and `Object` methods."
     [ctx & specs]
     `(reify-nice-fake* ~ctx ~&form ~&env ~@specs)))

;;;;;;;;;;;;;;;;;;;;;;;; Self-tests
(defn ^:no-doc -fake->str
  [ctx fake-type f]
  (let [p (-position ctx f)
        base-str (str fake-type " from " (:file p) ", " (:line p) ":" (:column p))
        desc (get (:fake-descs @ctx) f)]
    (if desc
      (str base-str " (" desc ")")
      base-str)))

(defn self-test-unused-fakes
  "Raises an exception when some fake was never called after its creation."
  [ctx]
  {:pre [ctx]}
  (if-let [descriptions (not-empty (map (partial -fake->str ctx "non-optional fake")
                                        (:unused-fakes @ctx)))]
    (throw (ex-info (str "Self-test: no call detected for:\n"
                         (string/join "\n" descriptions))
                    {}))))

(defn self-test-unchecked-fakes
  "Raises an exception if some recorded fake was never marked checked, i.e. you forgot to assert its calls."
  [ctx]
  {:pre [ctx]}
  (if-let [descriptions (not-empty (map (partial -fake->str ctx "recorded fake")
                                        (:unchecked-fakes @ctx)))]
    (throw (ex-info (str "Self-test: no check performed on:\n"
                         (string/join "\n" descriptions))
                    {}))))

;;;;;;;;;;;;;;;;;;;;;;;; Assertions
(defn ^:no-doc -was-called-times
  "Checks that function:
   1) was called the specified number of times and
   2) at least once with the specified args."
  [ctx total-calls-count-pred total-calls-count-str k args-matcher]
  (mark-checked ctx k)
  (let [k-calls (calls ctx k)
        total-calls-count (count k-calls)
        matched-call (-find-first #(args-match? args-matcher (:args %))
                                  k-calls)]
    (cond
      (not (total-calls-count-pred total-calls-count))
      (throw (ex-info (str "Function was not called the expected number of times. "
                           "Expected: " total-calls-count-str ". "
                           "Actual: " total-calls-count ".")
                      {}))

      (nil? matched-call)
      (throw (ex-info (str "Function was never called with the expected args.\n"
                           "Args matcher: " (args-matcher->str args-matcher)
                           ".\nActual calls:\n" (with-out-str (pprint/pprint k-calls)))
                      {}))

      :else true)))

(defn was-called-once
  "Checks that recorded fake was called strictly once and that the call was with the specified args."
  [ctx k args-matcher]
  {:pre [ctx (-recorded? ctx k) (satisfies? ArgsMatcher args-matcher)]}
  (-was-called-times ctx #(= % 1) "1" k args-matcher))

(defn was-called
  "Checks that recorded fake was called at least once with the specified args."
  [ctx k args-matcher]
  {:pre [ctx (-recorded? ctx k) (satisfies? ArgsMatcher args-matcher)]}
  (-was-called-times ctx #(> % 0) "> 0" k args-matcher))

(defn was-not-called
  "Checks that recorded fake was never called."
  [ctx k]
  {:pre [ctx (-recorded? ctx k)]}
  (mark-checked ctx k)
  (let [k-calls (calls ctx k)]
    (or (empty? k-calls)
        (throw (ex-info (str "Function is expected to be never called. Actual calls:\n" (pr-str k-calls) ".") {})))))

(defn ^:no-doc -call-matches?
  [k args-matcher [f {:keys [args]} :as _call_]]
  (and (= k f)
       (args-match? args-matcher args)))

(defn were-called-in-order
  "Checks that recorded fakes were called in specified order with the specified args.
  It does not check that there were not other calls.

  Syntax:
  ```
  (were-called-in-order ctx
    f1 args-matcher1
    f2 args-matcher2
    ...)
   ```"
  [ctx & fns-and-matchers]
  {:pre [ctx
         (pos? (count fns-and-matchers))
         (even? (count fns-and-matchers))
         (every? (partial -recorded? ctx) (-take-nth-from-group 1 2 fns-and-matchers))
         (every? #(satisfies? ArgsMatcher %) (-take-nth-from-group 2 2 fns-and-matchers))]}
  ; loop over provided pairs
  (loop [fn-matcher-pairs (partition 2 fns-and-matchers)
         unchecked-calls (calls ctx)
         step 1]
    (when-let [[k args-matcher] (first fn-matcher-pairs)]
      (mark-checked ctx k)

      ; find matched call
      (let [[matched-call rest-unchecked-calls] (-find-and-take-after (partial -call-matches? k args-matcher)
                                                                      unchecked-calls)]
        ; not found error
        (when (nil? matched-call)
          (throw (ex-info (str "Could not find a call satisfying step #" step
                               ":\n" (-fake->str ctx "recorded fake" k)
                               "\nargs matcher: " (args-matcher->str args-matcher)) {})))

        ; otherwise, check next pair
        (recur (rest fn-matcher-pairs)
               rest-unchecked-calls
               (inc step)))))
  true)

;;;;;;;;;;;;;;;;;;;;;;;; Assertions for protocol methods
(defn method-was-called-once
  "[[was-called-once]] for protocol method fakes."
  [ctx f obj args-matcher]
  {:pre [ctx f obj (satisfies? ArgsMatcher args-matcher)]}
  (was-called-once ctx (method ctx obj f) (-with-any-first-arg args-matcher)))

(defn method-was-called
  "[[was-called]] for protocol method fakes."
  [ctx f obj args-matcher]
  {:pre [ctx f obj (satisfies? ArgsMatcher args-matcher)]}
  (was-called ctx (method ctx obj f) (-with-any-first-arg args-matcher)))

(defn method-was-not-called
  "[[was-not-called]] for protocol method fakes."
  [ctx f obj]
  {:pre [ctx f obj]}
  (was-not-called ctx (method ctx obj f)))

(defn methods-were-called-in-order
  "[[were-called-in-order]] for protocol method fakes.

  Syntax:
  ```
  (methods-were-called-in-order ctx
    f1 obj1 args-matcher1
    f2 obj2 args-matcher2
    ...)
  ```"
  [ctx & fns-objs-and-matchers]
  {:pre [ctx
         (pos? (count fns-objs-and-matchers))
         (zero? (rem (count fns-objs-and-matchers) 3))
         (every? #(not (nil? %)) (-take-nth-from-group 1 3 fns-objs-and-matchers))
         (every? #(not (nil? %)) (-take-nth-from-group 2 3 fns-objs-and-matchers))
         (every? #(satisfies? ArgsMatcher %) (-take-nth-from-group 3 3 fns-objs-and-matchers))
         ]}
  (let [fns-and-matchers (mapcat (fn [[f obj args-matcher]]
                                   [(method ctx obj f) (-with-any-first-arg args-matcher)])
                                 (partition 3 fns-objs-and-matchers))]
    (apply were-called-in-order ctx fns-and-matchers)))

;;;;;;;;;;;;;;;;;;;;;;;; Monkey patching
(defn ^:no-doc -save-original-val!
  [ctx a-var]
  {:pre [ctx a-var]}
  (when-not (contains? (:original-vals @ctx) a-var)
    (swap! ctx assoc-in [:original-vals a-var] @a-var)))

(defn ^:no-doc -save-unpatch!
  [ctx a-var unpatch-fn]
  {:pre [ctx a-var (fn? unpatch-fn)]}
  (swap! ctx assoc-in [:unpatches a-var] unpatch-fn))

(defn original-val
  "Given a patched variable returns its original value."
  [ctx a-var]
  {:pre [ctx a-var]}
  (assert (contains? (:original-vals @ctx) a-var) "Specified var is not patched")
  (get (:original-vals @ctx) a-var))

#?(:clj
   (defmacro ^:no-doc -set-var!
     "Portable var set. ClojureScript version expects passed variable to be a pair, e.g. `(var foo)`."
     [a-var val]
     (if (-cljs-env? &env)
       ; ClojureScript
       (let [[_ var-symbol] a-var]
         `(set! ~var-symbol ~val))

       ; Clojure
       `(alter-var-root ~a-var (constantly ~val)))))

#?(:clj
   (defmacro patch!
     "Changes variable value in all threads."
     [ctx var-expr val]
     `(do
        (-save-original-val! ~ctx ~var-expr)
        (-save-unpatch! ~ctx
                        ~var-expr
                        #(-set-var! ~var-expr (original-val ~ctx ~var-expr)))
        (-set-var! ~var-expr ~val))))

(defn unpatch!
  "Restores original variable value."
  [ctx a-var]
  {:pre [ctx a-var]}
  (assert (contains? (:unpatches @ctx) a-var) "Specified var is not patched")
  ((get (:unpatches @ctx) a-var)))

(defn unpatch-all!
  "Restores original values for all the variables patched in the specified context."
  [ctx]
  (doseq [[_var_ unpatch-fn] (:unpatches @ctx)]
    (unpatch-fn)))

;;;;;;;;;;;;;;;;;;;;;;;; Utils
(defn cyclically
  "Returns a function that:

  1. takes any number of arguments;
  2. on each call returns the next value from `coll`, cyclically."
  [coll]
  (let [vals (atom (cycle coll))]
    (fn cyclical-fn [& _args_]
      (let [result (first @vals)]
        (swap! vals next)
        result))))