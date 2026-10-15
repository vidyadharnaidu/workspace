/**
 * Functions, `this`, closures, currying, debounce/throttle, prototypes.
 *
 * Run: node javascript/02_functions_this_closures/functions_this_closures.js
 *
 * REMEMBER - how `this` is decided (check in this order):
 *  1. new Foo()        -> the new object
 *  2. explicit bind/call/apply -> what you passed
 *  3. obj.method()     -> obj (the caller before the dot)
 *  4. plain call()     -> undefined in strict mode / globalThis otherwise
 *  ARROW FUNCTIONS have no own `this`: they capture it from where they were DEFINED.
 *  Losing `this` (passing obj.method as a callback) is the #1 cause of "cannot read property of undefined".
 */

function check(label, actual, expected) {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
  console.log(`${label.padEnd(36)} ${a}`);
}

// ------------------------------------------------------------------ closures

function makeCounter() {
  let count = 0; // private state, kept alive by the closure
  return {
    increment: () => ++count,
    get value() {
      return count;
    },
  };
}

function once(fn) {
  let called = false;
  let result;
  return (...args) => {
    if (!called) {
      called = true;
      result = fn(...args);
    }
    return result;
  };
}

function memoize(fn, keyFn = (...args) => JSON.stringify(args)) {
  const cache = new Map();
  return (...args) => {
    const key = keyFn(...args);
    if (!cache.has(key)) cache.set(key, fn(...args));
    return cache.get(key);
  };
}

// ------------------------------------------------------------------ this

const user = {
  name: "ann",
  regular() {
    return this?.name;
  },
  delayed() {
    // arrow inside a method captures the method's `this` - the classic fix
    return [1].map(() => this.name)[0];
  },
};

function describe() {
  return `${this.role}:${this.name}`;
}

// ------------------------------------------------------------------ call/apply/bind implemented by hand

function myBind(fn, context, ...bound) {
  return function (...args) {
    return fn.apply(context, [...bound, ...args]);
  };
}

// ------------------------------------------------------------------ currying & composition

function curry(fn) {
  return function curried(...args) {
    return args.length >= fn.length ? fn(...args) : (...rest) => curried(...args, ...rest);
  };
}

const pipe = (...fns) => (value) => fns.reduce((acc, fn) => fn(acc), value);
const compose = (...fns) => (value) => fns.reduceRight((acc, fn) => fn(acc), value);

// ------------------------------------------------------------------ debounce & throttle (with an injectable clock)

function debounce(fn, waitMs, scheduler) {
  let timer = null;
  return (...args) => {
    if (timer) scheduler.clear(timer);
    timer = scheduler.set(() => fn(...args), waitMs); // only the LAST call in a quiet period runs
  };
}

function throttle(fn, intervalMs, now) {
  let last = -Infinity;
  return (...args) => {
    if (now() - last >= intervalMs) {
      last = now(); // at most one call per interval
      return fn(...args);
    }
  };
}

/** Tiny fake scheduler so the tests are deterministic (no real waiting). */
function createFakeScheduler() {
  let time = 0;
  let nextId = 1;
  const tasks = new Map();
  return {
    now: () => time,
    set(fn, delay) {
      tasks.set(nextId, { fn, at: time + delay });
      return nextId++;
    },
    clear(id) {
      tasks.delete(id);
    },
    advance(ms) {
      time += ms;
      for (const [id, task] of [...tasks].sort((a, b) => a[1].at - b[1].at)) {
        if (task.at <= time) {
          tasks.delete(id);
          task.fn();
        }
      }
    },
  };
}

// ------------------------------------------------------------------ prototypes & classes

function Animal(name) {
  this.name = name;
}
Animal.prototype.speak = function () {
  return `${this.name} makes a sound`;
};

class Dog extends Animal {
  constructor(name) {
    super(name);
  }
  speak() {
    return `${super.speak()} (woof)`;
  }
  static create(name) {
    return new Dog(name);
  }
  #secret = "hidden"; // truly private field (not accessible outside the class)
  reveal() {
    return this.#secret;
  }
}

if (require.main === module) {
  const counter = makeCounter();
  counter.increment();
  counter.increment();
  check("closure counter", counter.value, 2);

  let initCalls = 0;
  const init = once(() => ++initCalls);
  init();
  init();
  check("once() runs a single time", [init(), initCalls], [1, 1]);

  let slowCalls = 0;
  const slowSquare = memoize((n) => {
    slowCalls++;
    return n * n;
  });
  slowSquare(9);
  slowSquare(9);
  check("memoize caches", [slowSquare(9), slowCalls], [81, 1]);

  check("this: obj.method()", user.regular(), "ann");
  const detached = user.regular;
  check("this: detached method", detached() ?? "lost this", "lost this");
  check("this: bind fixes it", detached.bind(user)(), "ann");
  check("this: arrow in method", user.delayed(), "ann");
  check("call / apply", [describe.call({ role: "admin", name: "ann" }), describe.apply({ role: "dev", name: "bo" })],
        ["admin:ann", "dev:bo"]);
  check("hand-written bind", myBind(describe, { role: "qa", name: "cy" })(), "qa:cy");

  const add3 = curry((a, b, c) => a + b + c);
  check("curry", [add3(1)(2)(3), add3(1, 2)(3), add3(1, 2, 3)], [6, 6, 6]);
  const double = (n) => n * 2;
  const increment = (n) => n + 1;
  check("pipe / compose", [pipe(double, increment)(5), compose(double, increment)(5)], [11, 12]);

  const scheduler = createFakeScheduler();
  const calls = [];
  const search = debounce((term) => calls.push(term), 300, scheduler);
  search("a");
  search("ab");
  scheduler.advance(100);
  search("abc"); // resets the timer again
  scheduler.advance(300);
  check("debounce keeps only the last", calls, ["abc"]);

  const scheduler2 = createFakeScheduler();
  const saved = [];
  const save = throttle((n) => saved.push(n), 1000, scheduler2.now);
  save(1);
  save(2); // dropped: too soon
  scheduler2.advance(1000);
  save(3);
  check("throttle limits the rate", saved, [1, 3]);

  const dog = Dog.create("rex");
  check("class inheritance + super", dog.speak(), "rex makes a sound (woof)");
  check("private field", dog.reveal(), "hidden");
  check("prototype chain", [dog instanceof Dog, dog instanceof Animal, Object.getPrototypeOf(Dog.prototype) === Animal.prototype],
        [true, true, true]);

  console.log("\nall checks passed");
}
