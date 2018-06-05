A CLI tool for CDIG developers

### Dev

First, run `yarn` to fetch the deps.

Build a dev version with `cljs build dev` or `cljs watch dev`, and then use the `cdig` bin in this folder.

Build a prod version with `cljs build prod` or `cdig watch prod`, then push to github and install with `npm i -g cdig/cli`.

UPDATE — to build use:
```
clj -m cljs.main --target node --output-to cdig.js --optimizations simple -c cdig.cli
```
