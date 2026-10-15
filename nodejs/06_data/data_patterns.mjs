/**
 * Backend data patterns: connection pooling, transactions, caching, idempotency,
 * cursor pagination, the N+1 problem, batching, circuit breakers, outbox.
 *
 * Run: node nodejs/06_data/data_patterns.mjs
 *
 * Everything runs against a FAKE database so the concepts (not a driver) are the point.
 *
 * REMEMBER:
 *  * Never open a connection per request: use a POOL and always release it in `finally`.
 *  * A transaction is BEGIN -> work -> COMMIT, with ROLLBACK on any error.
 *  * Cursor (keyset) pagination beats OFFSET at scale: `WHERE id < :cursor ORDER BY id DESC LIMIT n`.
 *  * N+1 = 1 query for the list + 1 per row. Fix with a JOIN, an IN query, or a DataLoader-style batch.
 *  * Retries need idempotency keys, or you create duplicate orders/charges.
 *  * Cache: read-through + TTL + invalidate on write; guard against stampedes.
 */
import { randomUUID } from 'node:crypto';
import { setTimeout as sleep } from 'node:timers/promises';

function check(label, actual, expected) {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
  console.log(`${label.padEnd(44)} ${a}`);
}

// ---------------------------------------------------------------- a fake database + connection pool

class FakeDatabase {
  rows = new Map([
    [1, { id: 1, name: 'Ann', teamId: 10 }],
    [2, { id: 2, name: 'Bob', teamId: 10 }],
    [3, { id: 3, name: 'Cat', teamId: 20 }],
  ]);
  teams = new Map([
    [10, { id: 10, name: 'Platform' }],
    [20, { id: 20, name: 'Payments' }],
  ]);
  queryCount = 0;

  async query(sql, params = []) {
    this.queryCount++;
    await sleep(2); // pretend network + disk
    if (sql.startsWith('SELECT users')) return [...this.rows.values()];
    if (sql.startsWith('SELECT team WHERE id =')) return [this.teams.get(params[0])].filter(Boolean);
    if (sql.startsWith('SELECT teams WHERE id IN')) return params.map((id) => this.teams.get(id)).filter(Boolean);
    throw new Error(`unknown query: ${sql}`);
  }
}

class ConnectionPool {
  #size;
  #available;
  #waiting = [];
  peakInUse = 0;

  constructor(size, factory) {
    this.#size = size;
    this.#available = Array.from({ length: size }, (_, i) => factory(i));
  }

  async acquire() {
    if (this.#available.length === 0) {
      await new Promise((resolve) => this.#waiting.push(resolve)); // queue instead of opening more connections
    }
    const connection = this.#available.pop();
    this.peakInUse = Math.max(this.peakInUse, this.#size - this.#available.length);
    return connection;
  }

  release(connection) {
    this.#available.push(connection);
    this.#waiting.shift()?.();
  }

  /** The pattern that prevents leaks: the caller can never forget to release. */
  async withConnection(fn) {
    const connection = await this.acquire();
    try {
      return await fn(connection);
    } finally {
      this.release(connection);
    }
  }
}

// ---------------------------------------------------------------- transactions

async function transfer(connection, from, to, amount) {
  const log = connection.log;
  log.push('BEGIN');
  try {
    if (from.balance < amount) throw new Error('insufficient funds');
    from.balance -= amount;
    to.balance += amount;
    log.push('COMMIT');
    return { ok: true };
  } catch (error) {
    log.push('ROLLBACK'); // any failure must undo everything
    throw error;
  }
}

// ---------------------------------------------------------------- N+1 and batching

async function loadUsersWithTeams_N1(db) {
  const users = await db.query('SELECT users');
  const result = [];
  for (const user of users) {
    const [team] = await db.query('SELECT team WHERE id = ?', [user.teamId]); // 1 query PER USER
    result.push({ ...user, team: team.name });
  }
  return result;
}

async function loadUsersWithTeams_Batched(db) {
  const users = await db.query('SELECT users');
  const teamIds = [...new Set(users.map((u) => u.teamId))]; // dedupe, then ONE query
  const teams = await db.query('SELECT teams WHERE id IN', teamIds);
  const byId = new Map(teams.map((team) => [team.id, team]));
  return users.map((user) => ({ ...user, team: byId.get(user.teamId).name }));
}

/** DataLoader idea: collect ids during one tick, then fetch them in a single batch. */
function createLoader(batchFn) {
  let pending = [];
  let scheduled = false;
  return (id) =>
    new Promise((resolve, reject) => {
      pending.push({ id, resolve, reject });
      if (scheduled) return;
      scheduled = true;
      queueMicrotask(async () => {
        const batch = pending;
        pending = [];
        scheduled = false;
        try {
          const values = await batchFn(batch.map((item) => item.id));
          batch.forEach((item, index) => item.resolve(values[index]));
        } catch (error) {
          batch.forEach((item) => item.reject(error));
        }
      });
    });
}

// ---------------------------------------------------------------- cache with TTL + stampede protection

class Cache {
  #entries = new Map();
  #inFlight = new Map();
  hits = 0;
  misses = 0;

  constructor(ttlMs = 50, clock = () => Date.now()) {
    this.ttlMs = ttlMs;
    this.clock = clock;
  }

  async getOrLoad(key, loader) {
    const entry = this.#entries.get(key);
    if (entry && entry.expiresAt > this.clock()) {
      this.hits++;
      return entry.value;
    }
    // STAMPEDE PROTECTION: 100 concurrent misses must trigger ONE load, not 100
    const existing = this.#inFlight.get(key);
    if (existing) return existing;

    this.misses++;
    const promise = loader()
      .then((value) => {
        this.#entries.set(key, { value, expiresAt: this.clock() + this.ttlMs });
        return value;
      })
      .finally(() => this.#inFlight.delete(key));

    this.#inFlight.set(key, promise);
    return promise;
  }

  invalidate(key) {
    this.#entries.delete(key); // write-through: invalidate on every update
  }
}

// ---------------------------------------------------------------- idempotency

class IdempotentProcessor {
  #results = new Map();
  charges = 0;

  async process(idempotencyKey, operation) {
    const existing = this.#results.get(idempotencyKey);
    if (existing) return { ...existing, replayed: true }; // safe retry: same answer, no second charge
    const result = await operation();
    this.charges++;
    this.#results.set(idempotencyKey, result);
    return { ...result, replayed: false };
  }
}

// ---------------------------------------------------------------- cursor pagination

function paginateByCursor(rows, { cursor = null, limit = 2 } = {}) {
  const sorted = [...rows].sort((a, b) => b.id - a.id); // newest first
  const start = cursor === null ? 0 : sorted.findIndex((row) => row.id === cursor) + 1;
  const page = sorted.slice(start, start + limit);
  return { items: page, nextCursor: page.length === limit ? (page.at(-1)?.id ?? null) : null };
}

// ---------------------------------------------------------------- circuit breaker

function createCircuitBreaker(fn, { failureThreshold = 3, openMs = 50, clock = () => Date.now() } = {}) {
  let failures = 0;
  let state = 'CLOSED';
  let openedAt = 0;

  return async (...args) => {
    if (state === 'OPEN') {
      if (clock() - openedAt < openMs) throw new Error('circuit open'); // fail fast, protect the dependency
      state = 'HALF_OPEN';
    }
    try {
      const result = await fn(...args);
      failures = 0;
      state = 'CLOSED';
      return result;
    } catch (error) {
      failures++;
      if (state === 'HALF_OPEN' || failures >= failureThreshold) {
        state = 'OPEN';
        openedAt = clock();
      }
      throw error;
    }
  };
}

// ---------------------------------------------------------------- transactional outbox

function saveOrderWithOutbox(db, order) {
  // The order row AND the event are written in the SAME transaction, so they can never disagree.
  db.orders.push(order);
  db.outbox.push({ id: randomUUID(), type: 'order.created', payload: { orderId: order.id }, publishedAt: null });
  return order;
}

async function publishOutbox(db, publish) {
  const unpublished = db.outbox.filter((event) => event.publishedAt === null);
  for (const event of unpublished) {
    await publish(event); // at-least-once: consumers must be idempotent
    event.publishedAt = Date.now();
  }
  return unpublished.length;
}

// ================================================================ run

const db = new FakeDatabase();

const pool = new ConnectionPool(2, (id) => ({ id, log: [] }));
const poolResults = await Promise.all(
  [1, 2, 3, 4, 5].map((n) => pool.withConnection(async (connection) => {
    await sleep(5);
    return `${n}:conn${connection.id}`;
  })),
);
check('pool serves everyone', poolResults.length, 5);
check('pool never exceeded its size', pool.peakInUse <= 2, true);

const accounts = { a: { balance: 100 }, b: { balance: 0 } };
const txConnection = { log: [] };
await transfer(txConnection, accounts.a, accounts.b, 40);
check('transaction commits', [txConnection.log, accounts.a.balance, accounts.b.balance], [['BEGIN', 'COMMIT'], 60, 40]);

const failConnection = { log: [] };
await transfer(failConnection, accounts.a, accounts.b, 1000).catch(() => {});
check('transaction rolls back', failConnection.log, ['BEGIN', 'ROLLBACK']);

db.queryCount = 0;
await loadUsersWithTeams_N1(db);
const n1Queries = db.queryCount;
db.queryCount = 0;
const batched = await loadUsersWithTeams_Batched(db);
const batchedQueries = db.queryCount;
check('N+1 result is correct but costly', [n1Queries, batchedQueries], [4, 2]);
check('batched result matches', batched.map((u) => u.team), ['Platform', 'Platform', 'Payments']);

let batchCalls = 0;
const teamLoader = createLoader(async (ids) => {
  batchCalls++;
  const teams = await db.query('SELECT teams WHERE id IN', [...new Set(ids)]);
  const byId = new Map(teams.map((team) => [team.id, team]));
  return ids.map((id) => byId.get(id));
});
const loaded = await Promise.all([teamLoader(10), teamLoader(20), teamLoader(10)]);
check('DataLoader batches into one call', [batchCalls, loaded.map((team) => team.name)],
      [1, ['Platform', 'Payments', 'Platform']]);

let loads = 0;
const cache = new Cache(50);
const loader = async () => {
  loads++;
  await sleep(5);
  return 'expensive value';
};
const concurrent = await Promise.all(Array.from({ length: 20 }, () => cache.getOrLoad('key', loader)));
check('cache stampede protection', [loads, new Set(concurrent).size], [1, 1]);
await cache.getOrLoad('key', loader);
check('cache hit on the second call', [cache.hits, cache.misses], [1, 1]);
cache.invalidate('key');
await cache.getOrLoad('key', loader);
check('invalidate forces a reload', loads, 2);

const processor = new IdempotentProcessor();
const key = randomUUID();
const first = await processor.process(key, async () => ({ chargeId: 'ch_1', amount: 999 }));
const retryResult = await processor.process(key, async () => ({ chargeId: 'ch_2', amount: 999 }));
check('idempotent retry returns the first result', [first.chargeId, retryResult.chargeId, retryResult.replayed],
      ['ch_1', 'ch_1', true]);
check('customer charged exactly once', processor.charges, 1);

const rows = Array.from({ length: 5 }, (_, i) => ({ id: i + 1 }));
const page1 = paginateByCursor(rows, { limit: 2 });
const page2 = paginateByCursor(rows, { cursor: page1.nextCursor, limit: 2 });
check('cursor pagination', [page1.items.map((r) => r.id), page2.items.map((r) => r.id)], [[5, 4], [3, 2]]);

let serviceUp = false;
const callService = createCircuitBreaker(async () => {
  if (!serviceUp) throw new Error('service down');
  return 'ok';
});
const failures = [];
for (let i = 0; i < 5; i++) failures.push(await callService().catch((error) => error.message));
check('circuit opens after 3 failures', failures, ['service down', 'service down', 'service down', 'circuit open', 'circuit open']);

const outboxDb = { orders: [], outbox: [] };
saveOrderWithOutbox(outboxDb, { id: 'order-1', total: 25 });
const published = [];
check('outbox published once', await publishOutbox(outboxDb, async (event) => published.push(event.type)), 1);
check('outbox is not republished', await publishOutbox(outboxDb, async (event) => published.push(event.type)), 0);
check('event content', published, ['order.created']);

console.log('\nall checks passed');
