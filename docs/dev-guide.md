# Tests

Autorun Clojure tests: `lein test-refresh`

Run ClojureScript tests: `lein clean && lein doo phantom test once`
(clean is needed because there's an issue: ClojureScript plugin does not seem to recompile macros)

Autorun ClojureScript tests: `fswatch -o ./src ./test | xargs -n1 -I{} sh -c 'echo wait.... && lein clean && lein doo rhino test once'`

# Documentation

Project uses [MkDocs](http://www.mkdocs.org/) to generate documentation static site and 
[Codox](https://github.com/weavejester/codox) for API reference.

Serve site locally with automatic build: `mkdocs serve`

Build site: `mkdocs build`

Generate API docs into site folder: `lein doc`


# Deploying

Deploy to Clojars: `lein deploy clojars`

Deploy site to gh-pages branch: `mkdocs gh-deploy`