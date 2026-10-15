# The System Design Interview Framework

Use the same 7 steps for every question. Interviewers score the **process** as much as the final diagram.

> **Mnemonic: "R-E-A-D  H-D-W"**: **R**equirements, **E**stimates, **A**PI, **D**ata model, **H**igh-level design, **D**eep dives, **W**rap-up.

## Timeline for a 45-minute interview

| Step | Time | What you do | Output on the whiteboard |
|---|---|---|---|
| 1. Requirements | 5 min | Ask questions. Agree on 3–5 core features and the scale. | Functional / non-functional / out-of-scope lists |
| 2. Estimates | 3–5 min | Users → QPS → storage → bandwidth. Only what changes a decision. | 4–6 numbers |
| 3. API | 3–5 min | Main endpoints with params and responses | `POST /urls {longUrl} → {shortUrl}` |
| 4. Data model | 3–5 min | Entities, keys, **access patterns** → pick the database | Tables / key design |
| 5. High-level design | 10 min | Boxes and arrows. Walk one request end to end. | Client → LB → services → cache / DB / queue / storage |
| 6. Deep dives | 10–15 min | The 2–3 hardest parts (let the interviewer choose) | Scaling, consistency, failure handling |
| 7. Wrap-up | 2–3 min | Bottlenecks, trade-offs, monitoring, what you'd do next | Bullet list |

## Step 1: Requirements (never skip)

**Functional:** what can users do? List the core features only, and say what's out of scope.

**Non-functional:** the questions that change the architecture:

- **Scale:** DAU? Requests per second? Data size? Read-heavy or write-heavy?
- **Latency:** e.g. p99 < 200 ms for reads?
- **Availability vs consistency:** is stale data OK (feed, likes) or not (payments, seat booking)?
- **Durability:** can we ever lose data?
- **Geography:** one region or global?
- **Other:** security, privacy, compliance (PCI, GDPR)

> Say it out loud: *"Since a slightly stale feed is fine but losing a post isn't, I'll favor availability for reads and durability for writes."*

## Step 5: The default high-level diagram (start here, then adapt)

```
                 +-----+
 Client -------->| CDN |  (static files, images, video)
   |             +-----+
   v
+---------------+     +-----------------------------+
| Load Balancer |---->| API Gateway                 |  auth, rate limiting, routing
+---------------+     +-----------------------------+
                          |            |           |
                          v            v           v
                     +---------+  +---------+  +---------+
                     |Service A|  |Service B|  |Service C|   stateless, scale horizontally
                     +---------+  +---------+  +---------+
                       |    |        |             |
                       v    v        v             v
                  +-------+ +----+ +-------------+ +---------------+
                  | Cache | | DB | | Message     | | Object storage|
                  | Redis | |    | | queue/Kafka | | S3            |
                  +-------+ +----+ +-------------+ +---------------+
                                        |
                                        v
                                  +-----------+
                                  |  Workers  |  async jobs
                                  +-----------+
```

## Step 6: Deep-dive checklist (pick what fits)

- **Hot path:** what happens on the most frequent request? Can we cache it?
- **Scaling data:** replication for reads, sharding for writes. What's the shard key?
- **Hot spots:** celebrity users, viral content, hot partitions
- **Consistency:** transactions, idempotency, race conditions (double booking, double charge)
- **Async work:** what can move to a queue (emails, thumbnails, analytics)?
- **Failures:** what if a service, cache, DB node or whole region dies? Retries, timeouts, circuit breakers
- **Monitoring:** which metrics and alerts?

## The standard scaling path (useful for "how would this grow?")

1. **Single server:** app and DB on one box
2. **Split the DB** onto its own server
3. **Load balancer + many stateless app servers:** sessions move to Redis or a JWT
4. **Cache:** Redis for hot reads; **CDN** for static content
5. **Read replicas** for read-heavy load
6. **Message queue:** move slow or bursty work to async workers
7. **Sharding:** split writes across DB partitions
8. **Microservices:** split by domain when teams or scaling needs differ
9. **Multi-region:** geo-DNS, replicated data, failover

## How to talk about trade-offs

Use this sentence shape: *"Option A gives us X but costs Y. Because our requirement is Z, I'll choose A. If Z changed, I'd switch to B."*

## Common mistakes

| Mistake | Fix |
|---|---|
| Jumping straight to boxes | Spend the first 5 minutes on requirements |
| Naming tech with no reason ("use Kafka") | Say *why*: "we need replayable, ordered, high-throughput events" |
| Doing math that decides nothing | Estimate only what picks a design (e.g. "180 TB → must shard") |
| One giant box called "backend" | Split by responsibility; walk a request through it |
| Ignoring failures | For each component, ask "what if this dies?" |
| Over-engineering from the start | Start simple, then scale the bottleneck the numbers show |
| Going silent | Think out loud; check in: "Should I go deeper on X or Y?" |
