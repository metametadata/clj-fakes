# Tests

Autorun Clojure tests: `lein test-refresh`

Run ClojureScript tests: `lein clean && lein doo rhino test once`
(clean is needed because there's an [issue](https://github.com/bensu/doo/issues/51): plugin does not seem to recompile macros)

# Documentation

Project uses [MkDocs](http://www.mkdocs.org/) to generate documentation static site and 
[Codox](https://github.com/weavejester/codox) for API reference.

Serve site locally with automatic build: `mkdocs serve`

Build site and API docs into site folder: `mkdocs build --clean && lein codox`

# Deploying

Deploy to Clojars: `lein deploy clojars`

Deploy site to gh-pages branch: `ghp-import -p site`