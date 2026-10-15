# Design a URL Shortener (TinyURL / bit.ly)

> **Remember:** *"Unique ID → Base62 → key-value store → cache → 302 redirect, with analytics sent async."*

## 1. Requirements

**Functional**
- Shorten a long URL → short URL (`sho.rt/aZ3kQ9x`)
- Redirect short → long
- Optional: custom alias, expiration, click analytics

**Non-functional**
- Very **read-heavy** (redirects ≫ creates), low redirect latency (< 50 ms)
- Highly available (broken links are bad); short codes must be unique and not easily guessable

**Out of scope:** user accounts UI, link previews

## 2. Estimates

| | Calculation | Result |
|---|---|---|
| New URLs | 100M / day | ~1,200 writes/s |
| Redirects (10:1) | 1B / day | ~12,000 reads/s (peak ~25k) |
| Records in 5 years | 100M × 365 × 5 | ~180 billion |
| Storage | 180B × 500 bytes | ~90 TB → **needs sharding or a KV store** |
| Code length | 62^7 ≈ 3.5 trillion | **7 Base62 characters** is plenty |

## 3. API

```
POST /v1/urls        { "longUrl": "...", "customAlias": "opt", "expiresAt": "opt" }
                     -> 201 { "shortUrl": "https://sho.rt/aZ3kQ9x" }
GET  /{code}         -> 302 Location: <longUrl>     (404 if unknown, 410 if expired)
```

**301 or 302?** A **301** (permanent) is cached by browsers, which means less load but no click tracking. A **302** (temporary) sends every click through us, so use it when analytics matter.

## 4. Data model

```
urls:  code (PK, 7 chars) | long_url | user_id | created_at | expires_at
```
Access pattern = get by `code` only → a **key-value store** (DynamoDB/Cassandra) or Postgres sharded by `code`.

## 5. High-level design

```
            POST /urls                       +----------------+
Client ----------------> [LB] -> [URL Service] -> | ID generator   |  (range-based counter / Snowflake)
   |                                   |          +----------------+
   |                                   v
   |                              [ KV store: code -> long_url ]
   |  GET /aZ3kQ9x                     ^
   +------------> [LB] -> [Redirect Service] --> [ Redis cache (hot codes) ]
                                   |
                                   +--(async click event)--> [Kafka] -> [Analytics workers] -> [OLAP store]
```

## 6. Deep dives

### Generating the short code (the key question)

| Approach | How | Pros | Cons |
|---|---|---|---|
| **Counter + Base62** ⭐ | Unique 64-bit id → Base62 encode | No collisions, short | Sequential codes are guessable → shuffle bits / add salt; need distributed counter |
| Hash + truncate | `base62(md5(longUrl))[0..7]` | Same URL → same code (dedupe) | **Collisions** → check DB & retry with salt |
| Pre-generated keys (KGS) | Offline service creates random unused codes; servers take batches | Fast, random | Extra service; mark keys used atomically |

**Distributed counter without a bottleneck:** each server leases a **range** of ids (e.g. 1,000,000–1,999,999) from ZooKeeper/etcd or a DB row, and hands them out locally. If a server dies, only its unused range is wasted. Snowflake-style ids also work.
→ See `04_low_level_design/UrlShortener.java` and `SnowflakeIdGenerator.java`.

### Read path performance
- The cache (Redis, LRU) holds hot codes, and the 80/20 rule applies. Cache hit → redirect in about 1 ms.
- Cache "not found" results briefly to stop enumeration attacks from hammering the DB.
- Put the redirect service at edge regions or behind a CDN for global latency.

### Custom aliases & expiration
- Custom alias: insert with a uniqueness constraint (conditional put). Return 409 if it's taken.
- Expiration: check `expires_at` on read (lazy), plus a background job that deletes expired rows in batches.

### Analytics
- Never block the redirect. Publish a click event (code, time, country, referrer) to Kafka, aggregate in stream processing, and store in a columnar DB (ClickHouse).

## 7. Bottlenecks & trade-offs
- The KV store is sharded by `code` (a hash) for an even spread.
- Abuse: rate-limit creation per user/IP and check URLs against malware/phishing lists.
- The same long URL creating many codes is fine (simpler). Dedupe only if required (it needs an index on `long_url_hash`).

## 8. Follow-up questions
- **How do you prevent guessing all URLs?** Non-sequential codes (random KGS or permuted ids), rate limiting, optional auth for private links.
- **Can we lose data if the cache dies?** No, the cache isn't the source of truth. Reads fall back to the KV store.
- **Multi-region?** Globally replicated KV (DynamoDB Global Tables). Redirects are read-local; writes use region-prefixed id ranges to avoid conflicts.
