# React Interview Questions

React 19 + TypeScript. Every component is **type-checked against the real React 19 types** and **server-rendered** to prove it works, and the pure logic runs as a self-checking script.

## Run the checks

```bash
node react/04_logic/pure_logic.js      # no install needed: reducers, validation, memo semantics, races

cd react && npm install                # only needed for the next two
npm run typecheck                      # tsc --noEmit against @types/react 19
npm run verify                         # renders every component with react-dom/server and checks the HTML
npm run check                          # all three
```

## Files

| Folder | File | What's inside |
|---|---|---|
| `01_fundamentals/` | `components_and_props.tsx` | Typed props, children/composition, lists and **why index keys break**, controlled inputs, derived state, conditional rendering, the `count && <X/>` renders-"0" trap, React 19 `ref` as a prop, `ComponentPropsWithoutRef` |
| | `hooks_basics.tsx` | `useState` (stale closure vs functional update, lazy init), `useEffect` (deps, cleanup, **abort + race conditions**), `useLayoutEffect`, `useRef` (DOM, timers, render count), `useReducer` cart state machine, `useContext` provider done right, `useMemo`/`useCallback`/`useId` |
| | `custom_hooks.tsx` | `useToggle`, `usePrevious`, `useDebouncedValue`, `useEventCallback`, `useInterval` (the ref pattern), `useLocalStorage`, `useAsync` with a state machine, `useOnClickOutside`, `useMediaQuery` with `useSyncExternalStore` |
| `02_patterns/` | `component_patterns.tsx` | **Compound components** (`<Tabs><Tabs.Tab/></Tabs>`), render props, HOC, container/presentational, **error boundary** (class), controlled-or-uncontrolled component API |
| | `performance.tsx` | What actually causes re-renders, `memo` + stable props, custom comparators, move state down, children-as-a-prop, `useMemo`/`useCallback` where they help, `useTransition`/`useDeferredValue`, `lazy` + `Suspense`, a hand-written **virtual list**, anti-patterns to name |
| `03_forms_data/` | `forms_and_data.tsx` | Controlled form with validation + accessibility, uncontrolled form with `FormData`, **React 19 actions** (`useActionState`, `useFormStatus`), **`useOptimistic`**, pagination hook, the four data states |
| `04_logic/` | `pure_logic.js` | Runnable: cart reducer, form validation, `shallowEqual` (what `memo` compares), immutable update helpers, selectors, pagination math, debounce, stale-response guard |
| `05_interview_qa/` | `react_qa.md` | ~45 Q&A: virtual DOM, reconciliation, keys, Fiber, batching, StrictMode, every hook, React 19 features, state management choices, SSR/RSC/hydration, code-review red flags |
| | `testing_guide.md` | RTL principles, query priority, async and `act`, MSW mocking, testing hooks, what not to test, the testing pyramid |
| `verify/` | `render_test.tsx` | Renders all 24 components and asserts the HTML |

## The answers worth memorizing

| Question | One-line answer |
|---|---|
| Why keys? | They identify items across renders; index keys reuse the wrong DOM node when a list changes. |
| Why immutable state? | React compares references, so a mutated object looks unchanged. |
| Why does my state look stale? | Closures capture that render's values → use `setX(prev => ...)`. |
| When do effects run? | After paint; cleanup runs before the next effect and on unmount. |
| When do I NOT need an effect? | Derived data (compute it), event responses (do it in the handler), resetting state on a prop change (use a `key`). |
| What re-renders a component? | Its state, its parent rendering, or a context it reads. |
| Why doesn't `memo` help? | Inline objects/functions create new props every render. |
| `useMemo` vs `useCallback`? | Cache a value vs cache a function identity. |
| Controlled vs uncontrolled? | React owns the value vs the DOM owns the value. |
| Context vs a state library? | Context passes values (all consumers re-render); libraries add selectors, caching and devtools. |

## Practice routine

1. Write `useDebouncedValue`, `useInterval` and `useLocalStorage` from memory, then compare with `01_fundamentals/custom_hooks.tsx`.
2. Build a todo list with add/toggle/filter and no `useEffect` for derived data.
3. Explain out loud why a list re-renders and what you'd measure before optimizing it.
4. Take one component from `02_patterns/` and write RTL tests for it using `05_interview_qa/testing_guide.md`.

Related folders: `javascript/`, `typescript/`, `python/`, `java/` (Java), `db/` (SQL), `system-design/`.
