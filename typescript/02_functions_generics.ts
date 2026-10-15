/**
 * Functions, generics and the generic utilities interviewers ask you to type.
 *
 * Run:   node typescript/02_functions_generics.ts
 *
 * REMEMBER:
 *  * A generic is a TYPE PARAMETER: it links the input type to the output type instead of using `any`.
 *  * Constrain it: <T extends object>, <K extends keyof T> - then the compiler knows what you can do.
 *  * `keyof T` = union of keys. `T[K]` = indexed access (the value type). `typeof value` = the value's type.
 *  * Overloads describe several call shapes; the implementation signature is NOT callable from outside.
 *  * Prefer generics over `any`; prefer `unknown` over `any` when the type is genuinely unknown.
 */

function check(label: string, actual: unknown, expected: unknown): void {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
  console.log(`${label.padEnd(34)} ${a}`);
}

// ---------------------------------------------------------------- function types

type Transformer<In, Out> = (value: In) => Out;
type Predicate<T> = (value: T) => boolean;

function applyAll<T>(value: T, ...fns: Transformer<T, T>[]): T {
  return fns.reduce((acc, fn) => fn(acc), value);
}

// default parameters, rest parameters, optional parameters
function greet(name: string, greeting = "hello", ...titles: string[]): string {
  return [greeting, ...titles, name].join(" ");
}

// ---------------------------------------------------------------- overloads

function parseInput(value: string): string[];
function parseInput(value: number): number[];
function parseInput(value: string | number): string[] | number[] {
  return typeof value === "string" ? value.split(",") : [value];
}

// ---------------------------------------------------------------- generics with constraints

function identity<T>(value: T): T {
  return value;
}

/** K extends keyof T guarantees the key exists AND returns the right value type. */
function getProperty<T extends object, K extends keyof T>(obj: T, key: K): T[K] {
  return obj[key];
}

function pluck<T, K extends keyof T>(items: T[], key: K): T[K][] {
  return items.map((item) => item[key]);
}

function pick<T extends object, K extends keyof T>(obj: T, keys: K[]): Pick<T, K> {
  return Object.fromEntries(keys.map((k) => [k, obj[k]])) as Pick<T, K>;
}

function groupBy<T, K extends string | number>(items: T[], keyFn: (item: T) => K): Record<K, T[]> {
  return items.reduce((acc, item) => {
    const key = keyFn(item);
    (acc[key] ??= []).push(item);
    return acc;
  }, {} as Record<K, T[]>);
}

function sortBy<T>(items: T[], ...keyFns: ((item: T) => string | number)[]): T[] {
  return [...items].sort((a, b) => {
    for (const keyFn of keyFns) {
      const [x, y] = [keyFn(a), keyFn(b)];
      if (x < y) return -1;
      if (x > y) return 1;
    }
    return 0;
  });
}

function unique<T, K>(items: T[], keyFn: (item: T) => K = (item) => item as unknown as K): T[] {
  const seen = new Set<K>();
  return items.filter((item) => {
    const key = keyFn(item);
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

function partition<T>(items: T[], predicate: Predicate<T>): [T[], T[]] {
  const pass: T[] = [];
  const fail: T[] = [];
  for (const item of items) (predicate(item) ? pass : fail).push(item);
  return [pass, fail];
}

/** Typed memoize: the wrapper keeps the original signature. */
function memoize<Args extends unknown[], Result>(
  fn: (...args: Args) => Result,
  keyFn: (...args: Args) => string = (...args) => JSON.stringify(args),
): (...args: Args) => Result {
  const cache = new Map<string, Result>();
  return (...args: Args): Result => {
    const key = keyFn(...args);
    const cached = cache.get(key);
    if (cached !== undefined) return cached;
    const result = fn(...args);
    cache.set(key, result);
    return result;
  };
}

/** Generic constraint with a default type parameter. */
function createStore<State extends object = Record<string, unknown>>(initial: State) {
  let state = initial;
  return {
    get: (): State => state,
    update: (patch: Partial<State>): State => (state = { ...state, ...patch }),
  };
}

// ---------------------------------------------------------------- keyof / typeof / indexed access

const defaultSettings = { theme: "dark", fontSize: 14, beta: false };
type Settings = typeof defaultSettings;            // { theme: string; fontSize: number; beta: boolean }
type SettingKey = keyof Settings;                  // "theme" | "fontSize" | "beta"
type FontSize = Settings["fontSize"];              // number

function updateSetting<K extends SettingKey>(settings: Settings, key: K, value: Settings[K]): Settings {
  return { ...settings, [key]: value };
}

const employees = [
  { name: "ann", dept: "it", salary: 120 },
  { name: "bob", dept: "hr", salary: 95 },
  { name: "cat", dept: "it", salary: 130 },
];

let memoCalls = 0;
const slowSquare = memoize((n: number) => {
  memoCalls++;
  return n * n;
});

check("applyAll pipeline", applyAll(3, (n) => n * 2, (n) => n + 1), 7);
check("defaults and rest params", [greet("ann"), greet("bob", "hi", "dr.")], ["hello ann", "hi dr. bob"]);
check("overloads", [parseInput("a,b"), parseInput(5)], [["a", "b"], [5]]);
check("identity keeps the type", [identity("x"), identity(7)], ["x", 7]);
check("getProperty", getProperty(employees[0]!, "salary"), 120);
check("pluck", pluck(employees, "name"), ["ann", "bob", "cat"]);
check("pick", pick(employees[0]!, ["name", "dept"]), { name: "ann", dept: "it" });
check("groupBy dept", Object.keys(groupBy(employees, (e) => e.dept)), ["it", "hr"]);
check("sortBy dept then -salary", sortBy(employees, (e) => e.dept, (e) => -e.salary).map((e) => e.name),
      ["bob", "cat", "ann"]);
check("unique with key", unique(employees, (e) => e.dept).map((e) => e.name), ["ann", "bob"]);
check("partition", partition([1, 2, 3, 4], (n) => n % 2 === 0), [[2, 4], [1, 3]]);

slowSquare(9);
check("typed memoize", [slowSquare(9), memoCalls], [81, 1]);

const store = createStore({ count: 0, user: "ann" });
store.update({ count: 5 });
check("generic store with defaults", store.get(), { count: 5, user: "ann" });
check("keyof / indexed access", updateSetting(defaultSettings, "fontSize", 18).fontSize, 18);

console.log("\nall checks passed");

export {};
