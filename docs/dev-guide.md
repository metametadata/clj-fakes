# Tests

Autorun Clojure tests: `lein test-refresh`

Run ClojureScript tests: `lein do clean, doo phantom test once`
(clean is needed because there's an [issue](https://github.com/bensu/doo/issues/51): plugin does not seem to recompile macros)

Use `min-deps` profile to test with minimal supported dependencies (instead of default and usually latest versions):
 
`lein with-profiles +min-deps test-refresh`

This profile cannot be applied to the ClojureScript version of the library because usage of the latest ClojureScript is always assumed.

# Documentation

Project uses [MkDocs](http://www.mkdocs.org/) with [Cinder](https://github.com/chrissimpkins/cinder) theme to generate documentation static site and 
[Codox](https://github.com/weavejester/codox) for API reference.

Tasks are scripted using [Lumo](https://github.com/anmonteiro/lumo).
Run `yarn` in order to install NodeJS dependencies for tasks.

Build only site pages: `./tasks.cljs mkdocs`

Build API reference into site folder: `./tasks.cljs api`

Build the whole site: `./tasks.cljs site`

Serve site pages locally with automatic build (but it won't work for index page): `mkdocs serve`

# Deploying

Deploy to Clojars: `lein deploy clojars`

Deploy site to gh-pages branch: `ghp-import -p site`