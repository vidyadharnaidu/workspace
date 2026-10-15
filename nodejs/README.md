# Node.js Backend Interview Questions

Runnable backend examples using **only the Node standard library** — no npm install, no framework. Every file checks its own answers and fails loudly if a result is wrong.

```bash
node nodejs/run_all.mjs                     # run everything, PASS/FAIL per file
node nodejs/04_http_api/http_server.mjs     # run one topic
node nodejs/run_all.mjs streams             # filter by path
```

Requires Node 18+ (Node 24 used here).

## Files

| Folder | File | What's inside |
|---|---|---|
| `01_core/` | `event_loop.mjs` | The phases explained and **measured**: sync → nextTick → microtasks → timers/check, the ESM-vs-CJS `nextTick` nuance, why `setImmediate` wins inside I/O, 60 ms of CPU work delaying a timer, event-loop-lag measurement, parallel I/O |
| `02_async/` | `async_patterns.mjs` | `promisify`, sequential vs parallel timing, a **concurrency-limited map**, retry with backoff + jitter, timeouts and `AbortController` cancellation, EventEmitter (including the `error`-event trap), async generators, error translation with `cause`, `map(load)` argument trap |
| `03_streams/` | `streams.mjs` | Readable/Writable/Transform, `pipeline` error propagation, **backpressure observed**, line-splitting across chunk boundaries (NDJSON), 8 MB streamed with flat memory, gzip round trip |
| `04_http_api/` | `http_server.mjs` | A REST API on `node:http`: router with params, body size limits, validation, 201/204/404/405/409/413/429, one error handler that never leaks internals, request ids, rate limiting, **graceful shutdown** — then 12 self-tests over a real socket |
| `05_security/` | `auth_and_crypto.mjs` | scrypt password hashing with per-user salts, `timingSafeEqual`, **a JWT implemented and attacked** (tampering, wrong secret, `alg:none`, expiry), revocable sessions, AES-256-GCM, webhook signatures with replay protection, escaping and parameterized queries |
| `06_data/` | `data_patterns.mjs` | Connection pool with `withConnection`, transaction commit/rollback, **N+1 vs batched (4 queries → 2)**, a DataLoader, cache with TTL + stampede protection, idempotency keys, cursor pagination, circuit breaker, transactional outbox |
| `07_performance/` | `workers_and_scaling.mjs` | The same CPU job on the main thread (blocks everything) vs in a `worker_thread` (loop keeps ticking), a worker pool across cores, `SharedArrayBuffer` + `Atomics`, memory usage, bounded vs unbounded caches, cluster sketch |
| `08_interview_qa/` | `nodejs_qa.md` | ~60 Q&A: runtime, event loop, async, streams, HTTP/API design, security, databases, queues, observability, deployment, "what does this print?", code-review red flags |
| | `testing_guide.md` | `node:test`, the testing pyramid, HTTP handler tests on an ephemeral port, test doubles, `MockAgent`, mock timers, testcontainers |

## Answers worth memorizing

| Question | One-line answer |
|---|---|
| Is Node single-threaded? | Your JS is; libuv uses a thread pool for fs/dns/zlib/crypto, and you can add `worker_threads`. |
| Event loop order? | sync → `nextTick` → promises → timers → check(`setImmediate`) → close. |
| `setTimeout(0)` vs `setImmediate`? | Undefined at the top level; inside an I/O callback `setImmediate` always wins. |
| What blocks the loop? | Sync I/O, big JSON, crypto, huge loops — everything else stalls with it. |
| CPU-bound work? | `worker_threads` or a job queue; use `cluster`/replicas for cores. |
| What is backpressure? | `write()` returns false → wait for `'drain'`; `pipeline()` does it for you. |
| `pipe` vs `pipeline`? | `pipeline` forwards errors and destroys streams; `pipe` leaks. |
| How do you store passwords? | scrypt/bcrypt/argon2 with a per-user salt, compared in constant time. |
| JWT vs sessions? | Stateless but unrevokable vs revokable server state; short expiry + refresh is the usual compromise. |
| Fixing N+1? | JOIN, `IN` query, or DataLoader batching. |
| Safe retries? | Idempotency keys + exponential backoff with jitter + a circuit breaker. |
| Graceful shutdown? | Stop accepting → drain in-flight → close dependencies → force-close after a timeout. |

## Practice routine

1. Write a concurrency-limited `mapWithConcurrency` from memory, then compare with `02_async/`.
2. Explain what happens between `server.listen()` and a JSON response — name every layer.
3. Take `04_http_api/http_server.mjs` and add `PUT /users/:id` with optimistic concurrency (an `If-Match` version).
4. Describe how you'd find and fix a memory leak in production, then check yourself against `08_interview_qa/nodejs_qa.md`.

Related folders: `javascript/` (language), `typescript/` (types), `system-design/` (architecture), `db/` (SQL), `react/`, `angular/`, `python/`, `java/` (Java).
