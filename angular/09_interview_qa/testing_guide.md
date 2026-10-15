# Testing Angular

## The tools

| Tool | Use |
|---|---|
| `TestBed` | Configures a testing module: declares the component under test, provides fakes |
| `ComponentFixture` | Wraps the component instance + its DOM (`fixture.nativeElement`, `fixture.detectChanges()`) |
| `HttpTestingController` | Intercepts HTTP calls made through `HttpClient` and lets you flush fake responses |
| `fakeAsync` + `tick()` | Run timers/microtasks synchronously in a test |
| `Jasmine/Karma` (default) or `Jest`/`Vitest` | Test runner and assertions |
| Playwright / Cypress | End-to-end tests |

## Component test (signals + OnPush)

```ts
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TodoListComponent } from './component_basics';

describe('TodoListComponent', () => {
  let fixture: ComponentFixture<TodoListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [TodoListComponent] }).compileComponents();
    fixture = TestBed.createComponent(TodoListComponent);
    fixture.detectChanges();                       // first render
  });

  it('shows the empty state', () => {
    fixture.componentInstance.todos.set([]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Nothing to do');
  });

  it('adds a todo', () => {
    fixture.componentInstance.draft.set('write tests');
    fixture.componentInstance.add(new Event('submit'));
    fixture.detectChanges();
    expect(fixture.componentInstance.todos().length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('write tests');
  });
});
```

**Setting a signal input** on a component under test: `fixture.componentRef.setInput('todo', myTodo)` — not assignment.

## Testing a service with HttpClient

```ts
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());              // fails if any request went unanswered

  it('loads a user', () => {
    let result: User | undefined;
    service.getUser(7).subscribe((user) => (result = user));

    const request = httpMock.expectOne('/api/users/7');
    expect(request.request.method).toBe('GET');
    request.flush({ id: 7, name: 'Ann', email: 'a@b.c' });

    expect(result?.name).toBe('Ann');
  });

  it('translates a 404', () => {
    let error: Error | undefined;
    service.getUser(7).subscribe({ error: (e: Error) => (error = e) });
    httpMock.expectOne('/api/users/7').flush('nope', { status: 404, statusText: 'Not Found' });
    expect(error?.message).toBe('User not found');
  });
});
```

## Testing signals and computed

```ts
it('derives stats', () => {
  const store = TestBed.inject(TodoStore);
  store.load([{ id: '1', title: 'a', done: true, priority: 'low' }]);
  expect(store.stats()).toEqual({ total: 1, done: 1, remaining: 0 });   // just call the signals
});
```

Effects run during change detection, so in a test either use `TestBed.flushEffects()` (or `fixture.detectChanges()`) before asserting their side effects.

## fakeAsync for timers and debounce

```ts
it('debounces the search', fakeAsync(() => {
  const results: string[] = [];
  component.onType('an');
  component.results$.subscribe((r) => results.push(...r.map((p) => p.name)));
  tick(299);
  expect(results.length).toBe(0);      // still waiting
  tick(1);
  flush();                             // drain remaining timers
  expect(results.length).toBeGreaterThan(0);
}));
```

`waitForAsync` + `fixture.whenStable()` is the alternative when real promises are involved.

## Testing a guard / interceptor (they're just functions)

```ts
it('redirects anonymous users', () => {
  TestBed.configureTestingModule({ providers: [provideRouter([])] });
  const result = TestBed.runInInjectionContext(() => authGuard({} as any, { url: '/admin' } as any));
  expect(result).not.toBe(true);       // a UrlTree pointing at /login
});
```

`TestBed.runInInjectionContext` is the key: functional guards, resolvers and interceptors use `inject()`, so they need an injection context.

## Testing a reactive form

```ts
it('validates the password', () => {
  const form = fixture.componentInstance.form;
  form.controls.password.setValue('short');
  expect(form.controls.password.hasError('strongPassword')).toBe(true);
  form.controls.password.setValue('Passw0rd');
  expect(form.controls.password.valid).toBe(true);
});
```

Better still: test the validator function directly, with no TestBed at all (see `08_logic/pure_logic.mjs`).

## What to test

✅ Rendered output per state (empty, loading, error, data), user interactions, validators, guards, store/service logic, error translation.
❌ Private methods, framework behaviour, exact DOM structure (brittle), snapshot dumps of whole templates.

**Pyramid:** many pure unit tests (validators, store logic, pipes) → component tests with TestBed → a few end-to-end journeys.

## Common failures

| Symptom | Cause |
|---|---|
| "NullInjectorError: No provider for X" | The testing module doesn't provide `X` — add a fake or the real provider |
| View doesn't update in the test | You forgot `fixture.detectChanges()` (or the component is OnPush and you mutated state) |
| "1 periodic timer(s) still in the queue" | A `fakeAsync` test left a timer running — `flush()` or `discardPeriodicTasks()` |
| Test passes alone, fails in a suite | Shared state between tests — reset stores/spies in `beforeEach` |
