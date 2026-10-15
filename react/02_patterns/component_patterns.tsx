/**
 * Component patterns interviewers ask you to name and use.
 *
 * WHEN TO USE WHAT:
 *  Composition / children ... default answer to "how do I reuse layout?"
 *  Custom hook .............. reuse LOGIC (this replaced most HOC/render-prop use)
 *  Compound components ...... a family that shares implicit state (<Tabs><Tab/></Tabs>)
 *  Render prop / children fn  caller decides the markup, component owns the behaviour
 *  HOC ...................... wrap a component to add props (legacy; still in older codebases)
 *  Container / presentational separate data fetching from rendering
 *  Error boundary ........... the only remaining REQUIRED class component
 *  Portal ................... render outside the parent DOM node (modals, tooltips)
 */
import {
  Component,
  createContext,
  useContext,
  useId,
  useMemo,
  useState,
  type ComponentType,
  type ErrorInfo,
  type ReactNode,
} from "react";

// ---------------------------------------------------------------- compound components

interface TabsContextValue {
  activeId: string;
  select: (id: string) => void;
  baseId: string;
}

const TabsContext = createContext<TabsContextValue | null>(null);

function useTabsContext(component: string): TabsContextValue {
  const context = useContext(TabsContext);
  if (!context) throw new Error(`<${component}> must be rendered inside <Tabs>`);
  return context;
}

export function Tabs({ defaultId, children }: { defaultId: string; children: ReactNode }) {
  const [activeId, setActiveId] = useState(defaultId);
  const baseId = useId();
  const value = useMemo(() => ({ activeId, select: setActiveId, baseId }), [activeId, baseId]);
  return <TabsContext value={value}>{children}</TabsContext>;
}

// Attaching sub-components gives the <Tabs.List> API without prop drilling
Tabs.List = function TabList({ children }: { children: ReactNode }) {
  return (
    <div role="tablist" className="tabs__list">
      {children}
    </div>
  );
};

Tabs.Tab = function Tab({ id, children }: { id: string; children: ReactNode }) {
  const { activeId, select, baseId } = useTabsContext("Tabs.Tab");
  const selected = activeId === id;
  return (
    <button
      role="tab"
      id={`${baseId}-tab-${id}`}
      aria-selected={selected}
      aria-controls={`${baseId}-panel-${id}`}
      onClick={() => select(id)}
    >
      {children}
    </button>
  );
};

Tabs.Panel = function TabPanel({ id, children }: { id: string; children: ReactNode }) {
  const { activeId, baseId } = useTabsContext("Tabs.Panel");
  if (activeId !== id) return null;
  return (
    <div role="tabpanel" id={`${baseId}-panel-${id}`} aria-labelledby={`${baseId}-tab-${id}`}>
      {children}
    </div>
  );
};

// ---------------------------------------------------------------- render props / children as a function

interface ToggleRenderProps {
  children: (state: { on: boolean; toggle: () => void }) => ReactNode;
  initial?: boolean;
}

export function Toggle({ children, initial = false }: ToggleRenderProps) {
  const [on, setOn] = useState(initial);
  return <>{children({ on, toggle: () => setOn((value) => !value) })}</>;
}

export function RenderPropExample() {
  return (
    <Toggle>
      {({ on, toggle }) => (
        <button onClick={toggle} aria-pressed={on}>
          {on ? "ON" : "OFF"}
        </button>
      )}
    </Toggle>
  );
}

// ---------------------------------------------------------------- higher-order component (legacy pattern)

export function withLogging<P extends object>(Wrapped: ComponentType<P>, name: string): ComponentType<P> {
  function WithLogging(props: P) {
    // In real code this would log mount/update; a custom hook is usually the better modern answer.
    return <Wrapped {...props} />;
  }
  WithLogging.displayName = `withLogging(${name})`;   // keeps React DevTools readable
  return WithLogging;
}

// ---------------------------------------------------------------- container / presentational

interface Order {
  id: string;
  total: number;
}

/** Presentational: pure, easy to test and to reuse; no data fetching. */
export function OrderTable({ orders, onSelect }: { orders: Order[]; onSelect?: (id: string) => void }) {
  return (
    <table>
      <tbody>
        {orders.map((order) => (
          <tr key={order.id} onClick={() => onSelect?.(order.id)}>
            <td>{order.id}</td>
            <td>{order.total.toFixed(2)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

/** Container: owns data/state and renders the presentational component. */
export function OrderTableContainer({ orders }: { orders: Order[] }) {
  const [selected, setSelected] = useState<string | null>(null);
  const sorted = useMemo(() => [...orders].sort((a, b) => b.total - a.total), [orders]);
  return (
    <>
      <OrderTable orders={sorted} onSelect={setSelected} />
      {selected && <p>Selected: {selected}</p>}
    </>
  );
}

// ---------------------------------------------------------------- error boundary (must be a class)

interface ErrorBoundaryProps {
  fallback: (error: Error, reset: () => void) => ReactNode;
  children: ReactNode;
}

interface ErrorBoundaryState {
  error: Error | null;
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  override state: ErrorBoundaryState = { error: null };

  // Render the fallback on the next render
  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { error };
  }

  // Side effects: report to Sentry/logs
  override componentDidCatch(error: Error, info: ErrorInfo): void {
    console.error("ErrorBoundary caught", error.message, info.componentStack);
  }

  override render(): ReactNode {
    if (this.state.error) return this.props.fallback(this.state.error, () => this.setState({ error: null }));
    return this.props.children;
  }
}
// NOTE: error boundaries do NOT catch errors in event handlers, async code, SSR, or in the boundary itself.
// Use try/catch for those.

// ---------------------------------------------------------------- controlled vs uncontrolled component API

interface RatingProps {
  value?: number;          // provided -> controlled
  defaultValue?: number;   // provided -> uncontrolled
  onChange?: (value: number) => void;
  max?: number;
}

export function Rating({ value, defaultValue = 0, onChange, max = 5 }: RatingProps) {
  const [internal, setInternal] = useState(defaultValue);
  const isControlled = value !== undefined;         // the standard library-component pattern
  const current = isControlled ? value : internal;

  const select = (next: number) => {
    if (!isControlled) setInternal(next);
    onChange?.(next);
  };

  return (
    <div role="radiogroup" aria-label="rating">
      {Array.from({ length: max }, (_, i) => i + 1).map((star) => (
        <button key={star} role="radio" aria-checked={star === current} onClick={() => select(star)}>
          {star <= current ? "*" : "-"}
        </button>
      ))}
    </div>
  );
}

// ---------------------------------------------------------------- putting it together

export function PatternsDemo() {
  return (
    <ErrorBoundary fallback={(error, reset) => (
      <div role="alert">
        {error.message} <button onClick={reset}>Retry</button>
      </div>
    )}>
      <Tabs defaultId="orders">
        <Tabs.List>
          <Tabs.Tab id="orders">Orders</Tabs.Tab>
          <Tabs.Tab id="settings">Settings</Tabs.Tab>
        </Tabs.List>
        <Tabs.Panel id="orders">
          <OrderTableContainer orders={[{ id: "A1", total: 25 }, { id: "B2", total: 99 }]} />
        </Tabs.Panel>
        <Tabs.Panel id="settings">
          <RenderPropExample />
          <Rating defaultValue={3} />
        </Tabs.Panel>
      </Tabs>
    </ErrorBoundary>
  );
}
