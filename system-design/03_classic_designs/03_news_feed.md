# Design a News Feed (Twitter / Instagram / Facebook timeline)

> **Remember:** *"Hybrid fan-out: push post ids into followers' feed caches, but pull celebrity posts at read time, then merge, rank and paginate with a cursor."*

## 1. Requirements

**Functional**
- Create a post (text, images)
- Follow / unfollow users
- View home feed: posts from people you follow, newest first (optionally ranked)

**Non-functional**
- Feed load p99 < 300 ms; **read-heavy**
- Eventual consistency is OK (a post can appear a few seconds late)
- Highly available; posts must never be lost

**Out of scope:** ads, comments, search, DMs

## 2. Estimates
- 200M DAU; 100M posts/day → **~1.2k writes/s**
- Feed reads: 200M × 10/day = 2B → **~23k reads/s** (peak ~50k), so optimize reads
- Average 200 followers per user; some celebrities have **100M followers**
- Media: 10% of posts × 500 KB → 5 TB/day → S3 + CDN

## 3. API
```
POST /v1/posts                 { text, mediaIds[] }                 -> 201 { postId }
POST /v1/users/{id}/follow  |  DELETE /v1/users/{id}/follow
GET  /v1/feed?cursor=<opaque>&limit=20                             -> { posts[], nextCursor }
POST /v1/media/upload-url      -> { uploadUrl (pre-signed S3), mediaId }
```

## 4. Data model
```
users       (user_id PK, name, follower_count, ...)
posts       (post_id PK (Snowflake, time-sortable), author_id, text, media_urls, created_at)   sharded by post_id / author_id
follows     (follower_id, followee_id, created_at)  PK(follower_id, followee_id) + reverse index by followee_id
feed cache  Redis: feed:{user_id} -> list of post_ids (latest ~500)
```

## 5. High-level design

```
 write path
 Client -> [LB] -> [Post Service] -> [Posts DB] 
                          |
                          +--> [Kafka: post_created] -> [Fan-out Workers] --(follower ids from Graph Service)--> [Feed Cache: Redis lists]
 read path
 Client -> [LB] -> [Feed Service] -> 1. read feed:{me} post ids from Redis
                                     2. pull recent posts from followed celebrities
                                     3. merge + rank + take page
                                     4. hydrate post ids -> [Post Cache / Posts DB], [User Cache]
 media:  Client --pre-signed PUT--> [S3] ---> [CDN] <--- Client loads images
```

## 6. Deep dives

### Fan-out on write vs fan-out on read (THE key trade-off)

| | Fan-out on **write** (push) | Fan-out on **read** (pull) |
|---|---|---|
| How | When X posts, insert post id into every follower's feed list | At read time, fetch recent posts of everyone I follow and merge |
| Read | ⚡ Fast: feed is precomputed | Slow: many queries + merge per request |
| Write | Expensive for users with many followers | Cheap |
| Problem | **Celebrity with 100M followers → 100M inserts** | Heavy reads for users following thousands |
| Wasted work | Pushes to inactive users | None |

**Hybrid (what real systems do):**
- Normal users get fan-out on write.
- Celebrities (e.g. > 10k followers) get **no fan-out**. Their posts are **pulled** at read time and merged.
- Skip pushing to inactive users (not seen for 30 days). Rebuild their feed on their next login.

### Feed cache
- Store only **post ids** (tiny), capped at ~500 per user, with LTRIM on push.
- Hydrate ids in batch (multi-get) from the post cache, which is shared by all readers.
- **Cursor pagination:** the cursor is the last post id (time-sortable), so the next page has `post_id < cursor`.

### Ranking (if asked)
- Candidate generation (the merged feed) → scoring model (recency, affinity, engagement, media type) → re-rank for diversity → return.
- Precompute features offline and score online in a ranking service with a strict latency budget.

### Counters (likes, views)
- Not a column updated synchronously on every like. Write events to Kafka, aggregate, and update counts periodically. Show approximate counts from the cache.

## 7. Bottlenecks & trade-offs
- Fan-out workers lag during spikes. It's OK (eventual). Scale workers on queue lag.
- Hot posts (viral) get a local cache + CDN for media.
- The follow graph is sharded by `follower_id`, with a reverse index for `followee_id` (who follows me) used by fan-out.
- Deletions / unfollows are handled lazily: filter at read time + async cleanup.

## 8. Follow-up questions
- **A celebrity posts. How fast do followers see it?** Instantly on their next feed load, via the pull path.
- **A user follows someone new. When do their posts appear?** Backfill recent posts of the new followee into the feed asynchronously, or rely on read-time merge.
- **How do you handle the feed cache being lost?** Rebuild on demand from the follow list + recent posts (slower first load), and warm active users.
- **Real-time "new posts" banner?** SSE/WebSocket or periodic lightweight polling: "5 new posts".
