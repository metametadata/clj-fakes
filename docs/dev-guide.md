# Tests

Autorun Clojure tests: `lein test-refresh`

Run ClojureScript tests: `lein clean && lein doo phantom test once`
(clean is needed because there's an [issue](https://github.com/bensu/doo/issues/51): plugin does not seem to recompile macros)

Use `min-deps` profile to test with minimal supported dependencies (instead of default and usually latest versions):
 
`lein with-profiles +min-deps test-refresh`

`lein clean && lein with-profiles +min-deps doo phantom test once`

# Documentation

Project uses [MkDocs](http://www.mkdocs.org/) with [Cinder](https://github.com/chrissimpkins/cinder) theme to generate documentation static site and 
[Codox](https://github.com/weavejester/codox) for API reference.
Tasks are scripted using [PyInvoke](http://www.pyinvoke.org/).

Serve site pages locally with automatic build (but it won't work for index page): `mkdocs serve`

Build only site pages: `inv mkdocs`

Build API reference into site folder: `inv api`

Build the whole site: `inv site`

# Deploying

Deploy to Clojars: `lein deploy clojars`

Deploy site to gh-pages branch: `ghp-import -p site`