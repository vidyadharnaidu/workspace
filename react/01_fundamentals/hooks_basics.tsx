/**
 * The built-in hooks, with the traps interviewers ask about.
 *
 * RULES OF HOOKS (and why):
 *  1. Only call hooks at the TOP LEVEL - never in conditions, loops or nested functions.
 *  2. Only call them from React function components or other hooks.
 *  React matches hooks to state by CALL ORDER, so a conditional hook shifts every later hook's state.
 *
 * WHICH HOOK:
 *  useState      - a value that changes and should re-render
 *  useReducer    - several values that change together / complex transitions (a state machine)
 *  useRef        - a value that survives renders but must NOT trigger one (DOM node, timer id, previous value)
 *  useEffect     - synchronize with something OUTSIDE React (network, subscriptions, timers, document.title)
 *  useLayoutEffect - same, but before the browser paints (measure/adjust DOM to avoid flicker)
 *  useMemo       - cache an expensive VALUE;  useCallback - cache a FUNCTION identity
 *  useContext    - read shared data without prop drilling
 */
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useId,
  useLayoutEffect,
  useMemo,
  useReducer,
  useRef,
  useState,
  type ReactNode,
} from "react";

// ---------------------------------------------------------------- useState

export function Counter() {
  const [count, setCount] = useState(0);

  // TRAP: setCount(count + 1) twice in one handler adds ONE, because `count` is captured per render.
  const addTwoWrong = () => {
    setCount(count + 1);
    setCount(count + 1); // same stale `count` -> +1 total
  };

  // FIX: the functional updater always sees the latest value.
  const addTwoRight = () => {
    setCount((current) => current + 1);
    setCount((current) => current + 1); // +2
  };

  // Lazy initializer: the function runs only on the FIRST render (skip expensive work every render)
  const [expensive] = useState(() => Array.from({ length: 1000 }, (_, i) => i).reduce((a, b) => a + b, 0));

  return (
    <div>
      <output>{count}</output> <small>{expensive}</small>
      <button onClick={addTwoWrong}>+2 (wrong)</button>
      <button onClick={addTwoRight}>+2 (right)</button>
    </div>
  );
}

// ---------------------------------------------------------------- useEffect

interface User {
  id: number;
  name: string;
}

export function UserProfile({ userId }: { userId: number }) {
  const [user, setUser] = useState<User | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // RACE CONDITION: a slower earlier request must not overwrite a newer one.
    const controller = new AbortController();
    setUser(null);
    setError(null);

    fetch(`/api/users/${userId}`, { signal: controller.signal })
      .then((response) => {
        if (!response.ok) throw new Error(`HTTP ${response.status}`); // fetch does NOT reject on 4xx/5xx
        return response.json() as Promise<User>;
      })
      .then(setUser)
      .catch((err: unknown) => {
        if (err instanceof Error && err.name !== "AbortError") setError(err.message);
      });

    return () => controller.abort(); // CLEANUP: runs before the next effect and on unmount
  }, [userId]); // dependency array: re-run only when userId changes

  // TRAPS:
  //  * no array        -> runs after EVERY render
  //  * []              -> runs once (plus once more in StrictMode dev, on purpose)
  //  * missing deps    -> stale values inside the effect
  //  * setState with an object dependency defined inline -> new reference every render -> infinite loop
  if (error) return <p role="alert">{error}</p>;
  return <p>{user ? user.name : "Loading..."}</p>;
}

export function DocumentTitle({ title }: { title: string }) {
  useEffect(() => {
    const previous = document.title;
    document.title = title;
    return () => {
      document.title = previous;
    };
  }, [title]);
  return null;
}

// useLayoutEffect: measure/mutate the DOM before paint (no visible flicker)
export function MeasuredBox({ children }: { children: ReactNode }) {
  const boxRef = useRef<HTMLDivElement>(null);
  const [height, setHeight] = useState(0);

  useLayoutEffect(() => {
    if (boxRef.current) setHeight(boxRef.current.getBoundingClientRect().height);
  }, [children]);

  return (
    <div ref={boxRef}>
      {children}
      <small>height: {Math.round(height)}px</small>
    </div>
  );
}

// ---------------------------------------------------------------- useRef

export function StopwatchAndFocus() {
  const inputRef = useRef<HTMLInputElement>(null);          // DOM handle
  const timerRef = useRef<ReturnType<typeof setInterval>>(undefined); // mutable value, no re-render
  const renderCount = useRef(0);                            // changing this does NOT re-render
  const [seconds, setSeconds] = useState(0);

  renderCount.current += 1;

  const start = () => {
    if (timerRef.current) return;
    timerRef.current = setInterval(() => setSeconds((s) => s + 1), 1000);
  };

  const stop = () => {
    clearInterval(timerRef.current);
    timerRef.current = undefined;
  };

  useEffect(() => stop, []); // clear the timer on unmount: a classic memory-leak question

  return (
    <div>
      <input ref={inputRef} />
      <button onClick={() => inputRef.current?.focus()}>Focus</button>
      <button onClick={start}>Start</button>
      <button onClick={stop}>Stop</button>
      <span>
        {seconds}s / renders: {renderCount.current}
      </span>
    </div>
  );
}

// ---------------------------------------------------------------- useReducer (state machine)

export interface CartItem {
  sku: string;
  qty: number;
  price: number;
}

export interface CartState {
  items: CartItem[];
  coupon: string | null;
}

export type CartAction =
  | { type: "add"; item: CartItem }
  | { type: "remove"; sku: string }
  | { type: "setQty"; sku: string; qty: number }
  | { type: "applyCoupon"; code: string }
  | { type: "clear" };

export function cartReducer(state: CartState, action: CartAction): CartState {
  switch (action.type) {
    case "add": {
      const existing = state.items.find((i) => i.sku === action.item.sku);
      return existing
        ? { ...state, items: state.items.map((i) => (i.sku === action.item.sku ? { ...i, qty: i.qty + action.item.qty } : i)) }
        : { ...state, items: [...state.items, action.item] };
    }
    case "remove":
      return { ...state, items: state.items.filter((i) => i.sku !== action.sku) };
    case "setQty":
      return { ...state, items: state.items.map((i) => (i.sku === action.sku ? { ...i, qty: action.qty } : i)) };
    case "applyCoupon":
      return { ...state, coupon: action.code };
    case "clear":
      return { items: [], coupon: null };
    default: {
      const exhaustive: never = action;   // compile error if an action type is unhandled
      return exhaustive;
    }
  }
}

export const cartTotal = (state: CartState): number =>
  state.items.reduce((sum, i) => sum + i.price * i.qty, 0) * (state.coupon === "SAVE10" ? 0.9 : 1);

export function Cart() {
  const [state, dispatch] = useReducer(cartReducer, { items: [], coupon: null });
  return (
    <div>
      <button onClick={() => dispatch({ type: "add", item: { sku: "BOOK", qty: 1, price: 20 } })}>Add book</button>
      <button onClick={() => dispatch({ type: "clear" })}>Clear</button>
      <output>{cartTotal(state).toFixed(2)}</output>
    </div>
  );
}

// ---------------------------------------------------------------- useContext

interface Theme {
  mode: "light" | "dark";
  toggle: () => void;
}

const ThemeContext = createContext<Theme | null>(null);

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [mode, setMode] = useState<"light" | "dark">("light");

  // useMemo here matters: without it, every provider render creates a new object
  // and re-renders EVERY consumer.
  const value = useMemo<Theme>(
    () => ({ mode, toggle: () => setMode((m) => (m === "light" ? "dark" : "light")) }),
    [mode],
  );

  return <ThemeContext value={value}>{children}</ThemeContext>;   // React 19: <Context> works as the provider
}

export function useTheme(): Theme {
  const theme = useContext(ThemeContext);
  if (!theme) throw new Error("useTheme must be used inside <ThemeProvider>"); // fail loudly, and narrow the type
  return theme;
}

// ---------------------------------------------------------------- useMemo / useCallback / useId

interface Product {
  id: number;
  name: string;
  price: number;
}

export function ProductSearch({ products, onPick }: { products: Product[]; onPick: (p: Product) => void }) {
  const [query, setQuery] = useState("");
  const inputId = useId();   // stable unique id for label/input pairs (SSR-safe)

  // useMemo: skip the filter+sort unless the inputs actually changed
  const visible = useMemo(
    () => products.filter((p) => p.name.toLowerCase().includes(query.toLowerCase())).sort((a, b) => a.price - b.price),
    [products, query],
  );

  // useCallback: keep the same function identity so memoized children don't re-render
  const handlePick = useCallback((product: Product) => onPick(product), [onPick]);

  return (
    <div>
      <label htmlFor={inputId}>Search</label>
      <input id={inputId} value={query} onChange={(e) => setQuery(e.target.value)} />
      <ul>
        {visible.map((product) => (
          <li key={product.id}>
            <button onClick={() => handlePick(product)}>{product.name}</button>
          </li>
        ))}
      </ul>
    </div>
  );
}
