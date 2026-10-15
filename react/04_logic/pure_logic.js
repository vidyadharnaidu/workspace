/**
 * The parts of a React app you can test WITHOUT React: reducers, validation, selectors, immutable updates.
 *
 * Run: node react/04_logic/pure_logic.js
 *
 * WHY THIS FILE EXISTS: in an interview, pulling logic out of components is the strongest signal you can give.
 * Pure functions are trivial to test, easy to reason about, and they make components thin.
 * The same reducer/validator appears in the .tsx files; here it runs and is checked.
 */

function check(label, actual, expected) {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
  console.log(`${label.padEnd(36)} ${a}`);
}

// ---------------------------------------------------------------- cart reducer (same as hooks_basics.tsx)

function cartReducer(state, action) {
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
    default:
      throw new Error(`unhandled action: ${action.type}`);
  }
}

const cartTotal = (state) =>
  state.items.reduce((sum, i) => sum + i.price * i.qty, 0) * (state.coupon === "SAVE10" ? 0.9 : 1);

// ---------------------------------------------------------------- form validation (same as forms_and_data.tsx)

function validateSignup(values) {
  const errors = {};
  if (!values.email.trim()) errors.email = "Email is required";
  else if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(values.email)) errors.email = "Enter a valid email";
  if (values.password.length < 8) errors.password = "At least 8 characters";
  else if (!/\d/.test(values.password)) errors.password = "Include a number";
  const age = Number(values.age);
  if (!values.age) errors.age = "Age is required";
  else if (!Number.isInteger(age) || age < 18 || age > 120) errors.age = "Age must be 18-120";
  return errors;
}

// ---------------------------------------------------------------- what React.memo actually compares

/** memo/PureComponent use a SHALLOW comparison - this is why inline objects defeat them. */
function shallowEqual(a, b) {
  if (Object.is(a, b)) return true;
  if (typeof a !== "object" || typeof b !== "object" || a === null || b === null) return false;
  const keysA = Object.keys(a);
  const keysB = Object.keys(b);
  return keysA.length === keysB.length && keysA.every((key) => Object.is(a[key], b[key]));
}

// ---------------------------------------------------------------- immutable update helpers

const updateItem = (list, id, patch) => list.map((item) => (item.id === id ? { ...item, ...patch } : item));
const removeItem = (list, id) => list.filter((item) => item.id !== id);
const insertAt = (list, index, item) => [...list.slice(0, index), item, ...list.slice(index)];
const toggleIn = (list, value) => (list.includes(value) ? list.filter((v) => v !== value) : [...list, value]);

/** Nested update without mutating (what an immer-free reducer looks like). */
const setNested = (state, path, value) => {
  const [head, ...rest] = path;
  return { ...state, [head]: rest.length ? setNested(state[head] ?? {}, rest, value) : value };
};

// ---------------------------------------------------------------- selectors / derived data

const selectVisibleTodos = (todos, filter) =>
  filter === "all" ? todos : todos.filter((t) => (filter === "done" ? t.done : !t.done));

const selectStats = (todos) => ({
  total: todos.length,
  done: todos.filter((t) => t.done).length,
  remaining: todos.filter((t) => !t.done).length,
});

// ---------------------------------------------------------------- pagination math

const paginate = (totalItems, pageSize, currentPage) => ({
  totalPages: Math.max(1, Math.ceil(totalItems / pageSize)),
  offset: (currentPage - 1) * pageSize,
  hasPrevious: currentPage > 1,
  hasNext: currentPage < Math.ceil(totalItems / pageSize),
});

// ---------------------------------------------------------------- debounce (useDebouncedValue without React)

function debounce(fn, waitMs, scheduler) {
  let timer = null;
  return (...args) => {
    if (timer !== null) scheduler.clear(timer);
    timer = scheduler.set(() => fn(...args), waitMs);
  };
}

function createFakeScheduler() {
  let time = 0;
  let nextId = 1;
  const tasks = new Map();
  return {
    set(fn, delay) {
      tasks.set(nextId, { fn, at: time + delay });
      return nextId++;
    },
    clear(id) {
      tasks.delete(id);
    },
    advance(ms) {
      time += ms;
      for (const [id, task] of [...tasks].sort((a, b) => a[1].at - b[1].at)) {
        if (task.at <= time) {
          tasks.delete(id);
          task.fn();
        }
      }
    },
  };
}

// ---------------------------------------------------------------- stale-response guard (the useEffect race)

/** Simulates two overlapping requests: only the newest result may win. */
async function loadLatest(requests) {
  let latestId = 0;
  let result = null;
  await Promise.all(
    requests.map(async ({ id, delay, value }) => {
      latestId = Math.max(latestId, id);
      await new Promise((resolve) => setTimeout(resolve, delay));
      if (id === latestId) result = value;   // ignore anything that is no longer the latest request
    }),
  );
  return result;
}

async function main() {
  let cart = { items: [], coupon: null };
  cart = cartReducer(cart, { type: "add", item: { sku: "BOOK", qty: 1, price: 20 } });
  cart = cartReducer(cart, { type: "add", item: { sku: "BOOK", qty: 2, price: 20 } });   // merges quantity
  cart = cartReducer(cart, { type: "add", item: { sku: "PEN", qty: 4, price: 2 } });
  check("reducer merges same sku", cart.items, [{ sku: "BOOK", qty: 3, price: 20 }, { sku: "PEN", qty: 4, price: 2 }]);
  check("cart total", cartTotal(cart), 68);
  cart = cartReducer(cart, { type: "applyCoupon", code: "SAVE10" });
  check("coupon applies 10%", Number(cartTotal(cart).toFixed(2)), 61.2);
  check("reducer is immutable", cartReducer(cart, { type: "clear" }) !== cart, true);

  check("validation: empty form", Object.keys(validateSignup({ email: "", password: "", age: "" })),
        ["email", "password", "age"]);
  check("validation: bad email/password", validateSignup({ email: "nope", password: "short", age: "30" }),
        { email: "Enter a valid email", password: "At least 8 characters" });
  check("validation: password needs a digit", validateSignup({ email: "a@b.co", password: "password", age: "30" }),
        { password: "Include a number" });
  check("validation: valid form", validateSignup({ email: "a@b.co", password: "passw0rd", age: "30" }), {});

  check("shallowEqual: same values", shallowEqual({ a: 1, b: "x" }, { a: 1, b: "x" }), true);
  check("shallowEqual: nested object breaks memo", shallowEqual({ style: { color: "red" } }, { style: { color: "red" } }), false);
  check("shallowEqual: inline arrow breaks memo", shallowEqual({ onClick: () => {} }, { onClick: () => {} }), false);

  const todos = [
    { id: "1", title: "write", done: false },
    { id: "2", title: "review", done: true },
  ];
  check("updateItem", updateItem(todos, "1", { done: true })[0], { id: "1", title: "write", done: true });
  check("original unchanged", todos[0].done, false);
  check("removeItem / insertAt / toggleIn",
        [removeItem(todos, "1").length, insertAt([1, 3], 1, 2), toggleIn(["a"], "a"), toggleIn(["a"], "b")],
        [1, [1, 2, 3], [], ["a", "b"]]);
  check("setNested", setNested({ user: { address: { city: "NY" }, name: "ann" } }, ["user", "address", "city"], "Berlin"),
        { user: { address: { city: "Berlin" }, name: "ann" } });

  check("selectVisibleTodos", [selectVisibleTodos(todos, "all").length, selectVisibleTodos(todos, "done").length,
                               selectVisibleTodos(todos, "active").length], [2, 1, 1]);
  check("selectStats", selectStats(todos), { total: 2, done: 1, remaining: 1 });

  check("paginate", paginate(95, 10, 3), { totalPages: 10, offset: 20, hasPrevious: true, hasNext: true });
  check("paginate empty", paginate(0, 10, 1), { totalPages: 1, offset: 0, hasPrevious: false, hasNext: false });

  const scheduler = createFakeScheduler();
  const searches = [];
  const search = debounce((term) => searches.push(term), 300, scheduler);
  search("r");
  search("re");
  scheduler.advance(100);
  search("rea");
  scheduler.advance(300);
  check("debounce fires once with the last value", searches, ["rea"]);

  const winner = await loadLatest([
    { id: 1, delay: 30, value: "slow old request" },
    { id: 2, delay: 5, value: "fast new request" },
  ]);
  check("stale response is ignored", winner, "fast new request");

  console.log("\nall checks passed");
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
