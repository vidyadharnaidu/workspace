/**
 * Services, dependency injection and HttpClient.
 *
 * DI IN ONE MINUTE:
 *  Angular has a hierarchical injector tree. When a class asks for a dependency, Angular walks up:
 *    element/component injector -> parent components -> environment (root) injector -> platform -> null.
 *  @Injectable({ providedIn: 'root' }) = one singleton for the app, tree-shakable (dropped if unused).
 *  Providing a service in a component's `providers` gives EACH instance of that component its own copy.
 *
 * PROVIDER RECIPES:
 *  useClass    - swap the implementation      { provide: Logger, useClass: ConsoleLogger }
 *  useValue    - constant/config              { provide: API_URL, useValue: 'https://api' }
 *  useFactory  - build it at runtime          { provide: X, useFactory: () => new X(inject(Y)) }
 *  useExisting - alias to another token       { provide: OldApi, useExisting: NewApi }
 *  multi: true - contribute to an array       HTTP_INTERCEPTORS-style extension points
 *
 * MODIFIERS: @Optional() (null if missing), @Self(), @SkipSelf(), @Host() - with inject():
 *   inject(TOKEN, { optional: true, self: true, skipSelf: true, host: true })
 */
import { HttpClient, HttpErrorResponse, HttpParams, httpResource, type HttpInterceptorFn } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, Injectable, InjectionToken, computed, inject, signal } from '@angular/core';
import { catchError, map, retry, throwError, timer, type Observable } from 'rxjs';

// ---------------------------------------------------------------- injection tokens for config

export interface ApiConfig {
  baseUrl: string;
  pageSize: number;
}

/** InjectionToken is how you inject something that has no class (config objects, primitives, functions). */
export const API_CONFIG = new InjectionToken<ApiConfig>('API_CONFIG', {
  providedIn: 'root',
  factory: () => ({ baseUrl: '/api', pageSize: 20 }), // default so the app works without explicit providers
});

// ---------------------------------------------------------------- an abstract dependency (swap in tests)

export abstract class Logger {
  abstract log(message: string): void;
}

@Injectable()
export class ConsoleLogger extends Logger {
  override log(message: string): void {
    console.log(`[app] ${message}`);
  }
}

@Injectable()
export class NoopLogger extends Logger {
  readonly messages: string[] = [];
  override log(message: string): void {
    this.messages.push(message);
  }
}

// ---------------------------------------------------------------- typed HTTP service

export interface User {
  id: number;
  name: string;
  email: string;
}

export interface Page<T> {
  items: T[];
  total: number;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  // inject() instead of constructor parameters: works in field initializers, functions and guards
  private readonly http = inject(HttpClient);
  private readonly config = inject(API_CONFIG);
  private readonly logger = inject(Logger, { optional: true });

  getUser(id: number): Observable<User> {
    return this.http.get<User>(`${this.config.baseUrl}/users/${id}`).pipe(
      // retry transient failures with a growing delay
      retry({ count: 2, delay: (_error, attempt) => timer(attempt * 200) }),
      catchError((error: HttpErrorResponse) => {
        this.logger?.log(`getUser failed: ${error.status}`);
        // Translate a transport error into a domain error; never swallow it silently
        return throwError(() => new Error(error.status === 404 ? 'User not found' : 'Could not load user'));
      }),
    );
  }

  searchUsers(term: string, page = 1): Observable<Page<User>> {
    // HttpParams is immutable: every set() returns a NEW instance (classic gotcha)
    const params = new HttpParams()
      .set('q', term)
      .set('page', page)
      .set('size', this.config.pageSize);

    return this.http.get<Page<User>>(`${this.config.baseUrl}/users`, { params }).pipe(
      map((page) => ({ ...page, items: page.items.filter((user) => user.email.includes('@')) })),
    );
  }

  createUser(user: Omit<User, 'id'>): Observable<User> {
    return this.http.post<User>(`${this.config.baseUrl}/users`, user);
  }
}

// ---------------------------------------------------------------- functional interceptors (v15+)

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const token = inject(TokenStore).token();
  // Requests are IMMUTABLE: clone to modify
  return next(token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request);
};

export const loggingInterceptor: HttpInterceptorFn = (request, next) => {
  const logger = inject(Logger, { optional: true });
  logger?.log(`${request.method} ${request.url}`);
  return next(request);
};

@Injectable({ providedIn: 'root' })
export class TokenStore {
  readonly token = signal<string | null>(null);
  setToken(value: string | null): void {
    this.token.set(value);
  }
}

/**
 * Registration in main.ts:
 *   provideHttpClient(withFetch(), withInterceptors([authInterceptor, loggingInterceptor]))
 */

// ---------------------------------------------------------------- component-scoped service

/** Not providedIn: 'root' - each component that lists it in `providers` gets its OWN instance. */
@Injectable()
export class WizardState {
  readonly step = signal(1);
  next(): void {
    this.step.update((s) => s + 1);
  }
}

@Component({
  selector: 'app-wizard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [WizardState, { provide: Logger, useClass: ConsoleLogger }], // scoped to this component + children
  template: `
    <p>step {{ state.step() }} of {{ totalSteps }}</p>
    <button type="button" (click)="state.next()" [disabled]="state.step() >= totalSteps">next</button>
  `,
})
export class WizardComponent {
  protected readonly state = inject(WizardState);
  protected readonly totalSteps = 3;
}

// ---------------------------------------------------------------- httpResource: signal-based HTTP (v20+)

@Component({
  selector: 'app-user-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (user.isLoading()) {
      <p>Loading...</p>
    } @else if (user.error()) {
      <p role="alert">Could not load the user</p>
    } @else if (user.hasValue()) {
      <p>{{ user.value().name }} - {{ user.value().email }}</p>
    }
    <button type="button" (click)="user.reload()">reload</button>
  `,
})
export class UserCardComponent {
  readonly userId = signal(1);

  // httpResource re-fetches whenever the signals in its request function change,
  // and exposes value/isLoading/error/status as SIGNALS. No subscription, no unsubscribe.
  readonly user = httpResource<User>(() => `/api/users/${this.userId()}`);

  readonly displayName = computed(() => this.user.value()?.name ?? 'unknown');
}
