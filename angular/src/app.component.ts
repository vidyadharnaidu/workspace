import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TodoListComponent, CardDemoComponent } from '../01_components/component_basics';
import { RowsParentComponent, TotalsComponent } from '../01_components/change_detection';
import { DirectivesDemoComponent } from '../02_directives_pipes/directives_and_pipes';
import { SignupFormComponent } from '../05_forms/reactive_forms';

@Component({
  selector: 'app-root',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    TodoListComponent,
    CardDemoComponent,
    RowsParentComponent,
    TotalsComponent,
    DirectivesDemoComponent,
    SignupFormComponent,
  ],
  template: `
    <div class="layout">
      <!-- Top Navigation Header -->
      <header class="header">
        <div class="header__brand">
          <div class="logo-badge">NG21</div>
          <div>
            <h1>Angular 21 Component Showcase</h1>
            <p class="subtitle">Standalone &bull; Signals-First &bull; Strict Templates &bull; OnPush</p>
          </div>
        </div>

        <nav class="nav-tabs" role="tablist">
          <button
            type="button"
            class="tab-btn"
            [class.active]="activeTab() === 'components'"
            (click)="activeTab.set('components')"
          >
            01. Components &amp; Signals
          </button>
          <button
            type="button"
            class="tab-btn"
            [class.active]="activeTab() === 'detection'"
            (click)="activeTab.set('detection')"
          >
            02. Change Detection
          </button>
          <button
            type="button"
            class="tab-btn"
            [class.active]="activeTab() === 'directives'"
            (click)="activeTab.set('directives')"
          >
            03. Directives &amp; Pipes
          </button>
          <button
            type="button"
            class="tab-btn"
            [class.active]="activeTab() === 'forms'"
            (click)="activeTab.set('forms')"
          >
            04. Reactive Forms
          </button>
        </nav>
      </header>

      <!-- Main Showcase Canvas -->
      <main class="content">
        @switch (activeTab()) {
          @case ('components') {
            <section class="demo-section">
              <div class="section-heading">
                <h2>01_components / component_basics.ts</h2>
                <p>Demonstrates signal inputs, outputs, models, <code>&#64;for</code> with mandatory <code>track</code>, and two-way binding.</p>
              </div>

              <div class="grid-2">
                <!-- Todo List Card -->
                <div class="showcase-card">
                  <div class="card-header">
                    <span class="badge">TodoListComponent</span>
                    <h3>Interactive Signal Todo List</h3>
                  </div>
                  <div class="card-body">
                    <app-todo-list />
                  </div>
                  <div class="card-footer">
                    <code>signal()</code> + <code>computed()</code> + <code>&#64;for (todo of visibleTodos(); track todo.id)</code>
                  </div>
                </div>

                <!-- Card Projection Card -->
                <div class="showcase-card">
                  <div class="card-header">
                    <span class="badge">CardDemoComponent</span>
                    <h3>Content Projection &amp; model()</h3>
                  </div>
                  <div class="card-body">
                    <app-card-demo />
                  </div>
                  <div class="card-footer">
                    <code>&lt;ng-content select="[card-title]" /&gt;</code> and two-way <code>[(collapsed)]</code>
                  </div>
                </div>
              </div>
            </section>
          }

          @case ('detection') {
            <section class="demo-section">
              <div class="section-heading">
                <h2>01_components / change_detection.ts</h2>
                <p>Learn the OnPush mutation trap and how computed signals prevent unnecessary re-evaluations.</p>
              </div>

              <div class="grid-2">
                <!-- Mutation Trap Card -->
                <div class="showcase-card">
                  <div class="card-header">
                    <span class="badge badge-warning">The Mutation Trap</span>
                    <h3>RowsParentComponent (OnPush)</h3>
                  </div>
                  <div class="card-body">
                    <p class="desc">Clicking <strong>wrong</strong> pushes to the array without changing reference (OnPush skips). Clicking <strong>right</strong> updates immutably.</p>
                    <div class="demo-container">
                      <app-rows-parent />
                    </div>
                  </div>
                  <div class="card-footer">
                    <code>this.rows.update(curr =&gt; [...curr, newRow])</code>
                  </div>
                </div>

                <!-- Totals Component -->
                <div class="showcase-card">
                  <div class="card-header">
                    <span class="badge">TotalsComponent</span>
                    <h3>Computed vs Template Methods</h3>
                  </div>
                  <div class="card-body">
                    <p class="desc">Computed values cache results and only recompute when input dependencies change:</p>
                    <div class="demo-container highlight-box">
                      <span>Total sum of [10, 25, 45, 120]:</span>
                      <strong><app-totals [items]="[10, 25, 45, 120]" /></strong>
                    </div>
                  </div>
                  <div class="card-footer">
                    <code>readonly total = computed(() =&gt; this.items().reduce(...))</code>
                  </div>
                </div>
              </div>
            </section>
          }

          @case ('directives') {
            <section class="demo-section">
              <div class="section-heading">
                <h2>02_directives_pipes / directives_and_pipes.ts</h2>
                <p>Attribute directives, structural template reproduction, pure/impure pipes, and built-in pipes.</p>
              </div>

              <div class="showcase-card">
                <div class="card-header">
                  <span class="badge">DirectivesDemoComponent</span>
                  <h3>Directives &amp; Pipes Live Demo</h3>
                </div>
                <div class="card-body directives-box">
                  <app-directives-demo />
                </div>
                <div class="card-footer">
                  <code>[appHighlight]</code>, <code>*appRepeat="3"</code>, <code>| truncate: 10</code>, <code>| timeAgo</code>
                </div>
              </div>
            </section>
          }

          @case ('forms') {
            <section class="demo-section">
              <div class="section-heading">
                <h2>05_forms / reactive_forms.ts</h2>
                <p>Strictly typed <code>NonNullableFormBuilder</code>, cross-field password matching, async email validation, and FormArray.</p>
              </div>

              <div class="showcase-card max-w-form">
                <div class="card-header">
                  <span class="badge">SignupFormComponent</span>
                  <h3>Typed Reactive Form &amp; Validation</h3>
                </div>
                <div class="card-body">
                  <app-signup-form />
                </div>
                <div class="card-footer">
                  Try <code>taken&#64;example.com</code> or mismatched passwords to see real-time validator feedback!
                </div>
              </div>
            </section>
          }
        }
      </main>
    </div>
  `,
  styles: [`
    .layout {
      max-width: 1200px;
      margin: 0 auto;
      padding: 2rem 1.5rem 4rem;
    }

    .header {
      background: rgba(18, 24, 38, 0.7);
      backdrop-filter: blur(12px);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-xl);
      padding: 1.5rem 2rem;
      margin-bottom: 2rem;
      box-shadow: var(--shadow-subtle);
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
    }

    .header__brand {
      display: flex;
      align-items: center;
      gap: 1.25rem;
    }

    .logo-badge {
      width: 48px;
      height: 48px;
      border-radius: var(--radius-md);
      background: linear-gradient(135deg, #e11d48, #6366f1);
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 800;
      font-size: 1rem;
      color: white;
      box-shadow: 0 0 20px rgba(225, 29, 72, 0.4);
    }

    h1 {
      font-size: 1.5rem;
      font-weight: 700;
      letter-spacing: -0.02em;
    }

    .subtitle {
      color: var(--text-muted);
      font-size: 0.85rem;
      margin-top: 0.15rem;
    }

    .nav-tabs {
      display: flex;
      gap: 0.5rem;
      overflow-x: auto;
      padding-bottom: 0.25rem;
    }

    .tab-btn {
      background: transparent;
      border: 1px solid transparent;
      color: var(--text-muted);
      padding: 0.6rem 1.2rem;
      border-radius: var(--radius-md);
      font-size: 0.875rem;
      font-weight: 600;
      cursor: pointer;
      white-space: nowrap;
      transition: all 0.2s ease;
    }

    .tab-btn:hover {
      background: rgba(255, 255, 255, 0.05);
      color: var(--text-main);
    }

    .tab-btn.active {
      background: var(--bg-surface);
      color: #fff;
      border-color: var(--accent-primary);
      box-shadow: 0 0 15px rgba(99, 102, 241, 0.2);
    }

    .section-heading {
      margin-bottom: 1.5rem;
    }

    .section-heading h2 {
      font-size: 1.25rem;
      font-family: var(--font-mono);
      color: var(--accent-primary);
      margin-bottom: 0.25rem;
    }

    .section-heading p {
      color: var(--text-muted);
      font-size: 0.9rem;
    }

    .grid-2 {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
      gap: 1.5rem;
    }

    .showcase-card {
      background: rgba(18, 24, 38, 0.6);
      backdrop-filter: blur(10px);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-lg);
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }

    .max-w-form {
      max-width: 650px;
      margin: 0 auto;
    }

    .card-header {
      padding: 1.25rem 1.5rem;
      border-bottom: 1px solid var(--border-subtle);
      background: rgba(255, 255, 255, 0.02);
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .card-header h3 {
      font-size: 1.1rem;
      font-weight: 600;
    }

    .badge {
      align-self: flex-start;
      font-size: 0.72rem;
      font-family: var(--font-mono);
      font-weight: 600;
      padding: 0.2rem 0.5rem;
      border-radius: 4px;
      background: rgba(99, 102, 241, 0.15);
      color: #a5b4fc;
      border: 1px solid rgba(99, 102, 241, 0.3);
    }

    .badge-warning {
      background: rgba(245, 158, 11, 0.15);
      color: #fcd34d;
      border-color: rgba(245, 158, 11, 0.3);
    }

    .card-body {
      padding: 1.5rem;
      flex: 1;
    }

    .card-footer {
      padding: 0.75rem 1.5rem;
      border-top: 1px solid var(--border-subtle);
      background: rgba(10, 14, 23, 0.4);
      font-size: 0.75rem;
      color: var(--text-muted);
    }

    .card-footer code {
      color: #818cf8;
    }

    .desc {
      color: var(--text-muted);
      font-size: 0.875rem;
      margin-bottom: 1rem;
      line-height: 1.4;
    }

    .demo-container {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .highlight-box {
      padding: 1rem;
      background: rgba(255, 255, 255, 0.03);
      border-radius: var(--radius-sm);
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .highlight-box strong output {
      font-size: 1.25rem;
      color: var(--accent-success);
    }

    .directives-box p {
      margin-bottom: 0.5rem;
      padding: 0.4rem 0.6rem;
      border-radius: var(--radius-sm);
    }
  `],
})
export class AppComponent {
  readonly activeTab = signal<'components' | 'detection' | 'directives' | 'forms'>('components');
}
