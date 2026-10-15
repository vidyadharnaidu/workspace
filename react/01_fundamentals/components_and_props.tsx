/**
 * React fundamentals: components, props, state, lists, events, conditional rendering.
 * React 19 + TypeScript.
 *
 * REMEMBER:
 *  * A component is a function that takes props and returns JSX. Same props -> same output (pure).
 *  * PROPS ARE READ-ONLY. To change something, call a state setter; never mutate props or state.
 *  * KEYS must be stable, unique ids - NOT the array index when the list can reorder/insert/delete.
 *  * Lift state up to the closest common parent when two components need the same data.
 *  * Children are just a prop: composition ("slots") beats inheritance in React.
 *  * In React 19 `ref` is a normal prop for function components - forwardRef is no longer needed.
 */
import { useState, type ReactNode, type ChangeEvent, type FormEvent, type Ref } from "react";

// ---------------------------------------------------------------- typing props

interface BadgeProps {
  label: string;
  tone?: "info" | "warning" | "danger";   // literal union instead of `string`
  onDismiss?: () => void;                 // optional callback
}

export function Badge({ label, tone = "info", onDismiss }: BadgeProps) {
  return (
    <span className={`badge badge--${tone}`}>
      {label}
      {/* render the button only when a handler was passed */}
      {onDismiss && (
        <button type="button" aria-label={`dismiss ${label}`} onClick={onDismiss}>
          x
        </button>
      )}
    </span>
  );
}

// ---------------------------------------------------------------- children & composition ("slots")

interface CardProps {
  title: string;
  footer?: ReactNode;
  children: ReactNode;
}

export function Card({ title, footer, children }: CardProps) {
  return (
    <section className="card">
      <h3>{title}</h3>
      <div className="card__body">{children}</div>
      {footer ? <footer>{footer}</footer> : null}
    </section>
  );
}

// ---------------------------------------------------------------- lists and keys

export interface Todo {
  id: string;
  title: string;
  done: boolean;
}

interface TodoListProps {
  todos: Todo[];
  onToggle: (id: string) => void;
}

export function TodoList({ todos, onToggle }: TodoListProps) {
  if (todos.length === 0) return <p>Nothing to do.</p>;   // empty state first: fewer branches below

  return (
    <ul>
      {todos.map((todo) => (
        // key={todo.id}, never key={index}: with an index, React reuses the wrong DOM node
        // when items are inserted/removed, which loses input state and breaks animations.
        <li key={todo.id}>
          <label>
            <input type="checkbox" checked={todo.done} onChange={() => onToggle(todo.id)} />
            <span style={{ textDecoration: todo.done ? "line-through" : "none" }}>{todo.title}</span>
          </label>
        </li>
      ))}
    </ul>
  );
}

// ---------------------------------------------------------------- state, events, controlled input

export function TodoApp({ initialTodos = [] as Todo[] }) {
  const [todos, setTodos] = useState<Todo[]>(initialTodos);
  const [draft, setDraft] = useState("");

  // IMMUTABLE updates: create new arrays/objects so React sees a new reference and re-renders
  const addTodo = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const title = draft.trim();
    if (!title) return;
    setTodos((current) => [...current, { id: crypto.randomUUID(), title, done: false }]);
    setDraft("");
  };

  const toggle = (id: string) =>
    setTodos((current) => current.map((t) => (t.id === id ? { ...t, done: !t.done } : t)));

  const remaining = todos.filter((t) => !t.done).length;   // DERIVED state: compute, don't store

  return (
    <Card title="Todos" footer={<small>{remaining} remaining</small>}>
      <form onSubmit={addTodo}>
        {/* CONTROLLED input: value comes from state, onChange writes back to state */}
        <input
          value={draft}
          onChange={(event: ChangeEvent<HTMLInputElement>) => setDraft(event.target.value)}
          placeholder="What needs doing?"
        />
        <button type="submit" disabled={!draft.trim()}>
          Add
        </button>
      </form>
      <TodoList todos={todos} onToggle={toggle} />
    </Card>
  );
}

// ---------------------------------------------------------------- conditional rendering

interface StatusProps {
  status: "idle" | "loading" | "error" | "ready";
  error?: string;
  children: ReactNode;
}

export function StatusView({ status, error, children }: StatusProps) {
  // Early returns read better than nested ternaries
  if (status === "loading") return <p role="status">Loading...</p>;
  if (status === "error") return <p role="alert">{error ?? "Something went wrong"}</p>;
  if (status === "idle") return null;
  return <>{children}</>;                     // Fragment: no extra DOM node
}

// TRAP: `count && <Badge .../>` renders "0" when count is 0, because 0 is falsy but renderable.
export function CountBadge({ count }: { count: number }) {
  return <>{count > 0 ? <Badge label={`${count} new`} /> : null}</>;
}

// ---------------------------------------------------------------- React 19: ref is a normal prop

interface TextFieldProps {
  label: string;
  ref?: Ref<HTMLInputElement>;              // React 19: no forwardRef wrapper needed
  defaultValue?: string;
}

export function TextField({ label, ref, defaultValue }: TextFieldProps) {
  return (
    <label>
      {label}
      {/* UNCONTROLLED input: the DOM keeps the value, read it through the ref when submitting */}
      <input ref={ref} defaultValue={defaultValue} />
    </label>
  );
}

// ---------------------------------------------------------------- props spreading & default props

type ButtonProps = {
  variant?: "primary" | "ghost";
} & React.ComponentPropsWithoutRef<"button">;   // inherit every native button prop, fully typed

export function Button({ variant = "primary", ...rest }: ButtonProps) {
  return <button className={`btn btn--${variant}`} {...rest} />;
}
