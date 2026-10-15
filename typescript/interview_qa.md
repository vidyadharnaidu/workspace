# TypeScript Interview Q&A

## Core questions

| Question | Answer |
|---|---|
| Why TypeScript? | Errors at compile time instead of runtime, self-documenting signatures, safe refactoring, better autocomplete. Cost: a build step and some type ceremony. |
| Is it runtime-safe? | No. Types are **erased** at compile time. Validate data at the boundary (API responses, JSON, forms) with type guards or a schema library. |
| Structural vs nominal typing? | TS is **structural**: if the shape matches, the type matches. Use **branded types** (`type UserId = number & { __brand: "UserId" }`) when you need nominal behaviour. |
| `any` vs `unknown` vs `never`? | `any` disables checking (avoid); `unknown` accepts anything but forces narrowing before use; `never` means "cannot happen" (exhaustiveness checks, functions that always throw). |
| `interface` vs `type`? | Interfaces: object shapes, `extends`, **declaration merging** (used for augmenting libraries). Types: unions, tuples, primitives, mapped/conditional types. Pick one style; both are structural. |
| What does `strict` turn on? | `strictNullChecks`, `noImplicitAny`, `strictFunctionTypes`, `strictBindCallApply`, `strictPropertyInitialization`, `useUnknownInCatchVariables`, `alwaysStrict`. Always enable it. |
| `as` vs `satisfies`? | `as` is an unchecked assertion ("trust me"). `satisfies` **checks** the value against a type while keeping the narrower inferred type. Prefer `satisfies`. |
| `as const`? | Freezes literal types (`"admin"` instead of `string`) and makes arrays/objects readonly. Used to build unions from values. |
| Enum or union of literals? | Prefer `as const` objects + a union type: no runtime code emitted, fully erasable, works with `Object.values`. `const enum` is inlined but has caveats; numeric enums allow invalid values. |
| Generics: why? | They tie input types to output types: `identity<T>(x: T): T` keeps the caller's type instead of widening to `any`. Constrain with `extends`. |
| `keyof` / `typeof` / indexed access? | `keyof T` = union of keys; `typeof value` = the type of a value; `T[K]` = the type of that property. Together they give type-safe property access. |
| Conditional types and `infer`? | `T extends U ? X : Y`; `infer R` captures a type: `type Return<T> = T extends (...a: never[]) => infer R ? R : never`. |
| Mapped types? | `{ [K in keyof T]: ... }` with `+/-readonly` and `+/-?` modifiers; `as` clauses rename keys (template literal types). |
| Declaration merging? | Two interfaces with the same name merge. Used to extend third-party types (e.g. adding a field to `Express.Request`). |
| Function overloads? | Several signatures + one implementation. The implementation signature isn't visible to callers. |
| Covariance/contravariance in one line? | Return types are covariant, parameter types are contravariant (with `strictFunctionTypes`); method parameters are checked bivariantly for historical reasons. |
| `unknown` in `catch`? | Under strict mode `catch (err)` is `unknown`: narrow with `instanceof Error` before touching `.message`. |
| `readonly` vs `Readonly<T>` vs `as const`? | Property modifier / mapped type for a whole object / literal + deep readonly at the value level. |
| Utility types you use daily? | `Partial`, `Required`, `Pick`, `Omit`, `Record`, `Exclude`, `Extract`, `NonNullable`, `ReturnType`, `Parameters`, `Awaited`. |
| `Record<string, T>` vs index signature? | Same idea; `Record` is more readable and composes with unions (`Record<Status, string>` forces every key). |
| Type guard vs assertion function? | `value is T` narrows in the caller; `asserts value is T` throws if the check fails and narrows after the call. |
| Decorators? | Metadata/behaviour wrappers (Angular, NestJS). Stage-3 decorators are supported in TS 5; they need `experimentalDecorators` for the legacy form. |
| `.d.ts` files? | Type declarations without implementation: describing JS libraries, `declare module`, ambient types. |
| How do you type an API response? | Define the interface, fetch as `unknown`, validate (type guard or Zod), then use. Never `as` a raw response. |
| `strictNullChecks` benefit? | `string` no longer includes `null`/`undefined`, so the compiler catches the "cannot read property of undefined" class of bugs. |
| How do you migrate a JS project? | Add `tsconfig` with `allowJs`, rename files gradually, start with `strict: false` plus `noImplicitAny`, add types file by file, turn strictness up. |
| Type vs value space? | `type X` lives only in the type world; `const X` only in the value world; `class X` and `enum X` create both. |
| Why does `Object.keys` return `string[]`? | Because objects may have extra keys at runtime (structural typing). Use a helper: `Object.keys(o) as (keyof T)[]`. |

## Output / "will this compile?" questions

```ts
const a: string = null;            // error under strictNullChecks
const b = [] as const;             // readonly []
let c: number | undefined;
c.toFixed();                       // error: possibly undefined -> c?.toFixed()
const d = { x: 1 } as { x: number; y: number };   // error: assertion must be plausible (use `as unknown as`)
function f(x: string): void {}
f(42 as any);                      // compiles: `any` silences the check (that's why it's dangerous)
const e: readonly number[] = [1];
e.push(2);                         // error: push does not exist on readonly arrays
type T1 = "a" | "b";
const g: T1 = "c";                 // error: not assignable
interface I { a: number }
const h: I = { a: 1, extra: 2 };   // error: object literals get excess property checks
const i: I = { a: 1, extra: 2 } as I;      // no error: assertion skips the check
```

## Code review red flags

```ts
function parse(data: any) {}                   // any -> use unknown + narrowing or a generic
const user = response as User;                 // unchecked assertion of network data
catch (e) { console.log(e.message); }          // e is unknown: narrow first
// @ts-ignore                                   // hides the error; prefer @ts-expect-error with a comment
enum Status { Active, Inactive }               // numeric enum allows Status = 99; use a literal union
interface Props { onClick: Function }          // Function is untyped: use (e: MouseEvent) => void
const items: Array<any> = [];                  // typed container with an untyped element
!.                                             // non-null assertion: prove it instead (guard or default)
```

## Config flags worth knowing

| Flag | Why |
|---|---|
| `strict` | Turn it on. Everything below is part of it unless noted. |
| `noUncheckedIndexedAccess` | `arr[0]` becomes `T \| undefined` - honest about out-of-range access (not in `strict`). |
| `exactOptionalPropertyTypes` | `{ a?: number }` no longer accepts `a: undefined` explicitly (not in `strict`). |
| `noImplicitOverride` | Requires `override` when replacing a base-class member. |
| `isolatedModules` / `verbatimModuleSyntax` | Keeps each file independently transpilable (needed by esbuild/swc/Node type stripping); forces `import type`. |
| `erasableSyntaxOnly` | Rejects `enum`, `namespace` and parameter properties so the file can run directly in Node 22.6+. |
| `skipLibCheck` | Skips type-checking `.d.ts` files: faster builds. |
