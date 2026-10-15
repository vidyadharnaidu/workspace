# Design a Real-Time Leaderboard & Top-K / Trending (gaming leaderboard, "top 10 songs", trending hashtags)

> **Remember:** *"Exact leaderboard = Redis sorted set (ZINCRBY / ZREVRANGE / ZREVRANK). Top-K over a massive event stream = partition → count in windows (Count-Min Sketch + min-heap) → merge the partial top-Ks."*

## Part A: Game leaderboard

### 1. Requirements
- Update a player's score when they win a match
- Show the top 10 players, and **my rank** + neighbors (players around me)
- Real time (updates visible in < 1 s); leaderboards per season/day/region

### 2. Estimates
- 50M DAU, 10 score updates per user/day → 500M/day ≈ **~6k updates/s**; rank queries ~20k/s
- Redis sorted set with 50M members ≈ several GB in memory → fits in one large node or a small cluster

### 3. API
```
POST /v1/leaderboards/{boardId}/scores   { playerId, delta }           (server-side only, from game service)
GET  /v1/leaderboards/{boardId}/top?limit=10
GET  /v1/leaderboards/{boardId}/players/{playerId}   -> { rank, score, neighbors[] }
```

### 4. Design

```
 [Game Server] --match result--> [Score Service] --(1) ZINCRBY board:season5 delta player42--> [Redis Sorted Set]
                                        |                                                    ^
                                        +--(2) event--> [Kafka] --> [Score DB (source of truth, history)]
 [Client] --top 10 / my rank--> [Leaderboard API] --ZREVRANGE / ZREVRANK / ZSCORE------------+
```

| Operation | Redis command | Complexity |
|---|---|---|
| Add to score | `ZINCRBY board delta player` | O(log N) |
| Top 10 | `ZREVRANGE board 0 9 WITHSCORES` | O(log N + 10) |
| My rank | `ZREVRANK board player` | O(log N) |
| Players around me | `ZREVRANGE board rank-5 rank+5` | O(log N + 10) |

A sorted set is a skip list + hash map, which is why rank lookups are logarithmic. (In SQL, "my rank" is `COUNT(*) WHERE score > mine`, which is slow at scale.)

### 5. Deep dives
- **Ties:** encode a tiebreaker in the score, e.g. `score * 10^10 + (MAX_TS - achieved_at)`, so the earlier achiever ranks higher.
- **Durability:** Redis AOF + replica. The Score DB / Kafka log can rebuild the sorted set.
- **Hundreds of millions of players:** shard by score range (each shard is its own sorted set with known bounds; global rank = count in higher shards + rank within shard) or shard by region/league. The top 10 only needs the highest shard.
- **Daily/weekly boards:** separate keys (`board:2025-06-10`) with TTL, or `ZUNIONSTORE` to build weekly from dailies.
- **Cheating:** only the trusted game server can submit scores, and anomaly detection flags suspicious jumps.

## Part B: Top-K heavy hitters over a huge stream (trending, most played songs)

### 1. Requirements
- Top 100 songs/hashtags in the last 1 hour / 24 hours, refreshed every minute
- Billions of events/day; approximate counts are OK

### 2. Design

```
 [Play/post events] -> [Kafka topic partitioned by item_id]
                               |
               +---------------+----------------+
               v               v                v
        [Stream worker]  [Stream worker]  [Stream worker]      (Flink/Kafka Streams)
         per 1-min window: count items (exact hash map if it fits,
         else Count-Min Sketch) + min-heap of local top-K
               \               |                /
                v              v               v
             [Aggregator: merge local top-Ks per window, then sum the last 60 windows]
                               |
                        [Top-K cache (Redis)] <-- [API: GET /trending?window=1h]
 Batch path (accurate, delayed): events -> data lake -> Spark daily exact counts -> corrects/replaces
```

### 3. Deep dives
- **Count-Min Sketch:** a d × w counter table with d hash functions. Increment d cells, and estimate = the minimum of the d cells. Memory is fixed, and it only **over**-counts (never under). Pair it with a heap of size K for the current heavy hitters.
- **Why partition by item_id?** All events for one item land on one worker, so the local counts are exact per item and merging top-Ks is safe.
- **Sliding windows:** keep per-minute buckets and sum the last 60 (drop the oldest). For decayed "trending", use exponentially decayed counts so recent activity matters more.
- **Lambda-style:** a fast approximate stream path + a slow exact batch path that corrects history.

## Follow-up questions
- **Why not `SELECT item, COUNT(*) ... ORDER BY 2 DESC LIMIT 10` on the events table?** It scans billions of rows per refresh: too slow and expensive. Pre-aggregate incrementally.
- **Trending vs popular?** Trending = growth relative to a baseline (z-score of the current window vs the historical average), not raw counts, otherwise the same popular items are always on top.
- **Exact rank for everyone in a 1B-player game?** Show exact ranks for the top N and **percentile buckets** ("top 5%") for everyone else, computed periodically.
