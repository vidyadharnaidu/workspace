/**
 * Classic algorithm problems in JavaScript (strings, arrays, recursion, data structures).
 *
 * Run: node javascript/05_algorithms/algorithm_problems.js
 *
 * REMEMBER (JS specifics):
 *  * Map/Set give O(1) lookups and keep insertion order; plain objects coerce keys to strings.
 *  * There is no built-in heap or deque: use an array (shift() is O(n) - for BFS keep an index pointer).
 *  * String concatenation in a loop is fine in modern engines, but array.push + join is still clearer.
 *  * Recursion limit is ~10k frames: prefer iteration for deep structures.
 */

function check(label, actual, expected) {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
  console.log(`${label.padEnd(32)} ${a}`);
}

// ---------------------------------------------------------------- strings

const reverseString = (s) => [...s].reverse().join(""); // [...s] handles emoji/surrogate pairs better than split("")

const isPalindrome = (s) => {
  const clean = s.toLowerCase().replace(/[^a-z0-9]/g, "");
  return clean === [...clean].reverse().join("");
};

const isAnagram = (a, b) => [...a.toLowerCase()].sort().join("") === [...b.toLowerCase()].sort().join("");

function charFrequency(s) {
  return [...s].reduce((acc, ch) => acc.set(ch, (acc.get(ch) ?? 0) + 1), new Map());
}

function firstUniqueChar(s) {
  const counts = charFrequency(s);
  return [...s].find((ch) => counts.get(ch) === 1) ?? null;
}

function groupAnagrams(words) {
  const groups = new Map();
  for (const word of words) {
    const key = [...word].sort().join("");
    groups.set(key, [...(groups.get(key) ?? []), word]);
  }
  return [...groups.values()];
}

function longestCommonPrefix(words) {
  if (!words.length) return "";
  const [first, last] = [words.toSorted().at(0), words.toSorted().at(-1)];
  let i = 0;
  while (i < first.length && first[i] === last[i]) i++;
  return first.slice(0, i);
}

function longestPalindrome(s) {
  let best = "";
  const expand = (l, r) => {
    while (l >= 0 && r < s.length && s[l] === s[r]) [l, r] = [l - 1, r + 1];
    return s.slice(l + 1, r);
  };
  for (let i = 0; i < s.length; i++) {
    for (const candidate of [expand(i, i), expand(i, i + 1)]) {
      if (candidate.length > best.length) best = candidate;
    }
  }
  return best;
}

function isValidParentheses(s) {
  const pairs = { ")": "(", "]": "[", "}": "{" };
  const stack = [];
  for (const ch of s) {
    if (ch in pairs) {
      if (stack.pop() !== pairs[ch]) return false;
    } else {
      stack.push(ch);
    }
  }
  return stack.length === 0;
}

function longestUniqueSubstring(s) {
  const lastSeen = new Map();
  let left = 0;
  let best = 0;
  for (let right = 0; right < s.length; right++) {
    const ch = s[right];
    if (lastSeen.has(ch) && lastSeen.get(ch) >= left) left = lastSeen.get(ch) + 1;
    lastSeen.set(ch, right);
    best = Math.max(best, right - left + 1);
  }
  return best;
}

const wordFrequencies = (text, top) =>
  [...(text.toLowerCase().match(/[a-z']+/g) ?? []).reduce((m, w) => m.set(w, (m.get(w) ?? 0) + 1), new Map())]
    .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
    .slice(0, top);

// ---------------------------------------------------------------- arrays

function twoSum(nums, target) {
  const seen = new Map();
  for (let i = 0; i < nums.length; i++) {
    if (seen.has(target - nums[i])) return [seen.get(target - nums[i]), i];
    seen.set(nums[i], i);
  }
  return [];
}

const maxSubArray = (nums) =>
  nums.reduce(
    (acc, n) => {
      acc.current = Math.max(n, acc.current + n);
      acc.best = Math.max(acc.best, acc.current);
      return acc;
    },
    { current: -Infinity, best: -Infinity }
  ).best;

function maxProfit(prices) {
  let cheapest = Infinity;
  let best = 0;
  for (const price of prices) {
    cheapest = Math.min(cheapest, price);
    best = Math.max(best, price - cheapest);
  }
  return best;
}

const missingNumber = (nums) => (nums.length * (nums.length + 1)) / 2 - nums.reduce((a, b) => a + b, 0);

const findDuplicates = (nums) => [...charFrequency(nums.map(String)).entries()].filter(([, c]) => c > 1).map(([n]) => Number(n));

function mergeIntervals(intervals) {
  return intervals
    .toSorted((a, b) => a[0] - b[0])
    .reduce((merged, [start, end]) => {
      const last = merged.at(-1);
      if (last && start <= last[1]) last[1] = Math.max(last[1], end);
      else merged.push([start, end]);
      return merged;
    }, []);
}

function rotate(nums, k) {
  const shift = k % nums.length;
  return [...nums.slice(-shift), ...nums.slice(0, -shift)];
}

function moveZeros(nums) {
  let write = 0;
  for (let i = 0; i < nums.length; i++) {
    if (nums[i] !== 0) {
      // TRAP: `[nums[write], nums[i]] = [nums[i], nums[write++]]` increments write mid-expression
      // and writes to the wrong slot. Keep the swap and the increment separate.
      [nums[write], nums[i]] = [nums[i], nums[write]];
      write++;
    }
  }
  return nums;
}

const topKFrequent = (nums, k) =>
  [...charFrequency(nums.map(String))].sort((a, b) => b[1] - a[1]).slice(0, k).map(([n]) => Number(n));

function binarySearch(nums, target) {
  let [lo, hi] = [0, nums.length - 1];
  while (lo <= hi) {
    const mid = (lo + hi) >> 1; // >> 1 is integer division by 2
    if (nums[mid] === target) return mid;
    if (nums[mid] < target) lo = mid + 1;
    else hi = mid - 1;
  }
  return -1;
}

// ---------------------------------------------------------------- recursion & structures

const fibonacci = (n, memo = new Map()) =>
  n < 2 ? n : memo.get(n) ?? (memo.set(n, fibonacci(n - 1, memo) + fibonacci(n - 2, memo)), memo.get(n));

const factorial = (n) => (n <= 1 ? 1 : n * factorial(n - 1));

function flattenDeepRecursive(items) {
  return items.reduce((flat, item) => flat.concat(Array.isArray(item) ? flattenDeepRecursive(item) : item), []);
}

function permutations(items) {
  if (items.length <= 1) return [items];
  return items.flatMap((item, i) =>
    permutations([...items.slice(0, i), ...items.slice(i + 1)]).map((rest) => [item, ...rest])
  );
}

/** Binary tree from a level-order array, then BFS/DFS. */
function buildTree(values) {
  if (!values.length) return null;
  const nodes = values.map((v) => (v === null ? null : { val: v, left: null, right: null }));
  let child = 1;
  for (const node of nodes) {
    if (!node) continue;
    if (child < nodes.length) node.left = nodes[child++];
    if (child < nodes.length) node.right = nodes[child++];
  }
  return nodes[0];
}

function levelOrder(root) {
  const result = [];
  let queue = root ? [root] : [];
  while (queue.length) {
    result.push(queue.map((n) => n.val));
    queue = queue.flatMap((n) => [n.left, n.right].filter(Boolean));
  }
  return result;
}

const maxDepth = (node) => (node ? 1 + Math.max(maxDepth(node.left), maxDepth(node.right)) : 0);

/** Graph BFS with an index pointer instead of shift() (O(1) per step). */
function bfs(graph, start) {
  const visited = new Set([start]);
  const queue = [start];
  const order = [];
  for (let head = 0; head < queue.length; head++) {
    const node = queue[head];
    order.push(node);
    for (const next of graph[node] ?? []) {
      if (!visited.has(next)) {
        visited.add(next);
        queue.push(next);
      }
    }
  }
  return order;
}

class LRUCache {
  // A Map keeps insertion order, so the first key is the least recently used.
  constructor(capacity) {
    this.capacity = capacity;
    this.map = new Map();
  }
  get(key) {
    if (!this.map.has(key)) return undefined;
    const value = this.map.get(key);
    this.map.delete(key);
    this.map.set(key, value); // move to the end = most recently used
    return value;
  }
  put(key, value) {
    if (this.map.has(key)) this.map.delete(key);
    this.map.set(key, value);
    if (this.map.size > this.capacity) this.map.delete(this.map.keys().next().value);
  }
  keys() {
    return [...this.map.keys()];
  }
}

if (require.main === module) {
  check("reverseString", reverseString("hello"), "olleh");
  check("isPalindrome", isPalindrome("A man, a plan, a canal: Panama"), true);
  check("isAnagram", isAnagram("Listen", "Silent"), true);
  check("firstUniqueChar", firstUniqueChar("swiss"), "w");
  check("groupAnagrams", groupAnagrams(["eat", "tea", "tan", "ate", "nat", "bat"]),
        [["eat", "tea", "ate"], ["tan", "nat"], ["bat"]]);
  check("longestCommonPrefix", longestCommonPrefix(["flower", "flow", "flight"]), "fl");
  check("longestPalindrome", longestPalindrome("forgeeksskeegfor"), "geeksskeeg");
  check("isValidParentheses", ["()[]{}", "([)]", "("].map(isValidParentheses), [true, false, false]);
  check("longestUniqueSubstring", longestUniqueSubstring("abcabcbb"), 3);
  check("wordFrequencies", wordFrequencies("the cat the hat the end", 2), [["the", 3], ["cat", 1]]);

  check("twoSum", twoSum([2, 7, 11, 15], 9), [0, 1]);
  check("maxSubArray", maxSubArray([-2, 1, -3, 4, -1, 2, 1, -5, 4]), 6);
  check("maxProfit", maxProfit([7, 1, 5, 3, 6, 4]), 5);
  check("missingNumber", missingNumber([3, 0, 1]), 2);
  check("findDuplicates", findDuplicates([1, 2, 3, 2, 4, 1, 1]), [1, 2]);
  check("mergeIntervals", mergeIntervals([[1, 3], [2, 6], [8, 10], [15, 18]]), [[1, 6], [8, 10], [15, 18]]);
  check("rotate", rotate([1, 2, 3, 4, 5, 6, 7], 3), [5, 6, 7, 1, 2, 3, 4]);
  check("moveZeros", moveZeros([0, 1, 0, 3, 12]), [1, 3, 12, 0, 0]);
  check("topKFrequent", topKFrequent([1, 1, 1, 2, 2, 3], 2), [1, 2]);
  check("binarySearch", [binarySearch([1, 3, 5, 7, 9], 7), binarySearch([1, 3, 5], 4)], [3, -1]);

  check("fibonacci(50)", fibonacci(50), 12586269025);
  check("factorial(10)", factorial(10), 3628800);
  check("flattenDeepRecursive", flattenDeepRecursive([1, [2, [3, [4]]]]), [1, 2, 3, 4]);
  check("permutations", permutations([1, 2, 3]).length, 6);

  const tree = buildTree([3, 9, 20, null, null, 15, 7]);
  check("levelOrder", levelOrder(tree), [[3], [9, 20], [15, 7]]);
  check("maxDepth", maxDepth(tree), 3);
  check("bfs", bfs({ a: ["b", "c"], b: ["d"], c: ["d"], d: [] }, "a"), ["a", "b", "c", "d"]);

  const cache = new LRUCache(2);
  cache.put("a", 1);
  cache.put("b", 2);
  cache.get("a");
  cache.put("c", 3);
  check("LRUCache", [cache.keys(), cache.get("b")], [["a", "c"], undefined]);

  console.log("\nall checks passed");
}
