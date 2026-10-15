/**
 * Custom hooks: the most common "write this live" React interview task.
 *
 * REMEMBER:
 *  * A custom hook is just a function starting with `use` that calls other hooks.
 *  * It shares LOGIC, not state: every component that calls it gets its own state.
 *  * Return a tuple for two values ([value, setValue]) or an object for more (named, extensible).
 *  * Clean up everything you start (timers, listeners, subscriptions, requests).
 */
import { useCallback, useEffect, useReducer, useRef, useState, useSyncExternalStore } from "react";

// ---------------------------------------------------------------- useToggle / usePrevious / useCounter

export function useToggle(initial = false): [boolean, () => void, (value: boolean) => void] {
  const [value, setValue] = useState(initial);
  const toggle = useCallback(() => setValue((v) => !v), []);
  return [value, toggle, setValue];
}

export function usePrevious<T>(value: T): T | undefined {
  const ref = useRef<T | undefined>(undefined);
  useEffect(() => {
    ref.current = value;    // runs AFTER render, so during render ref.current is still the previous value
  }, [value]);
  return ref.current;
}

// ---------------------------------------------------------------- useDebounce / useThrottle

export function useDebouncedValue<T>(value: T, delayMs = 300): T {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(timer);   // each keystroke cancels the previous timer
  }, [value, delayMs]);

  return debounced;
}

/** Keeps the latest callback in a ref so the returned function identity stays stable. */
export function useEventCallback<Args extends unknown[], R>(fn: (...args: Args) => R): (...args: Args) => R {
  const ref = useRef(fn);
  useEffect(() => {
    ref.current = fn;
  }, [fn]);
  return useCallback((...args: Args) => ref.current(...args), []);
}

// ---------------------------------------------------------------- useInterval (Dan Abramov's pattern)

export function useInterval(callback: () => void, delayMs: number | null): void {
  const savedCallback = useRef(callback);

  useEffect(() => {
    savedCallback.current = callback;   // always call the freshest callback...
  }, [callback]);

  useEffect(() => {
    if (delayMs === null) return;       // ...without restarting the interval on every render
    const id = setInterval(() => savedCallback.current(), delayMs);
    return () => clearInterval(id);
  }, [delayMs]);
}

// ---------------------------------------------------------------- useLocalStorage

export function useLocalStorage<T>(key: string, initialValue: T): [T, (value: T | ((current: T) => T)) => void] {
  const [stored, setStored] = useState<T>(() => {
    try {
      const raw = window.localStorage.getItem(key);
      return raw ? (JSON.parse(raw) as T) : initialValue;    // lazy init: read storage once
    } catch {
      return initialValue;                                    // private mode / bad JSON
    }
  });

  const setValue = useCallback(
    (value: T | ((current: T) => T)) => {
      setStored((current) => {
        const next = value instanceof Function ? value(current) : value;
        try {
          window.localStorage.setItem(key, JSON.stringify(next));
        } catch {
          /* storage full or blocked: keep the value in memory */
        }
        return next;
      });
    },
    [key],
  );

  return [stored, setValue];
}

// ---------------------------------------------------------------- useAsync / useFetch with a state machine

export type AsyncState<T> =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "success"; data: T }
  | { status: "error"; error: Error };

type AsyncAction<T> = { type: "start" } | { type: "success"; data: T } | { type: "error"; error: Error };

function asyncReducer<T>(_state: AsyncState<T>, action: AsyncAction<T>): AsyncState<T> {
  switch (action.type) {
    case "start":
      return { status: "loading" };
    case "success":
      return { status: "success", data: action.data };
    case "error":
      return { status: "error", error: action.error };
  }
}

export function useAsync<T>(fn: (signal: AbortSignal) => Promise<T>, deps: unknown[]): AsyncState<T> {
  const [state, dispatch] = useReducer(asyncReducer<T>, { status: "idle" } as AsyncState<T>);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;                       // guard against setting state after unmount
    dispatch({ type: "start" });

    fn(controller.signal)
      .then((data) => active && dispatch({ type: "success", data }))
      .catch((err: unknown) => {
        if (!active) return;
        if (err instanceof Error && err.name === "AbortError") return;
        dispatch({ type: "error", error: err instanceof Error ? err : new Error(String(err)) });
      });

    return () => {
      active = false;
      controller.abort();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  return state;
}

// ---------------------------------------------------------------- useOnClickOutside (dropdowns, modals)

export function useOnClickOutside<T extends HTMLElement>(ref: React.RefObject<T | null>, handler: () => void): void {
  useEffect(() => {
    const listener = (event: MouseEvent | TouchEvent) => {
      if (ref.current && !ref.current.contains(event.target as Node)) handler();
    };
    document.addEventListener("mousedown", listener);
    document.addEventListener("touchstart", listener);
    return () => {
      document.removeEventListener("mousedown", listener);   // ALWAYS remove listeners
      document.removeEventListener("touchstart", listener);
    };
  }, [ref, handler]);
}

// ---------------------------------------------------------------- useMediaQuery with useSyncExternalStore

/** useSyncExternalStore is the correct way to subscribe to browser/external state (SSR-safe, no tearing). */
export function useMediaQuery(query: string): boolean {
  const subscribe = useCallback(
    (onChange: () => void) => {
      const list = window.matchMedia(query);
      list.addEventListener("change", onChange);
      return () => list.removeEventListener("change", onChange);
    },
    [query],
  );

  return useSyncExternalStore(
    subscribe,
    () => window.matchMedia(query).matches,   // client snapshot
    () => false,                              // server snapshot (SSR)
  );
}

// ---------------------------------------------------------------- example usage

export function SearchBox({ onSearch }: { onSearch: (term: string) => void }) {
  const [term, setTerm] = useState("");
  const debouncedTerm = useDebouncedValue(term, 300);   // only fires 300ms after typing stops
  const previousTerm = usePrevious(debouncedTerm);
  const search = useEventCallback(onSearch);

  useEffect(() => {
    if (debouncedTerm && debouncedTerm !== previousTerm) search(debouncedTerm);
  }, [debouncedTerm, previousTerm, search]);

  return <input value={term} onChange={(e) => setTerm(e.target.value)} placeholder="Search" />;
}
