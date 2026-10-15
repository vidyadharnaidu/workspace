# Design Search Autocomplete / Typeahead (Google search box)

> **Remember:** *"Precompute top-K suggestions per prefix offline from query logs, serve prefix → top-K from an in-memory cache / trie (< 50 ms), with a small real-time path for trending queries."*

## 1. Requirements

**Functional**
- As the user types, return the top 5–10 query suggestions for the prefix
- Ranked by popularity (optionally personalization, trending, locale)

**Non-functional**
- **Very low latency:** < 100 ms end to end (the server budget is ~20–50 ms)
- Highly available; suggestions can be slightly stale (updated hourly/daily)
- Filter offensive/illegal suggestions

## 2. Estimates
- 10B searches/day, and each search ≈ 10 keystroke requests (with debouncing ~4) → **~400k–1M requests/s**. That's why it must be precomputed + cached.
- Unique prefixes (≤ 20 chars) for popular queries: hundreds of millions × top-10 lists → **hundreds of GB**, sharded in memory

## 3. API
```
GET /v1/suggest?q=how%20to%20m&locale=en-US&limit=10
-> { "suggestions": ["how to make money", "how to meditate", ...] }
```
Client side:
- **Debounce** (~100–150 ms)
- Cache previous prefix results (typing "how t" → "how to" often reuses them)
- Cancel stale requests

## 4. Data structures

**Option A: trie with top-K cached at every node** ⭐
```
root
 └─ h
     └─ o
         └─ w  [top: "how to", "how old is", "how many", ...]
             └─ ' '
                 └─ t  [top: "how to make money", "how to tie a tie", ...]
```
Lookup = walk the prefix (O(length)) → return the stored top-K (O(1)). No subtree search at query time.

**Option B: flat key-value map** `prefix → [top-K]` in Redis/RocksDB. Simpler to shard and replicate. The builder generates every prefix of every popular query.

→ Trie code with autocomplete: `java/src/trees/Trie.java`

## 5. High-level design

```
 online (read) path
 Browser --debounced--> [CDN / edge cache for very common prefixes]
                             |
                             v
                        [LB] -> [Suggest Service] -> [Suggestion store: in-memory trie / Redis, sharded by prefix]
                                        |
                                        +--> merge small real-time "trending" list (optional)
                                        +--> filter blocklist, personalize (recent searches from the user's history)

 offline (build) path
 [Search logs] -> [Kafka] -> [Aggregator: count queries per window (Spark/Flink)]
                                   -> [Builder: prefixes -> top-K by weighted score (recent counts weigh more)]
                                   -> new snapshot -> [Suggestion store] (blue/green swap)
```

## 6. Deep dives

### Building top-K
- Score = decayed popularity (e.g. counts from the last 7 days with exponential time decay) + freshness.
- For each query, for each prefix (up to N chars), push (query, score) and keep the top-K per prefix with a min-heap of size K.
- Rebuild daily/hourly as a new immutable snapshot, then atomically switch readers to it.

### Trending / real-time
- A streaming job (Flink) counts queries over sliding 5–15 minute windows, finding heavy hitters with a Count-Min Sketch + heap. It's merged in at read time for a small boost.

### Sharding
- Shard by prefix. Naive first-letter sharding is uneven (many words start with "s"), so use ranges chosen from the actual prefix distribution, or hash the first 2–3 chars.
- Replicate every shard (read-heavy). Keep the data in memory.

### Filtering & personalization
- Blocklist filter at build time (and at serve time for urgent removals).
- Personalization: blend global suggestions with the user's recent searches (small per-user store) at the edge of the request.

## 7. Trade-offs
- Precompute (fast, stale) vs compute on request (fresh, too slow at this QPS). Precompute + a trending overlay.
- Trie in memory (fast, complex to shard) vs KV prefix map (simple, more storage).
- Top-K per node uses more memory, but query latency is constant.

## 8. Follow-up questions
- **Typos ("hwo to")?** Fuzzy matching via edit-distance candidates or a spelling-correction service. Usually a separate step, only when there are few results.
- **Multiple languages?** A separate index per locale, with Unicode normalization.
- **How do you remove a harmful suggestion immediately?** A serve-time blocklist checked before returning, pushed to all servers in seconds (config service).
