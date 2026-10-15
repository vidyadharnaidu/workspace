/**
 * Array and object recipes: the "can you do this with map/filter/reduce?" round.
 *
 * Run: node javascript/04_arrays_objects/arrays_objects_recipes.js
 *
 * REMEMBER:
 *  * map/filter/reduce/flatMap/slice return NEW arrays. sort/reverse/splice/push MUTATE.
 *    Modern non-mutating twins: toSorted, toReversed, toSpliced, with.
 *  * reduce is the swiss army knife: sum, group, count, index, flatten, pipeline.
 *  * sort() converts to strings by default: [10, 9].sort() -> [10, 9] is wrong; pass (a, b) => a - b.
 *  * Object.keys/values/entries + Object.fromEntries convert between objects and arrays.
 *  * Use Map when keys are not strings or order/size matters; Set for uniqueness.
 */

function check(label, actual, expected) {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
  console.log(`${label.padEnd(34)} ${a}`);
}

const people = [
  { name: "ann", dept: "it", salary: 120, age: 30 },
  { name: "bob", dept: "hr", salary: 95, age: 45 },
  { name: "cat", dept: "it", salary: 120, age: 28 },
  { name: "dan", dept: "hr", salary: 130, age: 35 },
  { name: "eve", dept: "sales", salary: 60, age: 25 },
];

const groupBy = (items, keyFn) =>
  items.reduce((acc, item) => {
    const key = keyFn(item);
    (acc[key] ??= []).push(item);
    return acc;
  }, {});

const countBy = (items, keyFn) =>
  items.reduce((acc, item) => {
    const key = keyFn(item);
    acc[key] = (acc[key] ?? 0) + 1;
    return acc;
  }, {});

const sumBy = (items, valueFn) => items.reduce((total, item) => total + valueFn(item), 0);

const maxBy = (items, valueFn) => items.reduce((best, item) => (valueFn(item) > valueFn(best) ? item : best));

const keyById = (items, key = "id") => Object.fromEntries(items.map((item) => [item[key], item]));

const unique = (items) => [...new Set(items)];

// TRAP: new Map(entries) keeps the LAST item per key. Filter with a Set to keep the FIRST.
const uniqueBy = (items, keyFn) => {
  const seen = new Set();
  return items.filter((item) => !seen.has(keyFn(item)) && seen.add(keyFn(item)));
};
const uniqueByKeepLast = (items, keyFn) => [...new Map(items.map((item) => [keyFn(item), item])).values()];

const chunk = (items, size) =>
  Array.from({ length: Math.ceil(items.length / size) }, (_, i) => items.slice(i * size, i * size + size));

const flattenDeep = (items) => items.flat(Infinity);

const zip = (a, b) => a.map((value, i) => [value, b[i]]);

const range = (start, end, step = 1) =>
  Array.from({ length: Math.ceil((end - start) / step) }, (_, i) => start + i * step);

const sortByMultiple = (items, ...keyFns) =>
  [...items].sort((a, b) => {
    for (const keyFn of keyFns) {
      const [x, y] = [keyFn(a), keyFn(b)];
      if (x < y) return -1;
      if (x > y) return 1;
    }
    return 0;
  });

const pick = (obj, keys) => Object.fromEntries(keys.filter((k) => k in obj).map((k) => [k, obj[k]]));

const omit = (obj, keys) => Object.fromEntries(Object.entries(obj).filter(([k]) => !keys.includes(k)));

const invert = (obj) => Object.fromEntries(Object.entries(obj).map(([k, v]) => [v, k]));

const mapValues = (obj, fn) => Object.fromEntries(Object.entries(obj).map(([k, v]) => [k, fn(v, k)]));

/** Deep merge (objects only; arrays are replaced). */
function deepMerge(target, source) {
  const output = { ...target };
  for (const [key, value] of Object.entries(source)) {
    output[key] =
      value && typeof value === "object" && !Array.isArray(value) && typeof output[key] === "object"
        ? deepMerge(output[key], value)
        : value;
  }
  return output;
}

/** Deep equality without JSON.stringify (key order independent). */
function deepEqual(a, b) {
  if (a === b) return true;
  if (typeof a !== "object" || typeof b !== "object" || a === null || b === null) return false;
  const keysA = Object.keys(a);
  const keysB = Object.keys(b);
  return keysA.length === keysB.length && keysA.every((k) => deepEqual(a[k], b[k]));
}

/** Get a nested value by path with a default, e.g. get(obj, "a.b[0].c"). */
function get(obj, path, fallback) {
  const value = path
    .replace(/\[(\d+)]/g, ".$1")
    .split(".")
    .reduce((acc, key) => (acc == null ? undefined : acc[key]), obj);
  return value === undefined ? fallback : value;
}

/** Flatten nested objects: { a: { b: 1 } } -> { "a.b": 1 } */
function flattenObject(obj, prefix = "") {
  return Object.entries(obj).reduce((acc, [key, value]) => {
    const path = prefix ? `${prefix}.${key}` : key;
    return value && typeof value === "object" && !Array.isArray(value)
      ? { ...acc, ...flattenObject(value, path) }
      : { ...acc, [path]: value };
  }, {});
}

if (require.main === module) {
  check("groupBy dept -> names", mapValues(groupBy(people, (p) => p.dept), (g) => g.map((p) => p.name)),
        { it: ["ann", "cat"], hr: ["bob", "dan"], sales: ["eve"] });
  check("countBy dept", countBy(people, (p) => p.dept), { it: 2, hr: 2, sales: 1 });
  check("sumBy salary", sumBy(people, (p) => p.salary), 525);
  check("maxBy salary", maxBy(people, (p) => p.salary).name, "dan");
  check("highest paid per dept",
        mapValues(groupBy(people, (p) => p.dept), (g) => maxBy(g, (p) => p.salary).name),
        { it: "ann", hr: "dan", sales: "eve" });
  check("average age", Math.round(sumBy(people, (p) => p.age) / people.length), 33);
  check("keyById", Object.keys(keyById(people, "name")), ["ann", "bob", "cat", "dan", "eve"]);

  check("unique", unique([3, 1, 3, 2, 1]), [3, 1, 2]);
  check("uniqueBy keeps first", uniqueBy(people, (p) => p.salary).map((p) => p.name), ["ann", "bob", "dan", "eve"]);
  check("uniqueBy keeps last", uniqueByKeepLast(people, (p) => p.salary).map((p) => p.name), ["cat", "bob", "dan", "eve"]);
  check("chunk", chunk([1, 2, 3, 4, 5], 2), [[1, 2], [3, 4], [5]]);
  check("flatten deep", flattenDeep([1, [2, [3, [4]]]]), [1, 2, 3, 4]);
  check("flatMap", [1, 2, 3].flatMap((n) => [n, n * 10]), [1, 10, 2, 20, 3, 30]);
  check("zip", zip([1, 2], ["a", "b"]), [[1, "a"], [2, "b"]]);
  check("range", range(0, 10, 3), [0, 3, 6, 9]);

  check("sort numbers (needs comparator)", [10, 9, 100].sort((a, b) => a - b), [9, 10, 100]);
  check("sort strings default is wrong", [10, 9, 100].sort(), [10, 100, 9]);
  check("sortByMultiple dept, -salary",
        sortByMultiple(people, (p) => p.dept, (p) => -p.salary).map((p) => p.name),
        ["dan", "bob", "ann", "cat", "eve"]);
  check("toSorted does not mutate", (() => { const a = [3, 1]; const b = a.toSorted(); return [a, b]; })(),
        [[3, 1], [1, 3]]);

  check("find / findIndex / some / every",
        [people.find((p) => p.dept === "hr").name, people.findIndex((p) => p.salary > 125),
         people.some((p) => p.age > 40), people.every((p) => p.salary > 50)],
        ["bob", 3, true, true]);
  check("at(-1) / includes", [[1, 2, 3].at(-1), [1, 2, 3].includes(2)], [3, true]);

  check("pick / omit", [pick({ a: 1, b: 2, c: 3 }, ["a", "c"]), omit({ a: 1, b: 2, c: 3 }, ["b"])],
        [{ a: 1, c: 3 }, { a: 1, c: 3 }]);
  check("invert", invert({ a: "1", b: "2" }), { 1: "a", 2: "b" });
  check("mapValues", mapValues({ a: 1, b: 2 }, (v) => v * 10), { a: 10, b: 20 });
  check("deepMerge", deepMerge({ a: 1, nested: { x: 1, y: 2 } }, { nested: { y: 9, z: 3 } }),
        { a: 1, nested: { x: 1, y: 9, z: 3 } });
  check("deepEqual ignores key order", [deepEqual({ a: 1, b: { c: 2 } }, { b: { c: 2 }, a: 1 }), deepEqual([1], [2])],
        [true, false]);
  check("get by path", [get({ a: { b: [{ c: 7 }] } }, "a.b[0].c"), get({}, "x.y", "default")], [7, "default"]);
  check("flattenObject", flattenObject({ user: { name: "ann", address: { city: "NY" } }, ok: true }),
        { "user.name": "ann", "user.address.city": "NY", ok: true });

  const map = new Map([["a", 1]]);
  map.set({ id: 1 }, "object key");
  check("Map basics", [map.size, map.get("a"), [...map.keys()].length], [2, 1, 2]);
  check("Set operations", [...new Set([1, 2, 3])].filter((n) => new Set([2, 3, 4]).has(n)), [2, 3]);
  check("object <-> entries", Object.fromEntries(Object.entries({ a: 1 }).map(([k, v]) => [k, v + 1])), { a: 2 });

  console.log("\nall checks passed");
}
