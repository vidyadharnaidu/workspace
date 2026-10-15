/**
 * A REST API with node:http only - no Express - then tested against itself over a real socket.
 *
 * Run: node nodejs/04_http_api/http_server.mjs
 *
 * WHY NO FRAMEWORK: interviewers ask "what does Express actually do for you?". The answer is
 * routing, body parsing, middleware composition, error handling and a response helper - all below.
 *
 * REMEMBER:
 *  * Middleware = (req, res, next) composed in order; error middleware takes (err, req, res, next).
 *  * ALWAYS limit the body size (a request body is a stream - an unbounded read is a DoS).
 *  * Status codes: 200/201/204, 400 validation, 401 unauthenticated, 403 forbidden, 404, 409 conflict,
 *    422 semantic, 429 rate limited, 500 server, 503 unavailable.
 *  * Never leak stack traces or driver errors to clients; log them with a request id instead.
 *  * Graceful shutdown: stop accepting, finish in-flight requests, close the DB, then exit.
 */
import { createServer } from 'node:http';
import { randomUUID } from 'node:crypto';
import { once } from 'node:events';

function check(label, actual, expected) {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
  console.log(`${label.padEnd(44)} ${a}`);
}

// ---------------------------------------------------------------- tiny framework

class HttpError extends Error {
  constructor(status, message, code = 'ERROR', details) {
    super(message);
    this.name = 'HttpError';
    this.status = status;
    this.code = code;
    this.details = details; // e.g. per-field validation messages
  }
}

const MAX_BODY_BYTES = 1024 * 100; // 100 KB

async function readJsonBody(req) {
  const chunks = [];
  let size = 0;
  for await (const chunk of req) {
    size += chunk.length;
    if (size > MAX_BODY_BYTES) throw new HttpError(413, 'Payload too large', 'BODY_TOO_LARGE');
    chunks.push(chunk);
  }
  if (!chunks.length) return {};
  try {
    return JSON.parse(Buffer.concat(chunks).toString('utf8'));
  } catch {
    throw new HttpError(400, 'Invalid JSON body', 'INVALID_JSON');
  }
}

function sendJson(res, status, payload, headers = {}) {
  const body = JSON.stringify(payload);
  res.writeHead(status, {
    'content-type': 'application/json; charset=utf-8',
    'content-length': Buffer.byteLength(body),
    'x-request-id': res.requestId,
    ...headers,
  });
  res.end(body);
}

/** Router with :params, built on URLPattern-free string matching so it works everywhere. */
function createRouter() {
  const routes = [];

  const add = (method, pattern, handler) => {
    const keys = [];
    const regex = new RegExp(
      '^' +
        pattern
          .split('/')
          .map((segment) => {
            if (!segment.startsWith(':')) return segment;
            keys.push(segment.slice(1));
            return '([^/]+)';
          })
          .join('/') +
        '$',
    );
    routes.push({ method, regex, keys, handler });
  };

  return {
    get: (pattern, handler) => add('GET', pattern, handler),
    post: (pattern, handler) => add('POST', pattern, handler),
    delete: (pattern, handler) => add('DELETE', pattern, handler),
    match(method, pathname) {
      let pathMatched = false;
      for (const route of routes) {
        const match = route.regex.exec(pathname);
        if (!match) continue;
        pathMatched = true;
        // TRAP: don't return 405 on the first path match - the same path is registered once
        // per method, so keep looking until a route matches BOTH path and method.
        if (route.method !== method) continue;
        const params = Object.fromEntries(route.keys.map((key, i) => [key, decodeURIComponent(match[i + 1])]));
        return { handler: route.handler, params };
      }
      return pathMatched ? { methodMismatch: true } : null;
    },
  };
}

// ---------------------------------------------------------------- the API

const users = new Map([[1, { id: 1, name: 'Ann', email: 'ann@example.com' }]]);
let nextId = 2;
const requestLog = [];
const rateLimit = new Map(); // ip -> { count, resetAt }

function checkRateLimit(ip, limit = 50, windowMs = 1000) {
  const now = Date.now();
  const entry = rateLimit.get(ip);
  if (!entry || entry.resetAt <= now) {
    rateLimit.set(ip, { count: 1, resetAt: now + windowMs });
    return { allowed: true, remaining: limit - 1 };
  }
  entry.count++;
  return { allowed: entry.count <= limit, remaining: Math.max(0, limit - entry.count) };
}

function validateUser(payload) {
  const errors = {};
  if (typeof payload.name !== 'string' || !payload.name.trim()) errors.name = 'Name is required';
  if (typeof payload.email !== 'string' || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(payload.email)) {
    errors.email = 'A valid email is required';
  }
  if (Object.keys(errors).length) throw new HttpError(400, 'Validation failed', 'VALIDATION_ERROR', errors);
  return { name: payload.name.trim(), email: payload.email.toLowerCase() };
}

const router = createRouter();

router.get('/health', (_req, res) => sendJson(res, 200, { status: 'ok', uptime: Math.round(process.uptime()) }));

router.get('/users', (req, res) => {
  // pagination via query params, with sane bounds
  const limit = Math.min(Number(req.query.get('limit') ?? 10) || 10, 100);
  const items = [...users.values()].slice(0, limit);
  sendJson(res, 200, { items, total: users.size });
});

router.get('/users/:id', (req, res) => {
  const user = users.get(Number(req.params.id));
  if (!user) throw new HttpError(404, 'User not found', 'USER_NOT_FOUND');
  sendJson(res, 200, user);
});

router.post('/users', async (req, res) => {
  const payload = validateUser(await readJsonBody(req));
  if ([...users.values()].some((u) => u.email === payload.email)) {
    throw new HttpError(409, 'Email already registered', 'DUPLICATE_EMAIL');
  }
  const user = { id: nextId++, ...payload };
  users.set(user.id, user);
  sendJson(res, 201, user, { location: `/users/${user.id}` }); // 201 + Location for a created resource
});

router.delete('/users/:id', (req, res) => {
  const existed = users.delete(Number(req.params.id));
  if (!existed) throw new HttpError(404, 'User not found', 'USER_NOT_FOUND');
  res.writeHead(204).end(); // 204 = success, no body
});

let slowRequestArrived;
const slowRequestReceived = new Promise((resolve) => {
  slowRequestArrived = resolve;
});

router.get('/slow', async (_req, res) => {
  slowRequestArrived(); // let the test know the request is genuinely in flight
  await new Promise((resolve) => setTimeout(resolve, 50));
  sendJson(res, 200, { done: true });
});

router.get('/boom', () => {
  throw new Error('database connection lost'); // an UNEXPECTED error: must become a safe 500
});

// ---------------------------------------------------------------- server with middleware pipeline

const server = createServer(async (req, res) => {
  const startedAt = process.hrtime.bigint();
  res.requestId = req.headers['x-request-id'] ?? randomUUID(); // correlate logs across services

  try {
    const ip = req.socket.remoteAddress ?? 'unknown';
    const { allowed, remaining } = checkRateLimit(ip);
    if (!allowed) throw new HttpError(429, 'Too many requests', 'RATE_LIMITED');
    res.setHeader('x-ratelimit-remaining', remaining);

    const url = new URL(req.url, `http://${req.headers.host ?? 'localhost'}`);
    req.query = url.searchParams;

    const matched = router.match(req.method, url.pathname);
    if (!matched) throw new HttpError(404, 'Route not found', 'NOT_FOUND');
    if (matched.methodMismatch) throw new HttpError(405, 'Method not allowed', 'METHOD_NOT_ALLOWED');

    req.params = matched.params;
    await matched.handler(req, res); // await: so a rejected promise lands in the catch below
  } catch (error) {
    // ONE error handler for the whole app
    const isKnown = error instanceof HttpError;
    const status = isKnown ? error.status : 500;
    if (!isKnown) console.error(`[${res.requestId}] unhandled:`, error.message); // log the real cause
    sendJson(res, status, {
      error: {
        code: isKnown ? error.code : 'INTERNAL',
        message: isKnown ? error.message : 'Internal server error', // never leak internals
        ...(error.details ? { details: error.details } : {}),
        requestId: res.requestId,
      },
    });
  } finally {
    const ms = Number(process.hrtime.bigint() - startedAt) / 1e6;
    requestLog.push({ method: req.method, url: req.url, status: res.statusCode, ms });
  }
});

// ---------------------------------------------------------------- graceful shutdown

function gracefulShutdown(httpServer, { timeoutMs = 5000 } = {}) {
  return new Promise((resolve) => {
    let settled = false;
    const finish = (how) => {
      if (settled) return;
      settled = true;
      clearTimeout(forceTimer);
      resolve(how);
    };

    // The FORCE timer is not optional: HTTP keep-alive means clients hold sockets open, so
    // close() alone may never call back. Drain first, then cut the rest after the grace period.
    const forceTimer = setTimeout(() => {
      httpServer.closeAllConnections?.();
      finish('forced');
    }, timeoutMs);

    httpServer.close(() => finish('closed')); // stop accepting, wait for in-flight requests
    httpServer.closeIdleConnections?.(); // kick out sockets that are between requests
  });
}

// ---------------------------------------------------------------- self-test against the running server

server.listen(0, '127.0.0.1');
await once(server, 'listening');
const { port } = server.address();
const base = `http://127.0.0.1:${port}`;

const health = await fetch(`${base}/health`);
check('GET /health', [health.status, (await health.json()).status], [200, 'ok']);
check('request id header present', health.headers.has('x-request-id'), true);

const list = await fetch(`${base}/users`);
check('GET /users', (await list.json()).total, 1);

const created = await fetch(`${base}/users`, {
  method: 'POST',
  headers: { 'content-type': 'application/json' },
  body: JSON.stringify({ name: ' Bob ', email: 'BOB@Example.com' }),
});
const createdBody = await created.json();
check('POST /users creates', [created.status, created.headers.get('location'), createdBody.name, createdBody.email],
      [201, '/users/2', 'Bob', 'bob@example.com']);

const duplicate = await fetch(`${base}/users`, {
  method: 'POST',
  headers: { 'content-type': 'application/json' },
  body: JSON.stringify({ name: 'Bob2', email: 'bob@example.com' }),
});
check('POST duplicate -> 409', [duplicate.status, (await duplicate.json()).error.code], [409, 'DUPLICATE_EMAIL']);

const invalid = await fetch(`${base}/users`, {
  method: 'POST',
  headers: { 'content-type': 'application/json' },
  body: JSON.stringify({ name: '', email: 'not-an-email' }),
});
check('POST invalid -> 400', [invalid.status, (await invalid.json()).error.code], [400, 'VALIDATION_ERROR']);

const badJson = await fetch(`${base}/users`, {
  method: 'POST',
  headers: { 'content-type': 'application/json' },
  body: '{ not json',
});
check('malformed JSON -> 400', [badJson.status, (await badJson.json()).error.code], [400, 'INVALID_JSON']);

const missing = await fetch(`${base}/users/999`);
check('GET missing -> 404', [missing.status, (await missing.json()).error.code], [404, 'USER_NOT_FOUND']);

const wrongMethod = await fetch(`${base}/health`, { method: 'POST' });
check('wrong method -> 405', wrongMethod.status, 405);

const deleted = await fetch(`${base}/users/2`, { method: 'DELETE' });
check('DELETE -> 204 with no body', [deleted.status, await deleted.text()], [204, '']);

const boom = await fetch(`${base}/boom`);
const boomBody = await boom.json();
check('unexpected error -> safe 500', [boom.status, boomBody.error.code, boomBody.error.message],
      [500, 'INTERNAL', 'Internal server error']);
check('500 response hides internals', JSON.stringify(boomBody).includes('database connection lost'), false);

const oversized = await fetch(`${base}/users`, {
  method: 'POST',
  headers: { 'content-type': 'application/json' },
  body: JSON.stringify({ name: 'x'.repeat(200_000), email: 'a@b.co' }),
});
check('oversized body -> 413', oversized.status, 413);

// graceful shutdown: a request already in flight must still finish.
// Wait until the server has RECEIVED it, otherwise we race the client's socket setup.
const inFlight = fetch(`${base}/slow`);
await slowRequestReceived;
const [shutdownHow, inFlightStatus] = await Promise.all([
  gracefulShutdown(server, { timeoutMs: 300 }), // short grace period for the test
  inFlight.then((response) => response.status),
]);
check('in-flight request finished', inFlightStatus, 200);
check('shutdown completed', ['closed', 'forced'].includes(shutdownHow), true);
console.log(`${'shutdown ended by'.padEnd(44)} "${shutdownHow}" (keep-alive sockets need the force timer)`);
check('requests were logged', requestLog.length >= 11, true);
console.log(`${'requests handled'.padEnd(44)} ${requestLog.length}`);

console.log('\nall checks passed');
