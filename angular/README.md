# Angular Interview Questions

Angular 21 + TypeScript, signal-first and standalone. Every file is **compiled by the Angular compiler with `strictTemplates`**, so the templates are checked too — not just the TypeScript.

## Run the checks

```bash
node angular/08_logic/pure_logic.mjs    # no install: validators, store logic, signals from scratch, operator semantics

cd angular && npm install               # needed for the next two
npm run rxjs                            # the same operator semantics verified against real RxJS
npm run typecheck                       # ngc with strictTemplates over every component
npm run check                           # all three
```

## Files

| Folder | File | What's inside |
|---|---|---|
| `01_components/` | `component_basics.ts` | Standalone components, `input.required()`/`output()`/`model()`, `@if`/`@for`(with `track`)/`@switch`, `viewChild()` signal queries, `host` bindings, content projection, `effect()`, `DestroyRef` |
| | `change_detection.ts` | Zone.js vs zoneless, Default vs OnPush and its 5 triggers, **the mutation trap**, why method calls in templates are slow, all lifecycle hooks in order, `markForCheck` vs `detectChanges`, `ExpressionChangedAfterItHasBeenChecked` |
| `02_directives_pipes/` | `directives_and_pipes.ts` | Attribute directive with host bindings, **structural directive** built from `TemplateRef` + `ViewContainerRef` (with `ngTemplateContextGuard`), pure/impure pipes, why a filter pipe is a trap, and the standalone-imports trap for built-in pipes |
| `03_services_di/` | `services_and_di.ts` | `providedIn: 'root'` vs component providers, `InjectionToken` with a factory, all provider recipes, `inject()` with `optional/self/skipSelf`, typed `HttpClient` service with `retry`+`catchError`, **functional interceptors**, `httpResource()` |
| `04_rxjs/` | `rxjs_patterns.ts` | **switchMap/mergeMap/concatMap/exhaustMap** with the use case for each, subject types, a BehaviorSubject store, typeahead pipeline, `forkJoin`/`combineLatest`/`withLatestFrom`, `shareReplay`, cold vs hot, `toSignal`/`toObservable`, unsubscribe strategies |
| `05_forms/` | `reactive_forms.ts` | Typed reactive forms with `NonNullableFormBuilder`, sync/async/cross-field validators, `FormArray`, `updateOn: 'blur'`, error display rules, `getRawValue()` vs `value`, template-driven comparison |
| `06_routing/` | `routing.ts` | Lazy `loadComponent`/`loadChildren`, functional `CanActivateFn`/`CanMatchFn`/`CanDeactivateFn`/`ResolveFn`, params as component inputs, `paramMap` for reused components, redirects and the wildcard route |
| `07_state_signals/` | `signals_and_state.ts` | A signal store (private writable + public readonly + computed + actions), `linkedSignal`, `resource()`, `untracked`, and when to choose signals / NgRx SignalStore / NgRx |
| `08_logic/` | `pure_logic.mjs` | **Runnable with no dependencies**: validators, store logic, a ~40-line signal implementation proving lazy+cached `computed`, and the four flattening operators implemented by hand |
| | `rxjs_operators.mjs` | The same operator behaviour asserted against real RxJS (so the hand-written version is provably right) |
| `09_interview_qa/` | `angular_qa.md` | ~60 Q&A: change detection, signals, DI, RxJS, forms, routing, performance, `@defer`, SSR, version history, code-review red flags |
| | `testing_guide.md` | TestBed, `componentRef.setInput`, `HttpTestingController`, testing signals/effects, `fakeAsync`, guards via `runInInjectionContext`, common failures |

## The answers worth memorizing

| Question | One-line answer |
|---|---|
| Why didn't my OnPush view update? | You mutated instead of replacing — the reference never changed. |
| What triggers OnPush? | Input reference change, event from the component, async pipe emit, signal read in the template, `markForCheck()`. |
| `signal` / `computed` / `effect`? | Writable value / derived, lazy and cached / side effect — never derive state in an effect. |
| switchMap vs concatMap vs exhaustMap vs mergeMap? | Cancel previous / queue in order / ignore while busy / run concurrently. |
| How do you avoid subscription leaks? | `async` pipe, `takeUntilDestroyed()`, or `toSignal()`. |
| `providedIn: 'root'` vs component `providers`? | One app-wide singleton vs one instance per component instance. |
| How do you shrink the bundle? | Lazy routes (`loadComponent`/`loadChildren`), `@defer` blocks, standalone imports, AOT. |
| CanActivate vs CanMatch? | Blocks activation vs prevents the lazy chunk from even being downloaded. |
| Reactive vs template-driven forms? | Model in the class (typed, testable) vs model in the template (quick, harder to test). |
| Why is `track` required in `@for`? | It matches items to DOM nodes across renders, like React's `key`. |

## Practice routine

1. Write a signal store (state + computed + actions) from memory, then compare with `07_state_signals/`.
2. Explain the difference between the four flattening operators, then run `08_logic/pure_logic.mjs` to check yourself.
3. Take `05_forms/reactive_forms.ts` and add a new async validator with tests.
4. Describe, out loud, what happens between a click and the DOM updating — with Zone.js and without it.

Related folders: `react/` (same problems, other framework), `typescript/`, `javascript/`, `python/`, `java/` (Java), `db/` (SQL), `system-design/`.
