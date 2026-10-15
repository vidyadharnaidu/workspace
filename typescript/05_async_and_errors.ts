/**
 * Typed async code and error handling.
 *
 * Run:   node typescript/05_async_and_errors.ts
 *
 * REMEMBER:
 *  * `catch (err)` gives you `unknown` (with useUnknownInCatchVariables, part of strict) - narrow before use.
 *  * Custom errors: extend Error, set `name`, and narrow with instanceof.
 *  * A Result/Either type makes failure part of the SIGNATURE instead of a surprise throw.
 *  * Promise<T> types flow through async/await; `Awaited<T>` unwraps nested promises.
 *  * Type the boundary: parse and validate unknown JSON before trusting it (types are erased at runtime).
 */

function check(label: string, actual: unknown, expected: unknown): void {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
  console.log(`${label.padEnd(34)} ${a}`);
}

const sleep = (ms: number): Promise<void> => new Promise((resolve) => setTimeout(resolve, ms));

// ---------------------------------------------------------------- custom errors

class HttpError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "HttpError";
    this.status = status;
  }
}

class ValidationError extends Error {
  readonly field: string;

  constructor(field: string, message: string) {
    super(message);
    this.name = "ValidationError";
    this.field = field;
  }
}

function describeError(err: unknown): string {
  // `err` is unknown: narrow it, most specific first
  if (err instanceof ValidationError) return `invalid ${err.field}: ${err.message}`;
  if (err instanceof HttpError) return `http ${err.status}: ${err.message}`;
  if (err instanceof Error) return `error: ${err.message}`;
  return `unknown thrown value: ${String(err)}`;
}

// ---------------------------------------------------------------- Result type (errors as values)

type Result<T, E = Error> = { ok: true; value: T } | { ok: false; error: E };

const ok = <T>(value: T): Result<T, never> => ({ ok: true, value });
const fail = <E>(error: E): Result<never, E> => ({ ok: false, error });

function parsePort(raw: string): Result<number, ValidationError> {
  const port = Number(raw);
  if (!Number.isInteger(port) || port < 1 || port > 65535) {
    return fail(new ValidationError("port", `${raw} is not a valid port`));
  }
  return ok(port);
}

/** Wrap a throwing async function so callers must handle both branches. */
async function attempt<T>(fn: () => Promise<T>): Promise<Result<T>> {
  try {
    return ok(await fn());
  } catch (err) {
    return fail(err instanceof Error ? err : new Error(String(err)));
  }
}

// ---------------------------------------------------------------- typed async helpers

interface User {
  id: number;
  name: string;
}

async function fetchUser(id: number): Promise<User> {
  await sleep(1);
  if (id <= 0) throw new HttpError(404, `user ${id} not found`);
  return { id, name: `user${id}` };
}

async function fetchAll<T>(ids: number[], loader: (id: number) => Promise<T>): Promise<T[]> {
  return Promise.all(ids.map(loader));             // parallel, typed as T[]
}

async function withTimeout<T>(promise: Promise<T>, ms: number): Promise<T> {
  let timer: ReturnType<typeof setTimeout> | undefined;
  const timeout = new Promise<never>((_, reject) => {
    timer = setTimeout(() => reject(new Error("timeout")), ms);
  });
  try {
    return await Promise.race([promise, timeout]);
  } finally {
    if (timer) clearTimeout(timer);
  }
}

async function retry<T>(fn: (attempt: number) => Promise<T>, attempts = 3): Promise<T> {
  let lastError: unknown;
  for (let attempt = 1; attempt <= attempts; attempt++) {
    try {
      return await fn(attempt);
    } catch (err) {
      lastError = err;
      await sleep(1);
    }
  }
  throw lastError instanceof Error ? lastError : new Error(String(lastError));
}

// ---------------------------------------------------------------- validating unknown data at the boundary

interface ApiOrder {
  id: number;
  total: number;
  status: "paid" | "pending";
}

function isApiOrder(value: unknown): value is ApiOrder {
  if (typeof value !== "object" || value === null) return false;
  const candidate = value as Record<string, unknown>;
  return (
    typeof candidate["id"] === "number" &&
    typeof candidate["total"] === "number" &&
    (candidate["status"] === "paid" || candidate["status"] === "pending")
  );
}

function parseOrder(json: string): Result<ApiOrder, ValidationError> {
  try {
    const data: unknown = JSON.parse(json);
    return isApiOrder(data) ? ok(data) : fail(new ValidationError("body", "unexpected shape"));
  } catch {
    return fail(new ValidationError("body", "invalid JSON"));
  }
}

async function main(): Promise<void> {
  check("typed await", (await fetchUser(7)).name, "user7");
  check("Promise.all is typed T[]", (await fetchAll([1, 2], fetchUser)).map((u) => u.name), ["user1", "user2"]);

  check("narrow unknown errors",
        [describeError(new ValidationError("email", "missing")), describeError(new HttpError(500, "boom")),
         describeError(new Error("plain")), describeError("just a string")],
        ["invalid email: missing", "http 500: boom", "error: plain", "unknown thrown value: just a string"]);

  const good = parsePort("8080");
  const bad = parsePort("99999");
  check("Result type (ok / fail)",
        [good.ok ? good.value : null, bad.ok ? null : bad.error.field],
        [8080, "port"]);

  const failed = await attempt(() => fetchUser(-1));
  check("attempt wraps throws", failed.ok ? "unexpected" : describeError(failed.error), "http 404: user -1 not found");

  check("withTimeout success", (await withTimeout(fetchUser(3), 100)).id, 3);
  check("withTimeout rejects", await withTimeout(sleep(50), 5).then(() => "no", (e: unknown) => describeError(e)),
        "error: timeout");

  let attemptsUsed = 0;
  const value = await retry(async (attempt) => {
    attemptsUsed = attempt;
    if (attempt < 3) throw new HttpError(503, "unavailable");
    return "recovered";
  });
  check("retry until success", [value, attemptsUsed], ["recovered", 3]);

  check("validate unknown JSON",
        [parseOrder('{"id":1,"total":9.5,"status":"paid"}').ok,
         parseOrder('{"id":"one"}').ok,
         parseOrder("not json").ok],
        [true, false, false]);

  const allSettled = await Promise.allSettled([fetchUser(1), fetchUser(-1)]);
  check("allSettled statuses", allSettled.map((r) => r.status), ["fulfilled", "rejected"]);

  console.log("\nall checks passed");
}

void main();

export {};
