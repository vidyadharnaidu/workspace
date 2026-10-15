/**
 * The Angular concepts you can run and verify WITHOUT Angular: validators, store logic,
 * a 40-line signal implementation, and the flattening-operator semantics.
 *
 * Run: node angular/08_logic/pure_logic.mjs     (no dependencies)
 *
 * WHY: in an interview, being able to explain signals and switchMap/concatMap/exhaustMap by
 * writing them is far stronger than naming them. Everything here mirrors the .ts files.
 */

function check(label, actual, expected) {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
  console.log(`${label.padEnd(40)} ${a}`);
}

// ================================================================ 1. form validators (05_forms)

function strongPassword(minLength = 8) {
  return (value) => {
    const text = String(value ?? '');
    if (!text) return null;
    const missing = [];
    if (text.length < minLength) missing.push(`at least ${minLength} characters`);
    if (!/[0-9]/.test(text)) missing.push('a number');
    if (!/[A-Z]/.test(text)) missing.push('an uppercase letter');
    return missing.length ? { strongPassword: { missing } } : null;
  };
}

const passwordsMatch = (group) =>
  group.password && group.confirmPassword && group.password !== group.confirmPassword
    ? { passwordsMismatch: true }
    : null;

// ================================================================ 2. store logic (07_state_signals)

const visibleTodos = (todos, filter, query = '') =>
  todos
    .filter((t) => (filter === 'all' ? true : filter === 'done' ? t.done : !t.done))
    .filter((t) => t.title.toLowerCase().includes(query.toLowerCase()));

const stats = (todos) => {
  const done = todos.filter((t) => t.done).length;
  return { total: todos.length, done, remaining: todos.length - done };
};

// ================================================================ 3. signals from scratch (~40 lines)

/**
 * The core ideas: a computed is LAZY (it only recomputes when read after a dependency changed),
 * dependencies are tracked automatically while the computation runs, and equality (Object.is)
 * stops the propagation when the value did not really change.
 */
let activeConsumer = null;

function createSignal(initial, equal = Object.is) {
  let value = initial;
  const consumers = new Set();

  const read = () => {
    if (activeConsumer) {
      consumers.add(activeConsumer);
      activeConsumer.sources.add(read);
    }
    return value;
  };

  read.set = (next) => {
    if (equal(value, next)) return; // no change -> no notifications
    value = next;
    for (const consumer of [...consumers]) consumer.markDirty();
  };
  read.update = (fn) => read.set(fn(value));
  return read;
}

function createComputed(compute) {
  let cached;
  let dirty = true;
  const consumers = new Set();
  const node = {
    sources: new Set(),
    markDirty() {
      if (dirty) return;
      dirty = true;
      for (const consumer of [...consumers]) consumer.markDirty();
    },
  };

  const read = () => {
    if (activeConsumer) {
      consumers.add(activeConsumer);
      activeConsumer.sources.add(read);
    }
    if (dirty) {
      const previous = activeConsumer;
      activeConsumer = node;
      node.sources.clear();
      cached = compute();      // dependencies register themselves while this runs
      activeConsumer = previous;
      dirty = false;
    }
    return cached;
  };
  return read;
}

function createEffect(fn) {
  const node = {
    sources: new Set(),
    markDirty() {
      run(); // a real implementation schedules this; running now keeps the demo synchronous
    },
  };
  const run = () => {
    const previous = activeConsumer;
    activeConsumer = node;
    node.sources.clear();
    fn();
    activeConsumer = previous;
  };
  run();
  return { destroy: () => node.sources.clear() };
}

// ================================================================ 4. flattening operators (04_rxjs)

/** Minimal observable: subscribe(observer) -> unsubscribe function. Enough to show the semantics. */
const timed = (values, stepMs = 10) => (observer) => {
  let cancelled = false;
  const timers = values.map((value, index) =>
    setTimeout(() => {
      if (!cancelled) observer.next(value);
      if (!cancelled && index === values.length - 1) observer.complete?.();
    }, (index + 1) * stepMs),
  );
  return () => {
    cancelled = true;               // <- cancellation: this is what switchMap uses
    timers.forEach(clearTimeout);
  };
};

/**
 * Runs `project` for each source value with the four flattening strategies and records
 * everything the subscriber saw, so the differences are visible instead of memorized.
 */
function flatten(strategy, sourceValues, project, gapMs = 5) {
  return new Promise((resolve) => {
    const received = [];
    const queue = [];
    let cancelActive = null;
    let running = 0;
    let sourceDone = false;

    const finishIfDone = () => {
      if (sourceDone && running === 0 && queue.length === 0) setTimeout(() => resolve(received), gapMs);
    };

    const startInner = (value) => {
      running++;
      let alive = true;
      const release = () => {
        if (!alive) return; // a cancelled inner must not also "complete"
        alive = false;
        running--;
      };

      const unsubscribe = project(value)({
        next: (innerValue) => {
          if (alive) received.push(innerValue);
        },
        complete: () => {
          release();
          cancelActive = null;
          if (strategy === 'concatMap' && queue.length) startInner(queue.shift());
          finishIfDone();
        },
      });

      if (strategy === 'switchMap') {
        cancelActive = () => {
          unsubscribe();
          release();
        };
      }
    };

    sourceValues.forEach((value, index) => {
      setTimeout(() => {
        if (strategy === 'switchMap' && cancelActive) cancelActive(); // cancel the previous inner
        if (strategy === 'exhaustMap' && running > 0) return;          // ignore while one is active
        if (strategy === 'concatMap' && running > 0) {
          queue.push(value);                                           // queue it, keep order
          return;
        }
        startInner(value);
      }, index * gapMs);
    });

    // the source completes after its last value, whatever each strategy decided to do with them
    setTimeout(() => {
      sourceDone = true;
      finishIfDone();
    }, sourceValues.length * gapMs);
  });
}

// ================================================================ run everything

async function main() {
  // 1. validators
  check('strongPassword: too short', strongPassword()('Ab1'), { strongPassword: { missing: ['at least 8 characters'] } });
  check('strongPassword: no digit', strongPassword()('Password'), { strongPassword: { missing: ['a number'] } });
  check('strongPassword: valid', strongPassword()('Passw0rd'), null);
  check('strongPassword: empty is required-s job', strongPassword()(''), null);
  check('passwordsMatch: mismatch', passwordsMatch({ password: 'a', confirmPassword: 'b' }), { passwordsMismatch: true });
  check('passwordsMatch: equal', passwordsMatch({ password: 'a', confirmPassword: 'a' }), null);

  // 2. store logic
  const todos = [
    { id: '1', title: 'learn signals', done: false },
    { id: '2', title: 'learn rxjs', done: true },
    { id: '3', title: 'ship feature', done: false },
  ];
  check('visibleTodos: active', visibleTodos(todos, 'active').map((t) => t.id), ['1', '3']);
  check('visibleTodos: done + query', visibleTodos(todos, 'done', 'rxjs').map((t) => t.id), ['2']);
  check('stats', stats(todos), { total: 3, done: 1, remaining: 2 });

  // 3. signals
  let computeCount = 0;
  const first = createSignal('Ada');
  const last = createSignal('Lovelace');
  const fullName = createComputed(() => {
    computeCount++;
    return `${first()} ${last()}`;
  });

  check('computed is lazy (not run yet)', computeCount, 0);
  check('computed value', fullName(), 'Ada Lovelace');
  check('computed caches', [fullName(), computeCount], ['Ada Lovelace', 1]);

  first.set('Grace');
  check('recomputes after a dependency changed', [fullName(), computeCount], ['Grace Lovelace', 2]);

  first.set('Grace'); // same value -> Object.is equality stops the notification
  check('setting the same value does nothing', computeCount, 2);

  const seen = [];
  createEffect(() => seen.push(fullName()));
  last.set('Hopper');
  check('effect re-runs on change', seen, ['Grace Lovelace', 'Grace Hopper']);

  const counter = createSignal(0);
  const doubled = createComputed(() => counter() * 2);
  counter.update((n) => n + 5);
  check('update() + derived chain', [counter(), doubled()], [5, 10]);

  // 4. flattening operators: the source emits A, B, C quickly; each inner emits 2 values slowly
  const project = (value) => timed([`${value}1`, `${value}2`], 12);
  const source = ['A', 'B', 'C'];

  check('switchMap  cancels previous', await flatten('switchMap', source, project), ['C1', 'C2']);
  check('exhaustMap ignores while busy', await flatten('exhaustMap', source, project), ['A1', 'A2']);
  check('concatMap  queues, keeps order', await flatten('concatMap', source, project),
        ['A1', 'A2', 'B1', 'B2', 'C1', 'C2']);
  const merged = await flatten('mergeMap', source, project);
  check('mergeMap   runs all concurrently', [merged.length, [...merged].sort()],
        [6, ['A1', 'A2', 'B1', 'B2', 'C1', 'C2']]);

  console.log('\nall checks passed');
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
