# JavaScript Interview Questions

Runnable Node.js files (no dependencies, no build step). Every file checks its own answers and throws on a mismatch, so a wrong result fails loudly.

```bash
node javascript/03_async/async_promises_eventloop.js   # run one topic
node javascript/run_all.js                             # run everything, PASS/FAIL per file
node javascript/run_all.js async                       # filter by path
```

Requires Node 18+ (Node 24 was used here). Uses modern syntax on purpose: `??`, `?.`, `#private`, `toSorted`, `structuredClone`, `Array.at`.

## Folders

| Folder | File | What's inside |
|---|---|---|
| `01_language_fundamentals/` | `language_fundamentals.js` | Hoisting, `var`/`let`/`const`, closures in loops, `const` vs `Object.freeze`, copy semantics, coercion quiz, `\|\|` vs `??`, destructuring, optional chaining, number/string traps |
| `02_functions_this_closures/` | `functions_this_closures.js` | Closures, `once`, memoize, the four `this` rules, losing `this`, `call`/`apply`/`bind` (+ a hand-written bind), currying, pipe/compose, **debounce & throttle with a fake clock**, prototypes, classes, private fields |
| `03_async/` | `async_promises_eventloop.js` | Event-loop ordering, sequential vs parallel timing, `all`/`allSettled`/`race`/`any`, promisify, try/catch/finally, retry with backoff, **concurrency limit pool**, timeout via race, async iteration, the `forEach`-doesn't-await trap |
| `04_arrays_objects/` | `arrays_objects_recipes.js` | groupBy, countBy, sumBy, maxBy, uniqueBy (first vs last!), chunk, flatten, zip, range, multi-key sort, pick/omit/invert/mapValues, deepMerge, deepEqual, get-by-path, flattenObject, Map/Set |
| `05_algorithms/` | `algorithm_problems.js` | The classic problem set in JS: strings, arrays, binary search, recursion, tree BFS/DFS, graph BFS, LRU cache with `Map` |
| `06_patterns/` | `patterns_and_utilities.js` | Module pattern, singleton, EventEmitter, factory, strategy, **deepClone with cycles**, polyfills (`map`/`filter`/`reduce`/`flat`), **a Promise implementation from scratch**, immutability helpers |
| `07_interview_qa/` | `tricky_questions.md` | "What does this print?" table, event-loop quiz, ~30 one-line concept answers, browser/DOM questions, code-review red flags |

## The 10 utilities to be able to write from memory

1. `debounce(fn, wait)` and `throttle(fn, interval)` → `02_functions_this_closures`
2. `memoize(fn)` and `once(fn)` → `02_functions_this_closures`
3. `curry(fn)`, `pipe`, `compose` → `02_functions_this_closures`
4. `deepClone(obj)` (with cycles) and `deepEqual(a, b)` → `06_patterns`, `04_arrays_objects`
5. `groupBy` / `countBy` / `uniqueBy` with `reduce` → `04_arrays_objects`
6. `promisify(fn)` and `retry(fn, attempts)` → `03_async`
7. A concurrency-limited `mapWithConcurrency(items, limit, worker)` → `03_async`
8. `EventEmitter` (on / once / off / emit) → `06_patterns`
9. Polyfills for `map`, `filter`, `reduce`, `flat`, `bind` → `06_patterns`
10. A minimal `Promise` (then/catch, microtasks, thenable assimilation) → `06_patterns`

## Cheat sheet

```js
// falsy: false 0 -0 0n "" null undefined NaN        ([] and {} are TRUTHY)
x ?? d          // default only for null/undefined      x || d  // default for any falsy value
obj?.a?.b       // optional chaining                    fn?.()  // call if it exists
[...new Set(a)]                        // unique
Object.fromEntries(Object.entries(o).map(([k, v]) => [k, f(v)]))   // map over values
arr.toSorted((a, b) => a - b)          // sort numbers without mutating
structuredClone(obj)                   // real deep copy (keeps Date/Map/Set, handles cycles)
await Promise.all(items.map(work))     // parallel;  for (const i of items) await work(i)  // sequential
```

| Operation | Array | Map / Set | Object |
|---|---|---|---|
| Lookup by key | `indexOf` O(n) | O(1) | O(1) |
| Insertion order kept | yes | yes | yes (string keys, integer-like keys sort first) |
| Any key type | - | ✅ | strings/symbols only |
| Size | `.length` | `.size` | `Object.keys(o).length` |

Related folders: `typescript/` (typed version plus type-system questions), `python/`, `java/` (Java), `db/` (SQL), `system-design/`.
