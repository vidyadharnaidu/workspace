# System Design Interview Prep

A repeatable framework, the building blocks, 15 classic designs in one consistent template, and runnable Java for the low-level pieces interviewers ask you to code.

## Suggested study order

1. `01_fundamentals/01_interview_framework.md`: the 7-step process (**R-E-A-D H-D-W**)
2. `01_fundamentals/02_back_of_envelope_estimation.md`: numbers to memorize and a worked example
3. `01_fundamentals/03_core_concepts.md`: CAP, consistency, replication, sharding, idempotency
4. `02_building_blocks/*`: load balancers, caching, databases, queues, APIs, reliability
5. `03_classic_designs/*`: practice each design out loud in 45 minutes
6. `04_low_level_design/*`: run the Java, then code them yourself
7. `05_quick_reference/cheat_sheet.md`: the night before

## Contents

### 01 Fundamentals
| File | Covers |
|---|---|
| [Interview framework](01_fundamentals/01_interview_framework.md) | Timeline, requirement questions, default architecture, deep-dive checklist, scaling path, mistakes |
| [Estimation](01_fundamentals/02_back_of_envelope_estimation.md) | Time/size/latency/availability numbers, formulas, Twitter-scale example |
| [Core concepts](01_fundamentals/03_core_concepts.md) | CAP/PACELC, consistency models, quorums, sharding, consensus, delivery guarantees, real-time options |

### 02 Building blocks
| File | Covers |
|---|---|
| [Load balancers, gateways, CDN, DNS](02_building_blocks/01_load_balancers_gateways_cdn.md) | L4 vs L7, algorithms, API gateway, CDN push/pull, GeoDNS |
| [Caching](02_building_blocks/02_caching.md) | Cache-aside & friends, eviction, stampede/penetration/avalanche, Redis structures |
| [Databases & storage](02_building_blocks/03_databases_and_storage.md) | Picking a DB, replication, sharding, B-tree vs LSM, object storage, NoSQL modeling |
| [Queues & streams](02_building_blocks/04_messaging_queues_streams.md) | Queue vs log, Kafka, outbox, CDC, saga, event sourcing, CQRS, DLQ |
| [API design & real-time](02_building_blocks/05_api_design_and_realtime.md) | REST/gRPC/GraphQL, pagination, idempotency, WebSockets/SSE, webhooks |
| [Reliability, observability, security](02_building_blocks/06_reliability_observability_security.md) | Timeouts, retries, circuit breakers, RPO/RTO, RED/USE, SLOs, security checklist |

### 03 Classic designs
Every design uses the same template: **requirements → estimates → API → data model → diagram → deep dives → trade-offs → follow-ups**, plus a one-line **Remember** summary at the top.

| # | Design | The key insight |
|---|---|---|
| 01 | [URL shortener](03_classic_designs/01_url_shortener.md) | Unique id → Base62; cache; 302 for analytics |
| 02 | [Rate limiter](03_classic_designs/02_rate_limiter.md) | Token bucket in Redis via atomic Lua |
| 03 | [News feed](03_classic_designs/03_news_feed.md) | Hybrid fan-out (push normal users, pull celebrities) |
| 04 | [Chat / messaging](03_classic_designs/04_chat_messaging.md) | WebSocket gateways + registry; persist then deliver; sync on reconnect |
| 05 | [Notification system](03_classic_designs/05_notification_system.md) | Preferences + priority queues per channel + retries/DLQ + dedupe |
| 06 | [Web crawler](03_classic_designs/06_web_crawler.md) | Frontier with per-host politeness; Bloom filter dedupe |
| 07 | [Typeahead / autocomplete](03_classic_designs/07_typeahead_autocomplete.md) | Precomputed top-K per prefix, served from memory |
| 08 | [Video streaming](03_classic_designs/08_video_streaming.md) | Chunked parallel transcoding → HLS/DASH → CDN, adaptive bitrate |
| 09 | [Ride sharing](03_classic_designs/09_ride_sharing.md) | Geospatial index + atomic driver lock + trip state machine |
| 10 | [Key-value store](03_classic_designs/10_key_value_store.md) | Consistent hashing, quorums, hinted handoff, Merkle trees, LSM |
| 11 | [Payment system](03_classic_designs/11_payment_system.md) | Idempotency + state machine + double-entry ledger + reconciliation |
| 12 | [Ticket booking](03_classic_designs/12_ticket_booking.md) | Seat holds with atomic conditional update + TTL; waiting room |
| 13 | [File storage & sync](03_classic_designs/13_file_storage_sync.md) | Content-hashed chunks, dedupe, change journal cursor |
| 14 | [Distributed job scheduler](03_classic_designs/14_distributed_job_scheduler.md) | `FOR UPDATE SKIP LOCKED` / partition ownership + leased workers |
| 15 | [Leaderboard & top-K](03_classic_designs/15_leaderboard_top_k.md) | Redis sorted sets; streaming Count-Min Sketch + heap |

### 04 Low-level design (runnable Java 17)
See [04_low_level_design/README.md](04_low_level_design/README.md): rate limiters, consistent hashing, URL shortener, Snowflake ids, Bloom filter, circuit breaker, parking lot, plus an LLD question bank, SOLID and design patterns.

```bash
java system-design/04_low_level_design/ConsistentHashing.java
```

### 05 Quick reference
[Cheat sheet](05_quick_reference/cheat_sheet.md): requirement → technology map, the big trade-offs, 40 rapid-fire Q&As, estimation numbers.

## Practice routine

1. Pick a design and set a 45-minute timer.
2. Talk it through with the framework and draw on paper.
3. Compare with the file, and note what you missed (usually requirements, a failure mode, or a trade-off).
4. Next day, redo only the deep dives you missed.
