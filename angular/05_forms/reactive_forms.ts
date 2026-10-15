/**
 * Forms: reactive (typed) and template-driven.
 *
 * WHICH ONE?
 *  REACTIVE  - the form model lives in the class: typed, testable, dynamic, easy validation. Default choice.
 *  TEMPLATE-DRIVEN - ngModel in the template: quick for tiny forms, harder to test and type.
 *
 * REMEMBER:
 *  * Typed forms (v14+): FormGroup<{...}> infers value types. Use NonNullableFormBuilder so
 *    control.value is T instead of T | null after reset.
 *  * Validators: sync return ValidationErrors | null; async return Observable<ValidationErrors | null>.
 *  * Cross-field rules go on the GROUP, not the control.
 *  * Control state: pristine/dirty, touched/untouched, valid/invalid/pending, enabled/disabled.
 *    Show errors only when (touched || submitted) so the form doesn't shout while typing.
 *  * setValue requires every field; patchValue updates part of it.
 *  * valueChanges/statusChanges are observables: debounce them for autosave/live validation.
 */
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormArray,
  FormControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
  type AsyncValidatorFn,
  type ValidationErrors,
  type ValidatorFn,
} from '@angular/forms';
import { debounceTime, map, of, switchMap, timer } from 'rxjs';

// ---------------------------------------------------------------- custom validators (pure, testable)

/** Sync validator: returns null when valid, or an error object that the template can read. */
export function strongPassword(minLength = 8): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = String(control.value ?? '');
    if (!value) return null; // let `required` report emptiness
    const problems: string[] = [];
    if (value.length < minLength) problems.push(`at least ${minLength} characters`);
    if (!/[0-9]/.test(value)) problems.push('a number');
    if (!/[A-Z]/.test(value)) problems.push('an uppercase letter');
    return problems.length ? { strongPassword: { missing: problems } } : null;
  };
}

/** Cross-field validator: goes on the GROUP so it can see both controls. */
export const passwordsMatch: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
  const password = group.get('password')?.value;
  const confirm = group.get('confirmPassword')?.value;
  return password && confirm && password !== confirm ? { passwordsMismatch: true } : null;
};

/** Async validator: debounce, then ask the server. Control status is PENDING while it runs. */
export function uniqueEmailValidator(taken: readonly string[]): AsyncValidatorFn {
  return (control: AbstractControl) =>
    timer(300).pipe(
      switchMap(() => of(taken.includes(String(control.value).toLowerCase()))),
      map((isTaken) => (isTaken ? { emailTaken: true } : null)),
    );
}

// ---------------------------------------------------------------- typed reactive form

@Component({
  selector: 'app-signup-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule],
  template: `
    <form [formGroup]="form" (ngSubmit)="submit()">
      <label>
        Email
        <input type="email" formControlName="email" [attr.aria-invalid]="showError('email')" />
      </label>
      @if (showError('email')) {
        <span role="alert">
          @if (form.controls.email.hasError('required')) { Email is required }
          @else if (form.controls.email.hasError('email')) { Enter a valid email }
          @else if (form.controls.email.hasError('emailTaken')) { That email is already registered }
        </span>
      }
      @if (form.controls.email.pending) {
        <span>checking availability...</span>
      }

      <label>
        Password
        <input type="password" formControlName="password" />
      </label>
      @if (showError('password') && form.controls.password.hasError('strongPassword')) {
        <span role="alert">Needs {{ passwordProblems().join(', ') }}</span>
      }

      <label>
        Confirm
        <input type="password" formControlName="confirmPassword" />
      </label>
      @if (form.hasError('passwordsMismatch') && form.controls.confirmPassword.touched) {
        <span role="alert">Passwords do not match</span>
      }

      <fieldset formArrayName="tags">
        <legend>Tags</legend>
        @for (tag of tags.controls; track $index) {
          <input [formControlName]="$index" />
        }
        <button type="button" (click)="addTag()">add tag</button>
      </fieldset>

      <button type="submit" [disabled]="form.invalid || submitting()">
        {{ submitting() ? 'Creating...' : 'Create account' }}
      </button>
    </form>
    <p>draft saved: {{ autosaved() }}</p>
  `,
})
export class SignupFormComponent {
  private readonly fb = inject(NonNullableFormBuilder); // non-nullable: values are never null after reset

  readonly form = this.fb.group(
    {
      email: this.fb.control('', {
        validators: [Validators.required, Validators.email],
        asyncValidators: [uniqueEmailValidator(['taken@example.com'])],
        updateOn: 'blur', // don't hit the server on every keystroke
      }),
      password: this.fb.control('', [Validators.required, strongPassword(8)]),
      confirmPassword: this.fb.control('', Validators.required),
      tags: this.fb.array<FormControl<string>>([]),
    },
    { validators: passwordsMatch },
  );

  readonly submitting = signal(false);
  readonly submitted = signal(false);

  get tags(): FormArray<FormControl<string>> {
    return this.form.controls.tags;
  }

  // valueChanges is an Observable: debounce for autosave, and clean up with takeUntilDestroyed
  readonly autosaved = toSignal(
    this.form.valueChanges.pipe(
      debounceTime(500),
      map((value) => `${value.email ?? ''} (${value.tags?.length ?? 0} tags)`),
      takeUntilDestroyed(),
    ),
    { initialValue: '' },
  );

  passwordProblems(): string[] {
    const error = this.form.controls.password.getError('strongPassword') as { missing: string[] } | null;
    return error?.missing ?? [];
  }

  showError(field: 'email' | 'password' | 'confirmPassword'): boolean {
    const control = this.form.controls[field];
    return control.invalid && (control.touched || this.submitted());
  }

  addTag(): void {
    this.tags.push(this.fb.control(''));
  }

  submit(): void {
    this.submitted.set(true);
    if (this.form.invalid) {
      this.form.markAllAsTouched(); // reveal every error at once
      return;
    }
    this.submitting.set(true);
    const value = this.form.getRawValue(); // getRawValue() includes DISABLED controls; .value does not
    console.log('submit', value.email, value.tags.length);
    this.submitting.set(false);
    this.form.reset(); // with NonNullableFormBuilder this resets to the initial values, not null
  }
}

/**
 * TEMPLATE-DRIVEN equivalent (FormsModule):
 *
 *   <form #f="ngForm" (ngSubmit)="save(f.value)">
 *     <input name="email" [(ngModel)]="model.email" required email #email="ngModel" />
 *     @if (email.invalid && email.touched) { <span>Invalid email</span> }
 *     <button [disabled]="f.invalid">Save</button>
 *   </form>
 *
 * Differences to mention: the model lives in the template, validation is directive-based,
 * updates are asynchronous, and it is harder to unit test - fine for a login box, not for a wizard.
 */
