/**
 * RxJS in Angular: the operators and subscription rules interviews focus on.
 *
 * THE FLATTENING OPERATORS (the most asked RxJS question):
 *   switchMap  - CANCEL the previous inner observable     -> typeahead, latest-wins reads, route param changes
 *   mergeMap   - run all inner observables CONCURRENTLY   -> independent parallel work (order not guaranteed)
 *   concatMap  - QUEUE them, one after another, in order  -> writes that must keep order (PUT/POST sequences)
 *   exhaustMap - IGNORE new ones while one is running     -> login/submit buttons (prevents double submits)
 *
 * SUBJECTS:
 *   Subject         - multicast, no initial value, late subscribers miss past values
 *   BehaviorSubject - holds the CURRENT value, emits it immediately to new subscribers (classic store)
 *   ReplaySubject   - replays the last N values
 *   AsyncSubject    - emits only the final value on complete
 *
 * UNSUBSCRIBING (memory leaks are a favourite interview trap):
 *   1. `| async` in the template: Angular subscribes and unsubscribes for you.  <- prefer this
 *   2. takeUntilDestroyed() in an injection context.
 *   3. toSignal(...) - converts to a signal and cleans up automatically.
 *   HttpClient completes after one value, so those subscriptions do not leak, but timers and
 *   subjects DO leak forever if you never unsubscribe.
 */
import { AsyncPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, Injectable, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import {
  BehaviorSubject,
  Observable,
  Subject,
  catchError,
  combineLatest,
  concatMap,
  debounceTime,
  distinctUntilChanged,
  exhaustMap,
  filter,
  forkJoin,
  interval,
  map,
  merge,
  of,
  scan,
  shareReplay,
  startWith,
  switchMap,
  tap,
  withLatestFrom,
} from 'rxjs';

export interface Product {
  id: number;
  name: string;
  price: number;
}

// ---------------------------------------------------------------- a BehaviorSubject store (pre-signals pattern)

@Injectable({ providedIn: 'root' })
export class CartStore {
  // private writable subject + public readonly observable = encapsulation
  private readonly itemsSubject = new BehaviorSubject<Product[]>([]);
  readonly items$ = this.itemsSubject.asObservable();

  // derived streams
  readonly count$ = this.items$.pipe(map((items) => items.length));
  readonly total$ = this.items$.pipe(
    map((items) => items.reduce((sum, item) => sum + item.price, 0)),
    shareReplay({ bufferSize: 1, refCount: true }), // share one computation with every subscriber
  );

  add(product: Product): void {
    this.itemsSubject.next([...this.itemsSubject.value, product]); // immutable update
  }

  clear(): void {
    this.itemsSubject.next([]);
  }
}

// ---------------------------------------------------------------- typeahead: the canonical switchMap pipeline

@Injectable({ providedIn: 'root' })
export class SearchService {
  private readonly http = inject(HttpClient);

  search(term: string): Observable<Product[]> {
    return this.http.get<Product[]>('/api/products', { params: { q: term } });
  }
}

@Component({
  selector: 'app-typeahead',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AsyncPipe],
  template: `
    <input (input)="onType($any($event.target).value)" placeholder="Search products" />
    <!-- async pipe: subscribes, re-renders on emit, unsubscribes on destroy -->
    @for (product of results$ | async; track product.id) {
      <p>{{ product.name }}</p>
    }
  `,
})
export class TypeaheadComponent {
  private readonly search = inject(SearchService);
  private readonly terms = new Subject<string>();

  readonly results$ = this.terms.pipe(
    debounceTime(300), //     wait for a pause in typing
    distinctUntilChanged(), // ignore "abc" -> "abc"
    filter((term) => term.length >= 2),
    switchMap((term) => this.search.search(term).pipe(catchError(() => of([])))), // cancel the previous request
    startWith([] as Product[]),
  );

  onType(term: string): void {
    this.terms.next(term);
  }
}

// ---------------------------------------------------------------- submit: exhaustMap prevents double submits

@Component({
  selector: 'app-submit-button',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<button type="button" (click)="clicks.next()">Save</button>`,
})
export class SubmitButtonComponent {
  private readonly http = inject(HttpClient);
  readonly clicks = new Subject<void>();

  constructor() {
    this.clicks
      .pipe(
        exhaustMap(() => this.http.post<void>('/api/save', {})), // clicks during the request are ignored
        takeUntilDestroyed(), // must be called in an injection context (constructor/field initializer)
      )
      .subscribe();
  }
}

// ---------------------------------------------------------------- combining streams

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  /** forkJoin: wait for ALL to complete, emit once (like Promise.all) - good for parallel initial loads. */
  loadDashboard(): Observable<{ user: { id: number }; products: Product[] }> {
    return forkJoin({
      user: this.http.get<{ id: number }>('/api/me'),
      products: this.http.get<Product[]>('/api/products'),
    });
  }

  /** combineLatest: emit whenever ANY source emits (after all have emitted once) - live filters. */
  filteredProducts(products$: Observable<Product[]>, query$: Observable<string>): Observable<Product[]> {
    return combineLatest([products$, query$]).pipe(
      map(([products, query]) => products.filter((p) => p.name.toLowerCase().includes(query.toLowerCase()))),
    );
  }

  /** withLatestFrom: one stream drives, the other only contributes its latest value. */
  saveOnClick(clicks$: Observable<void>, form$: Observable<Product>): Observable<Product> {
    return clicks$.pipe(
      withLatestFrom(form$),
      map(([, form]) => form),
      concatMap((form) => this.http.post<Product>('/api/products', form)), // keep submit order
    );
  }
}

// ---------------------------------------------------------------- signals <-> observables interop

@Component({
  selector: 'app-interop',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <p>query: {{ query() }}</p>
    <p>ticks: {{ ticks() }}</p>
    <p>results: {{ results().length }}</p>
  `,
})
export class InteropComponent {
  private readonly search = inject(SearchService);
  readonly query = signal('');

  // signal -> observable, pipe with RxJS, then back to a signal for the template
  readonly results = toSignal(
    toObservable(this.query).pipe(
      debounceTime(300),
      switchMap((term) => (term ? this.search.search(term) : of([]))),
    ),
    { initialValue: [] as Product[] },
  );

  // toSignal subscribes and unsubscribes automatically when the component is destroyed
  readonly ticks = toSignal(interval(1000).pipe(map((n) => n + 1)), { initialValue: 0 });
}

// ---------------------------------------------------------------- creating and inspecting streams

export function rxjsRecipes(): Record<string, Observable<unknown>> {
  const clicks$ = new Subject<number>();

  return {
    // accumulate state over time (a mini reducer)
    runningTotal$: clicks$.pipe(scan((total, value) => total + value, 0)),
    // merge several sources into one stream
    anyEvent$: merge(of('a'), of('b')),
    // side effects for logging/debugging without changing the stream
    debugged$: clicks$.pipe(tap((value) => console.log('click', value))),
    // multicast an expensive source to many subscribers
    shared$: clicks$.pipe(shareReplay({ bufferSize: 1, refCount: true })),
  };
}

/**
 * COLD vs HOT
 *   Cold: each subscriber gets its own execution (HttpClient - two subscribes = two requests).
 *   Hot:  one execution shared by all subscribers (Subject, or cold + share()/shareReplay()).
 *   This is why an unshared http$ used twice in a template (with two async pipes) fires two requests.
 *
 * SIGNALS OR RXJS?
 *   Signals: synchronous state you render (component state, derived values).
 *   RxJS:    asynchronous EVENT streams over time (typeahead, websockets, retries, cancellation, combining).
 *   Convert at the edges with toSignal/toObservable.
 */
