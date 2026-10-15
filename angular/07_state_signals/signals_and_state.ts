/**
 * Signals and state management (the centre of modern Angular).
 *
 * THE THREE PRIMITIVES:
 *   signal(value)          - a writable value: set(), update(), read with mySignal()
 *   computed(() => ...)    - derived, LAZY and CACHED; recomputes only when a dependency it actually read changes
 *   effect(() => ...)      - side effects after render (logging, localStorage, DOM); NOT for deriving state
 *
 * PLUS: linkedSignal() - writable state that RESETS when its source changes (v19+)
 *       resource()/httpResource()/rxResource() - async data as signals with value/isLoading/error
 *       untracked(fn)    - read a signal WITHOUT subscribing to it inside a computed/effect
 *
 * RULES OF THUMB:
 *  * Never call set()/update() inside a computed - computed must be pure.
 *  * Don't sync state with an effect when a computed can derive it.
 *  * Signals are synchronous and glitch-free: readers always see a consistent snapshot.
 *  * Equality: signals use Object.is by default, so replace objects/arrays instead of mutating them
 *    (or pass a custom `equal` function).
 */
import { Injectable, computed, effect, inject, linkedSignal, resource, signal, untracked } from '@angular/core';

export interface Todo {
  id: string;
  title: string;
  done: boolean;
  priority: 'low' | 'high';
}

export type Filter = 'all' | 'active' | 'done';

// ---------------------------------------------------------------- a signal store (no NgRx needed for most apps)

@Injectable({ providedIn: 'root' })
export class TodoStore {
  // 1. PRIVATE writable state
  private readonly _todos = signal<readonly Todo[]>([]);
  private readonly _filter = signal<Filter>('all');
  private readonly _query = signal('');

  // 2. PUBLIC readonly signals (encapsulation: only the store may write)
  readonly todos = this._todos.asReadonly();
  readonly filter = this._filter.asReadonly();
  readonly query = this._query.asReadonly();

  // 3. DERIVED state with computed - the view never filters by hand
  readonly visibleTodos = computed(() => {
    const filter = this._filter();
    const query = this._query().toLowerCase();
    return this._todos()
      .filter((todo) => (filter === 'all' ? true : filter === 'done' ? todo.done : !todo.done))
      .filter((todo) => todo.title.toLowerCase().includes(query));
  });

  readonly stats = computed(() => {
    const todos = this._todos();
    const done = todos.filter((t) => t.done).length;
    return { total: todos.length, done, remaining: todos.length - done };
  });

  readonly isEmpty = computed(() => this.stats().total === 0);
  readonly allDone = computed(() => !this.isEmpty() && this.stats().remaining === 0);

  constructor() {
    // 4. EFFECT for side effects only (persistence here), never to compute state
    effect(() => {
      const todos = this._todos();
      // untracked: read something without making it a dependency of this effect
      const filter = untracked(() => this._filter());
      globalThis.localStorage?.setItem('todos', JSON.stringify({ todos, filter }));
    });
  }

  // 5. ACTIONS: the only way to change state - immutable updates
  add(title: string, priority: Todo['priority'] = 'low'): void {
    const todo: Todo = { id: crypto.randomUUID(), title, done: false, priority };
    this._todos.update((current) => [...current, todo]);
  }

  toggle(id: string): void {
    this._todos.update((current) => current.map((t) => (t.id === id ? { ...t, done: !t.done } : t)));
  }

  remove(id: string): void {
    this._todos.update((current) => current.filter((t) => t.id !== id));
  }

  setFilter(filter: Filter): void {
    this._filter.set(filter);
  }

  setQuery(query: string): void {
    this._query.set(query);
  }

  load(todos: readonly Todo[]): void {
    this._todos.set(todos);
  }
}

// ---------------------------------------------------------------- linkedSignal: writable state derived from a source

@Injectable({ providedIn: 'root' })
export class SelectionState {
  private readonly store = inject(TodoStore);

  /**
   * linkedSignal = local writable state that RESETS when its source changes.
   * Here: the selected todo resets to the first visible one whenever the filter/search changes,
   * but the user can still select something else. Before v19 this needed an effect + a bug.
   */
  readonly selectedId = linkedSignal<readonly Todo[], string | null>({
    source: () => this.store.visibleTodos(),
    computation: (todos, previous) => {
      const previousId = previous?.value ?? null;
      const stillVisible = todos.some((todo) => todo.id === previousId);
      return stillVisible ? previousId : (todos[0]?.id ?? null);
    },
  });

  readonly selectedTodo = computed(() => this.store.todos().find((t) => t.id === this.selectedId()) ?? null);

  select(id: string): void {
    this.selectedId.set(id);
  }
}

// ---------------------------------------------------------------- resource(): async state as signals

export interface UserProfile {
  id: number;
  name: string;
}

@Injectable({ providedIn: 'root' })
export class ProfileState {
  readonly userId = signal<number | undefined>(undefined);

  /**
   * resource() re-runs the loader whenever `params` changes, gives you value/isLoading/error signals,
   * aborts the previous request (the loader receives an AbortSignal), and supports reload().
   * For plain HTTP prefer httpResource(); for an Observable source use rxResource().
   */
  readonly profile = resource({
    params: () => this.userId(),
    loader: async ({ params: id, abortSignal }) => {
      const response = await fetch(`/api/users/${id}`, { signal: abortSignal });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      return (await response.json()) as UserProfile;
    },
  });

  readonly greeting = computed(() => {
    if (this.profile.isLoading()) return 'loading...';
    if (this.profile.error()) return 'could not load profile';
    return `hello ${this.profile.value()?.name ?? 'stranger'}`;
  });
}

/**
 * STATE MANAGEMENT CHOICES (what to say in an interview)
 *
 *  Component signals        - state used by one component and its children. Start here.
 *  A signal store service   - shared feature/app state (the TodoStore above). Covers most apps.
 *  NgRx SignalStore         - a structured store with signals + entities + RxJS methods; less boilerplate than classic NgRx.
 *  NgRx (Redux style)       - actions/reducers/selectors/effects: strict unidirectional flow, time-travel devtools,
 *                             good for big teams and complex flows; a lot of ceremony for a small app.
 *  Server state             - httpResource/rxResource, or a caching layer; don't hand-roll caches in a store.
 *
 *  SIGNALS vs RxJS: signals for synchronous state you render, RxJS for event streams over time
 *  (typeahead, websockets, retries, cancellation). Convert at the boundary with toSignal/toObservable.
 */
