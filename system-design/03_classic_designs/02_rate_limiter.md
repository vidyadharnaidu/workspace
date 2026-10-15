# Design a Distributed Rate Limiter

> **Remember:** *"Token bucket in Redis with an atomic Lua script, at the API gateway, keyed by user/API key, returning 429 + Retry-After."*

## 1. Requirements

**Functional**
- Limit requests per client key (user id, API key, IP) per rule, e.g. 100 requests/minute per user or 10 logins/hour per IP
- Return **429 Too Many Requests** with headers when limited
- Rules are configurable without redeploying

**Non-functional**
- Adds very little latency (< 1–2 ms)
- Works across many API servers (a **shared** count)
- Highly available: if the limiter fails, **fail open** (allow) for most APIs and **fail closed** for security-sensitive ones (login, OTP)

## 2. Estimates
- 1M active clients × ~100 bytes of state each ≈ 100 MB of memory, so a Redis cluster fits easily.
- Throughput: 50k requests/s, meaning 50k Redis ops/s. That's fine for a small cluster.

## 3. Algorithms (the core of this question)

| Algorithm | How | Pros | Cons |
|---|---|---|---|
| **Token bucket** ⭐ | Bucket holds up to `capacity` tokens; refills at `rate`/s; each request takes 1 | Allows short **bursts**, smooth average; tiny state (tokens, last_refill) | Two params to tune |
| Leaky bucket | Requests enter a queue that drains at a fixed rate | Perfectly smooth output | Bursts are delayed/dropped; queue state |
| Fixed window counter | Count per `floor(now / window)` | Simplest, 1 counter | **Boundary burst**: 2× limit around the window edge |
| Sliding window log | Store timestamp of each request; count those in last window | Exact | Memory = one entry per request |
| **Sliding window counter** ⭐ | current window count + previous count × overlap % | Near-exact, 2 counters | Approximation |

→ All four are implemented with a fake clock in `04_low_level_design/RateLimiters.java`.

```
Fixed window boundary problem (limit 100/min):
 ... 100 requests at 00:59 | 100 requests at 01:00 ...  -> 200 requests in 2 seconds, all allowed
```

## 4. API / behaviour

```
Response headers:  X-RateLimit-Limit: 100
                   X-RateLimit-Remaining: 37
                   X-RateLimit-Reset: 1735689600
When limited:      HTTP 429, Retry-After: 12
```

## 5. High-level design

```
Client -> [ API Gateway / middleware ] --(check key)--> [ Redis Cluster ]  (counters/buckets, TTL'd)
                 |          ^
                 |          +----- rules cached locally, refreshed from [ Rules Store / config service ]
                 v
          [ Backend services ]   (only if allowed)
```

**Where to put it:** at the API gateway or edge (one place, protects everything), or as a library in each service (more flexible), or both for defense in depth.

## 6. Deep dives

### Atomicity (race conditions)
"Read count → compare → increment" from two servers at the same time lets both requests pass. Fix: do it atomically in Redis with a **Lua script** (or `INCR` + `EXPIRE` for a fixed window, since `INCR` is atomic).

```lua
-- token bucket in Redis (KEYS[1]=bucket key; ARGV: capacity, refill_per_sec, now_ms)
local b = redis.call('HMGET', KEYS[1], 'tokens', 'ts')
local tokens = tonumber(b[1]) or tonumber(ARGV[1])
local ts = tonumber(b[2]) or tonumber(ARGV[3])
tokens = math.min(tonumber(ARGV[1]), tokens + (tonumber(ARGV[3]) - ts) / 1000 * tonumber(ARGV[2]))
local allowed = tokens >= 1
if allowed then tokens = tokens - 1 end
redis.call('HMSET', KEYS[1], 'tokens', tokens, 'ts', ARGV[3])
redis.call('PEXPIRE', KEYS[1], 60000)
return allowed and 1 or 0
```

### Scaling & latency
- Shard Redis by the limiter key (Redis Cluster hash slots).
- For extreme scale, keep **local in-memory counters** per server and sync to Redis periodically. That's approximate but very fast.
- Use Redis in the same region or zone. Multi-region usually means per-region limits (a global exact limit is expensive).

### Rule design
- Several dimensions: per user, per IP, per endpoint, global. Apply the most specific rule, or all of them.
- Tiers (free vs paid) are stored in config and cached in the gateway.

## 7. Trade-offs
- Exact vs approximate (sliding log vs counter).
- Fail open vs fail closed.
- Hard limit (reject) vs soft limit (queue / degrade / lower priority).
- Client-side limits help but are not trusted.

## 8. Follow-up questions
- **How do you rate limit by IP behind NAT (many users, one IP)?** Prefer authenticated keys. Use higher IP limits combined with other signals (device fingerprint).
- **How do you tell clients when to retry?** `Retry-After`, and clients should use exponential backoff with jitter.
- **How do you avoid Redis becoming the bottleneck or SPOF?** Cluster with replicas, a local fallback limiter, and fail open with alerting.
