# Python Interview Coding Samples

Runnable Python 3.10+ files, organized by topic. **Standard library only** - nothing to install.

Every file checks its own answers with `assert` and prints what it computed, so you can see the expected result next to the code.

```bash
python python/02_strings/string_problems.py     # run one topic
python python/run_all.py                        # run everything, PASS/FAIL per file
python python/run_all.py trees -v               # filter by path, show output
```

## Folders

| Folder | File | What's inside |
|---|---|---|
| `01_python_basics/` | `language_fundamentals.py` | Mutable default args, `is` vs `==`, shallow/deep copy, late-binding closures, `*args/**kwargs`, unpacking, comprehensions, truthiness, float/rounding traps, f-strings, walrus, `match` |
| | `functions_decorators_generators.py` | Decorators (with args, class-based, `functools.wraps`), memoization, generators, `yield from`, `send()`, iterator protocol, context managers |
| | `oop_dataclasses.py` | Dunder methods, `__slots__`, dataclasses (frozen/order), property, class/static methods, MRO & `super()`, ABCs, Enum, singleton, shared-class-attribute bug |
| | `pythonic_collections_itertools.py` | Counter, defaultdict, deque, OrderedDict, namedtuple, ChainMap, heapq, bisect, itertools, functools, operator |
| `02_strings/` | `string_problems.py` | Reverse, palindrome, anagrams, first unique char, common prefix, longest palindrome, valid parentheses, longest unique substring, compression, word frequencies |
| `03_arrays_hashing/` | `array_problems.py` | Two sum, duplicates, missing number, Kadane, stock profit, product except self, merge intervals, top-K, majority, longest consecutive, prefix sums, matrix rotate/spiral |
| `04_two_pointers_sliding_window/` | `two_pointers_sliding_window.py` | Two sum sorted, 3-sum, container with most water, rain water, and the full sliding-window family (min window, anagrams, window max) |
| `05_linked_lists/` | `linked_lists.py` | Reverse, middle, cycle start, merge two/K, nth from end, palindrome, add numbers, reorder, rotate, reverse in k-groups, merge sort |
| `06_stacks_queues/` | `stacks_queues.py` | Min stack, queue from stacks, circular queue, moving average, RPN, calculator, decode string, monotonic stack problems, histogram |
| `07_trees_tries/` | `binary_trees.py` | Traversals (generators + iterative), level order, depth, invert, symmetric, balanced, diameter, LCA, validate BST, kth smallest, serialize/deserialize |
| | `trie.py` | Insert/search/prefix count, autocomplete, delete, shortest unique prefix, word search II |
| `08_graphs/` | `graphs.py` | BFS/DFS, shortest path, islands, rotting oranges, topological sort, Dijkstra, union-find, bipartite, word ladder, clone graph |
| `09_heaps/` | `heaps.py` | Kth largest (+ stream), top-K frequent, K closest, merge K sorted, meeting rooms, ropes, median finder (two heaps), reorganize string |
| `10_binary_search/` | `binary_search.py` | Both templates, bisect module, first/last position, rotated array, peak, sqrt, 2D matrix, binary search on the answer (Koko, shipping) |
| `11_backtracking/` | `backtracking.py` | Subsets, permutations, combinations, combination sum, phone letters, parentheses, N-Queens, word search, palindrome partition, Sudoku |
| `12_dynamic_programming/` | `dynamic_programming.py` | `@cache` memoization, stairs, house robber, coin change, LIS, LCS, edit distance, knapsack, partition, grid paths, word break, decode ways |
| `13_math_bits/` | `math_and_bits.py` | Sieve, prime factors, gcd/lcm, modular pow, palindrome/reverse number, Excel columns, Roman numerals, string multiplication, happy number, bit tricks |
| `14_sorting/` | `sorting.py` | Bubble/insertion/selection/merge/quick/counting sort, quickselect, Dutch flag, largest number (custom comparator), sorting with keys |
| `15_concurrency/` | `threads_processes_asyncio.py` | GIL explained, race condition + Lock, producer/consumer with `queue.Queue`, thread vs process pools, deadlock-free transfers, asyncio (gather, semaphore, timeout, queue) |
| `16_data_processing/` | `data_processing.py` | Employee/salary aggregations, word frequencies, CSV/JSON, log parsing with regex, pivot, dedupe, running totals, lazy file reading |
| `17_design/` | `design_problems.py` | LRU cache, TTL cache, token-bucket rate limiter, hit counter, TimeMap, event emitter, thread-safe account, peeking/nested iterators, consistent hashing |

## Python-specific things interviewers check

| Question | Short answer |
|---|---|
| Why is a mutable default argument a bug? | Defaults are evaluated once at `def` time, so the same list is reused across calls. |
| `is` vs `==`? | Identity vs value. Use `is` only for `None`, `True`, `False`. |
| Shallow vs deep copy? | `copy.copy` shares inner objects; `copy.deepcopy` copies everything. |
| List vs tuple vs set vs dict? | Mutable sequence / immutable (hashable) sequence / unique unordered / key-value; `in` is O(n) for lists, O(1) for sets and dicts. |
| What does the GIL mean for me? | Threads for I/O, processes for CPU. See `15_concurrency`. |
| Generator vs list? | Generators are lazy and memory-light, and can be consumed only once. |
| Decorator? | A function that wraps another; use `functools.wraps` to keep the metadata. |
| `@staticmethod` vs `@classmethod`? | No implicit first arg vs receives `cls` (used for alternative constructors). |
| `__str__` vs `__repr__`? | For users vs for developers; `repr` should be unambiguous. |
| How do you make an object hashable? | Define `__eq__` **and** `__hash__` (or use a frozen dataclass / tuple). |
| `*args` / `**kwargs`? | Collect extra positional / keyword arguments; also used for unpacking at call sites. |
| List comprehension vs `map`/`filter`? | Comprehensions are usually clearer; `map`/`filter` return lazy iterators. |
| How do you handle money? | `decimal.Decimal` or integer cents, never `float`. |
| How do you test this? | `assert` in small scripts, `pytest` in real projects; inject clocks instead of sleeping. |

## Complexity cheat sheet

| Operation | list | deque | dict / set |
|---|---|---|---|
| index / key lookup | O(1) | O(1) ends | O(1) average |
| append / add | O(1) amortized | O(1) both ends | O(1) |
| insert / pop at front | **O(n)** | O(1) | - |
| `x in container` | **O(n)** | O(n) | **O(1)** |
| sort | O(n log n) Timsort, stable | - | - |

Related folders in this repo: `java/` (the same problems in Java), `db/` (SQL), `system-design/`.
