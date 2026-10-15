# Angular Interview Q&A (Angular 21)

## Fundamentals

| Question | Answer |
|---|---|
| Angular vs AngularJS? | Different frameworks. AngularJS (1.x) used scopes, two-way digest and controllers. Angular (2+) is a TypeScript, component-based rewrite with a compiler, DI, and now signals. |
| Angular vs React, in one sentence? | Angular is a full framework (router, forms, HTTP, DI, testing, CLI) with strong conventions; React is a UI library where you assemble the rest yourself. |
| What is a component? | A class with a `@Component` decorator: a template, styles, and a selector. Since v19 components are **standalone** by default — no NgModule. |
| What replaced NgModules? | Standalone components with an `imports` array, plus `bootstrapApplication()` and `provide*()` functions (`provideRouter`, `provideHttpClient`). NgModules still exist for legacy code and libraries. |
| Template syntax basics? | `{{ interpolation }}`, `[property]="expr"`, `(event)="handler()"`, `[(ngModel)]="value"` (banana in a box = property + event), `#ref` template variables, `@if/@for/@switch` control flow. |
| Why does `@for` require `track`? | It identifies items across renders so Angular can move DOM nodes instead of destroying and rebuilding them — the same reason React needs `key`. |
| What is a directive? | A class that adds behaviour to an element. Components are directives with a template; attribute directives change appearance/behaviour; structural directives change the DOM shape via `<ng-template>`. |
| Pure vs impure pipe? | Pure (default) re-runs only when the input reference changes. Impure runs on every change-detection cycle — avoid; `AsyncPipe` is the notable exception. |
| Content projection? | `<ng-content>` — the parent passes markup into the child (like React's `children`); `select="[attr]"` creates named slots. |
| `ng-template` vs `ng-container`? | `ng-template` defines a template that is not rendered until instantiated; `ng-container` is a grouping element that renders no DOM node. |
| ViewChild vs ContentChild? | View = elements in **this** component's template; Content = elements projected **into** it. Signal queries: `viewChild()`, `contentChild()`. |

## Change detection (the classic deep-dive)

| Question | Answer |
|---|---|
| How does change detection work? | After an async event, Angular walks the component tree from the root, re-evaluates template bindings, and updates the DOM where values changed. |
| What is Zone.js? | A library that monkey-patches async APIs (events, timers, XHR) so Angular knows when something might have changed. Zoneless apps (v20+, `provideZonelessChangeDetection()`) drop it and rely on signals, the async pipe and `markForCheck`. |
| Default vs OnPush? | Default checks every component each cycle. **OnPush** checks only when an input reference changes, an event fires in the component, an async pipe emits, a signal read in the template changes, or `markForCheck()` is called. |
| Why didn't my OnPush view update? | You mutated an object/array instead of replacing it, so the reference (and the signal value) never changed. |
| `markForCheck` vs `detectChanges`? | `markForCheck()` marks this component and its ancestors dirty for the next cycle. `detectChanges()` runs change detection on this subtree immediately. `detach()`/`reattach()` remove a subtree from checking. |
| ExpressionChangedAfterItHasBeenCheckedError? | Dev-mode-only: bound state changed after the check (typically in `ngAfterViewInit`). Fix by setting it earlier, in a signal, or in `afterNextRender`. |
| Why avoid method calls in templates? | They re-run on every change detection. Use `computed()` (cached) or a pure pipe. |
| How do you make a list fast? | `track` in `@for`, OnPush, `computed()` for derived data, virtual scrolling (CDK) for very long lists, and `@defer` for below-the-fold content. |

## Signals (modern Angular's core)

| Question | Answer |
|---|---|
| What is a signal? | A reactive value with an explicit read (`count()`). Angular tracks which templates and computations read it, so it updates exactly those. |
| `signal` / `computed` / `effect`? | Writable value / derived, lazy, cached value / side effect after render. Never derive state in an effect — use `computed`. |
| Signals vs RxJS? | Signals: synchronous state you render. RxJS: asynchronous event streams (cancellation, retries, combining over time). Bridge with `toSignal`/`toObservable`. |
| `linkedSignal`? | Writable state that resets when a source changes — e.g. the selected row resets when the filtered list changes. |
| `resource()` / `httpResource()` / `rxResource()`? | Async data as signals: `value`, `isLoading`, `error`, `reload()`, with the previous request aborted automatically. |
| Why `update()` instead of mutation? | Signals compare with `Object.is`; a mutated array is the same reference, so nothing updates. |
| `untracked()`? | Read a signal inside a computed/effect **without** making it a dependency. |
| Signal inputs/outputs? | `input()`, `input.required()`, `output()`, `model()` for two-way. They replace `@Input`/`@Output` and work with OnPush by design. |

## Dependency injection

| Question | Answer |
|---|---|
| How does Angular DI work? | A hierarchical injector tree. A request walks up: element → component ancestors → root/environment → platform. The first provider found wins. |
| `providedIn: 'root'` vs `providers: []`? | Root = a tree-shakable app-wide singleton. A component's `providers` array gives every instance of that component its own instance (and its children). |
| Provider recipes? | `useClass`, `useValue`, `useFactory`, `useExisting`, plus `multi: true` for arrays of providers. |
| What is an InjectionToken? | A unique token for injecting something without a class — config objects, primitives, functions. It can carry a default `factory`. |
| `inject()` vs constructor injection? | `inject()` works in field initializers, functions (guards, interceptors, resolvers) and base classes without constructor plumbing. It must run in an injection context. |
| `@Optional`, `@Self`, `@SkipSelf`, `@Host`? | Don't throw if missing / only this injector / start at the parent / stop at the host component. With `inject()`: `{ optional, self, skipSelf, host }`. |
| How do you swap a dependency in tests? | Provide a fake with `TestBed.configureTestingModule({ providers: [{ provide: Real, useClass: Fake }] })`. |

## RxJS

| Question | Answer |
|---|---|
| Observable vs Promise? | Observables are lazy, can emit many values, are cancellable and composable with operators. Promises are eager, single-value and not cancellable. |
| switchMap vs mergeMap vs concatMap vs exhaustMap? | Cancel previous (typeahead) / run concurrently (independent work) / queue in order (sequential writes) / ignore while busy (submit buttons). Verified in `08_logic/`. |
| Subject types? | `Subject` (no initial value), `BehaviorSubject` (current value, emits on subscribe), `ReplaySubject` (last N), `AsyncSubject` (final value on complete). |
| Cold vs hot? | Cold observables run per subscriber (HttpClient: two subscribes = two requests). Hot ones share one execution (`Subject`, `share()`, `shareReplay()`). |
| How do you avoid subscription leaks? | The `async` pipe, `takeUntilDestroyed()`, or `toSignal()`. HTTP calls complete on their own, but timers and subjects leak forever. |
| Error handling? | `catchError` (recover or rethrow), `retry({ count, delay })`, `finalize` for cleanup. An unhandled error kills the stream, so put `catchError` inside the inner observable when you want the outer stream to survive. |
| `debounceTime` + `distinctUntilChanged` + `switchMap`? | The standard typeahead: wait for a pause, ignore repeats, cancel in-flight requests. |

## Forms

| Question | Answer |
|---|---|
| Reactive vs template-driven? | Reactive: the model is in the class — typed, testable, dynamic, synchronous updates. Template-driven: `ngModel` in the template — quick for tiny forms, harder to test. |
| Typed forms? | Since v14 `FormGroup<T>` infers value types; `NonNullableFormBuilder` avoids `T | null` after `reset()`. |
| Custom validator? | A function returning `ValidationErrors | null` (sync) or an Observable of that (async). Cross-field rules go on the **group**. |
| `setValue` vs `patchValue`? | `setValue` needs every control; `patchValue` updates a subset. |
| `value` vs `getRawValue()`? | `value` omits disabled controls; `getRawValue()` includes them. |
| `updateOn`? | `'change'` (default), `'blur'`, or `'submit'` — useful to avoid validating (or calling the server) on every keystroke. |
| How do you show errors nicely? | Only when `control.touched || submitted`, and `markAllAsTouched()` on submit. |

## Routing

| Question | Answer |
|---|---|
| How do you lazy load? | `loadComponent` for one component, `loadChildren` for a feature's routes. This is the main bundle-size lever. |
| Guard types? | `CanActivate` (can we enter?), `CanMatch` (should this route even match — prevents downloading a lazy chunk), `CanDeactivate` (unsaved changes), `Resolve` (pre-fetch data). All are plain functions now. |
| How do you read route params? | `withComponentInputBinding()` binds params/query/data to component **inputs**; otherwise `ActivatedRoute.paramMap` (an observable, because Angular reuses the component when only the param changes). |
| Resolver: yes or no? | It removes flicker but delays navigation. Modern preference: navigate immediately and show a loading state (or a `resource()`). |
| Route order matters? | Yes — first match wins, so the `**` wildcard goes last, and `pathMatch: 'full'` is needed for empty-path redirects. |

## Performance, build and the rest

| Question | Answer |
|---|---|
| How do you speed up an Angular app? | OnPush + signals, `track` in loops, lazy routes, `@defer` blocks, virtual scrolling, `trackBy`-style stable ids, avoid method calls in templates, tree-shake imports, SSR/hydration, and measure with the DevTools profiler. |
| What is `@defer`? | Template-level lazy loading: `@defer (on viewport) { <heavy-chart /> } @placeholder { ... }` — downloads the component only when the trigger fires. |
| AOT vs JIT? | AOT compiles templates at build time (smaller, faster, template errors at build). It's the default; JIT is dev/test only. |
| What is SSR / hydration? | `@angular/ssr` renders HTML on the server; the client reuses that DOM (incremental hydration in v19+) instead of re-rendering. Improves first paint and SEO. |
| Testing tools? | TestBed + ComponentFixture with Karma/Jasmine or Jest/Vitest; `HttpTestingController` for HTTP; `fakeAsync`/`tick` for timers. See `testing_guide.md`. |
| How do you structure a large app? | Feature folders with standalone components, a `core` (singletons, interceptors), `shared` (dumb components/pipes), lazy routes per feature, and a signal store per feature. |
| Angular version awareness? | v14 typed forms and standalone preview, v15 functional guards, v16 signals preview + `inject()` everywhere, v17 new control flow + `@defer`, v18 zoneless preview + Material 3, v19 standalone by default + `linkedSignal` + `resource`, v20 zoneless stable + `httpResource`, v21 signal-first APIs across the board. |

## Code review red flags

```ts
// leaks: no unsubscribe
ngOnInit() { this.service.data$.subscribe(d => this.data = d); }   // use async pipe / takeUntilDestroyed / toSignal

// mutation with OnPush: view never updates
this.items().push(newItem);                                         // use update(current => [...current, newItem])

// expensive work on every CD cycle
<p>{{ calculateTotal() }}</p>                                       // use computed()

// nested subscribes instead of composing
this.a$.subscribe(a => this.b$.subscribe(b => ...));                // use switchMap/concatMap

// any + non-null assertions everywhere
const user = res as any;                                            // type the response

// business logic in the component
// -> move it into a service/store so it can be unit tested
```
