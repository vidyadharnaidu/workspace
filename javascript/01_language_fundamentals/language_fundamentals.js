/**
 * JavaScript language fundamentals - the questions that decide interviews.
 *
 * Run: node javascript/01_language_fundamentals/language_fundamentals.js
 *
 * REMEMBER:
 *  * var is function-scoped and hoisted as undefined; let/const are block-scoped in the "temporal dead zone".
 *  * const freezes the BINDING, not the value (const objects can still be mutated).
 *  * Primitives are copied by value; objects are copied by REFERENCE.
 *  * == coerces types, === does not. Use === (the only common exception: x == null to catch null+undefined).
 *  * Falsy values: false, 0, -0, 0n, "", null, undefined, NaN. Everything else is truthy ([] and {} are truthy!).
 */

function check(label, actual, expected) {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
  console.log(`${label.padEnd(36)} ${a}`);
}

// Q1. Hoisting: var vs let
function hoisting() {
  const results = [];
  results.push(typeof varVariable); // declared below, hoisted as undefined
  var varVariable = 1;
  try {
    letVariable; // temporal dead zone
  } catch (err) {
    results.push(err.constructor.name);
  }
  let letVariable = 2;
  return results;
}

// Q2. Closures in loops - the classic bug
function closuresInLoop() {
  const withVar = [];
  for (var i = 0; i < 3; i++) withVar.push(() => i); // one shared `i`
  const withLet = [];
  for (let j = 0; j < 3; j++) withLet.push(() => j); // a new binding per iteration
  return { var: withVar.map((f) => f()), let: withLet.map((f) => f()) };
}

// Q3. const does not make objects immutable
function constMutation() {
  const config = { debug: false };
  config.debug = true; // allowed
  const frozen = Object.freeze({ debug: false });
  frozen.debug = true; // silently ignored (throws in strict mode)
  return { config: config.debug, frozen: frozen.debug };
}

// Q4. Copy semantics
function copySemantics() {
  const original = { name: "ann", tags: ["a"] };
  const reference = original; // same object
  const shallow = { ...original }; // new object, tags still shared
  const deep = structuredClone(original); // Node 17+ / modern browsers
  original.tags.push("b");
  return { reference: reference.tags.length, shallow: shallow.tags.length, deep: deep.tags.length };
}

// Q5. Type coercion quiz
function coercion() {
  return {
    "1 + '2'": 1 + "2", // "12"  (+ with a string concatenates)
    "'3' - 1": "3" - 1, // 2     (- always converts to numbers)
    "[] + {}": typeof ([] + {}), // "string"
    "0 == '0'": 0 == "0", // true (coerced)
    "0 === '0'": 0 === "0", // false
    "null == undefined": null == undefined, // true
    "null === undefined": null === undefined, // false
    "NaN === NaN": NaN === NaN, // false; use Number.isNaN / Object.is
    "Object.is(NaN, NaN)": Object.is(NaN, NaN),
    "typeof null": typeof null, // "object" (a historical bug)
    "0.1 + 0.2 === 0.3": 0.1 + 0.2 === 0.3, // false (floating point)
  };
}

// Q6. Default vs nullish: || vs ??
function defaults(value) {
  return { or: value || "fallback", nullish: value ?? "fallback" };
}

// Q7. Destructuring, rest and spread
function destructuring() {
  const { name, address: { city } = {}, ...rest } = { name: "ann", age: 30, address: { city: "NY" }, role: "dev" };
  const [first, , third = "default"] = ["a", "b"];
  const merged = { ...{ a: 1, b: 2 }, b: 3 }; // later wins
  const swapped = (([x, y]) => [y, x])([1, 2]);
  return { name, city, rest, first, third, merged, swapped };
}

// Q8. Optional chaining and short-circuit
function optionalChaining(user) {
  return {
    city: user?.address?.city ?? "unknown",
    called: user?.getName?.() ?? "no method",
    length: user?.tags?.length ?? 0,
  };
}

// Q9. Arrays are objects; equality is by reference
function referenceEquality() {
  return {
    "[] === []": [] === [],
    "JSON compare": JSON.stringify([1, 2]) === JSON.stringify([1, 2]),
    "typeof []": typeof [],
    "Array.isArray([])": Array.isArray([]),
  };
}

// Q10. Numbers and strings
function numbersAndStrings() {
  return {
    "parseInt('08')": parseInt("08", 10),
    "Number('')": Number(""),
    "(0.1+0.2).toFixed(2)": (0.1 + 0.2).toFixed(2),
    "Math.round(-2.5)": Math.round(-2.5), // -2 (rounds toward +Infinity)
    "big int": String(9007199254740993n * 2n),
    "'abc'.at(-1)": "abc".at(-1),
    padded: String(7).padStart(3, "0"),
  };
}

if (require.main === module) {
  check("Q1 hoisting var/let", hoisting(), ["undefined", "ReferenceError"]);
  check("Q2 closures in loop", closuresInLoop(), { var: [3, 3, 3], let: [0, 1, 2] });
  check("Q3 const vs Object.freeze", constMutation(), { config: true, frozen: false });
  check("Q4 reference/shallow/deep copy", copySemantics(), { reference: 2, shallow: 2, deep: 1 });
  check("Q5 coercion quiz", coercion(), {
    "1 + '2'": "12",
    "'3' - 1": 2,
    "[] + {}": "string",
    "0 == '0'": true,
    "0 === '0'": false,
    "null == undefined": true,
    "null === undefined": false,
    "NaN === NaN": false,
    "Object.is(NaN, NaN)": true,
    "typeof null": "object",
    "0.1 + 0.2 === 0.3": false,
  });
  check("Q6 || treats 0 as missing", defaults(0), { or: "fallback", nullish: 0 });
  check("Q6 ?? only for null/undefined", defaults(undefined), { or: "fallback", nullish: "fallback" });
  check("Q7 destructuring", destructuring(), {
    name: "ann",
    city: "NY",
    rest: { age: 30, role: "dev" },
    first: "a",
    third: "default",
    merged: { a: 1, b: 3 },
    swapped: [2, 1],
  });
  check("Q8 optional chaining", optionalChaining({ address: { city: "Berlin" } }), {
    city: "Berlin",
    called: "no method",
    length: 0,
  });
  check("Q8 optional chaining (undefined)", optionalChaining(undefined), {
    city: "unknown",
    called: "no method",
    length: 0,
  });
  check("Q9 reference equality", referenceEquality(), {
    "[] === []": false,
    "JSON compare": true,
    "typeof []": "object",
    "Array.isArray([])": true,
  });
  check("Q10 numbers and strings", numbersAndStrings(), {
    "parseInt('08')": 8,
    "Number('')": 0,
    "(0.1+0.2).toFixed(2)": "0.30",
    "Math.round(-2.5)": -2,
    "big int": "18014398509481986",
    "'abc'.at(-1)": "c",
    padded: "007",
  });

  console.log("\nall checks passed");
}
