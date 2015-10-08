# Tests

Autorun Clojure tests: `lein test-refresh`

Run ClojureScript tests: `lein clean && lein doo phantom test once`
(clean is needed because there's an issue: ClojureScript plugin does not seem to recompile macros)

Autorun ClojureScript tests: `fswatch -o ./src ./test | xargs -n1 -I{} sh -c 'echo wait.... && lein clean && lein doo rhino test once'`

# Documentation

Project uses [MkDocs](http://www.mkdocs.org/) to generate documentation static site and 
[Codox](https://github.com/weavejester/codox) for API reference.

Generate API docs: `lein doc`

Build site (after API docs are generated): `mkdocs build`

Serve site locally with automatic build: `mkdocs serve`

# Deploying

Deploy to Clojars: `lein deploy clojars`

Deploy docs to gh-pages branch: `mkdocs gh-deploy --clean`