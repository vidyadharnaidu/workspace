# Node.js Backend Interview Q&A

## Runtime and the event loop

| Question | Answer |
|---|---|
| What is Node.js? | A JavaScript runtime built on V8 with an event-driven, non-blocking I/O model (libuv). One thread runs your code; I/O happens elsewhere and callbacks are queued. |
| Is Node single-threaded? | Your JavaScript is. libuv has a thread pool (default 4) for fs, DNS, zlib and some crypto, and network I/O uses the OS event notification. You can add real threads with `worker_threads`. |
| Event loop phases? | timers → pending callbacks → poll → check (`setImmediate`) → close. Between every callback, `process.nextTick` then promise microtasks are drained fully. |
| `setTimeout(fn, 0)` vs `setImmediate`? | From the main module the order isn't guaranteed; inside an I/O callback `setImmediate` always runs first (check comes right after poll). |
| `process.nextTick` vs `Promise.then`? | `nextTick` has higher priority. Nuance: at the top level of an **ESM** file promises win, because module evaluation already runs in a promise job. |
| What blocks the event loop? | Any long synchronous work: big `JSON.parse`, sync fs, regex backtracking, crypto, loops over huge arrays. Symptom: latency spikes across *all* requests. |
| How do you detect a blocked loop? | Event loop lag (measure a timer's delay), `perf_hooks` monitoring, APM. Alert when p99 lag > ~100 ms. |
| CPU-heavy work — what do you do? | Move it off the request path: `worker_threads`, a job queue with worker services, a native module, or a different service. |
| How do you use all CPU cores? | `cluster` (or PM2) forks one process per core sharing a port; in containers, run more replicas. |
| CommonJS vs ESM? | `require` is synchronous, cached, dynamic; `import` is static, asynchronous, tree-shakeable, and the standard. ESM has no `__dirname` (use `import.meta.url`) and no `require` (use `createRequire`). |
| Is `require` cached? | Yes — a module is evaluated once per resolved path, which is why module-level state acts like a singleton. |
| `process.env`, arguments, exit codes? | Config from env (12-factor), `process.argv` for CLI args, `process.exit(code)` — 0 success, non-zero failure. Handle `SIGTERM` for graceful shutdown. |

## Async patterns

| Question | Answer |
|---|---|
| Callback hell → ? | Promises → `async/await`. Node's convention is error-first callbacks; `util.promisify` converts them. |
| Sequential vs parallel? | `for … await` is sequential; `Promise.all` is parallel. Unbounded parallelism can overwhelm a database, so cap it with a concurrency limit. |
| `Promise.all` vs `allSettled` vs `race` vs `any`? | Fail fast / all outcomes / first settled / first success. |
| How do you add a timeout? | `Promise.race` with a timer, or `AbortSignal.timeout(ms)` passed to `fetch`/streams. Always clear the timer in `finally`. |
| How do you cancel work? | `AbortController` — pass the signal down and check `signal.throwIfAborted()` in loops. |
| Retry strategy? | Only for idempotent operations: exponential backoff + jitter, a max attempt count, and a circuit breaker for a failing dependency. |
| What happens on an unhandled rejection? | Node terminates the process by default. Add `process.on('unhandledRejection')`/`uncaughtException` to log and exit — don't keep serving in an unknown state. |
| EventEmitter gotchas? | Listeners are synchronous; an `error` event with no listener throws; forgetting `removeListener` leaks (watch for the max-listeners warning). |
| What are async iterators for? | Streaming results: `for await (const page of paginate())`, reading a stream line by line, consuming a queue. |

## Streams

| Question | Answer |
|---|---|
| Why streams? | Constant memory regardless of payload size, and work starts before the data has fully arrived. |
| Four types? | Readable, Writable, Duplex, Transform. |
| What is backpressure? | `write()` returns `false` when the buffer is full; you must wait for `'drain'`. `pipeline()` handles it for you. Ignoring it means unbounded memory. |
| `pipe` vs `pipeline`? | `pipe` doesn't forward errors or clean up; `pipeline` (from `stream/promises`) destroys every stream and gives you one error. Always use `pipeline`. |
| `highWaterMark`? | The buffer size that decides when backpressure kicks in (default 64 KB, or 16 objects in object mode). |
| Where do you use streams in an API? | Uploads/downloads, CSV/NDJSON processing, gzip, proxying, server-sent events. |

## HTTP and API design

| Question | Answer |
|---|---|
| What does Express give you? | Routing, middleware composition, body parsing, error handling, response helpers. Fastify is faster with schema-based validation/serialization; NestJS adds structure and DI. |
| How does middleware work? | `(req, res, next)` in registration order; error middleware has four arguments. Order matters: logging → security → parsing → routes → 404 → error handler. |
| Status codes? | 200/201/204; 400 validation, 401 unauthenticated, 403 forbidden, 404, 409 conflict, 422 semantic, 429 rate limited, 500, 503. |
| REST vs GraphQL vs gRPC? | Resource URLs and HTTP caching / one flexible query per client (watch N+1 and query cost) / fast binary RPC with contracts, best service-to-service. |
| Idempotency? | GET/PUT/DELETE are idempotent by definition; make POST safe to retry with an `Idempotency-Key` stored with the result. |
| How do you version an API? | URL prefix (`/v1`) is simplest; keep changes additive and deprecate with headers and a timeline. |
| How do you paginate? | Cursor/keyset for feeds and large tables; offset only for small, stable data. |
| Validation? | At the boundary with zod/joi/Fastify schemas; reject unknown fields; never trust client input. |
| File uploads? | Stream to storage (S3 pre-signed URL) rather than buffering in the API; enforce size and type limits. |
| Graceful shutdown? | On `SIGTERM`: stop accepting, finish in-flight requests, close DB/queue connections, then exit — with a force timeout, because keep-alive sockets can hold connections open. |

## Security

| Question | Answer |
|---|---|
| How do you store passwords? | A slow KDF with a per-user salt: bcrypt/scrypt/argon2. Compare with `timingSafeEqual`. |
| JWT vs sessions? | JWT: stateless, scales horizontally, but can't be revoked before `exp`. Sessions: revocable server state, needs a shared store. Common answer: short-lived access JWT + refresh token + deny list. |
| JWT pitfalls? | The payload is readable (base64, not encrypted); always pin the algorithm (`alg: none` and RS256→HS256 confusion attacks); verify `exp`; keep the secret out of git. |
| Top web risks to mention? | Injection (parameterized queries), broken auth, XSS (escape output, CSP), CSRF (SameSite cookies + tokens), SSRF (allow-list outbound URLs), insecure deserialization, secrets in code, vulnerable dependencies. |
| Which headers? | `helmet` sets HSTS, X-Content-Type-Options, frame options and CSP. Configure CORS with an explicit origin allow-list. |
| How do you protect against DoS? | Rate limiting, body size limits, request timeouts, pagination caps, a reverse proxy/WAF, and never blocking the loop. |
| Secrets management? | Environment variables from a secret manager (Vault, AWS Secrets Manager), rotated; never committed; not logged. |

## Databases and data patterns

| Question | Answer |
|---|---|
| Why a connection pool? | Connections are expensive; a pool bounds them and queues extra work. Always release in `finally`. |
| Transaction handling? | BEGIN → work → COMMIT, ROLLBACK on error; keep transactions short; be aware of the isolation level. |
| N+1 problem? | 1 query for the list plus 1 per row. Fix with a JOIN, an `IN` query, or DataLoader-style batching. |
| Caching strategy? | Cache-aside with TTL, invalidate on write, protect against stampedes (single-flight), and add jitter to TTLs. |
| SQL vs NoSQL for a service? | Relational for transactional core data; document/KV for flexible or huge-scale access patterns; pick by access pattern, not fashion. |
| How do you handle migrations? | Versioned migration files in CI, expand → migrate → contract so old and new code can run together during a deploy. |
| Queues — why and which? | Decouple, absorb spikes, retry safely. BullMQ/Redis for jobs, Kafka/SQS/RabbitMQ for events. Consumers must be idempotent (at-least-once delivery). |
| Transactional outbox? | Write the row and the event in one transaction, publish asynchronously — so the database and the event stream can't disagree. |

## Testing, observability and operations

| Question | Answer |
|---|---|
| How do you test a Node service? | Unit tests for pure logic, integration tests for routes with a real-ish DB (testcontainers), a few end-to-end tests. `node:test` is built in; Jest/Vitest are common. |
| How do you test HTTP handlers? | Start the app on an ephemeral port and call it (supertest or `fetch`) — as `04_http_api/http_server.mjs` does. |
| What do you log? | Structured JSON (pino), one line per request with a request id, level, latency and status — never secrets. Correlate across services with a trace id. |
| What do you monitor? | RED metrics (rate, errors, duration), event loop lag, memory/RSS, GC pauses, queue depth, dependency latency. |
| Health checks? | `/health` (liveness: is the process alive) and `/ready` (readiness: are the DB/queue reachable). Kubernetes restarts on liveness, removes from the load balancer on readiness. |
| How do you debug a memory leak? | Watch RSS/heap over time, take heap snapshots (`--heap-prof`, Chrome DevTools), compare retained objects. Usual culprits: unbounded caches/maps, listeners never removed, closures holding big buffers, global arrays. |
| How do you deploy? | Container with a pinned Node version, `npm ci`, non-root user, env config, health checks, rolling deploys, and a process manager or orchestrator to restart on crash. |
| 12-factor points worth naming? | Config in the environment, stateless processes, logs to stdout, dev/prod parity, disposability (fast start, graceful shutdown). |

## "What does this print?"

```js
console.log('1');
setTimeout(() => console.log('2'), 0);
setImmediate(() => console.log('3'));
process.nextTick(() => console.log('4'));
Promise.resolve().then(() => console.log('5'));
console.log('6');
// CommonJS: 1 6 4 5 then 2/3 in an order that depends on startup timing
// ESM:      1 6 5 4 then 2/3  (module evaluation is already inside a promise job)
```

```js
const fs = require('node:fs');
fs.readFile(__filename, () => {
  setTimeout(() => console.log('timeout'), 0);
  setImmediate(() => console.log('immediate'));
});
// immediate, then timeout - inside an I/O callback the check phase comes first
```

## Code review red flags

```js
const data = fs.readFileSync(path);            // sync I/O on the request path -> blocks everything
app.get('/x', async (req, res) => { ... });    // no try/catch and no async error middleware -> hangs
await Promise.all(ids.map(load));              // unbounded parallelism against the database
JSON.parse(await streamToString(req));         // no body size limit -> memory DoS
catch (e) { res.status(500).json(e); }         // leaks stack traces and driver internals
const token = jwt.decode(raw);                 // decode does NOT verify the signature
setInterval(poll, 1000);                       // never cleared; keeps the process alive
new Map() at module scope, never evicted;      // unbounded cache -> memory leak
process.on('uncaughtException', () => {});     // swallowing crashes: log and exit instead
```
