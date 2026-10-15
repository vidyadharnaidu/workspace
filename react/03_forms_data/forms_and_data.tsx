/**
 * Forms, validation, data fetching and React 19's form/async features.
 *
 * REMEMBER:
 *  * Controlled input: value + onChange (React owns the value). Uncontrolled: defaultValue + ref (DOM owns it).
 *  * Validate on blur/submit, not on every keystroke, and show one clear message per field.
 *  * Every fetch needs 4 states: idle / loading / success / error (plus an empty state for lists).
 *  * Cancel or ignore stale responses (AbortController or an `active` flag) to avoid race conditions.
 *  * React 19: <form action={fn}>, useActionState, useOptimistic, useFormStatus, and use() for promises/context.
 */
import { useActionState, useOptimistic, useRef, useState, useTransition } from "react";
import { useFormStatus } from "react-dom";

// ---------------------------------------------------------------- controlled form with validation

export interface SignupValues {
  email: string;
  password: string;
  age: string;
}

export type FieldErrors = Partial<Record<keyof SignupValues, string>>;

/** Pure validation: no React, so it is trivial to unit test (see 04_logic/pure_logic.js). */
export function validateSignup(values: SignupValues): FieldErrors {
  const errors: FieldErrors = {};
  if (!values.email.trim()) errors.email = "Email is required";
  else if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(values.email)) errors.email = "Enter a valid email";
  if (values.password.length < 8) errors.password = "At least 8 characters";
  else if (!/\d/.test(values.password)) errors.password = "Include a number";
  const age = Number(values.age);
  if (!values.age) errors.age = "Age is required";
  else if (!Number.isInteger(age) || age < 18 || age > 120) errors.age = "Age must be 18-120";
  return errors;
}

export function SignupForm({ onSubmit }: { onSubmit: (values: SignupValues) => Promise<void> }) {
  const [values, setValues] = useState<SignupValues>({ email: "", password: "", age: "" });
  const [touched, setTouched] = useState<Partial<Record<keyof SignupValues, boolean>>>({});
  const [submitting, setSubmitting] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  const errors = validateSignup(values);                    // derived, not stored in state
  const isValid = Object.keys(errors).length === 0;

  // ONE handler for every field: name the inputs after the state keys
  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    setValues((current) => ({ ...current, [name]: value }));
  };

  const handleBlur = (event: React.FocusEvent<HTMLInputElement>) =>
    setTouched((current) => ({ ...current, [event.target.name]: true }));

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setTouched({ email: true, password: true, age: true });
    if (!isValid) return;
    setSubmitting(true);
    setServerError(null);
    try {
      await onSubmit(values);
    } catch (error) {
      setServerError(error instanceof Error ? error.message : "Something went wrong");
    } finally {
      setSubmitting(false);                                  // finally: always re-enable the button
    }
  };

  const fieldError = (field: keyof SignupValues) => (touched[field] ? errors[field] : undefined);

  return (
    <form onSubmit={handleSubmit} noValidate>
      {(["email", "password", "age"] as const).map((field) => (
        <label key={field}>
          {field}
          <input
            name={field}
            type={field === "password" ? "password" : field === "age" ? "number" : "email"}
            value={values[field]}
            onChange={handleChange}
            onBlur={handleBlur}
            aria-invalid={Boolean(fieldError(field))}
            aria-describedby={fieldError(field) ? `${field}-error` : undefined}
          />
          {fieldError(field) && (
            <span id={`${field}-error`} role="alert">
              {fieldError(field)}
            </span>
          )}
        </label>
      ))}
      {serverError && <p role="alert">{serverError}</p>}
      <button type="submit" disabled={submitting || !isValid}>
        {submitting ? "Creating..." : "Create account"}
      </button>
    </form>
  );
}

// ---------------------------------------------------------------- uncontrolled form (refs + FormData)

export function UncontrolledForm({ onSubmit }: { onSubmit: (data: Record<string, string>) => void }) {
  const formRef = useRef<HTMLFormElement>(null);

  return (
    <form
      ref={formRef}
      onSubmit={(event) => {
        event.preventDefault();
        // FormData reads every named input: less state, fewer re-renders
        const data = Object.fromEntries(new FormData(event.currentTarget)) as Record<string, string>;
        onSubmit(data);
        formRef.current?.reset();
      }}
    >
      <input name="title" defaultValue="" />
      <button type="submit">Save</button>
    </form>
  );
}

// ---------------------------------------------------------------- React 19: form actions + useActionState

export interface ActionResult {
  ok: boolean;
  message: string;
}

/** A submit button that knows whether the surrounding <form> is pending. */
function SubmitButton({ label }: { label: string }) {
  const { pending } = useFormStatus();                       // must be INSIDE the <form>
  return (
    <button type="submit" disabled={pending}>
      {pending ? "Saving..." : label}
    </button>
  );
}

export function CommentForm({ save }: { save: (text: string) => Promise<string> }) {
  // useActionState: (previousState, formData) => newState, and React tracks pending for you
  const [state, formAction] = useActionState<ActionResult | null, FormData>(async (_previous, formData) => {
    const text = String(formData.get("text") ?? "").trim();
    if (!text) return { ok: false, message: "Comment cannot be empty" };
    try {
      const saved = await save(text);
      return { ok: true, message: `Saved: ${saved}` };
    } catch (error) {
      return { ok: false, message: error instanceof Error ? error.message : "Failed" };
    }
  }, null);

  return (
    <form action={formAction}>
      <textarea name="text" />
      <SubmitButton label="Comment" />
      {state && <p role={state.ok ? "status" : "alert"}>{state.message}</p>}
    </form>
  );
}

// ---------------------------------------------------------------- React 19: optimistic updates

export interface Message {
  id: string;
  text: string;
  pending?: boolean;
}

export function MessageList({ messages, send }: { messages: Message[]; send: (text: string) => Promise<void> }) {
  const [optimisticMessages, addOptimistic] = useOptimistic<Message[], string>(messages, (current, text) => [
    ...current,
    { id: `temp-${text}`, text, pending: true },             // shown immediately, replaced by the real data
  ]);
  const [, startTransition] = useTransition();

  const handleSend = (text: string) => {
    startTransition(async () => {
      addOptimistic(text);                                   // instant feedback
      await send(text);                                      // if it fails, React rolls the optimistic state back
    });
  };

  return (
    <>
      <ul>
        {optimisticMessages.map((message) => (
          <li key={message.id} style={{ opacity: message.pending ? 0.5 : 1 }}>
            {message.text}
          </li>
        ))}
      </ul>
      <button onClick={() => handleSend("hello")}>Send</button>
    </>
  );
}

// ---------------------------------------------------------------- data fetching states + pagination

export interface Page<T> {
  items: T[];
  nextCursor: string | null;
}

export function usePaginated<T>(loadPage: (cursor: string | null) => Promise<Page<T>>) {
  const [items, setItems] = useState<T[]>([]);
  const [cursor, setCursor] = useState<string | null>(null);
  const [status, setStatus] = useState<"idle" | "loading" | "error" | "done">("idle");

  const loadMore = async () => {
    if (status === "loading" || status === "done") return;
    setStatus("loading");
    try {
      const page = await loadPage(cursor);
      setItems((current) => [...current, ...page.items]);     // append, never replace
      setCursor(page.nextCursor);
      setStatus(page.nextCursor ? "idle" : "done");
    } catch {
      setStatus("error");
    }
  };

  return { items, status, loadMore };
}

/**
 * IN A REAL APP use a data library (TanStack Query, SWR, RSC/Next.js) - say this in the interview.
 * It gives you caching, deduplication, retries, background refetch, stale-while-revalidate and
 * mutation invalidation, which is a lot of code to hand-roll in useEffect.
 */
