# Java Interview Code Tests

Java 17+. Every file has its own `main` and runs directly, with no build step:

```
java java/src/strings/LongestPalindrome.java
```

Run all of them: `for f in java/src/*/*.java; do echo "== $f"; java "$f"; done`

## Tricks to remember

| Problem | File | Trick |
|---|---|---|
| Longest common prefix | strings/LongestCommonPrefix | Sort, then compare only the first and last words |
| Shortest unique prefix | strings/LongestCommonPrefix | Count every prefix; use the first one whose count == 1 |
| Longest palindrome | strings/LongestPalindrome | Expand around each center (odd `c,c` and even `c,c+1`) |
| Valid parentheses | strings/ValidParentheses | Push the closing partner; on a closing bracket, pop and compare |
| First non-repeating char | strings/StringBasics | groupingBy into a LinkedHashMap with counting, then the first entry with count 1 |
| Anagram / group anagrams | strings/StringBasics | Sort the chars and use them as the key |
| Longest substring without repeats | strings/StringBasics | Sliding window plus a map of each char's last index |
| Median | arrays/Median | Sort; odd length takes the middle, even averages the two middles |
| Running median | arrays/Median | Two heaps: a max-heap for the low half, a min-heap for the high half |
| Two sum | arrays/ArrayProblems | Map of value to index; look up `target - x` |
| Kth largest | arrays/ArrayProblems | Min-heap of size k |
| Max subarray (Kadane) | arrays/ArrayProblems | `cur = max(x, cur + x)` |
| Stock buy/sell | arrays/ArrayProblems | Track the min so far; profit = price - min |
| Merge intervals | arrays/ArrayProblems | Sort by start; extend the last interval or add a new one |
| Rotate array | arrays/ArrayProblems | Reverse all, reverse the first k, reverse the rest |
| Missing number | arrays/ArrayProblems | `n(n+1)/2 - sum` |
| Highest salary by dept | collections/EmployeeQueries | `groupingBy(dept, maxBy(comparing(salary)))` |
| Nth highest salary | collections/EmployeeQueries | `distinct, sorted desc, skip(n-1), findFirst` |
| Frequent words | collections/FrequentWords | Split, group and count, sort by value desc, limit k |
| Duplicates | collections/Duplicates | `filter(x -> !seen.add(x))` |
| Stream one-liners | collections/StreamsCheatSheet | partitioningBy, flatMap, joining, toMap, sort a map by value |
| Producer/consumer | concurrency/ProducerConsumer | BlockingQueue, AtomicInteger counters, one poison pill per consumer |
| Producer/consumer (wait/notify) | concurrency/ProducerConsumerWaitNotify | `while (cond) wait();` then `notifyAll()` |
| Race condition / odd-even / CompletableFuture | concurrency/ThreadBasics | `count++` is read-add-write, so it is not atomic |
| Thread lifecycle, interrupt, daemon, deadlock, ThreadLocal | concurrency/ThreadLifecycle | `run()` runs on the current thread, only `start()` spawns; catching InterruptedException clears the flag, so restore it |
| CompletableFuture: compose, allOf, timeouts, error handling | concurrency/CompletableFutureExamples | `thenApply`=map, `thenCompose`=flatMap, `thenCombine`=zip; the default pool is the commonPool, so pass your own for I/O |
| Semaphore: rate limit, bounded pool, signalling | concurrency/SemaphoreExamples | Permits have no owner, so another thread can release; always `release()` in a finally |
| CountDownLatch / CyclicBarrier / Phaser | concurrency/LatchAndBarrier | Latch = gate that opens once (and a `CountDownLatch(1)` is a start gun); barrier = meeting point that resets each round |
| Run for 10 minutes, then cancel | concurrency/TimedCancellation | Five ways to stop a thread, all cooperative: `cancel(true)` only sets the interrupt flag, it kills nothing |
| LRU cache | design/LRUCache | `LinkedHashMap(cap, 0.75f, true)` plus `removeEldestEntry` |
| Singleton / immutable / equals-hashCode | design/DesignBasics | Enum singleton; final class with final fields and defensive copies |
| GoF creational (singleton, factory, abstract factory, builder, prototype) | designpatterns/CreationalPatterns | Builder returns `this` from each setter; enum singleton; prototype = deep copy |
| GoF structural (adapter, bridge, composite, decorator, facade, flyweight, proxy) | designpatterns/StructuralPatterns | Decorator ADDS behaviour, proxy CONTROLS access - same shape, different intent |
| GoF behavioral (chain, command, interpreter, iterator, mediator, memento, observer, state, strategy, template, visitor) | designpatterns/BehavioralPatterns | Strategy = caller picks, state = object picks and swaps itself, command = the call as an object |
| FizzBuzz, Fibonacci, primes, reverse linked list... | algorithms/ClassicAlgorithms | prev/curr/next; slow and fast pointers |
| **Sliding window template** | slidingwindow/SlidingWindow | `for right: add; while (invalid) remove left++; update best` |
| Max sum of k in a row | slidingwindow/SlidingWindow | Fixed window: add `a[i]`, subtract `a[i-k]` |
| Smallest subarray with sum >= target | slidingwindow/SlidingWindow | Shrink while the window is still valid |
| At most K distinct / repeating replacement / max ones | slidingwindow/SlidingWindow | Shrink while count map size > k, `len - maxFreq > k`, or zeros > k |
| Find anagrams / permutation in string | slidingwindow/SlidingWindow | Fixed window; compare two `int[26]` arrays |
| Minimum window substring | slidingwindow/SlidingWindow | Need-map plus a `missing` counter; shrink while missing == 0 |
| Sliding window maximum | slidingwindow/SlidingWindow | Deque of indexes with decreasing values; the front is the max |
| Subarray sum = k (negatives) | slidingwindow/SlidingWindow | Prefix sum; look up `sum - k` in a map |
| BFS / DFS / shortest path | graphs/Graphs | BFS = queue, marking visited when adding; DFS = recursion |
| Number of islands | graphs/Graphs | Count cells that start a flood fill; "sink" visited cells |
| Rotting oranges | graphs/Graphs | Multi-source BFS: one level = one minute |
| Topological sort / course schedule | graphs/Graphs | Kahn: start from in-degree 0; leftover nodes mean a cycle |
| Dijkstra | graphs/Graphs | PriorityQueue by distance; skip stale entries |
| Union-Find | graphs/Graphs | `find` with path compression; `union` returns false on a cycle |
| Bipartite | graphs/Graphs | BFS coloring with opposite colors |
| Traversals | trees/BinaryTrees | Pre/in/post = where the root goes; iterative in-order: go left, pop, go right |
| Level order / zigzag / right view | trees/BinaryTrees | Queue; loop `q.size()` times per level |
| Depth, invert, same, symmetric, balanced | trees/BinaryTrees | Solve left, solve right, combine at the root |
| Diameter | trees/BinaryTrees | Return height, update a global best with `l + r` |
| LCA | trees/BinaryTrees | Found on both sides means this node is the LCA; BST: the split point |
| Validate BST / kth smallest | trees/BinaryTrees | Pass min/max bounds down; BST in-order is sorted |
| Trie / autocomplete / unique prefix | trees/Trie | Map of children + isWord + count; walk the prefix |
| **Backtracking template** | backtracking/Backtracking | Choose, explore, un-choose; add a *copy* of the path |
| Subsets / combinations | backtracking/Backtracking | Loop from `start`, recurse with `i + 1`; bitmask version with streams |
| Subsets with duplicates | backtracking/Backtracking | Sort, then skip when `i > start && a[i] == a[i-1]` |
| Permutations | backtracking/Backtracking | Loop from 0 with a `used[]` array; string version: each char first + permutations of the rest |
| Combination sum | backtracking/Backtracking | Recurse with `i` to allow reuse; sort and break early |
| Phone letters / generate parentheses | backtracking/Backtracking | One digit per level; add `(` if open < n, `)` if close < open |
| N-Queens | backtracking/Backtracking | One queen per row; sets for columns, `r-c`, `r+c` |
| Word search / Sudoku / palindrome partition | backtracking/Backtracking | Mark cell `#` then restore; try 1-9 and undo; cut palindrome prefixes |
| **DP in 4 steps** | dp/DynamicProgramming | State, choice (recurrence), base case, answer |
| Fibonacci memo / stairs / house robber / decode ways | dp/DynamicProgramming | Only needs the last 2 values, so keep 2 variables |
| Coin change (min) / coin change (ways) | dp/DynamicProgramming | `dp[a] = min(dp[a-coin] + 1)`; for ways, loop coins in the outer loop |
| LIS | dp/DynamicProgramming | `dp[i] = 1 + max(dp[j])` for `a[j] < a[i]`; fast version: tails + binary search |
| LCS / edit distance / longest palindromic subsequence | dp/DynamicProgramming | `(m+1) x (n+1)` table: match = diagonal; else max/min of neighbours |
| 0/1 knapsack / partition equal subset | dp/DynamicProgramming | 1D array, loop capacity backwards |
| Unique paths / min path sum | dp/DynamicProgramming | From top + from left |
| Word break | dp/DynamicProgramming | `dp[i] = any j: dp[j] && word(j..i)` |
| Max product subarray | dp/DynamicProgramming | Track max and min; swap them when x < 0 |
| **Heap rules** | heaps/Heaps | Min-heap by default; K largest = min-heap of size k; K smallest/closest = max-heap of size k |
| Kth largest in stream / top K frequent / K closest | heaps/Heaps | Offer, then poll when size > k |
| Merge K sorted / kth smallest in matrix | heaps/Heaps | Heap holds `{value, source, index}`; push the next item from the same source |
| Meeting rooms II | heaps/Heaps | Sort by start; min-heap of end times |
| Last stone / connect ropes | heaps/Heaps | Poll the two top items, push the result |
| K-sorted array / reorganize string / task scheduler | heaps/Heaps | Heap of size k+1; hold back the last used char; `(max-1)*(n+1) + countMax` |
| **Linked list rules** | linkedlist/LinkedLists | Dummy node; reverse = save next, point back, move both |
| Middle / cycle / cycle start | linkedlist/LinkedLists | Slow and fast pointers; after they meet, restart one from head |
| Merge two / merge K / sort list | linkedlist/LinkedLists | Dummy + tail; heap of heads; split at middle and merge |
| Remove nth from end | linkedlist/LinkedLists | Move fast n+1 steps ahead, then move both |
| Palindrome / reorder | linkedlist/LinkedLists | Middle, reverse second half, compare or weave |
| Intersection | linkedlist/LinkedLists | At the end, jump to the other list's head |
| Add two numbers / rotate / k-group / swap pairs | linkedlist/LinkedLists | Carry loop; make a circle and cut; check k nodes then reverse recursively |
| **Binary search templates** | binarysearch/BinarySearch | Exact: `lo <= hi`. First true: `lo < hi; ok ? hi = mid : lo = mid + 1` |
| Insert position / first & last / count | binarysearch/BinarySearch | `lowerBound(t)` and `lowerBound(t+1) - 1` |
| Rotated array search / min | binarysearch/BinarySearch | One half is always sorted; compare mid with the right end |
| Peak / sqrt / 2D matrix | binarysearch/BinarySearch | Go uphill; long mid*mid; index -> `[i/cols][i%cols]`; matrix II starts top-right |
| Koko / ship packages / split array | binarysearch/BinarySearch | Binary search on the answer: first x where `feasible(x)` |
| Kth missing / single non-duplicate | binarysearch/BinarySearch | Missing count = `a[i] - (i+1)`; pairs start at even indexes |
| **Money** | banking/BalanceAmount | Use BigDecimal from a String, never double; compare with `compareTo`/`signum`, not `equals` |
| Balance per account / total / as of date / monthly | banking/BalanceAmount | `groupingBy(account, reducing(ZERO, Txn::signed, BigDecimal::add))` |
| Running balance / overdrawn / richest account | banking/BalanceAmount | Plain loop for running totals; filter `signum() < 0`; max by value |
| Minimum starting balance | banking/BalanceAmount | Prefix sum; answer = -(lowest point) |
| Thread-safe deposit/withdraw/transfer | banking/BalanceAmount | Lock both accounts in id order (no deadlock); total before == total after |
| Split expenses / settle up | banking/BalanceAmount | Net = paid - share; two max-heaps, pay the smaller amount, push back the rest |
| **Stack rules** | stacks/Stacks | `Deque` + `ArrayDeque` (not `Stack`); nesting = push state on `(`; monotonic stack for next greater |
| Min stack | stacks/Stacks | Each entry stores `{value, min so far}` |
| RPN / calculator / decode string | stacks/Stacks | Operator: pop b, pop a; on `(` push result and sign; on `]` prev + cur.repeat(k) |
| Simplify path / backspace / adjacent duplicates | stacks/Stacks | Stack of parts; StringBuilder as a stack |
| Next greater / daily temps / stock span | stacks/Stacks | While `top < current`: answer for top, pop; store indexes |
| Largest rectangle / rain water | stacks/Stacks | Popped bar's width = `i - newTop - 1`; two pointers, move the lower side |
| Remove K digits / asteroids | stacks/Stacks | Pop bigger digits while k > 0; only `+` on stack vs incoming `-` collide |
| **Queue rules** | queues/Queues | `offer`/`poll`/`peek`; old items leave from the front |
| Queue via 2 stacks / stack via queue | queues/Queues | Pour in to out when out is empty; rotate size-1 after push |
| Circular queue | queues/Queues | Array + head + size; `(head + i) % capacity` |
| Moving average / recent counter | queues/Queues | Offer new, poll expired, keep a running sum |
| First unique in stream / first negative in window | queues/Queues | Queue of candidates; drop invalid ones from the front |
| Binary numbers / reverse first K / Josephus | queues/Queues | Poll s, offer s+"0" and s+"1"; stack for k; rotate k-1, remove |
| **Matrix rules** | matrix/Matrix | `DIRS` array + bounds check; r+c and r-c mark diagonals |
| Rotate / transpose | matrix/Matrix | Rotate = transpose + reverse rows |
| Spiral order / generate spiral | matrix/Matrix | top/bottom/left/right boundaries; shrink after each side |
| Set zeroes / game of life | matrix/Matrix | Record first, write after (or use a copy) |
| Diagonal sum / zigzag traverse | matrix/Matrix | Group cells by `r + c`; reverse even diagonals |
| Valid Sudoku | matrix/Matrix | One Set: `"5 in row 0"`, `"5 in col 3"`, `"5 in box 0-1"` |
| Flood fill / nearest zero | matrix/Matrix | DFS over same color; multi-source BFS from all zeros |
| Maximal square / multiply / Pascal | matrix/Matrix | `1 + min(top, left, diag)`; row times column; sum of the two above |
| **Math rules** | math/MathProblems | Digits: `n % 10`, `n / 10`, `rev * 10 + d`; loop `i * i <= n` for divisors |
| Sieve / prime factors | math/MathProblems | Cross out multiples from `i*i`; divide out each p while it divides |
| GCD / LCM (arrays too) | math/MathProblems | `gcd(b, a % b)`; `lcm = a / gcd * b`; `reduce` over the array |
| Fast power / mod power | math/MathProblems | Odd exponent: multiply result; square base; halve exponent |
| Palindrome number / reverse with overflow | math/MathProblems | Reverse only half the digits; build in `long`, check int range |
| Perfect number / trailing zeros of n! | math/MathProblems | Add divisor pairs `i` and `n/i`; count factors of 5 |
| Excel column / Roman numerals | math/MathProblems | `n--` before `% 26`; subtract a symbol smaller than the next; greedy over 13 values |
| Add binary / multiply strings / plus one | math/MathProblems | Walk from the end with a carry; digit i*j goes to `[i+j, i+j+1]` |
| Happy number / digital root | math/MathProblems | Set of seen values; `1 + (n-1) % 9` |
| Fraction to recurring decimal | math/MathProblems | Long division; a repeated remainder means the digits repeat |
| nCr / nth ugly / base conversion / gcd of strings | math/MathProblems | `res * (n-k+i) / i`; 3 pointers; `Character.forDigit`; `a+b == b+a` |
| Leap year / clock angle / rectangle overlap | math/MathProblems | `%4 && (!%100 or %400)`; `30h + 0.5m` vs `6m`; overlap on both axes |
| Pythagorean triplets / shuffle / BigInteger | math/MathProblems | flatMap over (a, b); Fisher-Yates swaps i with `[0, i]` |
| Bit tricks | math/MathProblems | XOR cancels pairs; `n & (n-1)` drops the lowest bit; `bits[i] = bits[i>>1] + (i&1)` |

## SQL

The `db/` folder has 170 SQL interview questions (PostgreSQL), organized by topic, with a shared schema and sample data. See [db/README.md](db/README.md).

## Microservices

The `microservices/` folder has 15 microservice design patterns as Spring Boot 3 / Spring Cloud samples, one folder each: API gateway, service discovery, config server, Feign + client-side load balancing, Resilience4j (circuit breaker, retry, bulkhead, rate limiter, time limiter), saga (both orchestration and choreography), CQRS, event sourcing, transactional outbox, idempotency, observability and tracing, BFF/API composition, JWT security, and strangler fig. Every file leads with problem, solution and the trick to remember. See [microservices/README.md](microservices/README.md).

## Design Patterns (GoF)

The `java/src/designpatterns/` folder has all 23 Gang of Four patterns in three runnable files (creational, structural, behavioral), each with a one-line memory hook, the real JDK example, and the pairs that get confused in interviews. See [java/src/designpatterns/README.md](java/src/designpatterns/README.md).

## System Design

The `system-design/` folder has the interview framework, estimation numbers, building blocks, 15 classic designs (URL shortener, rate limiter, news feed, chat, payments, ticket booking…), runnable Java low-level design pieces and a cheat sheet. See [system-design/README.md](system-design/README.md).

## Python

The `python/` folder has the same problem set in Python (standard library only), organized by topic, plus Python-specific questions: decorators, generators, dataclasses, the GIL, asyncio. Each file checks itself with asserts. See [python/README.md](python/README.md).

## JavaScript & TypeScript

The `javascript/` folder covers language fundamentals, `this`/closures, the event loop and async patterns, array/object recipes, algorithms, and the utilities interviewers ask you to write from scratch (debounce, deepClone, EventEmitter, a Promise). The `typescript/` folder adds the type system: generics, utility types (including implementing them yourself), discriminated unions, typed patterns and generic data structures — all type-checked under `strict`. See [javascript/README.md](javascript/README.md) and [typescript/README.md](typescript/README.md).

## React

The `react/` folder covers React 19 + TypeScript: fundamentals and hooks with their traps, custom hooks, component patterns (compound components, error boundaries), performance, forms and data fetching including React 19 actions and `useOptimistic`, plus a runnable pure-logic file and a Q&A/testing guide. Components are type-checked against React 19 types and server-rendered as a smoke test. See [react/README.md](react/README.md).

## Angular

The `angular/` folder covers Angular 21: standalone components and signals, change detection (Zone.js vs zoneless, OnPush), directives and pipes, DI and HttpClient with functional interceptors, RxJS operator patterns, typed reactive forms, routing with functional guards and lazy loading, and a signal store — plus runnable logic (a signal implementation and the flattening operators, verified against real RxJS) and a Q&A/testing guide. Everything compiles under the Angular compiler with `strictTemplates`. See [angular/README.md](angular/README.md).

## Node.js (backend)

The `nodejs/` folder covers backend Node with the standard library only: the event loop measured rather than described, async patterns (concurrency limits, retries, cancellation), streams and backpressure, a REST API on `node:http` that tests itself over a real socket, auth/crypto (including a JWT built and then attacked), data patterns (pooling, N+1, caching, idempotency, outbox), worker threads and scaling, plus a Q&A and testing guide. See [nodejs/README.md](nodejs/README.md).
"# workspace" 
