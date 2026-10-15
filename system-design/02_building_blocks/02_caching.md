# Caching

> "There are only two hard things in computer science: cache invalidation and naming things."

## Where caches live (outermost → innermost)

Browser → CDN → API gateway / reverse proxy → **application cache (Redis/Memcached)** → database buffer pool

## Caching strategies (patterns)

| Pattern | Read path | Write path | Pros | Cons |
|---|---|---|---|---|
| **Cache-aside (lazy loading)** ⭐ | App reads cache → on miss reads DB → writes cache | App writes DB, then **deletes** the cache key | Simple; cache only holds what's used; cache failure ≠ outage | First read is slow; stale window |
| **Read-through** | Cache library loads from DB on miss | same as above | App code simpler | Needs cache provider support |
| **Write-through** | — | App writes cache, cache writes DB synchronously | Cache always fresh | Slower writes; caches unused data |
| **Write-behind (write-back)** | — | App writes cache, cache flushes to DB **async** | Very fast writes | Can lose data if the cache dies |
| **Refresh-ahead** | Cache refreshes hot keys before they expire | — | No latency spikes for hot keys | Wasted refreshes |

**Default answer:** cache-aside + TTL + delete on write.

> **Why delete instead of update on write?** Two concurrent writers can update the cache in the wrong order and leave stale data forever. Deleting makes the next reader load the fresh value.

## Eviction policies

| Policy | Evicts | Good for |
|---|---|---|
| **LRU** ⭐ | Least recently used | General purpose (see `java/src/design/LRUCache.java`) |
| LFU | Least frequently used | Stable popularity (hot items stay) |
| FIFO | Oldest inserted | Simple |
| TTL | Expired entries | Freshness guarantees |

## Classic cache problems

| Problem | What happens | Fix |
|---|---|---|
| **Cache stampede / thundering herd** | Hot key expires → thousands of requests hit the DB at once | Lock/single-flight (only one request rebuilds), refresh-ahead, stale-while-revalidate |
| **Cache penetration** | Requests for keys that **don't exist** always miss and hit the DB (attack or bug) | Cache the "not found" result with a short TTL; Bloom filter of valid keys |
| **Cache avalanche** | Many keys expire at the same moment, or the cache cluster dies | **Random jitter on TTLs**; replicas; circuit breaker; warm-up |
| **Hot key** | One key gets huge traffic (celebrity profile) | Local in-process cache in front of Redis; replicate the key (`key#1..N`) |
| **Stale data** | Cache and DB disagree | Short TTL, delete-on-write, change-data-capture (CDC) invalidation |
| **Big values** | Slow network and memory pressure | Compress, split, or cache only the needed fields |

## Redis vs Memcached

| | Redis ⭐ | Memcached |
|---|---|---|
| Data types | Strings, hashes, lists, sets, **sorted sets**, streams, geo, bitmaps, HyperLogLog | Strings only |
| Persistence | RDB snapshots / AOF log | None |
| Replication & cluster | Yes | Client-side sharding |
| Extra features | Pub/sub, Lua scripts, transactions, TTL per key | — |
| Threads | Mostly single-threaded core (very fast) | Multi-threaded |

**Redis structures used in interviews:**
- **Sorted set** for leaderboards and rate-limit windows
- **List** for feed ids
- **Hash** for objects
- **SET NX EX** for locks and seat holds
- **GEO** for nearby drivers
- **HyperLogLog** for unique counts
- **Bitmap** for daily active flags

## What to cache (and what not)

✅ Read-heavy, expensive to compute, tolerant of slight staleness: user profiles, product pages, feed ids, session data, config, rendered HTML fragments.
❌ Highly personalized one-off data, data that must be strongly consistent (account balance shown for a transfer), rapidly changing data with a low read ratio.

## Interview Q&A

**Q: How do you keep the cache and DB consistent?**
Cache-aside with delete-on-write and a TTL as a safety net. For stricter needs, use CDC (Debezium reads the DB log and invalidates keys), or a *delayed double delete*: delete, write the DB, delete again after a short delay.

**Q: The Redis cluster went down. What happens?**
Cache-aside means requests fall through to the DB, which may overload it. Protect the DB with rate limiting and a circuit breaker, serve degraded responses, and warm the cache on recovery.

**Q: How big should the cache be?**
Start with the 80/20 rule: 20% of objects get 80% of reads. Cache memory ≈ 20% of the daily read data size. Then tune by hit ratio (aim for >90% on hot paths).

**Q: Local (in-process) cache or distributed cache?**
A local cache (Caffeine/Guava) is fastest, but each server has its own copy, so data can be inconsistent across servers and invalidation is hard. A distributed cache (Redis) is shared. Common combination: small local cache (seconds TTL) + Redis.
