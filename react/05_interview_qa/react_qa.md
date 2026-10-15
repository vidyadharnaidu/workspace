# React Interview Q&A (React 19)

## Core mental model

| Question | Answer |
|---|---|
| What is React, in one sentence? | A library for building UIs from components: you describe what the UI should look like for a given state, and React updates the DOM to match. |
| What is the virtual DOM? | An in-memory tree of lightweight objects describing the UI. On a state change React builds a new tree, **diffs** it against the old one (reconciliation), and applies the minimum DOM changes. The DOM is slow; comparing objects is cheap. |
| What is reconciliation? | The diffing algorithm. Two heuristics keep it O(n): different element **types** mean "throw the subtree away and rebuild", and children are matched by **key**. |
| Why are keys important? | They tell React which item is which across renders. With index keys, inserting at the top makes React think every item changed: DOM nodes, input values and focus get reused by the wrong rows. Use stable ids. |
| What is Fiber? | React's reconciler architecture: the work is split into units that can be paused, prioritized and resumed. It's what makes concurrent rendering possible. |
| Declarative vs imperative? | You write `state -> UI`; React works out the DOM operations. Imperative code (jQuery) lists the steps instead. |
| What makes a component re-render? | Its own state changes, its parent re-renders, or a context it consumes changes. (Props changing matters only because the parent re-rendered.) |
| Is a re-render bad? | No. Rendering is computing JSX and diffing. It's only a problem when it's expensive or very frequent, which you confirm with the Profiler. |
| Controlled vs uncontrolled? | Controlled: React state is the source of truth (`value` + `onChange`) — validation, formatting, conditional UI. Uncontrolled: the DOM owns the value (`defaultValue` + ref/FormData) — simpler, fewer renders. |
| Props vs state? | Props come from the parent and are read-only; state is owned and updated by the component. Anything derivable from props/state should be computed, not stored. |
| Why is state immutable? | React compares references (`Object.is`) to decide what changed. Mutating an object keeps the same reference, so the update is missed. |
| Is `setState` synchronous? | No. Updates are queued and **batched** (in React 18+, in every context including promises and timeouts), then applied in one re-render. Reading the state variable right after setting it gives the old value; use the functional updater. |
| What is `StrictMode`? | A development-only wrapper that intentionally double-invokes render and effects (mount → unmount → mount) to surface impure renders and missing cleanups. It does nothing in production. |

## Hooks

| Question | Answer |
|---|---|
| Why hooks over classes? | Reuse logic without wrapper hell (HOCs/render props), no `this` binding, related logic stays together instead of being split across lifecycle methods. |
| Rules of hooks, and why? | Only at the top level, only from components/hooks. React tracks hook state by **call order**, so a conditional hook shifts every later hook. |
| `useEffect` vs `useLayoutEffect`? | `useEffect` runs after paint (asynchronous, doesn't block the browser). `useLayoutEffect` runs before paint — use it only to measure or mutate the DOM to avoid a visible flicker. |
| When is a cleanup function called? | Before the next run of the effect and on unmount. Use it for timers, subscriptions, listeners and aborting requests. |
| What does the dependency array do? | It decides when the effect re-runs. No array = every render; `[]` = once; `[a, b]` = when `a` or `b` change (compared with `Object.is`). |
| Common `useEffect` mistakes? | Missing dependencies (stale values); an object/array/function created inline as a dependency (new reference each render → infinite loop); setting state derived from props instead of computing it during render; fetching without cleanup (race conditions). |
| When do you NOT need an effect? | Transforming data for rendering (compute it), responding to a user event (do it in the handler), resetting state when a prop changes (use a `key` instead). |
| `useMemo` vs `useCallback`? | `useMemo(fn, deps)` caches a **value**; `useCallback(fn, deps)` caches a **function identity** (the same as `useMemo(() => fn, deps)`). Both matter when passing props to memoized children or when the computation is genuinely expensive. |
| `useRef` use cases? | DOM access (`focus`, measurement), mutable values that shouldn't re-render (timer ids, previous values, "has mounted" flags), and holding the latest callback. |
| `useState` vs `useReducer`? | Reducer when the next state depends on the previous one in non-trivial ways, when several values change together, or when you want the transitions testable and named (it's a state machine). |
| Why does my state look "stale" in a callback? | Closures capture the values of that render. Use the functional updater `setX(prev => ...)`, a ref for the latest value, or add the dependency. |
| What is `useSyncExternalStore` for? | Subscribing to state outside React (browser APIs, a store) without tearing, and with an SSR snapshot. |
| `useId`? | Generates stable unique ids for accessibility attributes that match between server and client. |
| `useTransition` / `useDeferredValue`? | Mark updates as non-urgent so typing stays responsive: the urgent update (the input) renders first, the heavy list catches up. |
| Custom hook rules? | Name it `use*`, call hooks unconditionally inside it, return a tuple or object. It shares logic, not state. |

## React 19 features

| Feature | What it gives you |
|---|---|
| **Actions** (`<form action={fn}>`) | Pass an async function to a form; React handles pending state, errors and resetting. |
| `useActionState(fn, initial)` | Runs an action, returns `[state, formAction, isPending]` — form submissions without hand-written loading flags. |
| `useFormStatus()` | Lets a nested submit button read the parent form's pending state without prop drilling. |
| `useOptimistic(state, reducer)` | Show the result immediately and roll back automatically if the request fails. |
| `use(promise)` / `use(context)` | Read a promise (suspends) or a context — and unlike hooks it may be called conditionally. |
| `ref` as a prop | `forwardRef` is no longer needed for function components. |
| `<Context>` as provider | `<ThemeContext value={...}>` instead of `<ThemeContext.Provider value={...}>`. |
| Document metadata | `<title>`, `<meta>`, `<link>` rendered anywhere get hoisted into `<head>`. |
| React Compiler (opt-in) | Auto-memoizes components, so most manual `useMemo`/`useCallback` becomes unnecessary. Know the concept; manual memoization is still the common interview answer. |

## State management & data

| Question | Answer |
|---|---|
| Context vs Redux vs Zustand vs React Query? | Context passes values down (it is **not** a state manager and re-renders all consumers when the value changes). Redux/Zustand manage client state with selectors and devtools. TanStack Query/SWR manage **server** state: caching, dedupe, retries, background refresh. Most apps need server-state tooling plus a little client state. |
| Context performance problem? | Every consumer re-renders when the value changes. Fixes: memoize the value, split into several contexts (state vs dispatch), or use a store with selectors. |
| How do you avoid prop drilling? | Composition (pass `children`), context, or a store. |
| Where should state live? | The closest common parent of the components that need it — no higher. "Lift state up", then push it back down when you can. |
| How do you fetch data in React today? | Server Components / framework loaders if you have them; otherwise a data library. Hand-rolled `useEffect` fetching needs cleanup, race-condition guards and caching, which is exactly why libraries exist. |
| How do you avoid a fetch waterfall? | Start requests in parallel (`Promise.all`), fetch at the route level, prefetch on hover/intent, or use Suspense with data started before render. |

## Rendering, routing and the server

| Question | Answer |
|---|---|
| CSR vs SSR vs SSG vs ISR? | Render in the browser / on each request / at build time / regenerate static pages in the background. Trade-off: time to first byte and SEO versus server cost and complexity. |
| What is hydration? | Attaching React to server-rendered HTML. A **hydration mismatch** happens when the server and client render different output (`Date.now()`, `window`, random ids). |
| React Server Components? | Components that run only on the server, can read the database directly, and send no JavaScript to the client. Client components opt in with `"use client"`. They reduce bundle size; they can't use state or effects. |
| What is Suspense? | A boundary that shows a fallback while children are loading (lazy components or data). Combined with `lazy()` for code splitting. |
| Error boundaries? | Class components with `getDerivedStateFromError`/`componentDidCatch`. They catch render-phase errors in their subtree — not event handlers, async code or errors thrown in the boundary itself. |
| Portals? | `createPortal(children, domNode)` renders into another DOM node (modals, tooltips) while keeping React context and event bubbling through the React tree. |
| How does React handle events? | Synthetic events with a consistent cross-browser API; since React 17 they're attached to the root container, not `document`. |

## Testing and quality

| Question | Answer |
|---|---|
| How do you test components? | React Testing Library: render, query the way a user would (`getByRole`, `getByLabelText`), interact with `user-event`, assert on visible output. Test behaviour, not implementation details. |
| How do you test a custom hook? | Through a component that uses it, or with `renderHook` from RTL. |
| What do you mock? | The network boundary (MSW or a fetch stub) — not your own modules, where possible. |
| Where does the `act()` warning come from? | State updated outside React's knowledge (an async resolution after the test finished). Await the assertion (`findBy*`) or wrap the update. |

## Common code-review flags

```jsx
{items.map((item, i) => <Row key={i} ... />)}          // index key in a dynamic list
useEffect(() => { setFull(`${first} ${last}`) }, [first, last]);  // derived state in an effect -> compute it
<Child style={{ margin: 8 }} onClick={() => go(id)} />  // new object/function each render defeats memo
const [state, setState] = useState(props.value);        // props copied into state: it won't update later
setCount(count + 1); setCount(count + 1);               // stale closure: use the updater form
useEffect(async () => { ... }, []);                      // effects can't be async: define an inner async fn
{count && <Badge />}                                     // renders "0"; use count > 0 ? ... : null
items.sort(...)                                          // sort mutates props/state; use toSorted or a copy
```

## Questions to ask them back

- How is server state handled — a data library, RSC, or hand-rolled effects?
- What's the testing strategy: unit, component, end-to-end?
- Are you on React 19 / the compiler, and how do you handle memoization today?
- How is the component library organized (design system, styling approach)?
