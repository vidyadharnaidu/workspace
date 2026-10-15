# JavaScript Interview Q&A (theory, output questions, browser)

## "What does this print?" questions

| Code | Output | Why |
|---|---|---|
| `console.log(typeof null)` | `"object"` | A bug kept for backwards compatibility. Use `value === null`. |
| `console.log(0.1 + 0.2 === 0.3)` | `false` | IEEE-754 floats. Compare with a small epsilon, or use integers/cents. |
| `console.log([] + {})` | `"[object Object]"` | Both convert to strings. `[]` becomes `""`. |
| `console.log([] == false)` | `true` | `[]` → `""` → `0`, and `false` → `0`. Use `===`. |
| `console.log([1,2,3] + [4])` | `"1,2,34"` | Arrays join into strings, then concatenate. |
| `for (var i=0;i<3;i++) setTimeout(()=>console.log(i))` | `3 3 3` | One `var` binding shared by all callbacks. `let` prints `0 1 2`. |
| `console.log(a); var a = 1;` | `undefined` | `var` is hoisted; the assignment isn't. |
| `console.log(b); let b = 1;` | `ReferenceError` | Temporal dead zone. |
| `console.log(typeof undeclaredVar)` | `"undefined"` | `typeof` doesn't throw on undeclared names. |
| `console.log(1 < 2 < 3, 3 > 2 > 1)` | `true, false` | Left to right: `3 > 2` → `true` → `1 > 1` is false. |
| `console.log("5" - 3, "5" + 3)` | `2, "53"` | `-` converts to numbers, `+` prefers string concatenation. |
| `console.log(Math.max())` | `-Infinity` | The identity for max over an empty list. |
| `console.log([1,2,3].map(parseInt))` | `[1, NaN, NaN]` | `map` passes the index as `radix`. Use `map(Number)`. |
| `console.log([..."héllo"].length)` | `5` | Spread iterates code points (safer than `split("")` for emoji). |
| `({}).toString()` vs `[].toString()` | `"[object Object]"`, `""` | |
| `console.log(NaN === NaN)` | `false` | Use `Number.isNaN(x)` or `Object.is`. |

## Event loop ordering question

```js
console.log("1");
setTimeout(() => console.log("2"), 0);
Promise.resolve().then(() => console.log("3"));
queueMicrotask(() => console.log("4"));
console.log("5");
// prints 1, 5, 3, 4, 2   (sync -> microtasks -> macrotasks)
```
In Node, `process.nextTick` runs before other microtasks. See `03_async/async_promises_eventloop.js`.

## Core concepts in one line each

| Question | Answer |
|---|---|
| `var` / `let` / `const`? | Function-scoped + hoisted / block-scoped / block-scoped and non-reassignable (the value can still mutate). |
| What is a closure? | A function plus the variables from where it was defined; it keeps them alive. Used for private state, memoization, currying. |
| How is `this` decided? | `new` → the new object; `bind/call/apply` → what you passed; `obj.method()` → `obj`; otherwise `undefined` (strict). Arrows inherit `this` lexically. |
| `==` vs `===`? | `==` coerces types; `===` doesn't. Always use `===` (except `x == null` for "null or undefined"). |
| Shallow vs deep copy? | `{...obj}` / `Object.assign` copy one level; `structuredClone(obj)` copies deeply (`JSON.parse(JSON.stringify())` loses `Date`, `undefined`, `Map`, functions and breaks on cycles). |
| Prototypal inheritance? | Objects delegate to a prototype object via `[[Prototype]]`; `class` is syntax sugar over it. |
| What is hoisting? | Declarations move to the top of their scope. `var` initializes to `undefined`; `let`/`const` stay in the temporal dead zone. |
| Call, apply, bind? | Call now with args listed / call now with an args array / return a new bound function. |
| Arrow vs regular function? | Arrows have no own `this`, `arguments`, `prototype`, and cannot be used with `new`. |
| Callback hell → ? | Promises → `async`/`await`. |
| Promise states? | pending → fulfilled or rejected (settled once, immutable after). |
| `Promise.all` vs `allSettled` vs `race` vs `any`? | All (fails fast) / all results with statuses / first settled / first fulfilled. |
| Event delegation? | One listener on a parent uses `event.target` to handle many children; works for dynamically added elements. |
| Bubbling vs capturing? | Events capture down then bubble up. `addEventListener(fn, { capture: true })` listens on the way down; `stopPropagation()` halts it. |
| Debounce vs throttle? | Debounce waits for quiet (search box); throttle allows at most one call per interval (scroll, resize). |
| `null` vs `undefined`? | Intentionally empty vs never assigned. |
| Map vs object? | Map: any key type, ordered, `.size`, better for frequent add/remove. Object: JSON-friendly, string/symbol keys. |
| `Set` / `WeakMap`? | Unique values / keys held weakly (no memory leak; used for private data and caches). |
| Generators? | `function*` + `yield`: pausable functions, lazy sequences, custom iterators. |
| `Symbol.iterator`? | Makes an object work with `for...of` and spread. |
| Modules: ESM vs CommonJS? | `import`/`export` (static, async, tree-shakeable) vs `require`/`module.exports` (dynamic, synchronous, Node's original). |
| Strict mode? | `"use strict"`: no implicit globals, `this` is `undefined` in plain calls, safer errors. Modules are always strict. |
| Currying? | Turning `f(a, b, c)` into `f(a)(b)(c)` for partial application. |
| Memory leaks in JS? | Forgotten timers/listeners, global variables, closures holding big objects, detached DOM nodes. |
| What is the DOM? | The browser's object model for the page; JS reads and updates it. |
| `localStorage` / `sessionStorage` / cookies? | ~5–10 MB persistent / per-tab / small and sent with every HTTP request. |
| CORS? | Browsers block cross-origin responses unless the server sends `Access-Control-Allow-*` headers. |
| `fetch` basics? | Returns a promise; it rejects only on network errors, so check `response.ok` yourself. |
| Event loop in one sentence? | One thread, a stack, a microtask queue (promises) and a macrotask queue (timers/IO), drained in that order. |
| How do you avoid blocking the UI? | Keep work small, use async I/O, `requestIdleCallback`, or a Web Worker for heavy CPU work. |
| Web Workers? | Background threads with no DOM access; communicate via `postMessage`. |

## Performance & quality checklist

- Prefer `const`; use `let` only when reassigning; avoid `var`.
- Don't mutate shared state; prefer `map/filter/reduce` and spread for updates.
- `for` or `for...of` beats `forEach` when you need `break`/`await`.
- Batch DOM reads and writes; avoid layout thrashing.
- Debounce input handlers; throttle scroll handlers.
- Watch bundle size: import only what you use.
- Handle every promise rejection; an unhandled rejection crashes Node.

## Code review red flags interviewers plant

```js
if (user.name == null) {}          // fine: catches null AND undefined
if (items.length == "0") {}        // bad: coercion, use === 0
for (var i = 0; i < n; i++) {}     // bad in async code: use let
arr.forEach(async (x) => await f(x));  // bad: forEach ignores the promises -> use for...of
JSON.parse(JSON.stringify(obj))    // loses Date/Map/undefined -> structuredClone
new Promise(async (resolve) => {}) // anti-pattern: the async executor swallows errors
catch (e) { console.log(e) }       // swallowing an error: rethrow or handle it
```
