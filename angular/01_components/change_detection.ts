/**
 * Change detection: the #1 Angular interview topic.
 *
 * HOW IT WORKS:
 *  Classic: Zone.js monkey-patches async APIs (events, timers, XHR). When one finishes, Angular checks
 *  the WHOLE component tree from the root and updates the DOM where bindings changed.
 *  Default strategy  -> check every component on every cycle.
 *  OnPush strategy   -> check this component only when:
 *      1. an @Input / signal input reference changes,
 *      2. an event fires from this component or its children,
 *      3. an async pipe in its template emits,
 *      4. a SIGNAL read in its template changes,
 *      5. markForCheck() is called explicitly.
 *  ZONELESS (v20+ stable, provideZonelessChangeDetection()): no Zone.js at all. Angular is notified by
 *  signals, async pipe, host listeners and markForCheck - which is why signals + OnPush are the future.
 *
 * INTERVIEW ANSWERS:
 *  * "Why didn't my view update with OnPush?" -> you MUTATED an object/array instead of replacing it.
 *  * "Why is my app slow?" -> Default CD + heavy method calls in templates + no track in @for.
 *  * Never call a method that does work in a template: it runs on every check. Use computed().
 *  * ExpressionChangedAfterItHasBeenCheckedError = you changed bound state after CD ran (usually in
 *    ngAfterViewInit); fix by setting it earlier, or in a signal/afterNextRender.
 */
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DoCheck,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
  afterNextRender,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';

// ---------------------------------------------------------------- the mutation trap

export interface Row {
  id: number;
  label: string;
}

@Component({
  selector: 'app-rows',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <!-- track by id: without it Angular destroys and recreates DOM nodes on every change -->
    @for (row of rows(); track row.id) {
      <span>{{ row.label }}</span>
    }
  `,
})
export class RowsComponent {
  readonly rows = input.required<readonly Row[]>();
}

@Component({
  selector: 'app-rows-parent',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RowsComponent],
  template: `<app-rows [rows]="rows()" /><button (click)="addWrong()">wrong</button><button (click)="addRight()">right</button>`,
})
export class RowsParentComponent {
  readonly rows = signal<Row[]>([{ id: 1, label: 'one' }]);

  addWrong(): void {
    // MUTATION: the array reference does not change, so an OnPush child may never re-render
    // (and signals see no change at all, because the signal value is the same reference).
    this.rows().push({ id: 2, label: 'two' } as Row);
  }

  addRight(): void {
    this.rows.update((current) => [...current, { id: current.length + 1, label: 'new' }]);
  }
}

// ---------------------------------------------------------------- methods vs computed in templates

@Component({
  selector: 'app-totals',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <!-- BAD: calling a plain METHOD here would run on every change-detection cycle.
         GOOD: computed() caches and only recomputes when its inputs change. -->
    <output>{{ total() }}</output>
  `,
})
export class TotalsComponent {
  readonly items = input.required<readonly number[]>();
  readonly total = computed(() => this.items().reduce((sum, n) => sum + n, 0));
}

// ---------------------------------------------------------------- lifecycle hooks in order

@Component({
  selector: 'app-lifecycle',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<p>{{ label() }}</p>`,
})
export class LifecycleComponent implements OnChanges, OnInit, DoCheck, AfterViewInit, OnDestroy {
  readonly label = input('');
  private readonly cdr = inject(ChangeDetectorRef);
  readonly log: string[] = [];

  constructor() {
    // afterNextRender runs once, browser-only: safe place to touch the DOM or measure
    afterNextRender(() => this.log.push('afterNextRender'));
  }

  // 1. before ngOnInit and on every @Input change (decorator inputs only; signal inputs don't trigger it)
  ngOnChanges(changes: SimpleChanges): void {
    this.log.push(`ngOnChanges:${Object.keys(changes).join(',')}`);
  }
  // 2. once, after the first ngOnChanges: initial data loading
  ngOnInit(): void {
    this.log.push('ngOnInit');
  }
  // 3. every change-detection run: expensive, use only for custom dirty checking
  ngDoCheck(): void {
    this.log.push('ngDoCheck');
  }
  // 4. after the view (and child views) are initialized
  ngAfterViewInit(): void {
    this.log.push('ngAfterViewInit');
    // Changing bound state here can cause ExpressionChangedAfterItHasBeenCheckedError in dev mode.
    this.cdr.markForCheck(); // ask for one more check, correctly
  }
  // 5. cleanup: unsubscribe, clear timers, remove listeners (or use DestroyRef.onDestroy)
  ngOnDestroy(): void {
    this.log.push('ngOnDestroy');
  }
}

/**
 * ChangeDetectorRef API
 *  markForCheck()   - mark this component and its ANCESTORS dirty; checked on the next cycle (OnPush + async data)
 *  detectChanges()  - run change detection for this component subtree NOW (synchronous)
 *  detach()/reattach() - take a subtree out of change detection (very high-frequency widgets)
 *
 * BOOTSTRAP (main.ts) in a modern app:
 *   bootstrapApplication(AppComponent, {
 *     providers: [
 *       provideZonelessChangeDetection(),   // no Zone.js: signals drive updates
 *       provideHttpClient(withFetch()),
 *       provideRouter(routes),
 *     ],
 *   });
 */
