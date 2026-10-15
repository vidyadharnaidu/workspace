/**
 * React performance: what actually causes re-renders and how to fix slowness.
 *
 * THE MENTAL MODEL:
 *  A component re-renders when (1) its state changes, (2) its parent re-renders, or (3) context it reads changes.
 *  A re-render is NOT automatically a problem - React diffs and usually touches little DOM.
 *  Optimize only what you measured (React DevTools Profiler).
 *
 * ORDER OF FIXES (cheapest and most effective first):
 *  1. Don't put state higher than it needs to be (move state down / split components).
 *  2. Pass children instead of re-rendering subtrees ("children as a prop" keeps them stable).
 *  3. Stable keys; avoid index keys in dynamic lists.
 *  4. memo + useCallback/useMemo at real boundaries (big lists, expensive children).
 *  5. Virtualize long lists; code-split routes with lazy + Suspense.
 *  6. useTransition / useDeferredValue to keep typing responsive.
 *
 * NOTE: React 19's compiler can auto-memoize; these hooks are still what interviews ask about.
 */
import {
  Suspense,
  lazy,
  memo,
  useCallback,
  useDeferredValue,
  useMemo,
  useState,
  useTransition,
  type ReactNode,
} from "react";

interface Row {
  id: number;
  label: string;
  price: number;
}

// ---------------------------------------------------------------- memo + stable props

/** memo skips the re-render when props are SHALLOW-equal. It's useless if props are new objects every render. */
export const ExpensiveRow = memo(function ExpensiveRow({ row, onPick }: { row: Row; onPick: (id: number) => void }) {
  return (
    <li>
      <button onClick={() => onPick(row.id)}>
        {row.label} - {row.price}
      </button>
    </li>
  );
});

export function RowList({ rows, onPick }: { rows: Row[]; onPick: (id: number) => void }) {
  // WRONG: onPick={(id) => onPick(id)} or style={{...}} creates a new value every render -> memo never helps.
  return (
    <ul>
      {rows.map((row) => (
        <ExpensiveRow key={row.id} row={row} onPick={onPick} />
      ))}
    </ul>
  );
}

// custom comparison: only re-render when the fields we render actually change
export const PriceTag = memo(
  function PriceTag({ row }: { row: Row }) {
    return <span>{row.price.toFixed(2)}</span>;
  },
  (prev, next) => prev.row.price === next.row.price,
);

// ---------------------------------------------------------------- move state down instead of memoizing everything

function SearchInput({ onChange }: { onChange: (value: string) => void }) {
  const [text, setText] = useState("");   // state lives HERE, so typing doesn't re-render the whole page
  return (
    <input
      value={text}
      onChange={(event) => {
        setText(event.target.value);
        onChange(event.target.value);
      }}
    />
  );
}

// ---------------------------------------------------------------- children as a prop keeps subtrees stable

/** `children` is created by the PARENT, so re-rendering this component does not re-render them. */
export function Panel({ title, children }: { title: string; children: ReactNode }) {
  const [collapsed, setCollapsed] = useState(false);
  return (
    <section>
      <button onClick={() => setCollapsed((c) => !c)}>{title}</button>
      {!collapsed && children}
    </section>
  );
}

// ---------------------------------------------------------------- useMemo for expensive work, useCallback for identity

export function ProductTable({ rows, onPick }: { rows: Row[]; onPick: (id: number) => void }) {
  const [query, setQuery] = useState("");
  const [isPending, startTransition] = useTransition();

  // useDeferredValue: keep the input responsive while the big list lags behind
  const deferredQuery = useDeferredValue(query);

  const visible = useMemo(() => {
    // pretend this is expensive: filtering + sorting thousands of rows
    return rows.filter((r) => r.label.toLowerCase().includes(deferredQuery.toLowerCase())).sort((a, b) => a.price - b.price);
  }, [rows, deferredQuery]);

  const total = useMemo(() => visible.reduce((sum, r) => sum + r.price, 0), [visible]);

  const handlePick = useCallback((id: number) => onPick(id), [onPick]);

  const sortExpensively = () => {
    // useTransition marks an update as non-urgent: React keeps the UI interactive
    startTransition(() => {
      setQuery((current) => current.trim());
    });
  };

  return (
    <div>
      <SearchInput onChange={setQuery} />
      <button onClick={sortExpensively}>Tidy</button>
      {isPending && <small>updating...</small>}
      <output>{total.toFixed(2)}</output>
      <RowList rows={visible} onPick={handlePick} />
    </div>
  );
}

// ---------------------------------------------------------------- code splitting

// lazy + Suspense: the chunk is downloaded only when the component first renders
const HeavyChart = lazy(async () => ({ default: function Chart() { return <div>chart</div>; } }));

export function Dashboard({ showChart }: { showChart: boolean }) {
  return (
    <Suspense fallback={<p>Loading chart...</p>}>
      {showChart && <HeavyChart />}
    </Suspense>
  );
}

// ---------------------------------------------------------------- simple windowing (virtualization idea)

export function VirtualList({ rows, height = 300, rowHeight = 30 }: { rows: Row[]; height?: number; rowHeight?: number }) {
  const [scrollTop, setScrollTop] = useState(0);
  const firstVisible = Math.floor(scrollTop / rowHeight);
  const visibleCount = Math.ceil(height / rowHeight) + 1;            // +1 buffer row
  const slice = rows.slice(firstVisible, firstVisible + visibleCount);

  // Render ~10 rows instead of 10,000: this is what react-window/virtuoso do for you.
  return (
    <div style={{ height, overflowY: "auto" }} onScroll={(e) => setScrollTop(e.currentTarget.scrollTop)}>
      <div style={{ height: rows.length * rowHeight, position: "relative" }}>
        {slice.map((row, index) => (
          <div
            key={row.id}
            style={{ position: "absolute", top: (firstVisible + index) * rowHeight, height: rowHeight }}
          >
            {row.label}
          </div>
        ))}
      </div>
    </div>
  );
}

/**
 * ANTI-PATTERNS TO NAME IN AN INTERVIEW
 *  * useMemo/useCallback on everything: they cost memory and comparisons, and hide the real problem.
 *  * Objects/arrays/functions created inline and passed to memoized children.
 *  * One giant context holding unrelated values -> split contexts, or use a store with selectors.
 *  * Derived data stored in state and synced with useEffect -> compute it during render instead.
 *  * Index keys in lists that reorder.
 *  * Fetching in a child that a parent also re-renders constantly (waterfalls) -> lift or use a data library.
 */
