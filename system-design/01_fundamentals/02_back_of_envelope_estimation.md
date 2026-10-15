# Back-of-the-Envelope Estimation

Goal: round numbers that **decide something**. Examples: do we need sharding, is caching worth it, how many servers? Always round aggressively.

## Numbers to memorize

### Time

| Fact | Value | Handy approximation |
|---|---|---|
| Seconds in a day | 86,400 | **~100,000 (10^5)** |
| Seconds in a month | ~2.6 million | 2.5 × 10^6 |
| Seconds in a year | ~31.5 million | 3 × 10^7 |
| 1 million requests/day | ~12 req/s | **1M/day ≈ 10/s** |
| 1 billion requests/day | ~11,600 req/s | **1B/day ≈ 10k/s** |
| Peak traffic | 2–3× the average | use ×2 unless told otherwise |

### Data sizes

| Unit | Bytes | | Item | Typical size |
|---|---|---|---|---|
| 1 KB | 10^3 (2^10) | | char (ASCII / UTF-8 Latin) | 1 byte |
| 1 MB | 10^6 (2^20) | | int / long | 4 / 8 bytes |
| 1 GB | 10^9 (2^30) | | UUID (binary / string) | 16 / 36 bytes |
| 1 TB | 10^12 (2^40) | | Tweet-like post with metadata | ~1 KB |
| 1 PB | 10^15 (2^50) | | Photo (compressed) | 200 KB – 2 MB |
| | | | 1 minute of 1080p video (~5 Mbps) | ~40 MB |

### Latency (the famous "numbers every engineer should know", rounded)

| Operation | Time | Remember as |
|---|---|---|
| CPU L1 cache reference | 0.5 ns | |
| Main memory reference | 100 ns | memory is fast |
| Compress 1 KB (fast codec) | ~3 µs | |
| Send 1 KB over 1 Gbps network | 10 µs | |
| Random read 4 KB from SSD | ~150 µs | |
| Read 1 MB sequentially from memory | 250 µs | |
| Round trip within a datacenter | 500 µs | **same DC ≈ 0.5 ms** |
| Read 1 MB sequentially from SSD | 1 ms | |
| HDD disk seek | 10 ms | disks are slow |
| Read 1 MB sequentially from HDD | 20 ms | |
| Round trip California ↔ Europe | 150 ms | **cross-continent ≈ 150 ms** |

**Takeaways:** memory ≫ SSD ≫ disk ≫ network across continents. Cache hot data, avoid disk seeks, and keep users close to the servers.

### Availability ("nines")

| Availability | Downtime / year | Downtime / month |
|---|---|---|
| 99% | 3.65 days | 7.3 hours |
| 99.9% | 8.8 hours | 44 minutes |
| 99.99% | 53 minutes | 4.4 minutes |
| 99.999% | 5.3 minutes | 26 seconds |

- **Components in series:** multiply availabilities. Two services at 99.9% give 99.8%.
- **Redundant copies in parallel:** `1 - (1-a)^n`. Two copies at 99% give 99.99%.

### Rough capacity of one machine (order of magnitude only, since it depends heavily on the workload)

| Component | Rough throughput |
|---|---|
| Stateless web/app server | 1k – 10k requests/s |
| PostgreSQL / MySQL (single primary, good indexes) | ~1k – 10k writes/s, more for simple reads |
| Redis | ~100k ops/s |
| Kafka broker | hundreds of MB/s |
| WebSocket server | ~100k – 1M concurrent idle connections (tuned) |

## The formulas

```
average QPS  = daily active users × actions per user per day / 86,400
peak QPS     = average QPS × 2 (or 3)
storage/day  = new items per day × size per item
storage total= storage/day × 365 × years × replication factor (usually 3)
bandwidth    = QPS × response size
servers      = peak QPS / QPS per server   (+ headroom for failures)
cache memory = hot fraction (e.g. 20%) × daily read data   (80/20 rule)
```

## Worked example: a Twitter-like service

**Assume:** 200M DAU. Each user posts 0.5 times a day and reads the feed 10 times a day. 10% of posts have an image of 500 KB. Keep data 5 years.

| Quantity | Calculation | Result |
|---|---|---|
| Posts per day | 200M × 0.5 | 100M |
| Write QPS | 100M / 10^5 | **~1,000/s** (peak ~2k) |
| Feed reads per day | 200M × 10 | 2B |
| Read QPS | 2B / 10^5 | **~20,000/s** (peak ~40k), so **read-heavy 20:1 → cache** |
| Text storage per day | 100M × 1 KB | 100 GB |
| Image storage per day | 10M × 500 KB | 5 TB |
| Media over 5 years | 5 TB × 365 × 5 | **~9 PB → object storage (S3) + CDN** |
| Text over 5 years ×3 replicas | 100 GB × 365 × 5 × 3 | **~550 TB → sharded DB** |

**What the numbers decided:** caching the feed, putting media in object storage behind a CDN, and sharding post metadata.

## Tips

- State assumptions first ("let's say 100M DAU"), and let the interviewer correct them.
- Use powers of 10 and round each step.
- Write the units down (per day vs per second is the most common slip).
- Do the estimate, say what it means ("that's ~1k writes/s, a single Postgres primary can handle it"), and move on.
