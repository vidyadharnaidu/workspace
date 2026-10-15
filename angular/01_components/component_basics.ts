/**
 * Angular components: standalone, signal inputs/outputs, the built-in control flow, lifecycle.
 * Angular 21 + TypeScript.
 *
 * REMEMBER:
 *  * Components are STANDALONE by default since v19 - no NgModule; list template dependencies in `imports`.
 *  * SIGNAL INPUTS:  readonly title = input.required<string>();  read as title() in code AND templates.
 *  * OUTPUTS:        readonly saved = output<Todo>();  emit with saved.emit(value).
 *  * TWO-WAY:        readonly value = model('');  binds with [(value)] in the parent.
 *  * CONTROL FLOW:   @if / @for / @switch are built in (no *ngIf / *ngFor imports). @for REQUIRES `track`.
 *  * CHANGE DETECTION: use OnPush everywhere; with signals, Angular knows exactly what to re-render.
 *  * Prefer computed() over getters in templates: it caches and only recomputes when dependencies change.
 */
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  computed,
  effect,
  inject,
  input,
  model,
  output,
  signal,
  viewChild,
  type OnInit,
} from '@angular/core';

export interface Todo {
  id: string;
  title: string;
  done: boolean;
}

// ---------------------------------------------------------------- presentational component

@Component({
  selector: 'app-todo-item',
  changeDetection: ChangeDetectionStrategy.OnPush,
  // `host` replaces @HostBinding/@HostListener decorators
  host: {
    '[class.todo--done]': 'todo().done',
    '(dblclick)': 'edit.emit(todo())',
  },
  template: `
    <li>
      <label>
        <input type="checkbox" [checked]="todo().done" (change)="toggled.emit(todo().id)" />
        <span [class.strike]="todo().done">{{ todo().title }}</span>
      </label>
      @if (showRemove()) {
        <button type="button" (click)="removed.emit(todo().id)">remove</button>
      }
    </li>
  `,
})
export class TodoItemComponent {
  // input.required<T>() has no default: the compiler forces the parent to pass it
  readonly todo = input.required<Todo>();
  readonly showRemove = input(true); // optional input with a default
  readonly toggled = output<string>();
  readonly removed = output<string>();
  readonly edit = output<Todo>();
}

// ---------------------------------------------------------------- container component

@Component({
  selector: 'app-todo-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TodoItemComponent], // standalone: declare what the template uses
  template: `
    <form (submit)="add($event)">
      <input #titleInput name="title" [value]="draft()" (input)="draft.set($any($event.target).value)" />
      <button type="submit" [disabled]="draft().trim().length === 0">Add</button>
    </form>

    @if (todos().length === 0) {
      <p>Nothing to do.</p>
    } @else {
      <ul>
        <!-- @for REQUIRES track: it is how Angular keeps DOM nodes matched to items (like React keys) -->
        @for (todo of visibleTodos(); track todo.id) {
          <app-todo-item [todo]="todo" (toggled)="toggle($event)" (removed)="remove($event)" />
        } @empty {
          <li>No todos match this filter.</li>
        }
      </ul>
    }

    @switch (filter()) {
      @case ('all') { <small>showing everything</small> }
      @case ('active') { <small>{{ remaining() }} remaining</small> }
      @default { <small>{{ todos().length - remaining() }} done</small> }
    }

    <button type="button" (click)="filter.set('all')">all</button>
    <button type="button" (click)="filter.set('active')">active</button>
    <button type="button" (click)="filter.set('done')">done</button>
  `,
})
export class TodoListComponent implements OnInit {
  // STATE as signals: setting a signal marks exactly the views that read it as dirty
  readonly todos = signal<Todo[]>([]);
  readonly filter = signal<'all' | 'active' | 'done'>('all');
  readonly draft = signal('');

  // DERIVED state: computed is lazy + cached, unlike a method call in the template
  readonly visibleTodos = computed(() => {
    const filter = this.filter();
    return this.todos().filter((todo) => (filter === 'all' ? true : filter === 'done' ? todo.done : !todo.done));
  });
  readonly remaining = computed(() => this.todos().filter((todo) => !todo.done).length);

  // Signal-based query: replaces @ViewChild
  private readonly titleInput = viewChild<ElementRef<HTMLInputElement>>('titleInput');
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    // effect() runs after render whenever a signal it reads changes - for side effects only
    effect(() => {
      const count = this.remaining();
      document.title = count > 0 ? `(${count}) Todos` : 'Todos';
    });
  }

  ngOnInit(): void {
    // Lifecycle hooks still exist; with signals you need them far less often.
    this.todos.set([{ id: '1', title: 'learn signals', done: false }]);
    this.destroyRef.onDestroy(() => console.log('cleanup without ngOnDestroy'));
  }

  add(event: Event): void {
    event.preventDefault();
    const title = this.draft().trim();
    if (!title) return;
    // IMMUTABLE update: a new array so OnPush/computed see the change
    this.todos.update((current) => [...current, { id: crypto.randomUUID(), title, done: false }]);
    this.draft.set('');
    this.titleInput()?.nativeElement.focus();
  }

  toggle(id: string): void {
    this.todos.update((current) => current.map((t) => (t.id === id ? { ...t, done: !t.done } : t)));
  }

  remove(id: string): void {
    this.todos.update((current) => current.filter((t) => t.id !== id));
  }
}

// ---------------------------------------------------------------- content projection & two-way binding

@Component({
  selector: 'app-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="card">
      <header><ng-content select="[card-title]" /></header>
      <div class="card__body"><ng-content /></div>
      @if (collapsible()) {
        <button type="button" (click)="collapsed.set(!collapsed())">
          {{ collapsed() ? 'expand' : 'collapse' }}
        </button>
      }
    </section>
  `,
})
export class CardComponent {
  readonly collapsible = input(false);
  // model() = two-way binding: parent writes [(collapsed)]="isCollapsed"
  readonly collapsed = model(false);
}

@Component({
  selector: 'app-card-demo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CardComponent],
  template: `
    <app-card [collapsible]="true" [(collapsed)]="isCollapsed">
      <h3 card-title>Projected title</h3>
      <p>Projected body</p>
    </app-card>
    <p>collapsed: {{ isCollapsed() }}</p>
  `,
})
export class CardDemoComponent {
  readonly isCollapsed = signal(false);
}
