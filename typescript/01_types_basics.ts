/**
 * TypeScript type basics: annotations, unions, narrowing, type vs interface.
 *
 * Run:   node typescript/01_types_basics.ts
 * Check: npx tsc --noEmit -p typescript
 *
 * REMEMBER:
 *  * TypeScript is STRUCTURAL: if the shape matches, the type matches (no `implements` needed).
 *  * `any` turns checking OFF; `unknown` keeps it ON (you must narrow before use). Prefer unknown.
 *  * `never` = impossible value; use it for exhaustiveness checks.
 *  * Narrowing tools: typeof, instanceof, `in`, truthiness, equality, Array.isArray, custom type guards.
 *  * DISCRIMINATED UNION (a shared literal "kind" field) is the most useful modelling pattern in TS.
 *  * Types are erased at runtime: you cannot `if (x is User)`. Write a type guard instead.
 */

function check(label: string, actual: unknown, expected: unknown): void {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
  console.log(`${label.padEnd(34)} ${a}`);
}

// ---------------------------------------------------------------- primitives, arrays, tuples

const id: number = 42;
const title: string = "engineer";
const active: boolean = true;
const tags: string[] = ["a", "b"];
const pair: [string, number] = ["age", 30];           // tuple: fixed length and types
const mixed: Array<string | number> = [1, "two"];

// ---------------------------------------------------------------- type vs interface

type Point = { x: number; y: number };                 // type alias: unions, primitives, tuples, mapped types
interface Shape { kind: string; area(): number }       // interface: object shapes, can be extended/merged

interface Named { name: string }
interface Named { age: number }                        // DECLARATION MERGING: interfaces only
const namedPerson: Named = { name: "ann", age: 30 };

type Point3D = Point & { z: number };                  // intersection extends a type alias

// ---------------------------------------------------------------- literal types & const objects (instead of enum)

type Status = "pending" | "shipped" | "delivered";     // union of literals: simple and fully erasable

const Role = { Admin: "admin", User: "user" } as const;   // `as const` freezes the literal types
type Role = (typeof Role)[keyof typeof Role];             // "admin" | "user"

// ---------------------------------------------------------------- optional, readonly, index signatures

interface Config {
  readonly host: string;          // cannot be reassigned after creation
  port?: number;                  // optional -> number | undefined
  [flag: string]: unknown;        // index signature for extra keys
}

// ---------------------------------------------------------------- narrowing

function formatValue(value: string | number | Date | null): string {
  if (value === null) return "none";                       // equality narrowing
  if (typeof value === "string") return value.toUpperCase();  // typeof narrowing
  if (typeof value === "number") return value.toFixed(2);
  return value.toISOString().slice(0, 10);                 // must be Date here
}

function describeInput(input: unknown): string {
  // `unknown` forces you to check before using it - this is why it beats `any`
  if (Array.isArray(input)) return `array of ${input.length}`;
  if (typeof input === "object" && input !== null && "name" in input) return `object with name`;
  return typeof input;
}

// custom type guard: the `x is T` return type teaches the compiler
function isUser(value: unknown): value is { name: string; email: string } {
  return typeof value === "object" && value !== null && "name" in value && "email" in value;
}

// ---------------------------------------------------------------- discriminated union + exhaustiveness

type Circle = { kind: "circle"; radius: number };
type Square = { kind: "square"; side: number };
type Rectangle = { kind: "rectangle"; width: number; height: number };
type Figure = Circle | Square | Rectangle;

function area(figure: Figure): number {
  switch (figure.kind) {
    case "circle":
      return Math.PI * figure.radius ** 2;    // narrowed to Circle here
    case "square":
      return figure.side ** 2;
    case "rectangle":
      return figure.width * figure.height;
    default: {
      // If a new variant is added and not handled, this line stops compiling.
      const exhaustive: never = figure;
      throw new Error(`unhandled figure: ${JSON.stringify(exhaustive)}`);
    }
  }
}

// ---------------------------------------------------------------- assertions vs satisfies

const rawJson = '{"id": 7, "name": "ann"}';
const parsed = JSON.parse(rawJson) as { id: number; name: string };   // assertion: "trust me" (unchecked!)

// `satisfies` CHECKS the value against a type but keeps the narrow literal types
const routes = {
  home: "/",
  profile: "/users/:id",
} satisfies Record<string, `/${string}`>;
type RouteName = keyof typeof routes;      // "home" | "profile" (a plain annotation would lose this)

// ---------------------------------------------------------------- null handling under strict mode

function getLength(text?: string | null): number {
  return text?.length ?? 0;                 // optional chaining + nullish coalescing
}

function firstItem<T>(items: T[]): T | undefined {
  return items[0];                          // noUncheckedIndexedAccess makes this honest
}

check("primitives and tuples", [id, title, active, tags.length, pair, mixed], [42, "engineer", true, 2, ["age", 30], [1, "two"]]);
check("interface declaration merging", namedPerson, { name: "ann", age: 30 });
check("intersection type", ((p: Point3D) => p.x + p.y + p.z)({ x: 1, y: 2, z: 3 }), 6);
check("const object instead of enum", [Role.Admin, Object.values(Role)], ["admin", ["admin", "user"]]);
check("status literal union", (["pending", "shipped"] as Status[]).length, 2);
check("readonly + optional config", ((c: Config) => [c.host, c.port ?? 8080, c["extra"]])({ host: "db", extra: true }),
      ["db", 8080, true]);

check("narrowing by typeof/null", [formatValue("abc"), formatValue(3.14159), formatValue(new Date("2026-01-15")), formatValue(null)],
      ["ABC", "3.14", "2026-01-15", "none"]);
check("unknown must be narrowed", [describeInput([1, 2]), describeInput({ name: "x" }), describeInput(5)],
      ["array of 2", "object with name", "number"]);
check("custom type guard", [isUser({ name: "a", email: "e" }), isUser({ name: "a" })], [true, false]);

check("discriminated union area",
      [Math.round(area({ kind: "circle", radius: 2 })), area({ kind: "square", side: 3 }),
       area({ kind: "rectangle", width: 2, height: 5 })],
      [13, 9, 10]);

check("as (assertion) vs satisfies", [parsed.name, routes.profile, Object.keys(routes) as RouteName[]],
      ["ann", "/users/:id", ["home", "profile"]]);
check("strict null handling", [getLength("hello"), getLength(null), getLength(), firstItem([]) ?? "empty"],
      [5, 0, 0, "empty"]);

console.log("\nall checks passed");

export {};
