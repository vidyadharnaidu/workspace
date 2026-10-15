# Testing a Node.js Backend

## The built-in test runner (no dependencies)

```js
// user.service.test.mjs   ->   node --test
import test from 'node:test';
import assert from 'node:assert/strict';
import { createUser } from './user.service.mjs';

test('creates a user', async (t) => {
  const repo = { save: t.mock.fn(async (user) => ({ ...user, id: 1 })) };
  const user = await createUser({ name: 'Ann', email: 'ann@example.com' }, repo);

  assert.equal(user.id, 1);
  assert.equal(repo.save.mock.callCount(), 1);
});

test('rejects an invalid email', async () => {
  await assert.rejects(() => createUser({ name: 'Ann', email: 'nope' }, {}), /valid email/);
});
```

Run: `node --test`, `node --test --watch`, `node --test --experimental-test-coverage`.
Jest and Vitest are the common alternatives; the structure below is identical.

## The testing pyramid for a service

| Layer | What it covers | Speed |
|---|---|---|
| **Unit** | Pure functions: validators, mappers, pricing, token signing | milliseconds |
| **Integration** | A route end to end with a real database (testcontainers or docker-compose), repositories, migrations | seconds |
| **Contract** | The API shape your consumers depend on (OpenAPI/Pact) | seconds |
| **End-to-end** | A few critical journeys against a deployed environment | minutes |

## Testing HTTP handlers

Start the app on an **ephemeral port** and make real requests — no mocking of the framework:

```js
import test from 'node:test';
import assert from 'node:assert/strict';
import { once } from 'node:events';
import { createApp } from './app.mjs';

test('POST /users', async (t) => {
  const server = createApp().listen(0);
  await once(server, 'listening');
  t.after(() => server.close());                        // always clean up

  const { port } = server.address();
  const response = await fetch(`http://127.0.0.1:${port}/users`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ name: 'Ann', email: 'ann@example.com' }),
  });

  assert.equal(response.status, 201);
  assert.match(response.headers.get('location'), /^\/users\/\d+$/);
});
```

(`supertest` does the same thing with a nicer API; `04_http_api/http_server.mjs` in this folder uses plain `fetch`.)

## Test doubles

| Kind | Use |
|---|---|
| **Stub** | Returns canned data (`{ findUser: async () => user }`) |
| **Mock/spy** | Records calls (`t.mock.fn()`, `vi.fn()`, `jest.fn()`) |
| **Fake** | A working lightweight implementation (in-memory repository) — usually the best choice |
| **Nock / MSW / undici MockAgent** | Intercept outbound HTTP so tests don't hit third parties |

```js
// intercept outgoing fetch calls (undici powers fetch in Node)
import { MockAgent, setGlobalDispatcher } from 'undici';
const agent = new MockAgent();
setGlobalDispatcher(agent);
agent.get('https://api.stripe.com').intercept({ path: '/v1/charges', method: 'POST' })
     .reply(200, { id: 'ch_1' });
```

## Controlling time and randomness

```js
t.mock.timers.enable({ apis: ['setTimeout'] });
t.mock.timers.tick(5000);                 // no real waiting
```
Inject the clock (`{ now = () => Date.now() } = {}`) and ids (`{ uuid = randomUUID } = {}`) so tests are deterministic — the pattern used throughout this folder.

## Integration tests with a real database

```js
// testcontainers: a throwaway Postgres per test run
const container = await new PostgreSqlContainer('postgres:16').start();
process.env.DATABASE_URL = container.getConnectionUri();
await runMigrations();
t.after(() => container.stop());
```
Prefer a **real** database over mocking the driver: SQL, constraints and transactions are exactly what you want to test. Reset state between tests with a transaction rollback or by truncating tables.

## What to test (and what not)

✅ Validation rules and error mapping (400/404/409 bodies), auth guards, pagination and filters, idempotency/retry behaviour, transaction rollback, queue consumer handling duplicates, graceful shutdown draining requests.
❌ Third-party libraries, trivial getters, exact log strings, or the framework itself.

## Useful practices

- **Arrange–Act–Assert**, one behaviour per test, and a name that states the expectation.
- Tests must be **independent**: no shared mutable state, no ordering assumptions, fresh data per test.
- Assert on **behaviour and contracts**, not internal call sequences.
- Keep a `test` script in `package.json`, run it in CI with `npm ci`, and fail the build on coverage drops — while remembering coverage is a signal, not a goal.
- For flaky async tests, await an observable condition instead of sleeping.
