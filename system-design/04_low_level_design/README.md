# Low-Level Design (LLD / Object-Oriented Design)

LLD rounds ask you to **write classes**: correct objects, clean responsibilities and extensibility, sometimes with concurrency. High-level design rounds are about boxes and arrows instead.

## Runnable building blocks (Java 17, no build step)

```bash
java system-design/04_low_level_design/RateLimiters.java
```

| File | What it shows | Used in design |
|---|---|---|
| `RateLimiters.java` | Token bucket, fixed window, sliding log, sliding counter + the **boundary burst** demo | Rate limiter, API gateway |
| `ConsistentHashing.java` | Ring + virtual nodes; ~1/N keys move vs 75% with `% N`; replicas | KV store, caches, sharding |
| `UrlShortener.java` | Base62, collision-free scrambled ids, custom alias | URL shortener |
| `SnowflakeIdGenerator.java` | 64-bit time-sortable unique ids across threads/machines | Chat, feed, any sharded DB |
| `BloomFilter.java` | Sizing formulas, ~1% false positives, 0 false negatives | Web crawler, LSM trees, cache penetration |
| `CircuitBreaker.java` | CLOSED → OPEN → HALF_OPEN with fail-fast fallback | Any service-to-service call |
| `ParkingLot.java` | Classic OOD: enums, records, Strategy pricing, thread-safe park/unpark | "Design a parking lot" |

Also in this repo:
- `java/src/design/LRUCache.java` (LRU cache)
- `java/src/concurrency/ProducerConsumer.java` (bounded queue)
- `java/src/banking/BalanceAmount.java` (thread-safe transfers, Splitwise)
- `java/src/trees/Trie.java` (autocomplete)

## How to answer an LLD question (5 steps)

1. **Clarify requirements and scope:** list the use cases ("park, unpark, pay, show availability").
2. **Identify entities:** nouns become classes/enums, verbs become methods. Draw the relationships (has-a, is-a).
3. **Define interfaces first** for things that vary (pricing, payment, notification channel, matching strategy).
4. **Write the core code:** the main flow end to end, with validation and edge cases.
5. **Discuss extensibility and concurrency:** new vehicle types, new pricing, two threads taking the same spot, persistence.

## Popular LLD questions: key classes and patterns

| Question | Key classes | Patterns / tricks |
|---|---|---|
| **Parking lot** | ParkingLot, Level, ParkingSpot, Vehicle, Ticket, PricingStrategy | Strategy (pricing), smallest-fit spot, synchronized allocation |
| **LRU / LFU cache** | Cache<K,V>, Node, DoublyLinkedList | HashMap + doubly linked list (LRU); frequency buckets (LFU) |
| **Rate limiter** | RateLimiter interface, TokenBucket, SlidingWindow | Strategy per algorithm; Redis for distributed state |
| **Elevator system** | ElevatorController, Elevator, Request, Direction, State | State pattern (idle/moving up/down); SCAN/LOOK scheduling |
| **Vending machine / ATM** | Machine, State (Idle, HasMoney, Dispensing), Inventory, Coin | **State pattern**; change-making with greedy/DP |
| **Movie ticket booking** | Show, Screen, Seat, Booking, SeatLock | Seat lock with timeout (see `03_classic_designs/12_ticket_booking.md`) |
| **Splitwise / expense sharing** | User, Group, Expense, Split (Equal/Exact/Percent), BalanceSheet | Strategy for split types; greedy settle-up with heaps |
| **Library management** | Book, BookCopy, Member, Loan, Fine | Observer (notify when available); fine strategy |
| **Chess / Tic-tac-toe** | Game, Board, Player, Piece (subclasses), Move | Polymorphic `canMove`; command pattern for undo |
| **Snake & ladder** | Game, Board, Dice, Player, Jump(snake/ladder) | Map of jumps; queue of players |
| **Logger framework** | Logger, Handler/Appender, Formatter, Level | **Chain of responsibility**, Singleton, async queue |
| **Notification service** | Notifier interface, EmailNotifier, SmsNotifier, Template | Strategy/Factory per channel; Observer/pub-sub |
| **Pub/Sub message broker** | Topic, Publisher, Subscriber, Broker, Offset | Observer; per-subscriber offsets; thread-safe queues |
| **Task scheduler** | Task, Scheduler, Worker, PriorityQueue by time | DelayQueue / ScheduledExecutorService |
| **Food delivery / ride matching** | Order/Ride, Driver, MatchingStrategy, State | State machine + Strategy |
| **In-memory key-value store with TTL** | Store, Entry(value, expiresAt), Cleaner | Lazy expiry on read + periodic cleanup |

## SOLID in one line each

| Principle | One-liner | Smell when violated |
|---|---|---|
| **S**ingle responsibility | A class has one reason to change | `OrderService` also sends emails and builds PDFs |
| **O**pen/closed | Add behaviour by adding code, not editing code | `if (type == ...) else if ...` growing forever |
| **L**iskov substitution | Subtypes must work wherever the parent works | `Square extends Rectangle` breaks `setWidth` |
| **I**nterface segregation | Many small interfaces beat one fat one | Classes implementing methods with `throw new UnsupportedOperationException()` |
| **D**ependency inversion | Depend on interfaces; inject implementations | `new MySqlRepository()` inside business logic |

## Design patterns to recognize quickly

| Pattern | Use when | Java example |
|---|---|---|
| **Strategy** | Swap algorithms at runtime | `Comparator`, pricing, split types |
| **State** | Behaviour depends on state; many `if (state == ...)` | Vending machine, order lifecycle |
| **Observer** | Notify many listeners of changes | Event listeners, pub/sub |
| **Factory** | Hide which subclass is created | `NotificationFactory.create(channel)` |
| **Builder** | Many optional constructor params | `HttpRequest.newBuilder()`, Lombok `@Builder` |
| **Singleton** | Exactly one instance (use sparingly) | Enum singleton (see `java/src/design/DesignBasics.java`) |
| **Decorator** | Add behaviour by wrapping | `BufferedInputStream(new FileInputStream(..))` |
| **Adapter** | Make an incompatible interface fit | PSP adapter over Stripe/Adyen SDKs |
| **Chain of responsibility** | Pass a request along handlers | Servlet filters, logging levels, approval chains |
| **Command** | Encapsulate an action (undo/redo, queues) | `Runnable` tasks, editor undo stack |
| **Template method** | Fixed algorithm skeleton, variable steps | `AbstractList`, JUnit lifecycle |
| **Proxy** | Control access (lazy, remote, caching) | Spring AOP proxies, Hibernate lazy loading |
