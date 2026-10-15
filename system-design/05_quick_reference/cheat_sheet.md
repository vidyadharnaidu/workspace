# System Design Cheat Sheet (read the night before)

## Requirement → what to reach for

| If you hear… | Reach for |
|---|---|
| "Read-heavy", "low latency reads" | Cache (Redis, cache-aside), read replicas, CDN, denormalized read models |
| "Write-heavy", "billions of events" | Kafka, LSM stores (Cassandra), batching, append-only logs |
| "Money", "inventory", "must not double" | SQL transactions, idempotency keys, conditional updates, ledger, outbox |
| "Real-time updates to clients" | WebSockets (two-way), SSE (server push), long polling (fallback) |
| "Large files / media" | Object storage (S3) + pre-signed URLs + multipart upload + CDN |
| "Search", "fuzzy", "filters" | Elasticsearch/OpenSearch fed by CDC |
| "Nearby", "location" | Geohash / quadtree / H3; Redis GEO; shard by region |
| "Top K", "trending", "leaderboard" | Redis sorted sets; streaming counts + Count-Min Sketch + heap |
| "Unique ids across servers" | Snowflake / UUIDv7 / ULID; range-leased counters |
| "Is it seen before?" at huge scale | Bloom filter (+ exact store behind it) |
| "Spiky traffic" | Queue to buffer, autoscaling, rate limiting, waiting room |
| "Slow/unreliable dependency" | Timeouts, retries with backoff + jitter, circuit breaker, bulkhead, fallback |
| "Run later / periodically" | Job scheduler: DB with next_run_time + queue + leased workers |
| "Multi-step across services" | Saga with compensations; outbox for events |
| "Global users" | GeoDNS, CDN, regional deployments, replicated data |
| "Analytics / reporting" | Data warehouse / columnar store; don't query OLTP for heavy reports |
| "Audit trail / history" | Append-only event log, event sourcing |

## The big trade-offs (say them out loud)

| Trade-off | Option A | Option B | Decide by |
|---|---|---|---|
| Consistency vs availability | CP: correct or error | AP: always answer, maybe stale | Cost of stale data (money vs likes) |
| Latency vs consistency | Async replication, fast | Sync replication, safe | Can we lose the last second of writes? |
| Push vs pull (feed fan-out) | Precompute on write | Compute on read | Follower count distribution → hybrid |
| SQL vs NoSQL | Transactions, joins | Horizontal scale, flexible schema | Access patterns + consistency needs |
| Normalize vs denormalize | No duplication, easy writes | Fast reads, duplicated data | Read/write ratio |
| Cache freshness vs load | Short TTL | Long TTL | Tolerance for staleness |
| Batch vs stream | Simple, accurate, delayed | Fresh, complex, approximate | How fresh must results be? |
| Monolith vs microservices | Simple ops | Independent scale/deploy | Team size, domain clarity |
| Exactly-once vs at-least-once | Very hard end to end | Easy + idempotent consumers | Always pick at-least-once + idempotency |
| Strong vs eventual counters | Exact, slow | Approximate, fast | Is the number user-critical? |
| Polling vs WebSockets | Simple, wasteful | Efficient, stateful connections | Update frequency and scale |

## 40 rapid-fire questions

1. **Horizontal vs vertical scaling?** Add machines vs a bigger machine. Horizontal needs stateless services.
2. **How do you make a service stateless?** Session in Redis/JWT; files in S3; no local-only state.
3. **CAP in one sentence?** During a network partition, choose consistency or availability.
4. **R + W > N?** Read and write quorums overlap, so reads see the latest write.
5. **Why consistent hashing?** Adding/removing a node moves ~1/N of keys instead of nearly all.
6. **Virtual nodes?** Each server appears many times on the ring for an even load.
7. **Cache-aside write path?** Update the DB, then delete the cache key.
8. **Cache stampede fix?** Single-flight lock, stale-while-revalidate, jittered TTLs.
9. **Cache penetration fix?** Cache negative results, Bloom filter.
10. **When not to cache?** Strongly consistent data or low read ratios.
11. **Replication lag problem?** Stale reads, so read-your-writes from the primary.
12. **Good shard key?** High cardinality, even distribution, used in most queries.
13. **Hot partition fix?** Key salting, dedicated shard, caching.
14. **How to paginate a feed?** Cursor/keyset, not offset.
15. **Idempotency key?** Client-generated id so retries don't duplicate side effects.
16. **Outbox pattern?** Write the event to a DB table in the same transaction and publish it asynchronously.
17. **Saga?** A chain of local transactions with compensating actions.
18. **Kafka ordering?** Per partition; use the entity id as the key.
19. **Consumer lag growing?** More consumers/partitions, batch processing, check poison messages.
20. **DLQ?** Holds messages that failed after N retries for inspection/replay.
21. **Why jitter in retries?** Avoid synchronized retry storms.
22. **Circuit breaker states?** Closed → open → half-open.
23. **Bulkhead?** Isolated resource pools per dependency.
24. **L4 vs L7 load balancer?** Transport-level (fast) vs HTTP-aware (routing, TLS).
25. **CDN pull vs push?** Fetched on first request vs uploaded ahead of time.
26. **Pre-signed URL?** A temporary signed permission to upload/download directly from object storage.
27. **WebSocket scaling?** Connection gateways + user→gateway registry + pub/sub.
28. **SSE vs WebSocket?** One-way server push over HTTP vs full duplex.
29. **Snowflake id layout?** 41-bit time, 10-bit machine, 12-bit sequence.
30. **Bloom filter guarantee?** No false negatives, tunable false positives.
31. **LSM vs B-tree?** Write-optimized append + compaction vs read-optimized in-place pages.
32. **WAL?** Log the change before applying it, for durability and recovery.
33. **Leader election tools?** etcd/ZooKeeper leases (Raft/ZAB), with fencing tokens.
34. **Distributed lock pitfall?** A paused holder with an expired lease, hence fencing tokens.
35. **RPO vs RTO?** How much data you can lose vs how long you can be down.
36. **SLI/SLO/SLA?** Measurement / internal target / external contract.
37. **RED metrics?** Rate, errors, duration.
38. **How to prevent double booking?** Atomic conditional update or unique constraint; holds with TTL.
39. **How to count views at scale?** Events to Kafka → aggregate in windows → periodic writes.
40. **Why not 2PC across microservices?** Blocking, a coordinator SPOF, poor availability, so use sagas + idempotency.

## Estimation quick numbers

- **Time:** 1 day ≈ 10^5 s. 1M/day ≈ 12/s. 1B/day ≈ 12k/s. Peak ≈ 2–3× the average.
- **Sizes:** 1 KB post, ~300 KB photo, ~40 MB per minute of 1080p video. 62^7 ≈ 3.5 trillion.
- **Latency:** same datacenter 0.5 ms, SSD read 0.15 ms, cross-continent 150 ms.
- **Availability:** 99.9% ≈ 8.8 h/year. 99.99% ≈ 53 min/year.
