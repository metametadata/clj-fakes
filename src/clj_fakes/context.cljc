(ns clj-fakes.context
  (:require [clojure.string :as string]
    #?@(:clj  [
            [clojure.pprint :as pprint]
            [cljs.analyzer :as a]
            [clojure.reflect :as reflect]
            [clj-fakes.macro :refer :all]
               ]
        :cljs [[cljs.pprint :as pprint]
               ]))
  #?(:cljs
     (:require-macros [clj-fakes.macro :refer [-cljs-env? P PP]])))

(defn context
  "Create a new context. Context fields should be considered a private API."
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
  (args-match? [this args] "Should return true or false."))

(extend-type #?(:clj  clojure.lang.Fn
                :cljs function)
  ArgsMatcher
  (args-match? [this args]
    (this args)))

(defn -arg-matches?
  [matcher arg]
  (if (fn? matcher)
    (matcher arg)
    (= arg matcher)))

(extend-type #?(:clj  clojure.lang.PersistentVector
                :cljs cljs.core.PersistentVector)
  ArgsMatcher
  (args-match? [this args]
    (or (and (empty? this) (empty? args))
        (and (= (count this) (count args))
             (every? true? (map -arg-matches? this args))))))

(defn any?
  "Matcher which matches any value."
  [_]
  true)

(defn -with-any-first-arg
  "Args matcher decorator which allows any first arg. The rest of the args will be checked by specified matcher.
  Returns a new matcher."
  [rest-args-matcher]
  {:pre [(satisfies? ArgsMatcher rest-args-matcher)]}
  (fn wrapper
    [args]
    (args-match? rest-args-matcher (rest args))))

;;;;;;;;;;;;;;;;;;;;;;;; Utils
(defn -find-first
  [pred coll]
  (first (filter pred coll)))

;;;;;;;;;;;;;;;;;;;;;;;; Fakes - core
(defn -config->f
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
        (throw (ex-info (str "Unexpected args are passed into fake: " args) {}))
        (if (fn? return-value)
          (apply return-value args)
          return-value)))))

(defn -optional-fake
  "Extracted for code readability."
  [ctx f]
  {:pre [ctx (fn? f)]}
  f)

(defn -mark-used
  "Self-test will not warn about specified fake after using this function."
  [ctx f]
  {:pre [ctx (fn? f)]}
  (swap! ctx update-in [:unused-fakes] #(remove #{f} %)))

(defn -required
  [ctx position f]
  {:pre [ctx position (fn? f)]}
  (letfn [(wrapper [& args]
                   (-mark-used ctx wrapper)
                   (apply f args))]
    (swap! ctx assoc-in [:positions wrapper] position)
    (swap! ctx update-in [:unused-fakes] conj wrapper)
    wrapper))

(defn -fake
  [ctx position f]
  (-required ctx position (-optional-fake ctx f)))

(defn -record-call
  [ctx k call]
  (swap! ctx update-in [:calls] conj [k call]))

(defn -recorded-as
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

(defn -recorded
  "Decorates the specified function in order to record its calls.
  Calls will be recorded by the returned decorated function."
  [ctx position f]
  (-recorded-as ctx position nil f))

(defn -recorded?
  [ctx k]
  (contains? (:recorded-fakes @ctx) k))

(defn mark-checked
  "Self-test will not warn about specified recorded fake after using this function."
  [ctx k]
  {:pre [ctx (-recorded? ctx k)]}
  (swap! ctx update-in [:unchecked-fakes] #(remove #{k} %)))

; Protocol with meaningful name is used for more readable stacktraces
(defprotocol FakeReturnValue)

(defn -fake-return-value
  []
  (reify FakeReturnValue))

(defn -set-desc
  "Saves a description for the specified fake. It can be later used for debugging."
  [ctx k desc]
  {:pre [ctx k]}
  (swap! ctx assoc-in [:fake-descs k] desc))

;;;;;;;;;;;;;;;;;;;;;;;; Fakes - API
(defn -position
  "Returns a position of the specified fake. Does not yet work for optional fakes."
  [ctx f]
  {:pre [ctx f]}
  (get (:positions @ctx) f))

(def default-fake-config
  "With this config fake will return a unique value for any combination of args."
  [any? (fn [& _] (-fake-return-value))])

(defn optional-fake
  ([ctx] (optional-fake ctx default-fake-config))
  ([ctx config] {:pre [ctx]} (-optional-fake ctx (-config->f config))))

#?(:clj
   (defn -emit-fake-fn-call-with-position
     "Emits code with call to specified fake-fn with correct position arg.
     Line and column are retrieved from macro's &form var which must be passed explicitly.
     Filepath is retrieved from a dummy var metadata.
     Works in Clojure and ClojureScript but can produce slightly different filepaths."
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

(defn -fake**
  "This function is the same as fake macro but position must be passed explicitly."
  [ctx position config]
  (-fake ctx position (-config->f config)))

#?(:clj
   (defmacro fake*
     "This is the same as fake macro but for reuse in other macros.
     form must be passed from the parent macro in order to correctly detect positions of fakes."
     [ctx form config]
     (-emit-fake-fn-call-with-position `-fake** ctx form config)))

#?(:clj
   (defmacro fake
     "It's made a macro instead of a function to be able to catch fake creation position for better error messages.
     form must be passed if this macro is called from another macro in order to correctly determine position."
     [ctx config]
     `(fake* ~ctx ~&form ~config)))

(defn -recorded-fake**
  "This function is the same as recorded-fake macro but position must be passed explicitly."
  ([ctx position] (-recorded-fake** ctx position default-fake-config))
  ([ctx position config] (-recorded ctx position (-config->f config))))

#?(:clj
   (defmacro recorded-fake*
     "This is the same as recorded-fake macro but for reuse in other macros.
     form must be passed from the parent macro in order to correctly detect positions of fakes."
     ([ctx form] (-emit-fake-fn-call-with-position `-recorded-fake** ctx form))
     ([ctx form config] (-emit-fake-fn-call-with-position `-recorded-fake** ctx form config))))

#?(:clj
   (defmacro recorded-fake
     "It's made a macro instead of a function to be able to catch fake creation position for better error messages.
     Use recorded-fake* in case this macro is called from another macro in order to correctly detect positions."
     ([ctx] `(recorded-fake* ~ctx ~&form))
     ([ctx config] `(recorded-fake* ~ctx ~&form ~config))))

(defn -recorded-fake-as*
  [ctx position k config]
  {:pre [ctx]}
  (-recorded-as ctx position k (-config->f config)))

#?(:clj
   (defmacro -recorded-fake-as
     "The same as recorded-fake but fake will be stored into context by specified key instead of its value.
     E.g. it is used in reify-fake in order to emualte recording calls on the protocol method.
     form must be passed if this macro is called from another macro in order to correctly determine position."
     ([ctx form k] (-emit-fake-fn-call-with-position `-recorded-fake-as* ctx form k `default-fake-config))
     ([ctx form k config] (-emit-fake-fn-call-with-position `-recorded-fake-as* ctx form k config))))

(defn calls
  ([ctx] (:calls @ctx))
  ([ctx k]
   {:pre [ctx (-recorded? ctx k)]}
   (->> (:calls @ctx)
        (filter #(= k (first %)))
        (mapv #(second %)))))

;;;;;;;;;;;;;;;;;;;;;;;; Protocol fakes
(defn -register-obj-id
  [ctx obj id]
  {:pre [ctx obj id]}
  (swap! ctx assoc-in [:object-ids obj] id))

(defn -obj->id
  "Finds id for reified protocol instance. Returns nil if not found."
  [ctx obj]
  {:pre [ctx obj]}
  (get (:object-ids @ctx) obj))

(defn -method-hash
  "Generates a unique hash from specified protocol instance id and function."
  [id f]
  {:pre [id f]}
  [id f])

(defn method
  "Returns a hash-key by which calls are recorded for the specified protocol instance method."
  [ctx obj f]
  {:pre [ctx obj (or (fn? f) (string? f))]}
  (-method-hash (-obj->id ctx obj) f))

(defn -with-any-first-arg-config
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
   (defn -add-any-first-arg-into-matchers
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
   (defn -looks-like-method-spec?
     [x]
     (list? x)))

#?(:clj
   (defn -parse-specs
     "Returns a map: protocol -> method-specs"
     [specs]
     (loop [ret {}
            specs specs]
       (if (seq specs)
         (recur (assoc ret (first specs) (take-while -looks-like-method-spec? (next specs)))
                (drop-while -looks-like-method-spec? (next specs)))
         ret))))

#?(:clj
   (defn -emit-method-full-sym
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
   (defn -resolves?
     [env sym]
     (if (-cljs-env? env)
       ; ClojureScript
       (not (nil? (:meta (a/resolve-var env sym))))

       ; Clojure
       (not (nil? (resolve sym))))))

#?(:clj
   (defn -emit-imp-value
     [form env ctx obj-id-sym protocol-sym
      [method-sym fake-type config :as _method-spec_]]
     (let [method-full-sym (-emit-method-full-sym env protocol-sym method-sym)
           method-hash `(-method-hash
                          ~obj-id-sym
                          ; if compiler cannot resolve a symbol then hash by its string repr instead of the value
                          ; if resolved symbol is really not a protocol/class it will fail later on reify macro expansion
                          ~(if (-resolves? env method-full-sym)
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
   (defn -resolve-protocol-with-specs
     "Returns a resolved protocol or nil if resolved object has no protocol specs."
     [env protocol-sym]
     (if (-cljs-env? env)
       ; ClojureScript
       (let [protocol (a/resolve-var env protocol-sym)]
         (when (not (nil? (-> protocol :meta :protocol-info)))
           protocol))

       ; Clojure
       (let [protocol (resolve protocol-sym)]
         (when (instance? clojure.lang.Var protocol)
           protocol)))))

#?(:clj
   (defn -resolves-to-Object?
     [env sym]
     (if (-cljs-env? env)
       ; ClojureScript
       (= 'Object sym)

       ; Clojure
       (= Object (resolve sym)))))

#?(:clj
   (defn -protocol-methods
     "Portable reflection helper. Returns different structures for different hosts.
     Protocol must be already resolved."
     [env protocol]
     (if (-cljs-env? env)
       ; ClojureScript
       (-> protocol :meta :protocol-info :methods)

       ; Clojure
       (-> protocol deref :sigs vals))))

#?(:clj
   (defn -protocol-method-name
     "Portable reflection helper."
     [env protocol-method]
     (if (-cljs-env? env)
       ; ClojureScript
       (first protocol-method)

       ; Clojure
       (:name protocol-method))))

#?(:clj
   (defn -protocol-method-argslist
     "Portable reflection helper."
     [env protocol-method]
     (if (-cljs-env? env)
       ; ClojureScript
       (second protocol-method)

       ; Clojure
       (:arglists protocol-method))))

#?(:clj
   (defn -reflect-interface-or-object
     "Raises an exception if cannot reflect on specified symbol."
     [env interface-sym]
     (assert (not (-cljs-env? env)) "Works only in Clojure for reflection on Java interfaces and Object class.")
     (if (-resolves-to-Object? env interface-sym)
       ; Object
       (reflect/reflect Object)

       ; Java interface?
       (try
         (reflect/type-reflect interface-sym)

         ; error
         (catch Exception e
           (assert nil (str "Unknown protocol or interface: " interface-sym
                            ". Underlying exception: " (pr-str e))))))))

#?(:clj
   (defn -remove-meta
     [x]
     (with-meta x nil)))

#?(:clj
   (defn -with-hint
     [hint sym]
     (with-meta sym {:tag hint})))

#?(:clj
   (defn -arg
     "Constructs arg symbol from the specified name."
     [name]
     (-> name
         ; sanitize in case Java name is passed in (e.g. java.lang.CharSequence)
         (string/replace #"\." "-")
         (gensym))))

#?(:clj
   (defn -fix-arglist
     "Passed arglist can have duplicated args and not allowed symbols.
     This function generates a correct arglist for code emitting.
     No type hints will be specified."
     [arglist]
     (mapv -arg arglist)))

#?(:clj
   (defn -fix-arglist-with-hints
     "The same as -fix-arglist and also adds type hints:
       for the first arg: protocol-sym will be used as a hint;
       for the rest: arg value itself will be used as a hint."
     [protocol-sym arglist]
     (into [(-with-hint protocol-sym (-arg (first arglist)))]
           (map #(-with-hint % (-arg %)) (rest arglist)))))

#?(:clj
   (defn -method-arglists
     "Given protocol and method returns all allowed method arglists:
     [[...] ...]

     If protocol is a Java Object or interface then args will be hinted which is needed
     to support overloaded methods."
     [env protocol-sym method-sym]
     (if-let [protocol (-resolve-protocol-with-specs env protocol-sym)]
       ; protocol
       (let [methods (-protocol-methods env protocol)
             method (-find-first #(= method-sym (-protocol-method-name env %)) methods)]
         (map -fix-arglist (-protocol-method-argslist env method)))

       ; not a protocol
       (if (-cljs-env? env)
         ; ClojureScript - error (note that Object is not supported yet)
         (assert nil (str "Unknown protocol: " protocol-sym))

         ; Clojure - Object, Java interface or error
         (let [interface (-reflect-interface-or-object env protocol-sym)
               members (filter #(and (= method-sym (:name %))
                                     (not (contains? (:flags %) :varargs)))
                               (:members interface))]
           (mapv #(-fix-arglist-with-hints protocol-sym
                                           (into ['this] (:parameter-types %)))
                 members))))))

#?(:clj
   (defn -hinted-method-sym
     [env protocol-sym method-sym]
     ; no hint is needed in ClojureScript or for Clojure protocol methods
     (if (or (-cljs-env? env)
             (-resolve-protocol-with-specs env protocol-sym))
       method-sym

       ; Object, Java interface or error
       (let [interface (-reflect-interface-or-object env protocol-sym)
             members (:members interface)
             member (-find-first #(= method-sym (:name %)) members)]
         (-with-hint (:return-type member) method-sym)))))

#?(:clj
   (defn -method-imp
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
   (defn -method-imps-for-protocol
     "Returns a list of maps:
      ({:imp-sym ...
       :imp-value ...
       :method-sym ...
       :arglists ...} ...)"
     [form env ctx obj-id-sym protocol-sym method-specs]
     (map (partial -method-imp form env ctx obj-id-sym protocol-sym) method-specs)))

#?(:clj
   (defn -generate-protocol-method-imps
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
   (defn -emit-imp-bindings
     "Emits a list of let bindings with implementations for reified methods:
     [imp-sym1 imp-value1
     imp-sym2 imp-value2 ...]"
     [protocol-method-imps]
     (mapcat #(vector (:imp-sym %) (:imp-value %))
             (flatten (vals protocol-method-imps)))))

#?(:clj
   (defn -emit-method-specs
     [{:keys [method-sym arglists imp-sym] :as _method-imp_}]
     (map (fn [arglist]
            `(~method-sym ~arglist
               (~imp-sym ~@(map -remove-meta arglist))))
          arglists)))

#?(:clj
   (defn -emit-protocol-specs
     [[protocol-sym method-imps]]
     (into [protocol-sym]
           (mapcat -emit-method-specs method-imps))))

#?(:clj
   (defn -emit-specs
     "Emits final specs for reify macro."
     [protocol-method-imps]
     (mapcat -emit-protocol-specs protocol-method-imps)))

#?(:clj
   (defn -nice-specs
     "Generates nice specs for all methods from the specified protocol.
     Does nothing for Java interfaces and unresolved symbols."
     [env protocol-sym]
     (when-let [protocol (-resolve-protocol-with-specs env protocol-sym)]
       (let [methods (-protocol-methods env protocol)
             nice-specs (map #(vector (-protocol-method-name env %) :-nice-fake) methods)]
         nice-specs))))

#?(:clj
   (defn -not-yet-in-specs [specs [nice-method-sym :as _spec_]]
     (nil?
       (-find-first (fn [[method-sym :as _spec_]]
                      (= method-sym nice-method-sym))
                    specs))))

#?(:clj
   (defn -add-nice-specs-for-protocol [env [protocol-sym parsed-specs :as _parsed_spec_]]
     (let [nice-specs (-nice-specs env protocol-sym)
           auto-specs (filter (partial -not-yet-in-specs parsed-specs) nice-specs)
           new-specs (concat parsed-specs auto-specs)]
       [protocol-sym new-specs])))

#?(:clj
   (defn -add-nice-specs
     "For all methods which are not explicitly defined adds a 'nice' default spec."
     [env parsed-specs]
     (map (partial -add-nice-specs-for-protocol env) parsed-specs)))

#?(:clj
   (defmacro -reify-fake*
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
     "The same as reify-fake but to be used inside the macros.
     form is needed to correctly determine fake positions, env is needed to determine target language.
     Syntax example:
     (reify-fake* my-context
       protocol-or-interface-or-Object
       [method1 :optional-fake [any? 123]]
       protocol-or-interface-or-Object
       [method2 :recorded-fake])"
     [ctx form env & specs]
     `(-reify-fake* ~ctx ~form ~env false ~@specs)))

#?(:clj
   (defmacro reify-nice-fake*
     "The same as reify-nice-fake but to be used inside macros."
     [ctx form env & specs]
     `(-reify-fake* ~ctx ~form ~env true ~@specs)))

#?(:clj
   (defmacro reify-fake
     [ctx & specs]
     `(reify-fake* ~ctx ~&form ~&env ~@specs)))

#?(:clj
   (defmacro reify-nice-fake
     [ctx & specs]
     `(reify-nice-fake* ~ctx ~&form ~&env ~@specs)))

;;;;;;;;;;;;;;;;;;;;;;;; Self-tests
(defn -fake->str
  [ctx fake-type f]
  (let [p (-position ctx f)
        base-str (str fake-type " from " (:file p) ", " (:line p) ":" (:column p))
        desc (get (:fake-descs @ctx) f)]
    (if desc
      (str base-str " (" desc ")")
      base-str)))

(defn self-test-unused-fakes
  [ctx]
  {:pre [ctx]}
  (if-let [descriptions (not-empty (map (partial -fake->str ctx "non-optional fake")
                                        (:unused-fakes @ctx)))]
    (throw (ex-info (str "Self-test: no call detected for:\n"
                         (string/join "\n" descriptions))
                    {}))))

(defn self-test-unchecked-fakes
  [ctx]
  {:pre [ctx]}
  (if-let [descriptions (not-empty (map (partial -fake->str ctx "recorded fake")
                                        (:unchecked-fakes @ctx)))]
    (throw (ex-info (str "Self-test: no check performed on:\n"
                         (string/join "\n" descriptions))
                    {}))))

;;;;;;;;;;;;;;;;;;;;;;;; Asserts
(defn -was-called-times
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
      (throw (ex-info (str "Function was never called with the expected args. "
                           "Args matcher: " (pr-str args-matcher) ". "
                           "Actual calls: " (with-out-str (pprint/pprint k-calls)))
                      {}))

      :else true)))

(defn was-called-once
  "Checks that function was called strictly once and that the call was with the specified args."
  ([ctx k] (was-called-once ctx k any?))
  ([ctx k args-matcher]
   {:pre [ctx (-recorded? ctx k) (satisfies? ArgsMatcher args-matcher)]}
   (-was-called-times ctx #(= % 1) "1" k args-matcher)))

(defn was-called
  "Checks that function was called at least once with the specified args."
  ([ctx k] (was-called ctx k any?))
  ([ctx k args-matcher]
   {:pre [ctx (-recorded? ctx k) (satisfies? ArgsMatcher args-matcher)]}
   (-was-called-times ctx #(> % 0) "> 0" k args-matcher)))

(defn was-not-called
  "Checks that function was never called."
  [ctx k]
  {:pre [ctx (-recorded? ctx k)]}
  (mark-checked ctx k)
  (let [k-calls (calls ctx k)]
    (or (empty? k-calls)
        (throw (ex-info (str "Function is expected to be never called. Actual calls: " (pr-str k-calls) ".") {})))))

(defn was-called-once-on
  "Shortcut for checking method call on specified instance."
  ([ctx obj f] (was-called-once-on ctx obj f any?))
  ([ctx obj f args-matcher]
   {:pre [ctx obj f (satisfies? ArgsMatcher args-matcher)]}
   (was-called-once ctx (method ctx obj f) (-with-any-first-arg args-matcher))))

(defn was-called-on
  "Shortcut for checking method call on specified instance."
  ([ctx obj f] (was-called-on ctx obj f any?))
  ([ctx obj f args-matcher]
   {:pre [ctx obj f (satisfies? ArgsMatcher args-matcher)]}
   (was-called ctx (method ctx obj f) (-with-any-first-arg args-matcher))))

(defn was-not-called-on
  "Shortcut for checking method call on specified instance."
  [ctx obj f]
  {:pre [ctx obj f]}
  (was-not-called ctx (method ctx obj f)))

;;;;;;;;;;;;;;;;;;;;;;;; Monkey patching
(defn -save-original-val!
  [ctx a-var]
  {:pre [ctx a-var]}
  (when-not (contains? (:original-vals @ctx) a-var)
    (swap! ctx assoc-in [:original-vals a-var] @a-var)))

(defn -save-unpatch!
  [ctx a-var unpatch-fn]
  {:pre [ctx a-var (fn? unpatch-fn)]}
  (swap! ctx assoc-in [:unpatches a-var] unpatch-fn))

(defn original-val
  "Given a patched var returns its original value."
  [ctx a-var]
  {:pre [ctx a-var]}
  (assert (contains? (:original-vals @ctx) a-var) "Specified var is not patched")
  (get (:original-vals @ctx) a-var))

#?(:clj
   (defmacro -set-var!
     "Portable var set. ClojureScript version expects passed var to be a pair, e.g. (var foo)."
     [a-var val]
     (if (-cljs-env? &env)
       ; ClojureScript
       (let [[_ var-symbol] a-var]
         `(set! ~var-symbol ~val))

       ; Clojure
       `(alter-var-root ~a-var (constantly ~val)))))

#?(:clj
   (defmacro patch!
     [ctx var-expr val]
     `(do
        (-save-original-val! ~ctx ~var-expr)
        (-save-unpatch! ~ctx
                        ~var-expr
                        #(-set-var! ~var-expr (original-val ~ctx ~var-expr)))
        (-set-var! ~var-expr ~val))))

(defn unpatch!
  [ctx a-var]
  {:pre [ctx a-var]}
  (assert (contains? (:unpatches @ctx) a-var) "Specified var is not patched")
  ((get (:unpatches @ctx) a-var)))

(defn unpatch-all!
  [ctx]
  (doseq [[_var_ unpatch-fn] (:unpatches @ctx)]
    (unpatch-fn)))