/**
 * Directives and pipes.
 *
 * THREE KINDS OF DIRECTIVE:
 *  1. Components          - a directive WITH a template (the one you use all day).
 *  2. Attribute directives- change appearance/behaviour of an element ([appHighlight], ngClass, ngStyle).
 *  3. Structural directives - change the DOM layout via a template (*appRepeat, historically *ngIf/*ngFor).
 *     Since v17 use the built-in @if/@for/@switch instead of *ngIf/*ngFor - but interviewers still ask
 *     how a structural directive works: the * syntax is sugar for <ng-template> + TemplateRef + ViewContainerRef.
 *
 * PIPES:
 *  pure (default) - re-runs only when the input REFERENCE changes: fast, cacheable.
 *  impure         - runs on every change-detection cycle: avoid (AsyncPipe is the notable impure exception).
 *  Never do heavy work or HTTP in a pipe; never mutate the input.
 */
import { CurrencyPipe, DatePipe, PercentPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  Directive,
  ElementRef,
  Pipe,
  TemplateRef,
  ViewContainerRef,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
  type PipeTransform,
} from '@angular/core';

// ---------------------------------------------------------------- attribute directive

@Directive({
  selector: '[appHighlight]',
  host: {
    '[style.background-color]': 'active() ? color() : null',
    '(mouseenter)': 'active.set(true)',
    '(mouseleave)': 'active.set(false)',
    '(click)': 'highlighted.emit(element.nativeElement.textContent ?? "")',
  },
})
export class HighlightDirective {
  readonly color = input('yellow', { alias: 'appHighlight' }); // alias so [appHighlight]="'cyan'" works
  readonly highlighted = output<string>();
  protected readonly active = signal(false);
  protected readonly element = inject(ElementRef<HTMLElement>);
}

// ---------------------------------------------------------------- structural directive

/** Usage: <p *appRepeat="3; let i = index">row {{ i }}</p> */
@Directive({ selector: '[appRepeat]' })
export class RepeatDirective {
  readonly times = input.required<number>({ alias: 'appRepeat' });

  private readonly templateRef = inject(TemplateRef<RepeatContext>);
  private readonly viewContainer = inject(ViewContainerRef);

  constructor() {
    effect(() => {
      const count = this.times();
      this.viewContainer.clear();
      for (let index = 0; index < count; index++) {
        // Each embedded view gets its own context: `let i = index` reads from it
        this.viewContainer.createEmbeddedView(this.templateRef, { $implicit: index, index });
      }
    });
  }

  // Tells strictTemplates what `let` variables exist inside the template
  static ngTemplateContextGuard(_dir: RepeatDirective, _ctx: unknown): _ctx is RepeatContext {
    return true;
  }
}

export interface RepeatContext {
  $implicit: number;
  index: number;
}

// ---------------------------------------------------------------- pipes

@Pipe({ name: 'truncate' }) // pure by default
export class TruncatePipe implements PipeTransform {
  transform(value: string, limit = 20, suffix = '...'): string {
    return value.length <= limit ? value : value.slice(0, limit).trimEnd() + suffix;
  }
}

@Pipe({ name: 'timeAgo' })
export class TimeAgoPipe implements PipeTransform {
  transform(value: Date | string, now: Date = new Date()): string {
    const date = typeof value === 'string' ? new Date(value) : value;
    const seconds = Math.floor((now.getTime() - date.getTime()) / 1000);
    const units: [number, string][] = [
      [60, 'second'],
      [3600, 'minute'],
      [86400, 'hour'],
      [2592000, 'day'],
    ];
    if (seconds < 60) return 'just now';
    for (let i = 1; i < units.length; i++) {
      const [limit, name] = units[i]!;
      if (seconds < limit) {
        const amount = Math.floor(seconds / units[i - 1]![0]);
        return `${amount} ${name}${amount === 1 ? '' : 's'} ago`;
      }
    }
    return date.toISOString().slice(0, 10);
  }
}

/**
 * IMPURE pipe: runs on EVERY change-detection cycle. Shown so you can explain why it is a trap -
 * a filter pipe like this is the classic Angular performance question. Filter with computed() instead.
 */
@Pipe({ name: 'filterBy', pure: false })
export class FilterByPipe implements PipeTransform {
  transform<T>(items: readonly T[], predicate: (item: T) => boolean): T[] {
    return items.filter(predicate);
  }
}

// ---------------------------------------------------------------- usage

@Component({
  selector: 'app-directives-demo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  // TRAP: in a standalone component every template dependency must be imported -
  // including Angular's built-in pipes (there is no CommonModule in scope automatically).
  imports: [HighlightDirective, RepeatDirective, TruncatePipe, TimeAgoPipe, CurrencyPipe, DatePipe, PercentPipe],
  template: `
    <p [appHighlight]="'lightgreen'" (highlighted)="lastClicked.set($event)">hover and click me</p>
    <p>last clicked: {{ lastClicked() }}</p>

    <p *appRepeat="3; let i = index">row {{ i }}</p>

    <p>{{ longText | truncate: 10 }}</p>
    <p>{{ posted | timeAgo: now }}</p>

    <!-- built-in pipes: date, currency, decimal, percent, json, keyvalue, slice, uppercase, titlecase, async -->
    <p>{{ price | currency: 'EUR' }} - {{ posted | date: 'mediumDate' }} - {{ ratio | percent: '1.0-1' }}</p>

    <!-- BETTER than a filter pipe: derive with computed() -->
    <ul>
      @for (name of visibleNames(); track name) {
        <li>{{ name }}</li>
      }
    </ul>
  `,
})
export class DirectivesDemoComponent {
  readonly lastClicked = signal('');
  readonly longText = 'a very long description that will be truncated';
  readonly posted = new Date('2026-01-15T10:00:00Z');
  readonly now = new Date('2026-01-15T12:30:00Z');
  readonly price = 19.99;
  readonly ratio = 0.256;

  readonly names = signal(['ann', 'bob', 'cat']);
  readonly query = signal('a');
  readonly visibleNames = computed(() => this.names().filter((n) => n.includes(this.query())));
}
