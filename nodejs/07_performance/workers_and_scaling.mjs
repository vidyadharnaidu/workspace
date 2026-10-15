/**
 * CPU-bound work, worker_threads, clustering and memory - how a single-threaded runtime scales.
 *
 * Run: node nodejs/07_performance/workers_and_scaling.mjs
 *
 * THE DECISION TABLE:
 *   I/O-bound (most APIs)      -> plain async code; one process handles thousands of connections
 *   CPU-bound (hashing, image, -> worker_threads (same process, shared memory possible) or a job queue
 *              parsing, ML)       run by separate worker services
 *   Scale across CPU cores     -> cluster / PM2 / multiple containers behind a load balancer
 *   Isolation / other language -> child_process (spawn/exec) or a separate service
 *
 * worker_threads vs cluster vs child_process:
 *   worker_threads - threads in ONE process; cheap to start; can share memory (SharedArrayBuffer);
 *                    each has its own event loop and V8 isolate. Best for CPU work inside a request.
 *   cluster        - forks N copies of the PROCESS, all sharing one listening port (the OS balances).
 *                    Best for using every core in a web server. No shared memory.
 *   child_process  - run any program; communicate over stdio/IPC. Best for isolation or other runtimes.
 */
import { Worker, isMainThread, parentPort, workerData } from 'node:worker_threads';
import { availableParallelism } from 'node:os';
import { performance } from 'node:perf_hooks';
import { createHash } from 'node:crypto';

// ---------------------------------------------------------------- the CPU-heavy function

/** Deliberately slow: this is the kind of work that must NOT run on the main thread. */
function hashRounds(input, rounds) {
  let value = input;
  for (let i = 0; i < rounds; i++) value = createHash('sha256').update(value).digest('hex');
  return value;
}

// ---------------------------------------------------------------- worker entry point
// The same file runs as the worker: `new Worker(new URL(import.meta.url))`.

if (!isMainThread) {
  const { input, rounds } = workerData;
  parentPort.postMessage(hashRounds(input, rounds)); // structured clone: objects are COPIED, not shared
} else {
  const check = (label, actual, expected) => {
    const a = JSON.stringify(actual);
    const e = JSON.stringify(expected);
    if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
    console.log(`${label.padEnd(44)} ${a}`);
  };

  const runInWorker = (input, rounds) =>
    new Promise((resolve, reject) => {
      const worker = new Worker(new URL(import.meta.url), { workerData: { input, rounds } });
      worker.once('message', resolve);
      worker.once('error', reject);
      worker.once('exit', (code) => code !== 0 && reject(new Error(`worker exited with ${code}`)));
    });

  const ROUNDS = 150_000;

  // ---------------------------------------------------------------- 1. blocking the loop

  let ticksWhileBlocking = 0;
  const blockingTimer = setInterval(() => ticksWhileBlocking++, 5);
  const blockingStart = performance.now();
  hashRounds('main-thread', ROUNDS); // blocks EVERYTHING
  const blockingMs = performance.now() - blockingStart;
  clearInterval(blockingTimer);

  console.log(`${'CPU work on the main thread'.padEnd(44)} ${blockingMs.toFixed(0)}ms, timer ticked ${ticksWhileBlocking}x`);
  check('main thread was blocked (few/no ticks)', ticksWhileBlocking <= 2, true);

  // ---------------------------------------------------------------- 2. the same work in a worker

  let ticksWhileWorking = 0;
  const workerTimer = setInterval(() => ticksWhileWorking++, 5);
  const workerStart = performance.now();
  const workerResult = await runInWorker('main-thread', ROUNDS);
  const workerMs = performance.now() - workerStart;
  clearInterval(workerTimer);

  console.log(`${'the same work in a worker_thread'.padEnd(44)} ${workerMs.toFixed(0)}ms, timer ticked ${ticksWhileWorking}x`);
  check('worker produced the same hash', workerResult, hashRounds('main-thread', ROUNDS));
  check('event loop kept running during the worker', ticksWhileWorking > 3, true);

  // ---------------------------------------------------------------- 3. a pool of workers uses every core

  const cores = availableParallelism();
  const poolStart = performance.now();
  const results = await Promise.all([1, 2, 3, 4].map((n) => runInWorker(`job-${n}`, ROUNDS)));
  const poolMs = performance.now() - poolStart;

  console.log(`${`4 CPU jobs across ${cores} cores`.padEnd(44)} ${poolMs.toFixed(0)}ms (serial would be ~${(blockingMs * 4).toFixed(0)}ms)`);
  check('all jobs completed', results.length, 4);
  check('parallel beat serial', poolMs < blockingMs * 4, true);

  // ---------------------------------------------------------------- 4. shared memory between threads

  // SharedArrayBuffer is the ONLY way threads share memory; everything else is copied.
  const shared = new SharedArrayBuffer(8);
  const counter = new Int32Array(shared);
  Atomics.add(counter, 0, 5); // Atomics = the lock-free primitives that make sharing safe
  Atomics.add(counter, 0, 3);
  check('SharedArrayBuffer + Atomics', Atomics.load(counter, 0), 8);

  // ---------------------------------------------------------------- 5. memory

  const memory = process.memoryUsage();
  console.log(
    `${'memory (rss / heapUsed / external)'.padEnd(44)} ` +
      `${(memory.rss / 1e6).toFixed(0)}MB / ${(memory.heapUsed / 1e6).toFixed(0)}MB / ${(memory.external / 1e6).toFixed(0)}MB`,
  );
  check('memoryUsage has the expected fields', Object.keys(memory).sort().slice(0, 3), ['arrayBuffers', 'external', 'heapTotal']);

  // A classic leak: an unbounded module-level cache. Bounded caches (LRU + TTL) or WeakMap avoid it.
  const leakyCache = new Map();
  for (let i = 0; i < 1000; i++) leakyCache.set(`key-${i}`, { payload: 'x'.repeat(100) });
  const boundedCache = new Map();
  for (let i = 0; i < 1000; i++) {
    boundedCache.set(`key-${i}`, { payload: 'x'.repeat(100) });
    if (boundedCache.size > 100) boundedCache.delete(boundedCache.keys().next().value); // evict oldest
  }
  check('unbounded cache grows forever', leakyCache.size, 1000);
  check('bounded cache stays small', boundedCache.size, 100);

  console.log('\nall checks passed');
}

/**
 * CLUSTER (sketch - not run here because it forks the process):
 *
 *   import cluster from 'node:cluster';
 *   import { availableParallelism } from 'node:os';
 *
 *   if (cluster.isPrimary) {
 *     for (let i = 0; i < availableParallelism(); i++) cluster.fork();
 *     cluster.on('exit', (worker) => cluster.fork());     // restart crashed workers
 *   } else {
 *     startServer();                                      // every worker listens on the SAME port
 *   }
 *
 * In containers the usual answer is "one process per container, scale replicas" - the orchestrator
 * does what cluster does, with better isolation and rolling deploys.
 *
 * PERFORMANCE CHECKLIST
 *  * Measure first: clinic.js, 0x, --cpu-prof, --heap-prof, event loop lag.
 *  * Never block the loop: no sync fs/crypto/JSON.parse of huge payloads on the request path.
 *  * Stream large payloads; set body limits; compress responses (gzip/brotli at the proxy).
 *  * Cache (in-memory LRU, then Redis); add ETags/Cache-Control for HTTP caching.
 *  * Pool connections; batch queries; index your database (that is usually the real bottleneck).
 *  * Keep-alive agents for outgoing HTTP; timeouts and circuit breakers on every dependency.
 *  * Use a queue for slow work (email, PDFs, video) and return 202 Accepted.
 */
