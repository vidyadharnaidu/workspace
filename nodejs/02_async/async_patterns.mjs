/**
 * Async patterns for backend code: callbacks -> promises, concurrency limits, retries,
 * timeouts/cancellation, EventEmitter, async iterators, error handling.
 *
 * Run: node nodejs/02_async/async_patterns.mjs
 *
 * REMEMBER:
 *  * Node's callback convention is ERROR-FIRST: fn(err, value). util.promisify converts it.
 *  * `await` in a loop = sequential. Promise.all = parallel but unbounded - use a concurrency limit.
 *  * Every external call needs a TIMEOUT. AbortSignal is the standard way to cancel.
 *  * Retry only IDEMPOTENT operations, with exponential backoff + jitter.
 *  * An unhandled promise rejection terminates the process by default: always attach a handler.
 *  * EventEmitter is synchronous; an 'error' event with no listener THROWS.
 */
import { EventEmitter, once } from 'node:events';
import { setTimeout as sleep } from 'node:timers/promises';
import { promisify } from 'node:util';

function check(label, actual, expected) {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
  console.log(`${label.padEnd(40)} ${a}`);
}

// ---------------------------------------------------------------- 1. callback -> promise

function readRecordCallback(id, callback) {
  setTimeout(() => (id > 0 ? callback(null, { id, name: `record-${id}` }) : callback(new Error('bad id'))), 5);
}

const readRecord = promisify(readRecordCallback); // util.promisify understands the error-first convention

// ---------------------------------------------------------------- 2. sequential vs parallel vs limited

async function sequential(ids, load) {
  const results = [];
  for (const id of ids) results.push(await load(id)); // one at a time: slow but gentle on the dependency
  return results;
}

async function parallel(ids, load) {
  // TRAP: ids.map(load) passes (value, index, array), so `load` receives extra arguments -
  // the same bug as [1,2,3].map(parseInt). Always wrap it: map((id) => load(id)).
  return Promise.all(ids.map((id) => load(id))); // all at once: fast, but can hammer a database
}

/** The interview favourite: run N at a time, keep input order, and report the real peak concurrency. */
async function mapWithConcurrency(items, limit, worker) {
  const results = new Array(items.length);
  let nextIndex = 0;
  let active = 0;
  let peak = 0;

  async function runWorker() {
    while (nextIndex < items.length) {
      const index = nextIndex++;
      active++;
      peak = Math.max(peak, active);
      try {
        results[index] = await worker(items[index], index);
      } finally {
        active--;
      }
    }
  }

  await Promise.all(Array.from({ length: Math.min(limit, items.length) }, runWorker));
  return { results, peak };
}

// ---------------------------------------------------------------- 3. retry with exponential backoff + jitter

async function retry(fn, { attempts = 3, baseDelayMs = 5, maxDelayMs = 100, signal } = {}) {
  let lastError;
  for (let attempt = 1; attempt <= attempts; attempt++) {
    try {
      return await fn(attempt);
    } catch (error) {
      lastError = error;
      if (attempt === attempts || signal?.aborted) break;
      const backoff = Math.min(maxDelayMs, baseDelayMs * 2 ** (attempt - 1));
      const jitter = Math.random() * backoff; // full jitter: avoids synchronized retry storms
      await sleep(backoff / 2 + jitter / 2);
    }
  }
  throw lastError;
}

// ---------------------------------------------------------------- 4. timeouts and cancellation

async function withTimeout(promise, ms, message = 'timeout') {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(new Error(message)), ms);
  try {
    return await Promise.race([
      promise,
      new Promise((_, reject) => controller.signal.addEventListener('abort', () => reject(controller.signal.reason))),
    ]);
  } finally {
    clearTimeout(timer); // ALWAYS clear the timer, or the process stays alive
  }
}

/** Real code: pass the signal down so the work itself stops, not just the waiting. */
async function cancellableWork(signal) {
  for (let step = 0; step < 100; step++) {
    signal.throwIfAborted(); // cooperative cancellation
    await sleep(2);
  }
  return 'finished';
}

// ---------------------------------------------------------------- 5. EventEmitter

class JobQueue extends EventEmitter {
  #jobs = [];

  add(job) {
    this.#jobs.push(job);
    this.emit('added', job); // listeners run SYNCHRONOUSLY, in registration order
    return this;
  }

  async run() {
    const done = [];
    for (const job of this.#jobs) {
      try {
        done.push(await job());
        this.emit('progress', done.length, this.#jobs.length);
      } catch (error) {
        this.emit('error', error); // an 'error' event with NO listener throws and crashes the process
      }
    }
    this.emit('done', done);
    return done;
  }
}

// ---------------------------------------------------------------- 6. async iterators / generators

async function* paginate(pageSize, total) {
  for (let offset = 0; offset < total; offset += pageSize) {
    await sleep(1); // pretend this is a database call
    yield Array.from({ length: Math.min(pageSize, total - offset) }, (_, i) => offset + i);
  }
}

// ---------------------------------------------------------------- 7. error handling patterns

class AppError extends Error {
  constructor(message, { status = 500, code = 'INTERNAL', cause } = {}) {
    super(message, { cause }); // `cause` keeps the original error (Node 16.9+)
    this.name = 'AppError';
    this.status = status;
    this.code = code;
  }
}

async function loadUserOrFail(id) {
  try {
    return await readRecord(id);
  } catch (error) {
    // Translate low-level errors into domain errors; never leak driver details to the client
    throw new AppError('Could not load user', { status: 404, code: 'USER_NOT_FOUND', cause: error });
  }
}

check('promisify', await readRecord(7), { id: 7, name: 'record-7' });
check('promisify rejects', await readRecord(-1).catch((error) => error.message), 'bad id');

const ids = [1, 2, 3, 4, 5, 6, 7, 8];
const startSeq = Date.now();
const sequentialResults = await sequential(ids.slice(0, 4), readRecord);
const seqMs = Date.now() - startSeq;

const startPar = Date.now();
const parallelResults = await parallel(ids.slice(0, 4), readRecord);
const parMs = Date.now() - startPar;

check('sequential results', sequentialResults.map((r) => r.id), [1, 2, 3, 4]);
check('parallel results (same order)', parallelResults.map((r) => r.id), [1, 2, 3, 4]);
check('parallel is faster', parMs < seqMs, true);
console.log(`${'sequential vs parallel'.padEnd(40)} ${seqMs}ms vs ${parMs}ms`);

const { results, peak } = await mapWithConcurrency(ids, 3, async (id) => {
  await sleep(5);
  return id * 10;
});
check('mapWithConcurrency keeps order', results, [10, 20, 30, 40, 50, 60, 70, 80]);
check('concurrency limit respected', peak <= 3, true);

let attemptsUsed = 0;
const recovered = await retry(async (attempt) => {
  attemptsUsed = attempt;
  if (attempt < 3) throw new Error('flaky dependency');
  return 'ok';
});
check('retry with backoff', [recovered, attemptsUsed], ['ok', 3]);
check('retry gives up', await retry(async () => { throw new Error('always down'); }, { attempts: 2 })
  .catch((error) => error.message), 'always down');

check('withTimeout resolves', await withTimeout(sleep(5).then(() => 'value'), 200), 'value');
check('withTimeout rejects', await withTimeout(sleep(500), 10).catch((error) => error.message), 'timeout');

const controller = new AbortController();
setTimeout(() => controller.abort(new Error('client disconnected')), 10);
check('cooperative cancellation', await cancellableWork(controller.signal).catch((error) => error.message),
      'client disconnected');

// AbortSignal.timeout(ms) is the built-in shorthand used with fetch: fetch(url, { signal: AbortSignal.timeout(2000) })
check('AbortSignal.timeout exists', typeof AbortSignal.timeout, 'function');

const queue = new JobQueue();
const events = [];
queue.on('added', () => events.push('added'));
queue.on('progress', (done, total) => events.push(`${done}/${total}`));
queue.on('error', (error) => events.push(`error:${error.message}`));
queue.add(async () => 'a').add(async () => { throw new Error('boom'); }).add(async () => 'c');
const jobResults = await queue.run();
check('EventEmitter flow', events, ['added', 'added', 'added', '1/3', 'error:boom', '2/3']);
check('jobs that succeeded', jobResults, ['a', 'c']);

const emitter = new EventEmitter();
setTimeout(() => emitter.emit('ready', 42), 5);
check('events.once() as a promise', await once(emitter, 'ready'), [42]); // await an event

const pages = [];
for await (const page of paginate(3, 7)) pages.push(page); // streams data without loading it all
check('async generator pagination', pages, [[0, 1, 2], [3, 4, 5], [6]]);

const appError = await loadUserOrFail(-1).catch((error) => error);
check('domain error wraps the cause', [appError.status, appError.code, appError.cause.message],
      [404, 'USER_NOT_FOUND', 'bad id']);

// Safety nets (put these in your entry point, log and exit - do NOT keep running in an unknown state)
process.on('unhandledRejection', (reason) => console.error('unhandledRejection', reason));
process.on('uncaughtException', (error) => console.error('uncaughtException', error));
check('process safety nets registered', process.listenerCount('unhandledRejection') > 0, true);

console.log('\nall checks passed');
