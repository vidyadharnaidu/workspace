/**
 * Generic data structures and algorithms, typed.
 * "Implement a typed Stack / LRU cache / binary tree" is the standard TypeScript coding round.
 *
 * Run:   node typescript/07_generic_data_structures.ts
 *
 * REMEMBER:
 *  * Make the container generic (<T>), not the values `any`.
 *  * Return `T | undefined` for operations that can be empty - strict mode forces the caller to handle it.
 *  * Implement `[Symbol.iterator]()` so your structure works with for...of and spread.
 *  * Constrain only what you need: <T>, <T extends { id: string }>, <K extends keyof T>.
 */

function check(label: string, actual: unknown, expected: unknown): void {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
  console.log(`${label.padEnd(34)} ${a}`);
}

// ---------------------------------------------------------------- Stack & Queue

class Stack<T> implements Iterable<T> {
  #items: T[] = [];

  push(item: T): this {
    this.#items.push(item);
    return this;
  }

  pop(): T | undefined {
    return this.#items.pop();
  }

  peek(): T | undefined {
    return this.#items.at(-1);
  }

  get size(): number {
    return this.#items.length;
  }

  *[Symbol.iterator](): Iterator<T> {
    yield* [...this.#items].reverse();       // top first
  }
}

class Queue<T> {
  #items: T[] = [];
  #head = 0;                                  // index pointer: dequeue stays O(1)

  enqueue(item: T): this {
    this.#items.push(item);
    return this;
  }

  dequeue(): T | undefined {
    if (this.#head >= this.#items.length) return undefined;
    const item = this.#items[this.#head];
    this.#items[this.#head++] = undefined as T;   // release the reference
    if (this.#head > 32 && this.#head * 2 > this.#items.length) {
      this.#items = this.#items.slice(this.#head);
      this.#head = 0;
    }
    return item;
  }

  get size(): number {
    return this.#items.length - this.#head;
  }
}

// ---------------------------------------------------------------- LRU cache

class LRUCache<K, V> {
  readonly #capacity: number;
  readonly #map = new Map<K, V>();          // Map keeps insertion order

  constructor(capacity: number) {
    if (capacity <= 0) throw new RangeError("capacity must be positive");
    this.#capacity = capacity;
  }

  get(key: K): V | undefined {
    if (!this.#map.has(key)) return undefined;
    const value = this.#map.get(key) as V;
    this.#map.delete(key);
    this.#map.set(key, value);               // move to the end = most recently used
    return value;
  }

  set(key: K, value: V): this {
    if (this.#map.has(key)) this.#map.delete(key);
    this.#map.set(key, value);
    if (this.#map.size > this.#capacity) {
      const oldest = this.#map.keys().next().value as K;
      this.#map.delete(oldest);
    }
    return this;
  }

  get keys(): K[] {
    return [...this.#map.keys()];
  }
}

// ---------------------------------------------------------------- linked list

interface ListNode<T> {
  value: T;
  next: ListNode<T> | null;
}

function toLinkedList<T>(values: T[]): ListNode<T> | null {
  return values.reduceRight<ListNode<T> | null>((next, value) => ({ value, next }), null);
}

function toArray<T>(head: ListNode<T> | null): T[] {
  const out: T[] = [];
  for (let node = head; node; node = node.next) out.push(node.value);
  return out;
}

function reverseList<T>(head: ListNode<T> | null): ListNode<T> | null {
  let prev: ListNode<T> | null = null;
  let current = head;
  while (current) {
    const next: ListNode<T> | null = current.next;
    current.next = prev;
    prev = current;
    current = next;
  }
  return prev;
}

// ---------------------------------------------------------------- binary tree

interface TreeNode<T> {
  value: T;
  left: TreeNode<T> | null;
  right: TreeNode<T> | null;
}

const node = <T>(value: T, left: TreeNode<T> | null = null, right: TreeNode<T> | null = null): TreeNode<T> =>
  ({ value, left, right });

function* inorder<T>(root: TreeNode<T> | null): Generator<T> {
  if (!root) return;
  yield* inorder(root.left);
  yield root.value;
  yield* inorder(root.right);
}

function levelOrder<T>(root: TreeNode<T> | null): T[][] {
  const levels: T[][] = [];
  let current = root ? [root] : [];
  while (current.length) {
    levels.push(current.map((n) => n.value));
    current = current.flatMap((n) => [n.left, n.right].filter((c): c is TreeNode<T> => c !== null));
  }
  return levels;
}

const maxDepth = <T>(root: TreeNode<T> | null): number =>
  root ? 1 + Math.max(maxDepth(root.left), maxDepth(root.right)) : 0;

// ---------------------------------------------------------------- generic algorithms

type Comparator<T> = (a: T, b: T) => number;

function binarySearch<T>(items: readonly T[], target: T, compare: Comparator<T>): number {
  let lo = 0;
  let hi = items.length - 1;
  while (lo <= hi) {
    const mid = (lo + hi) >> 1;
    const result = compare(items[mid] as T, target);
    if (result === 0) return mid;
    if (result < 0) lo = mid + 1;
    else hi = mid - 1;
  }
  return -1;
}

function mergeSort<T>(items: readonly T[], compare: Comparator<T>): T[] {
  if (items.length <= 1) return [...items];
  const mid = items.length >> 1;
  const left = mergeSort(items.slice(0, mid), compare);
  const right = mergeSort(items.slice(mid), compare);
  const merged: T[] = [];
  let i = 0;
  let j = 0;
  while (i < left.length && j < right.length) {
    merged.push(compare(left[i] as T, right[j] as T) <= 0 ? (left[i++] as T) : (right[j++] as T));
  }
  return [...merged, ...left.slice(i), ...right.slice(j)];
}

/** Typed priority queue (binary heap). */
class MinHeap<T> {
  readonly #items: T[] = [];
  readonly #compare: Comparator<T>;

  constructor(compare: Comparator<T>) {
    this.#compare = compare;
  }

  push(item: T): this {
    this.#items.push(item);
    let i = this.#items.length - 1;
    while (i > 0) {
      const parent = (i - 1) >> 1;
      if (this.#compare(this.#items[i] as T, this.#items[parent] as T) >= 0) break;
      this.#swap(i, parent);
      i = parent;
    }
    return this;
  }

  pop(): T | undefined {
    if (!this.#items.length) return undefined;
    const top = this.#items[0] as T;
    const last = this.#items.pop() as T;
    if (this.#items.length) {
      this.#items[0] = last;
      let i = 0;
      for (;;) {
        const [left, right] = [2 * i + 1, 2 * i + 2];
        let smallest = i;
        if (left < this.#items.length && this.#compare(this.#items[left] as T, this.#items[smallest] as T) < 0) smallest = left;
        if (right < this.#items.length && this.#compare(this.#items[right] as T, this.#items[smallest] as T) < 0) smallest = right;
        if (smallest === i) break;
        this.#swap(i, smallest);
        i = smallest;
      }
    }
    return top;
  }

  get size(): number {
    return this.#items.length;
  }

  #swap(a: number, b: number): void {
    [this.#items[a], this.#items[b]] = [this.#items[b] as T, this.#items[a] as T];
  }
}

interface Task {
  name: string;
  priority: number;
}

const stack = new Stack<number>().push(1).push(2).push(3);
check("Stack pop/peek/iterate", [stack.pop(), stack.peek(), [...stack], stack.size], [3, 2, [2, 1], 2]);

const queue = new Queue<string>().enqueue("a").enqueue("b");
check("Queue FIFO", [queue.dequeue(), queue.size, queue.dequeue(), queue.dequeue()], ["a", 1, "b", undefined]);

const cache = new LRUCache<string, number>(2).set("a", 1).set("b", 2);
cache.get("a");
cache.set("c", 3);
check("LRUCache<K, V>", [cache.keys, cache.get("b")], [["a", "c"], undefined]);

const list = toLinkedList([1, 2, 3, 4]);
check("generic linked list reverse", toArray(reverseList(list)), [4, 3, 2, 1]);

const tree = node(3, node(9), node(20, node(15), node(7)));
check("tree inorder generator", [...inorder(tree)], [9, 3, 15, 20, 7]);
check("tree levelOrder + depth", [levelOrder(tree), maxDepth(tree)], [[[3], [9, 20], [15, 7]], 3]);

const byNumber: Comparator<number> = (a, b) => a - b;
check("generic binarySearch", [binarySearch([1, 3, 5, 7], 5, byNumber), binarySearch([1, 3, 5, 7], 4, byNumber)], [2, -1]);
check("generic mergeSort", mergeSort([5, 2, 9, 1], byNumber), [1, 2, 5, 9]);
check("mergeSort with objects",
      mergeSort<Task>([{ name: "b", priority: 2 }, { name: "a", priority: 1 }], (x, y) => x.priority - y.priority)
        .map((t) => t.name),
      ["a", "b"]);

const heap = new MinHeap<Task>((a, b) => a.priority - b.priority);
heap.push({ name: "ship", priority: 3 }).push({ name: "plan", priority: 1 }).push({ name: "write", priority: 2 });
check("typed MinHeap order", [heap.pop()?.name, heap.pop()?.name, heap.pop()?.name, heap.pop()],
      ["plan", "write", "ship", undefined]);

console.log("\nall checks passed");

export {};
