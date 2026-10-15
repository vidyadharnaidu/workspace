/**
 * The Node.js event loop - the #1 backend interview topic.
 *
 * Run: node nodejs/01_core/event_loop.mjs
 *
 * THE ANSWER TO MEMORIZE:
 *   Node runs your JavaScript on ONE thread. Anything slow (disk, network, DNS, crypto) is handed to
 *   libuv, which uses the OS or a small THREAD POOL (default 4, UV_THREADPOOL_SIZE) and queues the
 *   callback for later. The event loop drains phases in this order, forever:
 *
 *     timers      -> setTimeout / setInterval callbacks whose time has come
 *     pending     -> some system callbacks (e.g. TCP errors)
 *     poll        -> wait for I/O; run I/O callbacks (this is where the loop spends its time)
 *     check       -> setImmediate callbacks
 *     close       -> 'close' events (socket.on('close'))
 *
 *   BETWEEN EVERY CALLBACK the microtask queues are drained completely:
 *     1. process.nextTick queue   (Node-specific, highest priority)
 *     2. promise microtasks       (.then / await continuations / queueMicrotask)
 *
 * CONSEQUENCES:
 *   * A CPU-heavy function BLOCKS everything: no timers, no I/O, no other requests.
 *   * process.nextTick can starve the loop if it schedules itself recursively.
 *   * setTimeout(fn, 0) vs setImmediate(fn): from the MAIN module the order is NOT guaranteed
 *     (it depends on how long startup took); inside an I/O callback setImmediate always wins.
 *   * ESM NUANCE (measured below): the top level of an ES module already runs inside a promise job,
 *     so promise callbacks scheduled there run BEFORE process.nextTick. In CommonJS - and inside any
 *     normal callback - nextTick comes first. Know this if an interviewer shows you a file and asks
 *     for the output: the answer depends on .mjs vs .cjs.
 */

function check(label, actual, expected) {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
  console.log(`${label.padEnd(42)} ${a}`);
}

import { readFile } from 'node:fs/promises';
import { open } from 'node:fs';
import { performance } from 'node:perf_hooks';

// ---------------------------------------------------------------- 1. ordering

function orderingDemo() {
  // Scheduled from INSIDE a phase callback = the normal case in a server (an I/O or timer handler).
  return new Promise((resolve) => {
    setImmediate(() => {
      const order = [];
      order.push('1 sync');
      setTimeout(() => order.push('timer'), 0);
      setImmediate(() => order.push('immediate'));
      Promise.resolve().then(() => order.push('4 promise'));
      queueMicrotask(() => order.push('5 queueMicrotask'));
      process.nextTick(() => order.push('3 nextTick'));
      order.push('2 sync end');
      setTimeout(() => resolve(order), 20);
    });
  });
}

// ---------------------------------------------------------------- 2. nextTick vs promises depends on the context

async function esmTopLevelOrder() {
  const order = [];
  Promise.resolve().then(() => order.push('promise'));
  process.nextTick(() => order.push('nextTick'));
  await new Promise((resolve) => setImmediate(resolve));
  return order;
}

// ---------------------------------------------------------------- 3. inside an I/O callback, setImmediate always wins

function insideIoCallback() {
  return new Promise((resolve) => {
    const order = [];
    // Once we are inside the POLL phase, `check` (setImmediate) comes before the next `timers` phase.
    open(new URL(import.meta.url), 'r', () => {
      setTimeout(() => {
        order.push('timeout');
        if (order.length === 2) resolve(order);
      }, 0);
      setImmediate(() => {
        order.push('immediate');
        if (order.length === 2) resolve(order);
      });
    });
  });
}

// ---------------------------------------------------------------- 4. blocking the loop

function blockingDemo() {
  const start = performance.now();
  let timerLag = 0;

  return new Promise((resolve) => {
    setTimeout(() => {
      timerLag = performance.now() - start; // how late did this 0ms timer fire?
      resolve(timerLag);
    }, 0);

    // synchronous CPU work: nothing else can run, not even the timer above
    const busyUntil = performance.now() + 60;
    while (performance.now() < busyUntil) {
      /* blocking on purpose */
    }
  });
}

// ---------------------------------------------------------------- 5. measuring event loop lag (health metric)

function measureLag(samples = 5, intervalMs = 10) {
  return new Promise((resolve) => {
    const lags = [];
    let scheduled = performance.now();

    const tick = () => {
      const now = performance.now();
      lags.push(now - scheduled - intervalMs); // how much later than requested did we run?
      if (lags.length === samples) {
        resolve(Math.max(...lags));
        return;
      }
      scheduled = now;
      setTimeout(tick, intervalMs);
    };
    setTimeout(tick, intervalMs);
  });
}

// ---------------------------------------------------------------- 6. async work does NOT block

async function concurrentIoDemo() {
  const start = performance.now();
  // Three file reads in parallel: libuv does them off-thread, so the total is ~the slowest, not the sum.
  await Promise.all([
    readFile(new URL(import.meta.url)),
    readFile(new URL(import.meta.url)),
    readFile(new URL(import.meta.url)),
  ]);
  return performance.now() - start;
}

const order = await orderingDemo();
// Deterministic part: all synchronous code, then nextTick, then promise microtasks, then macrotasks.
check('sync -> nextTick -> microtasks', order.slice(0, 5), [
  '1 sync',
  '2 sync end',
  '3 nextTick',
  '4 promise',
  '5 queueMicrotask',
]);
console.log(`${'timer vs immediate (NOT guaranteed here)'.padEnd(42)} ${JSON.stringify(order.slice(5))}`);

// The ESM nuance: at the top level of an .mjs file, promises win over nextTick.
const esmOrder = await esmTopLevelOrder();
check('ESM top level: promise before nextTick', esmOrder, ['promise', 'nextTick']);
console.log(`${'   (in CommonJS the order is reversed)'.padEnd(42)} ["nextTick","promise"]`);
check('inside I/O: immediate before timeout', await insideIoCallback(), ['immediate', 'timeout']);

const lag = await blockingDemo();
console.log(`${'60ms of CPU work delayed a 0ms timer by'.padEnd(42)} ${lag.toFixed(0)}ms`);
check('blocking work delays timers', lag > 50, true);

const idleLag = await measureLag();
console.log(`${'event loop lag while idle (max)'.padEnd(42)} ${idleLag.toFixed(1)}ms`);
check('idle lag is small', idleLag < 50, true);

const parallelMs = await concurrentIoDemo();
console.log(`${'3 parallel file reads took'.padEnd(42)} ${parallelMs.toFixed(1)}ms`);
check('parallel I/O is fast', parallelMs < 200, true);

console.log('\nall checks passed');

/**
 * FOLLOW-UP ANSWERS
 *  "How do you handle CPU-heavy work?"  -> worker_threads (see 07_performance/worker_threads.mjs),
 *                                          a job queue, a native addon, or another service.
 *  "How do you scale across cores?"     -> cluster / PM2 / container replicas; the loop is single-threaded.
 *  "How do you detect a blocked loop?"  -> event loop lag metric (above), /health endpoint, APM alerts.
 *  "What is the thread pool for?"       -> fs, dns.lookup, zlib, crypto (pbkdf2/scrypt) - NOT network I/O,
 *                                          which uses the OS event notification (epoll/kqueue/IOCP).
 */
