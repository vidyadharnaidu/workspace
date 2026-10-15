/**
 * Async JavaScript: the event loop, promises, async/await, concurrency patterns.
 *
 * Run: node javascript/03_async/async_promises_eventloop.js
 *
 * THE EVENT LOOP ANSWER (memorize):
 *   JS runs on ONE thread with a call stack. Async work is handed to the runtime (timers, I/O).
 *   When the stack is empty the loop drains:
 *     1. ALL MICROTASKS  (promise callbacks, queueMicrotask, await continuations)
 *     2. then ONE MACROTASK (setTimeout/setInterval, I/O callbacks)  -> microtasks again ...
 *   So: sync code -> promises -> setTimeout. Node also has process.nextTick (before other microtasks).
 *
 * REMEMBER:
 *  * await in a for-loop = SEQUENTIAL. Promise.all = PARALLEL.
 *  * Promise.all rejects on the first failure; allSettled always resolves; race settles first; any = first success.
 *  * Always catch: an unhandled rejection crashes modern Node.
 *  * A promise executor runs immediately; `.then` callbacks are always async.
 */

function check(label, actual, expected) {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
  console.log(`${label.padEnd(36)} ${a}`);
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/** Callback style -> promise (what util.promisify does). */
function promisify(fn) {
  return (...args) =>
    new Promise((resolve, reject) =>
      fn(...args, (err, value) => (err ? reject(err) : resolve(value)))
    );
}

function readUserCallback(id, callback) {
  setTimeout(() => (id > 0 ? callback(null, { id, name: `user${id}` }) : callback(new Error("bad id"))), 5);
}

/** Retry with exponential backoff - the most-asked async utility. */
async function retry(fn, { attempts = 3, baseDelayMs = 1, sleepFn = sleep } = {}) {
  let lastError;
  for (let attempt = 1; attempt <= attempts; attempt++) {
    try {
      return await fn(attempt);
    } catch (err) {
      lastError = err;
      if (attempt < attempts) await sleepFn(baseDelayMs * 2 ** (attempt - 1));
    }
  }
  throw lastError;
}

/** Run tasks with a concurrency limit (a promise pool). */
async function mapWithConcurrency(items, limit, worker) {
  const results = new Array(items.length);
  let index = 0;
  let inFlight = 0;
  let peak = 0;

  async function runNext() {
    if (index >= items.length) return;
    const current = index++;
    inFlight++;
    peak = Math.max(peak, inFlight);
    results[current] = await worker(items[current], current);
    inFlight--;
    await runNext();
  }

  await Promise.all(Array.from({ length: Math.min(limit, items.length) }, runNext));
  return { results, peak };
}

/** Timeout wrapper using Promise.race. */
function withTimeout(promise, ms, message = "timeout") {
  let timer;
  const timeout = new Promise((_, reject) => {
    timer = setTimeout(() => reject(new Error(message)), ms);
  });
  return Promise.race([promise, timeout]).finally(() => clearTimeout(timer));
}

/** Async generator: paginate lazily. */
async function* paginate(pages) {
  for (const page of pages) {
    await sleep(1);
    yield page;
  }
}

async function main() {
  // Q1. Event loop ordering
  const order = [];
  order.push("1 sync start");
  setTimeout(() => order.push("5 setTimeout (macrotask)"), 0);
  Promise.resolve().then(() => order.push("3 promise (microtask)"));
  queueMicrotask(() => order.push("4 queueMicrotask"));
  order.push("2 sync end");
  await sleep(10);
  check("event loop order", order, [
    "1 sync start",
    "2 sync end",
    "3 promise (microtask)",
    "4 queueMicrotask",
    "5 setTimeout (macrotask)",
  ]);

  // Q2. Sequential vs parallel
  const delays = [20, 20, 20];
  const startSeq = Date.now();
  const sequential = [];
  for (const ms of delays) {
    await sleep(ms);
    sequential.push(ms);
  }
  const seqTime = Date.now() - startSeq;

  const startPar = Date.now();
  const parallel = await Promise.all(delays.map(async (ms) => { await sleep(ms); return ms; }));
  const parTime = Date.now() - startPar;
  check("sequential vs parallel results", [sequential, parallel], [[20, 20, 20], [20, 20, 20]]);
  check("parallel is faster", parTime < seqTime, true);
  console.log(`${"".padEnd(36)} sequential ${seqTime}ms vs parallel ${parTime}ms`);

  // Q3. Promise combinators
  const ok = (v, ms = 5) => sleep(ms).then(() => v);
  const fail = (msg, ms = 5) => sleep(ms).then(() => Promise.reject(new Error(msg)));

  check("Promise.all", await Promise.all([ok(1), ok(2)]), [1, 2]);
  check("Promise.all rejects fast", await Promise.all([ok(1), fail("boom")]).catch((e) => e.message), "boom");
  check("Promise.allSettled", (await Promise.allSettled([ok(1), fail("boom")])).map((r) => r.status),
        ["fulfilled", "rejected"]);
  check("Promise.race (first settled)", await Promise.race([ok("fast", 1), ok("slow", 50)]), "fast");
  check("Promise.any (first success)", await Promise.any([fail("bad", 1), ok("good", 5)]), "good");

  // Q4. Callback -> promise
  const readUser = promisify(readUserCallback);
  check("promisify success", await readUser(7), { id: 7, name: "user7" });
  check("promisify failure", await readUser(-1).catch((e) => e.message), "bad id");

  // Q5. try/catch with async/await
  const result = await (async () => {
    try {
      await fail("db down", 1);
      return "unreachable";
    } catch (err) {
      return `caught: ${err.message}`;
    } finally {
      order.push("finally runs");
    }
  })();
  check("async try/catch/finally", [result, order.at(-1)], ["caught: db down", "finally runs"]);

  // Q6. Retry with backoff
  let attemptsUsed = 0;
  const flaky = async (attempt) => {
    attemptsUsed = attempt;
    if (attempt < 3) throw new Error("temporary");
    return "ok";
  };
  check("retry with backoff", [await retry(flaky), attemptsUsed], ["ok", 3]);

  // Q7. Concurrency limit
  const { results, peak } = await mapWithConcurrency([1, 2, 3, 4, 5, 6, 7, 8], 3, async (n) => {
    await sleep(5);
    return n * 2;
  });
  check("mapWithConcurrency results", results, [2, 4, 6, 8, 10, 12, 14, 16]);
  check("concurrency never exceeded 3", peak <= 3, true);

  // Q8. Timeout with Promise.race
  check("withTimeout wins", await withTimeout(ok("value", 1), 50), "value");
  check("withTimeout times out", await withTimeout(sleep(50), 5).catch((e) => e.message), "timeout");

  // Q9. for await ... of an async generator
  const pages = [];
  for await (const page of paginate(["p1", "p2", "p3"])) pages.push(page);
  check("async iteration", pages, ["p1", "p2", "p3"]);

  // Q10. Common trap: forEach does NOT await
  const collected = [];
  [1, 2, 3].forEach(async (n) => {
    await sleep(1);
    collected.push(n);
  });
  check("forEach does not wait", collected, []);
  await sleep(10);
  check("... it finished later", collected.length, 3);

  console.log("\nall checks passed");
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
