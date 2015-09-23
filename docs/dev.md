# Developer Guide
Some notes for framework developers.

## Tests

Autorun Clojure tests: `lein test-refresh`

Run ClojureScript tests: `lein clean && lein doo phantom test once`
(clean is needed because there's an issue: ClojureScript plugin does not seem to recompile macros)

Autorun ClojureScript tests: `fswatch -o ./src ./test | xargs -n1 -I{} sh -c 'echo wait.... && lein clean && lein doo rhino test once'`

## Documentation

Project uses [MkDocs](http://www.mkdocs.org/) to generate documentation static site.

Build: `mkdocs build`

Serve locally with automatic build: `mkdocs serve`

Deploy docs to gh-pages branch: `mkdocs gh-deploy --clean`