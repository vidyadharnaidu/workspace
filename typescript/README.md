# TypeScript Interview Questions

Runnable, **strictly type-checked** TypeScript. Every file checks its own answers at runtime, and the whole folder compiles under `strict` plus extra safety flags.

```bash
node typescript/run_all.js              # type-check everything, then run every file
node typescript/01_types_basics.ts      # run one file (Node 22.6+ strips types; Node 24 used here)
npx tsc --noEmit -p typescript          # type-check only
```

No dependencies to install beyond `typescript` itself (used through `npx`).

## Files

| File | What's inside |
|---|---|
| `01_types_basics.ts` | Primitives, tuples, `type` vs `interface`, declaration merging, literal unions, `as const` instead of `enum`, optional/readonly/index signatures, narrowing (typeof, `in`, `Array.isArray`, custom guards), **discriminated unions + exhaustiveness with `never`**, `as` vs `satisfies`, strict null handling |
| `02_functions_generics.ts` | Function types, overloads, generics with constraints, `keyof`/`typeof`/indexed access, generic utilities you may be asked to write: `getProperty`, `pluck`, `pick`, `groupBy`, `sortBy`, `unique`, `partition`, **typed `memoize`**, a generic store with a default type parameter |
| `03_utility_and_advanced_types.ts` | Built-in utility types, **implementing `Partial`/`Required`/`Pick`/`Omit`/`Exclude`/`ReturnType`/`Awaited` yourself**, compile-time `Equals` tests, `DeepPartial`/`DeepReadonly`, template literal types (`onClick` handler maps), branded (nominal) types |
| `04_classes_interfaces.ts` | Structural typing vs `implements`, generic class + repository interface, abstract classes, `#private` vs `private`, static members, getters, `this` return types (fluent builder), composition over inheritance |
| `05_async_and_errors.ts` | Typed promises, custom error classes + narrowing `unknown` in `catch`, **`Result<T, E>` type**, `attempt` wrapper, `withTimeout`, `retry`, validating unknown JSON at the boundary |
| `06_practical_patterns.ts` | Discriminated-union **state machine**, typed **reducer** with exhaustive actions, **type-safe event emitter**, generic **API client** with per-endpoint request/response types, DTO → domain mapping, options with defaults, typed `Object.keys` |
| `07_generic_data_structures.ts` | Generic `Stack<T>` (with `Symbol.iterator`), `Queue<T>`, `LRUCache<K, V>`, linked list, binary tree with a generator traversal, generic `binarySearch`/`mergeSort` with comparators, typed `MinHeap<T>` |
| `interview_qa.md` | ~25 core Q&A, "will this compile?" snippets, code review red flags, tsconfig flags worth knowing |
| `tsconfig.json` | `strict` + `noUncheckedIndexedAccess`, `noImplicitOverride`, `verbatimModuleSyntax`, `erasableSyntaxOnly` |

## The type-system moves worth memorizing

```ts
type Keys = keyof User;                       // union of property names
type Value = User["email"];                   // indexed access
type Fn = typeof someFunction;                // type of a value
type R = ReturnType<Fn>;                      // conditional type + infer
type Optional<T> = { [K in keyof T]?: T[K] }; // mapped type
type NoDraft = Exclude<Status, "draft">;      // distributive conditional type
type Handlers<T> = { [K in keyof T & string as `on${Capitalize<K>}`]: (v: T[K]) => void };  // key remap
const config = { port: 8080 } satisfies Config;   // check without widening
function isUser(v: unknown): v is User { ... }    // type guard
const exhaustive: never = action;                 // exhaustiveness check
type UserId = number & { readonly __brand: "UserId" };   // branded/nominal type
```

## Why the files avoid `enum` and parameter properties

`erasableSyntaxOnly` is on so each file runs directly with `node file.ts` (type stripping removes types but cannot emit runtime code). That rules out `enum`, `namespace` and `constructor(private x: number)`. Literal unions and `as const` objects are the modern replacements anyway — see `01_types_basics.ts`.

Related folders: `javascript/` (same runtime, no types), `python/`, `java/` (Java), `db/` (SQL), `system-design/`.
