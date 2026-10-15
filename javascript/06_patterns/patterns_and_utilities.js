/**
 * Patterns and "implement this utility" questions (the most common JS coding round).
 *
 * Run: node javascript/06_patterns/patterns_and_utilities.js
 *
 * REMEMBER: interviewers ask you to WRITE these from scratch:
 *   debounce, throttle, memoize, deepClone, EventEmitter, once, curry, promisify,
 *   a Promise from scratch, Array.prototype.map/reduce polyfills, bind polyfill, retry, LRU.
 */

function check(label, actual, expected) {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
  console.log(`${label.padEnd(34)} ${a}`);
}

// ---------------------------------------------------------------- module pattern & singleton

const CounterModule = (() => {
  let count = 0; // private: an IIFE closure was the pre-ES6 "module"
  return { increment: () => ++count, get: () => count };
})();

class Config {
  static #instance;
  #values = new Map();
  static getInstance() {
    return (Config.#instance ??= new Config());
  }
  set(key, value) {
    this.#values.set(key, value);
    return this;
  }
  get(key) {
    return this.#values.get(key);
  }
}

// ---------------------------------------------------------------- observer / event emitter

class EventEmitter {
  #listeners = new Map();

  on(event, handler) {
    this.#listeners.set(event, [...(this.#listeners.get(event) ?? []), handler]);
    return () => this.off(event, handler); // unsubscribe function
  }

  once(event, handler) {
    const unsubscribe = this.on(event, (...args) => {
      unsubscribe();
      handler(...args);
    });
    return unsubscribe;
  }

  off(event, handler) {
    this.#listeners.set(event, (this.#listeners.get(event) ?? []).filter((h) => h !== handler));
  }

  emit(event, ...args) {
    const handlers = this.#listeners.get(event) ?? [];
    handlers.forEach((handler) => handler(...args));
    return handlers.length;
  }
}

// ---------------------------------------------------------------- factory & strategy

const shapeFactory = {
  circle: (size) => ({ type: "circle", area: () => Math.PI * size ** 2 }),
  square: (size) => ({ type: "square", area: () => size ** 2 }),
  create(type, size) {
    const build = this[type];
    if (!build) throw new Error(`unknown shape: ${type}`);
    return build(size);
  },
};

const shippingStrategies = {
  standard: (weight) => weight * 1.0,
  express: (weight) => weight * 2.5,
  free: () => 0,
};
const shippingCost = (strategy, weight) => (shippingStrategies[strategy] ?? shippingStrategies.standard)(weight);

// ---------------------------------------------------------------- deep clone & structural helpers

function deepClone(value, seen = new WeakMap()) {
  if (value === null || typeof value !== "object") return value;
  if (seen.has(value)) return seen.get(value); // handles circular references
  if (value instanceof Date) return new Date(value);
  if (value instanceof Map) return new Map([...value].map(([k, v]) => [deepClone(k, seen), deepClone(v, seen)]));
  if (value instanceof Set) return new Set([...value].map((v) => deepClone(v, seen)));
  const clone = Array.isArray(value) ? [] : {};
  seen.set(value, clone);
  for (const [key, inner] of Object.entries(value)) clone[key] = deepClone(inner, seen);
  return clone;
}

// ---------------------------------------------------------------- polyfills (classic interview asks)

function myMap(array, callback) {
  const result = [];
  for (let i = 0; i < array.length; i++) result.push(callback(array[i], i, array));
  return result;
}

function myFilter(array, predicate) {
  const result = [];
  for (let i = 0; i < array.length; i++) if (predicate(array[i], i, array)) result.push(array[i]);
  return result;
}

function myReduce(array, reducer, ...initial) {
  let index = 0;
  let accumulator;
  if (initial.length) {
    accumulator = initial[0];
  } else {
    if (!array.length) throw new TypeError("Reduce of empty array with no initial value");
    accumulator = array[index++];
  }
  for (; index < array.length; index++) accumulator = reducer(accumulator, array[index], index, array);
  return accumulator;
}

function myFlat(array, depth = 1) {
  return depth < 1
    ? array.slice()
    : array.reduce((acc, item) => acc.concat(Array.isArray(item) ? myFlat(item, depth - 1) : item), []);
}

/** A minimal Promise implementation (then/resolve/reject + async callbacks). */
class MyPromise {
  #state = "pending";
  #value;
  #callbacks = [];

  constructor(executor) {
    const settle = (state) => (value) => {
      if (this.#state !== "pending") return;
      this.#state = state;
      this.#value = value;
      queueMicrotask(() => this.#callbacks.forEach((cb) => cb()));
    };
    const reject = settle("rejected");
    const resolve = (value) => {
      // THENABLE ASSIMILATION: resolving with a promise adopts its state instead of
      // wrapping it. Without this, `then(() => somePromise)` never chains.
      if (value && typeof value.then === "function") value.then(resolve, reject);
      else settle("fulfilled")(value);
    };
    try {
      executor(resolve, reject);
    } catch (err) {
      reject(err);
    }
  }

  then(onFulfilled, onRejected) {
    return new MyPromise((resolve, reject) => {
      const run = () => {
        try {
          if (this.#state === "fulfilled") {
            resolve(onFulfilled ? onFulfilled(this.#value) : this.#value);
          } else if (onRejected) {
            resolve(onRejected(this.#value));
          } else {
            reject(this.#value);
          }
        } catch (err) {
          reject(err);
        }
      };
      this.#state === "pending" ? this.#callbacks.push(run) : queueMicrotask(run);
    });
  }

  catch(onRejected) {
    return this.then(undefined, onRejected);
  }

  static resolve(value) {
    return new MyPromise((resolve) => resolve(value));
  }
}

// ---------------------------------------------------------------- immutability helpers

const updateIn = (obj, key, value) => ({ ...obj, [key]: value });
const addItem = (list, item) => [...list, item];
const removeItem = (list, index) => [...list.slice(0, index), ...list.slice(index + 1)];
const replaceItem = (list, index, item) => list.with(index, item); // ES2023

function deepFreeze(obj) {
  Object.values(obj).forEach((value) => {
    if (value && typeof value === "object") deepFreeze(value);
  });
  return Object.freeze(obj);
}

if (require.main === module) {
  CounterModule.increment();
  CounterModule.increment();
  check("module pattern (private state)", [CounterModule.get(), CounterModule.count], [2, undefined]);
  check("singleton", Config.getInstance().set("env", "prod") === Config.getInstance(), true);
  check("singleton keeps state", Config.getInstance().get("env"), "prod");

  const emitter = new EventEmitter();
  const received = [];
  const unsubscribe = emitter.on("order", (id) => received.push(`handler:${id}`));
  emitter.once("order", (id) => received.push(`once:${id}`));
  emitter.emit("order", 1);
  emitter.emit("order", 2);
  unsubscribe();
  emitter.emit("order", 3);
  check("EventEmitter on/once/off", received, ["handler:1", "once:1", "handler:2"]);

  check("factory", shapeFactory.create("square", 3).area(), 9);
  check("factory unknown type", (() => { try { shapeFactory.create("hexagon", 1); } catch (e) { return e.message; } })(),
        "unknown shape: hexagon");
  check("strategy", [shippingCost("express", 10), shippingCost("free", 10), shippingCost("unknown", 10)], [25, 0, 10]);

  const original = { name: "ann", tags: ["a"], meta: { created: new Date("2026-01-01") } };
  original.self = original; // circular
  const clone = deepClone(original);
  original.tags.push("b");
  check("deepClone independent", [clone.tags.length, clone.meta.created instanceof Date, clone.self === clone],
        [1, true, true]);

  check("myMap", myMap([1, 2, 3], (n) => n * 2), [2, 4, 6]);
  check("myFilter", myFilter([1, 2, 3, 4], (n) => n % 2 === 0), [2, 4]);
  check("myReduce with initial", myReduce([1, 2, 3], (a, b) => a + b, 10), 16);
  check("myReduce without initial", myReduce([1, 2, 3], (a, b) => a + b), 6);
  check("myReduce empty throws", (() => { try { myReduce([], (a, b) => a + b); } catch (e) { return e.constructor.name; } })(),
        "TypeError");
  check("myFlat depth 1 / Infinity", [myFlat([1, [2, [3]]]), myFlat([1, [2, [3]]], Infinity)], [[1, 2, [3]], [1, 2, 3]]);

  check("immutable update helpers",
        [updateIn({ a: 1 }, "b", 2), addItem([1], 2), removeItem([1, 2, 3], 1), replaceItem([1, 2, 3], 0, 9)],
        [{ a: 1, b: 2 }, [1, 2], [1, 3], [9, 2, 3]]);
  const frozen = deepFreeze({ nested: { value: 1 } });
  frozen.nested.value = 99;
  check("deepFreeze", frozen.nested.value, 1);

  MyPromise.resolve(2)
    .then((n) => n * 3)
    .then((n) => {
      check("MyPromise chaining", n, 6);
      return new MyPromise((_, reject) => reject(new Error("boom")));
    })
    .catch((err) => {
      check("MyPromise catch", err.message ?? err, "boom");
      console.log("\nall checks passed");
    });
}
