# Message Queues, Pub/Sub & Event Streams

## Why add a queue?

1. **Decouple:** the producer doesn't need the consumer to be up.
2. **Absorb spikes:** buffer bursts and let workers process at their own pace (load leveling).
3. **Async slow work:** respond fast, and do emails, encoding and ML later.
4. **Fan-out:** one event feeds many independent consumers.
5. **Retry & durability:** failed work isn't lost.

## Queue vs log/stream

| | Message queue | Event log / stream |
|---|---|---|
| Model | Message is **removed** when acked by one consumer | Messages are **retained**; consumers track their own **offset** |
| Replay | No | ✅ Yes, rewind the offset |
| Ordering | Per queue (FIFO queues) | **Per partition** |
| Scale consumers | Competing consumers on one queue | Consumer group: one consumer per partition |
| Examples | RabbitMQ, AWS SQS, ActiveMQ | **Kafka**, AWS Kinesis, Pulsar, Redpanda |
| Use for | Task queues, jobs, work distribution | Event sourcing, analytics pipelines, CDC, activity feeds, many consumers of the same events |

## Kafka in 60 seconds

```
Producer --(key)--> Topic "orders"
                    ├── Partition 0: [m0][m1][m2][m3] ...  --> Consumer A (group "billing")
                    ├── Partition 1: [m0][m1][m2]     ...  --> Consumer B (group "billing")
                    └── Partition 2: [m0][m1]         ...  --> Consumer C (group "billing")
                                        \------------------> group "analytics" reads the same data independently
```

- **Topic** → **partitions**, which are the unit of parallelism and ordering.
- **Same key → same partition**, which preserves order per key (e.g. all events of `order_id=42` in order).
- **Consumer group:** each partition is read by one consumer in the group, so consumers ≤ partitions for parallelism.
- **Replication factor 3**, `acks=all`, and `min.insync.replicas=2` give durability.
- **Retention** by time or size lets new consumers read history.

## Delivery semantics (recap)

- **At-least-once** (typical): process, **then** commit the offset / ack. A crash in between means reprocessing, so the **consumer must be idempotent**.
- At-most-once: commit first, then process. A crash means the message is lost.
- "Exactly-once": Kafka transactions for Kafka→Kafka flows. With external side effects, use idempotency keys.

## Patterns you should name

| Pattern | What it solves | How |
|---|---|---|
| **Competing consumers** | Scale workers | N workers pull from one queue |
| **Pub/Sub fan-out** | One event, many reactions | Topic → many subscriptions (SNS → SQS queues, Kafka consumer groups) |
| **Dead-letter queue (DLQ)** | Poison messages that always fail | After N retries, move to DLQ; alert; inspect/replay |
| **Retry with exponential backoff + jitter** | Transient failures | Delay queues / retry topics: 1s, 2s, 4s… plus randomness |
| **Transactional outbox** ⭐ | "Update DB **and** publish event" atomically | Write the event to an `outbox` table in the **same DB transaction**; a relay publishes it to Kafka |
| **Change data capture (CDC)** | Stream DB changes to others (search, cache, analytics) | Read the DB's WAL/binlog (Debezium) |
| **Saga** | Business transaction across services without 2PC | Sequence of local transactions + **compensating actions** on failure (orchestration or choreography) |
| **Event sourcing** | Full history / audit | Store events as the source of truth; build current state by replaying (+ snapshots) |
| **CQRS** | Different models for reads and writes | Write model emits events → build read-optimized views |
| **Priority queues** | Urgent work first | Separate queues per priority (OTP SMS ≠ marketing email) |
| **Back-pressure** | Consumers overwhelmed | Bounded queues, pause producers, auto-scale consumers on lag |

### Saga example (order checkout)

```
Order Service: create order (PENDING)
   -> Payment Service: charge card        (compensate: refund)
   -> Inventory Service: reserve items    (compensate: release items)
   -> Shipping Service: create shipment   (compensate: cancel shipment)
If any step fails -> run compensations for the completed steps in reverse order -> order CANCELLED
```

## Ordering tips

- Global ordering doesn't scale. Ask *"ordered per what?"* (per user, per conversation, per account).
- Use that entity id as the **partition key**.
- Consumers may still see duplicates and retries, so include a **sequence number/version** and ignore stale updates.

## Interview Q&A

**Q: RabbitMQ/SQS or Kafka?**
Task distribution with per-message acks, delays and simple ops → SQS/RabbitMQ. High-throughput event streams, replay, multiple independent consumers, ordering per key → Kafka.

**Q: A consumer is falling behind (lag growing). What do you do?**
Scale consumers (up to the partition count), add partitions, speed up processing (batching, fewer sync calls), move slow parts to another stage, and check for a poison message causing retries.

**Q: How do you avoid processing a payment event twice?**
The consumer stores processed event ids (or uses the payment's idempotency key) with a unique constraint, in the same transaction as the side effect. A duplicate is then a no-op.

**Q: Why not publish to Kafka right after committing the DB transaction?**
The app can crash between the two steps, so the DB and the events disagree (a lost event). Use the **outbox pattern** or CDC.
