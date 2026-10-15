# Core Concepts (the vocabulary of every system design answer)

## Scalability

| Term | Meaning | Remember |
|---|---|---|
| **Vertical scaling** (scale up) | Bigger machine | Simple, has a ceiling, still a single point of failure |
| **Horizontal scaling** (scale out) | More machines | Needs **stateless** services + load balancer; data must be partitioned |
| **Stateless service** | No user state kept in server memory between requests | Put sessions in Redis/DB or in a signed token (JWT) → any server can serve any request |
| **SPOF** | Single point of failure | Remove with redundancy: 2+ of everything, across zones |

## Latency, throughput, percentiles

- **Latency** = time for one request. **Throughput** = requests handled per second.
- Measure **p50 / p95 / p99**, not the average. The average hides slow outliers, and at scale the p99 is what users notice.
- **Tail latency amplification:** a request that fans out to 100 servers is slowed by the slowest one. Fixes: hedged requests, timeouts, fewer fan-outs.

## CAP theorem

During a network **P**artition, a distributed data store must choose between:

- **C**onsistency: every read sees the latest write (or gets an error).
- **A**vailability: every request gets a non-error response (possibly stale).

| Choice | Behaviour during partition | Use for | Examples |
|---|---|---|---|
| **CP** | Refuse or time out some requests | Payments, inventory, bookings, locks | ZooKeeper, etcd, HBase, Spanner, single-primary SQL |
| **AP** | Answer with possibly stale data, reconcile later | Feeds, likes, carts, DNS, product catalog | Cassandra, DynamoDB (default), CouchDB |

> P is not optional in a distributed system. The real choice is C vs A **when** a partition happens.

**PACELC** extends CAP: if **P**artition, choose **A** or **C**. **E**lse (normal operation), choose **L**atency or **C**onsistency. Example: DynamoDB and Cassandra are PA/EL; Spanner is PC/EC.

## Consistency models (strongest → weakest)

| Model | Guarantee | Example need |
|---|---|---|
| **Strong / linearizable** | Reads always see the latest committed write | Bank balance, seat booking |
| **Read-your-writes** | *You* always see your own writes | "I updated my profile and it still shows the old name" must not happen |
| **Monotonic reads** | You never see data go back in time | Refreshing comments shouldn't hide a comment you saw |
| **Causal** | Cause is seen before effect | A reply appears after the message it answers |
| **Eventual** | Replicas converge if writes stop | Like counts, view counts, feeds |

## Replication & quorums

- **Leader-follower (primary-replica):** writes go to the leader, reads can use followers. Replication lag makes follower reads eventually consistent.
- **Multi-leader:** writes accepted in several regions, so conflicts need resolution (last-write-wins, CRDTs, app logic).
- **Leaderless (Dynamo-style):** a write goes to W of N replicas and a read queries R of N.
  - **R + W > N** gives overlapping quorums and strong-ish reads. Typical setting: N=3, W=2, R=2.
  - W=1 or R=1 gives fast, available, but possibly stale results.
- **Synchronous** replication is durable but slower. **Asynchronous** is fast but can lose the last writes on failover.

## Partitioning (sharding)

| Strategy | How | Good | Bad |
|---|---|---|---|
| **Hash** of key | `hash(user_id) % N` or consistent hashing | Even spread | Range queries hit every shard; resharding with `% N` moves most keys |
| **Range** | A–F, G–M… or by date | Range scans | Hot spots (new dates, popular prefixes) |
| **Directory / lookup** | Lookup service maps key → shard | Flexible moves | Extra hop, lookup service must be HA |
| **Geo** | By region | Data locality, compliance | Uneven load |

**Choosing a shard key:** high cardinality, even distribution, and present in most queries. **Hot key fixes:** add a random suffix to split the key, cache it, or give it a dedicated shard.

## Consensus & coordination

- **Raft / Paxos:** a majority (quorum) of nodes agrees on a log. This tolerates `(N-1)/2` failures, so use 3 nodes to tolerate 1 and 5 nodes to tolerate 2.
- Used for **leader election, distributed locks, config and service discovery**: etcd, ZooKeeper, Consul.
- **Distributed lock:** a lease with a TTL plus a **fencing token** (increasing number), so a paused old lock holder can't overwrite newer work.

## Delivery guarantees & idempotency

| Guarantee | Meaning | How |
|---|---|---|
| At-most-once | May lose, never duplicates | Fire and forget |
| **At-least-once** | Never loses, may duplicate | Retry until ack (the default everywhere) |
| Exactly-once *effect* | Each message processed once | **At-least-once + idempotent consumer** (dedupe by id) |

**Idempotent** means doing it twice has the same effect as doing it once. Achieve it with a client-generated **idempotency key**, unique constraints, upserts, or "processed message ids" tables.

## Communication styles

| Style | When |
|---|---|
| **Synchronous** (REST/gRPC) | The caller needs the answer now (read profile, authorize payment) |
| **Asynchronous** (queue/events) | Slow, bursty, or fan-out work (emails, video encoding, analytics); decouples services and absorbs spikes |
| **Short polling** | Simple, but wasteful |
| **Long polling** | Server holds the request until there's data; OK for infrequent updates |
| **Server-Sent Events** | Server → client one-way stream (live scores, notifications) |
| **WebSockets** | Two-way, low latency (chat, multiplayer, live location) |
| **Push vs pull** | Push is low latency but can overwhelm consumers; pull lets the consumer control the pace |

## Monolith vs microservices

| | Monolith | Microservices |
|---|---|---|
| Start with | ✅ small team, unclear domain | ❌ |
| Deploy | One unit | Independent per service |
| Scale | Whole app | Per service |
| Cost | Simple ops, in-process calls | Network calls, distributed tracing, data consistency across services (sagas), more infra |

> Good answer: *"Start as a modular monolith; extract services where scaling or team boundaries demand it."*

## Other terms you should be able to define in one sentence

- **Back-pressure:** slow producers down when consumers can't keep up (bounded queues, 429s).
- **Circuit breaker:** stop calling a failing dependency for a while and fail fast instead.
- **Bulkhead:** isolate resources (thread pools, connection pools) so one failure can't sink everything.
- **Graceful degradation:** serve a reduced experience (cached or default results) instead of an error.
- **Blue-green / canary deploys:** switch all traffic at once to a new copy, or shift it gradually, to reduce release risk.
- **Write-ahead log (WAL):** append the change to a log before applying it, which gives durability and crash recovery.
- **LSM tree vs B-tree:** LSM (Cassandra, RocksDB) gives fast writes through append + compaction. B-tree (Postgres, MySQL) gives fast reads and in-place updates.
- **Bloom filter:** a tiny probabilistic set. "Definitely not present" or "maybe present" (false positives, never false negatives).
- **Heartbeat / gossip:** how nodes detect failures and share membership.
- **Geohash / quadtree:** index 2D locations so "find nearby" becomes a prefix or tree lookup.
