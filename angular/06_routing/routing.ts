/**
 * Routing: lazy loading, params, functional guards and resolvers.
 *
 * REMEMBER:
 *  * Standalone apps use provideRouter(routes) - no RouterModule.forRoot.
 *  * LAZY LOAD with loadComponent (one component) or loadChildren (a feature area): the code is
 *    downloaded only when the route is visited. This is the main answer to "how do you keep the bundle small?".
 *  * GUARDS are plain functions now: CanActivateFn / CanMatchFn / CanDeactivateFn (and inject() works inside).
 *    CanMatch is better than CanActivate for lazy routes: it prevents even DOWNLOADING the chunk.
 *  * Read params as INPUTS with withComponentInputBinding() - no ActivatedRoute boilerplate.
 *  * paramMap/queryParamMap are observables because Angular REUSES a component when only the param changes;
 *    ngOnInit does not run again, which is the classic "my page doesn't update" bug.
 */
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  ActivatedRoute,
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
  type CanActivateFn,
  type CanDeactivateFn,
  type CanMatchFn,
  type ResolveFn,
  type Routes,
} from '@angular/router';
import { map } from 'rxjs';
import { UserService, type User } from '../03_services_di/services_and_di';
import { TokenStore } from '../03_services_di/services_and_di';

// ---------------------------------------------------------------- functional guards

export const authGuard: CanActivateFn = (_route, state) => {
  const token = inject(TokenStore).token();
  const router = inject(Router);
  // Returning a UrlTree redirects instead of just blocking
  return token ? true : router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

export const adminOnlyGuard: CanMatchFn = (route) => {
  const roles = (route.data?.['roles'] as string[] | undefined) ?? [];
  const isAdmin = roles.includes('admin');
  // CanMatch on a lazy route: if it returns false the chunk is never downloaded
  return isAdmin || inject(Router).createUrlTree(['/forbidden']);
};

export interface CanLeave {
  hasUnsavedChanges: () => boolean;
}

export const unsavedChangesGuard: CanDeactivateFn<CanLeave> = (component) =>
  !component.hasUnsavedChanges() || confirm('You have unsaved changes. Leave anyway?');

// ---------------------------------------------------------------- resolver (data before the route activates)

export const userResolver: ResolveFn<User> = (route) => {
  const id = Number(route.paramMap.get('id'));
  return inject(UserService).getUser(id);
  // Trade-off: the navigation waits for the data (no flicker), but a slow API feels like a frozen app.
  // Modern alternative: navigate immediately and let the component show its own loading state.
};

// ---------------------------------------------------------------- route table

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'products' },

  // eager component
  {
    path: 'login',
    title: 'Sign in',
    loadComponent: () => import('./routing').then((m) => m.LoginComponent),
  },

  // lazy component + route params bound to component inputs
  {
    path: 'products',
    title: 'Products',
    loadComponent: () => import('./routing').then((m) => m.ProductListComponent),
    children: [
      {
        path: ':id',
        loadComponent: () => import('./routing').then((m) => m.ProductDetailComponent),
        resolve: { user: userResolver },
        canDeactivate: [unsavedChangesGuard],
      },
    ],
  },

  // lazy FEATURE with its own routes + guards
  {
    path: 'admin',
    canMatch: [adminOnlyGuard],
    canActivate: [authGuard],
    data: { roles: ['admin'] },
    loadChildren: () => import('./routing').then((m) => m.ADMIN_ROUTES),
  },

  { path: '**', loadComponent: () => import('./routing').then((m) => m.NotFoundComponent) }, // wildcard LAST
];

export const ADMIN_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./routing').then((m) => m.AdminHomeComponent) },
];

/**
 * main.ts:
 *   bootstrapApplication(AppComponent, {
 *     providers: [provideRouter(routes, withComponentInputBinding(), withViewTransitions())],
 *   });
 */

// ---------------------------------------------------------------- components

@Component({
  selector: 'app-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <nav>
      <a routerLink="/products" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Products</a>
      <a [routerLink]="['/products', 42]" [queryParams]="{ ref: 'nav' }">Product 42</a>
    </nav>
    <router-outlet />
  `,
})
export class ShellComponent {}

@Component({
  selector: 'app-product-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink],
  template: `
    <ul>
      @for (id of ids; track id) {
        <li><a [routerLink]="[id]">product {{ id }}</a></li>
      }
    </ul>
    <router-outlet />
  `,
})
export class ProductListComponent {
  readonly ids = [1, 2, 3];
}

@Component({
  selector: 'app-product-detail',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2>Product {{ id() }}</h2>
    <p>resolved user: {{ user()?.name ?? 'none' }}</p>
    <p>query ref: {{ ref() }}</p>
    <button type="button" (click)="dirty.set(true)">make dirty</button>
    <button type="button" (click)="goBackToList()">back</button>
  `,
})
export class ProductDetailComponent implements CanLeave {
  // withComponentInputBinding(): route params, query params and resolved data arrive as INPUTS
  readonly id = input.required<string>();
  readonly ref = input<string>();
  readonly user = input<User>();

  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  readonly dirty = signal(false);

  // The observable way (needed when the component is REUSED across param changes).
  // toSignal subscribes AND unsubscribes for you - never subscribe by hand without cleanup.
  readonly idFromParams = toSignal(this.route.paramMap.pipe(map((params) => params.get('id'))), {
    initialValue: null,
  });

  readonly title = computed(() => `Product ${this.id()}`);

  hasUnsavedChanges(): boolean {
    return this.dirty();
  }

  goBackToList(): void {
    void this.router.navigate(['../'], { relativeTo: this.route });
  }
}

@Component({
  selector: 'app-login',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<button type="button" (click)="signIn()">Sign in</button>`,
})
export class LoginComponent {
  private readonly tokens = inject(TokenStore);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  signIn(): void {
    this.tokens.setToken('demo-token');
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/products';
    void this.router.navigateByUrl(returnUrl);
  }
}

@Component({
  selector: 'app-admin-home',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<p>admin area</p>`,
})
export class AdminHomeComponent {}

@Component({
  selector: 'app-not-found',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<p>404 - not found</p>`,
})
export class NotFoundComponent {}
