/**
 * Utility types, mapped types, conditional types, template literal types - and how to implement them yourself.
 * "Write Partial/Pick/Omit from scratch" is the classic senior TypeScript question.
 *
 * Run:   node typescript/03_utility_and_advanced_types.ts
 *
 * REMEMBER:
 *  * MAPPED TYPE:      { [K in keyof T]: ... }   with modifiers +/- readonly and +/- ?
 *  * CONDITIONAL TYPE: T extends U ? X : Y       and `infer` extracts a type out of another type
 *  * Conditional types DISTRIBUTE over unions when the checked type is a naked type parameter.
 *  * TEMPLATE LITERAL TYPE: `on${Capitalize<K & string>}` builds string types from other types.
 *  * Most "magic" utility types are 1-3 lines - knowing that is the point of the question.
 */

function check(label: string, actual: unknown, expected: unknown): void {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
  console.log(`${label.padEnd(36)} ${a}`);
}

/** Compile-time assertion helper: fails to compile if A and B are different types. */
type Equals<A, B> = (<T>() => T extends A ? 1 : 2) extends <T>() => T extends B ? 1 : 2 ? true : false;
function expectType<Expected>(_: Expected): void {}

interface User {
  id: number;
  name: string;
  email?: string;
  readonly createdAt: string;
}

// ---------------------------------------------------------------- built-in utility types

type PartialUser = Partial<User>;                 // all optional
type RequiredUser = Required<User>;               // all required
type ReadonlyUser = Readonly<User>;               // all readonly
type UserPreview = Pick<User, "id" | "name">;     // keep these keys
type UserWithoutDates = Omit<User, "createdAt">;  // drop these keys
type UsersById = Record<number, User>;            // key -> value map
type NonNullableName = NonNullable<string | null | undefined>;   // string
type Status = "draft" | "published" | "archived";
type PublicStatus = Exclude<Status, "draft">;     // "published" | "archived"
type OnlyDraft = Extract<Status, "draft" | "deleted">;           // "draft"

function loadUser(id: number, includeEmail: boolean): User {
  return { id, name: `user${id}`, createdAt: "2026-01-01", ...(includeEmail ? { email: "a@b.c" } : {}) };
}
type LoadUserArgs = Parameters<typeof loadUser>;      // [number, boolean]
type LoadedUser = ReturnType<typeof loadUser>;        // User
type Unwrapped = Awaited<Promise<Promise<number>>>;   // number

// ---------------------------------------------------------------- implement them yourself

type MyPartial<T> = { [K in keyof T]?: T[K] };
type MyRequired<T> = { [K in keyof T]-?: T[K] };                  // -? removes optionality
type MyReadonly<T> = { readonly [K in keyof T]: T[K] };
type MyMutable<T> = { -readonly [K in keyof T]: T[K] };           // -readonly removes it
type MyPick<T, K extends keyof T> = { [P in K]: T[P] };
type MyOmit<T, K extends keyof never> = MyPick<T, Exclude<keyof T, K>>;
type MyRecord<K extends keyof never, V> = { [P in K]: V };
type MyExclude<T, U> = T extends U ? never : T;                   // distributes over the union
type MyExtract<T, U> = T extends U ? T : never;
type MyNonNullable<T> = T & {};
type MyReturnType<T> = T extends (...args: never[]) => infer R ? R : never;
type MyParameters<T> = T extends (...args: infer A) => unknown ? A : never;
type MyAwaited<T> = T extends Promise<infer V> ? MyAwaited<V> : T;   // recursive unwrap

// these fail to compile if the implementations are wrong
type _t1 = Equals<MyPartial<User>, Partial<User>>;
type _t2 = Equals<MyOmit<User, "createdAt">, Omit<User, "createdAt">>;
type _t3 = Equals<MyReturnType<typeof loadUser>, User>;
type _t4 = Equals<MyAwaited<Promise<Promise<string>>>, string>;
const typeTests: [_t1, _t2, _t3, _t4] = [true, true, true, true];

// ---------------------------------------------------------------- deep / recursive types

type DeepPartial<T> = T extends object ? { [K in keyof T]?: DeepPartial<T[K]> } : T;
type DeepReadonly<T> = T extends object ? { readonly [K in keyof T]: DeepReadonly<T[K]> } : T;

interface AppConfig {
  server: { host: string; port: number; tls: { enabled: boolean } };
  debug: boolean;
}

function mergeConfig(base: AppConfig, override: DeepPartial<AppConfig>): AppConfig {
  return {
    ...base,
    ...override,
    server: { ...base.server, ...override.server, tls: { ...base.server.tls, ...override.server?.tls } },
  };
}

// ---------------------------------------------------------------- template literal types

type EventName<K extends string> = `on${Capitalize<K>}`;
type Handlers<T> = { [K in keyof T & string as EventName<K>]: (value: T[K]) => void };
type Getters<T> = { [K in keyof T & string as `get${Capitalize<K>}`]: () => T[K] };

type ApiRoute = `/${string}`;
type HttpMethod = "GET" | "POST";
type Endpoint = `${HttpMethod} ${ApiRoute}`;      // "GET /users" etc.

// ---------------------------------------------------------------- branded (nominal) types

type Brand<T, B extends string> = T & { readonly __brand: B };
type UserId = Brand<number, "UserId">;
type OrderId = Brand<number, "OrderId">;

const toUserId = (value: number): UserId => value as UserId;
function fetchUserName(id: UserId): string {
  return `user${id}`;
}
// fetchUserName(42)            // compile error: a raw number is not a UserId
// fetchUserName(orderId)       // compile error: OrderId is not a UserId

// ---------------------------------------------------------------- runtime demos of the typed helpers

const handlers: Handlers<{ click: number; save: string }> = {
  onClick: (value) => calls.push(`click:${value}`),
  onSave: (value) => calls.push(`save:${value}`),
};
const calls: string[] = [];

const readonlyConfig: DeepReadonly<AppConfig> = {
  server: { host: "db", port: 5432, tls: { enabled: true } },
  debug: false,
};
// readonlyConfig.server.port = 1   // compile error: readonly at every level

const endpoints: Endpoint[] = ["GET /users", "POST /orders"];

check("built-in utility types compile", typeTests, [true, true, true, true]);
check("Pick / Omit at runtime",
      [({ id: 1, name: "ann" } satisfies UserPreview), Object.keys({ id: 1, name: "ann", email: "x" } satisfies UserWithoutDates)],
      [{ id: 1, name: "ann" }, ["id", "name", "email"]]);
check("Parameters / ReturnType", [((args: LoadUserArgs) => args)([7, false]), loadUser(7, false).name], [[7, false], "user7"]);
check("DeepPartial merge",
      mergeConfig({ server: { host: "localhost", port: 80, tls: { enabled: false } }, debug: false },
                  { server: { port: 8080, tls: { enabled: true } } }),
      { server: { host: "localhost", port: 8080, tls: { enabled: true } }, debug: false });

handlers.onClick(1);
handlers.onSave("draft");
check("template literal handler keys", [Object.keys(handlers), calls], [["onClick", "onSave"], ["click:1", "save:draft"]]);
check("DeepReadonly value", readonlyConfig.server.tls.enabled, true);
check("template literal endpoints", endpoints, ["GET /users", "POST /orders"]);
check("branded types", fetchUserName(toUserId(42)), "user42");

expectType<Status>("published");
check("Exclude / Extract", [("published" satisfies PublicStatus), ("draft" satisfies OnlyDraft)], ["published", "draft"]);
check("NonNullable / Awaited", [("x" satisfies NonNullableName), (3 satisfies Unwrapped)], ["x", 3]);

console.log("\nall checks passed");

export {};
