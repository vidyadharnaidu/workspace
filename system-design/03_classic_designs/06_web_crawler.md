# Design a Web Crawler (search engine crawler)

> **Remember:** *"Seed URLs → URL frontier (priority + per-host politeness queues) → fetch (robots.txt, DNS cache) → store raw page → parse links → dedupe URLs (Bloom filter) & content (hash) → back to the frontier."*

## 1. Requirements

**Functional**
- Start from seed URLs, download pages, extract links, repeat
- Store page content for indexing
- Recrawl pages periodically (freshness)

**Non-functional**
- **Politeness:** respect robots.txt, don't overload any single site
- Scale: 1B pages/month; robust to bad HTML, traps and slow servers
- Extensible (images, PDFs later)

## 2. Estimates
- 1B pages / month ≈ 1B / 2.6M s ≈ **~400 pages/s** (peak ~800)
- Average page 500 KB → **500 TB/month** raw (compress ~5× → ~100 TB) → object storage
- URLs seen: tens of billions × ~100 bytes → Bloom filter / sharded KV for the seen set

## 3. Components (no public API; it's a pipeline)

```
 [Seed URLs]
      |
      v
 +----------------------------- URL FRONTIER -----------------------------+
 |  Prioritizer (PageRank-ish, freshness, domain importance)              |
 |      -> front queues (by priority)                                     |
 |  Politeness router                                                     |
 |      -> back queues: ONE queue per host, with next-allowed-fetch time  |
 +------------------------------------------------------------------------+
      | (worker takes a URL whose host is ready)
      v
 [Fetcher workers] --[DNS cache]--> Internet
      | check robots.txt (cached per host), timeouts, max size
      v
 [Content store: S3]  <-- raw HTML (compressed), keyed by URL hash + fetch time
      |
      v
 [Parser] --extract links--> [URL normalizer] --> [URL seen? Bloom filter + KV] --new--> back to FRONTIER
      |
      +--> [Content dedupe: hash / SimHash seen?] --> if new --> [Indexer queue (Kafka)]
```

## 4. Data model
```
url_state     (url_hash PK, url, last_fetched, next_fetch_at, status, content_hash, priority)
host_state    (host PK, robots_txt, crawl_delay, next_allowed_at)
content store S3: /pages/{url_hash}/{timestamp}.html.gz
```

## 5. Deep dives

### Politeness
- **One back queue per host** plus a heap ordered by `next_allowed_at`. A worker only takes a URL from a host whose time has come, then sets `next_allowed_at = now + crawl_delay` (e.g. 1 s).
- Cache robots.txt per host (refresh daily) and obey `Disallow` and `Crawl-delay`.
- **Distribute by host:** `hash(host) → crawler node`, so politeness state stays local to one node.

### Deduplication
- **URL dedupe:** normalize (lowercase host, strip fragments, sort params, remove tracking params), then check a **Bloom filter** (fast, tiny memory, rare false positives mean skipping a page, which is acceptable), backed by a KV store for exact state.
- **Content dedupe:** many URLs have the same content (mirrors, session ids). Use an exact hash (SHA-256) for identical pages and **SimHash/MinHash** for near-duplicates.

### Crawler traps & robustness
- Max URL length, max depth per site, max pages per host, detect infinite calendars / session-id loops.
- Timeouts, max download size, content-type filtering, retries with backoff for 5xx, and give up on dead hosts.

### Freshness (recrawl)
- Schedule `next_fetch_at` from the observed change rate. News homepages get minutes, static docs get months. Use HTTP `If-Modified-Since` / ETag to skip unchanged pages.

### Fault tolerance
- The frontier is persisted (Kafka / disk-backed queues). Workers are stateless, so a dead worker's URLs are re-leased after a timeout.
- The raw content in S3 is replayable into new parsers/indexers.

## 6. Trade-offs
- BFS (breadth: discovers many sites) vs priority (quality first). Real crawlers use priority.
- A Bloom filter's false positives (occasionally skipping a new URL) are acceptable for memory savings.
- Rendering JavaScript (headless Chrome) is expensive, so only do it for important pages.

## 7. Follow-up questions
- **How do you crawl 10× faster?** More fetchers (I/O bound, async HTTP), more hosts in parallel (politeness is per host, not global), DNS caching, geographically distributed crawlers near sites.
- **How do you avoid being blocked?** Identify your user agent, obey robots.txt, adaptive rate per host based on response times/errors.
- **How do you prioritize?** Link-based importance (PageRank), domain authority, freshness needs, sitemap hints.

→ Bloom filter code: `04_low_level_design/BloomFilter.java`
