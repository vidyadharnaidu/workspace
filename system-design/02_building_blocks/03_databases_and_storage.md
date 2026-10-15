# Databases & Storage

## Choosing a database: decide by access pattern, not hype

| Need | Pick | Examples |
|---|---|---|
| Transactions, joins, constraints, flexible queries, money | **Relational (SQL)** | PostgreSQL, MySQL, Aurora, Spanner/CockroachDB (distributed SQL) |
| Simple get/put by key, massive scale, low latency | **Key-value** | DynamoDB, Redis, RocksDB |
| Huge write volume, time-ordered data per partition (messages, events, IoT) | **Wide-column** | Cassandra, ScyllaDB, HBase, Bigtable |
| Flexible JSON documents, nested data | **Document** | MongoDB, Couchbase, Firestore |
| Relationships are the query (friends of friends, fraud rings) | **Graph** | Neo4j, Neptune |
| Full-text search, fuzzy match, faceting | **Search index** | Elasticsearch, OpenSearch, Solr |
| Metrics, monitoring, time buckets | **Time-series** | Prometheus, InfluxDB, TimescaleDB |
| Analytics over billions of rows | **Columnar / warehouse** | BigQuery, Snowflake, Redshift, ClickHouse |
| Files, images, video, backups | **Object storage** | S3, GCS, Azure Blob |

> **Safe default:** PostgreSQL for core data, Redis for cache, S3 for blobs, Kafka for events, and Elasticsearch if search is needed. Add specialized stores only when a requirement demands them.

## SQL vs NoSQL: the one-minute answer

| | SQL | NoSQL |
|---|---|---|
| Schema | Fixed, enforced | Flexible |
| Transactions | ACID, multi-row | Often single-item/partition (many now offer more) |
| Queries | Ad-hoc, joins | Designed around known access patterns |
| Scaling | Vertical first; read replicas; sharding is manual/hard | Built for horizontal partitioning |
| Consistency | Strong | Often tunable/eventual |

## Replication

```
            writes                     async/sync replication
 App ------------------> [ Primary ] ----------------------------> [ Replica 1 ]  <--- reads
   \                                  \--------------------------> [ Replica 2 ]  <--- reads
    `-------------------------------------------- reads -----------------^
```

- **Read replicas** scale reads. **Replication lag** means stale reads, so send "read your own write" requests to the primary for a short time after the write.
- **Failover:** promote a replica (automated by Patroni, RDS Multi-AZ). Async replication can lose the last few writes.
- **Multi-region:** a primary in one region with cross-region replicas, or a globally distributed DB (Spanner, CockroachDB, DynamoDB Global Tables).

## Sharding (partitioning writes)

- **Shard key choice decides everything.** It needs high cardinality, an even spread, and must be used by most queries.
  - Chat messages → `conversation_id`. Orders → `customer_id`. Multi-tenant SaaS → `tenant_id`.
- **Consistent hashing** means adding a shard moves only ~1/N of keys (see `04_low_level_design/ConsistentHashing.java`).
- **Problems it creates:**
  - Cross-shard joins → denormalize or join in the app.
  - Cross-shard transactions → sagas/2PC (avoid).
  - Global secondary indexes → maintain a separate index table or a search index.
  - Resharding → consistent hashing, or pre-split into many logical shards mapped onto fewer physical nodes.
  - Hot shards → better key, key salting, splitting the shard.

## Indexes (see also db/13_concepts_performance)

- **B-tree** (default): equality + range + sort. **Hash:** equality only. **GIN/inverted:** full text, arrays, JSON. **Geo (R-tree, GiST):** location.
- **Composite index** column order: equality columns first, then range/sort (the leftmost prefix rule).
- Indexes speed up reads and slow down writes. Don't index everything.

## Storage engines

| | B-tree (Postgres, MySQL InnoDB) | LSM tree (Cassandra, RocksDB, LevelDB) |
|---|---|---|
| Write | Update pages in place (+ WAL) | Append to WAL + in-memory memtable → flush sorted SSTables → background **compaction** |
| Read | Fast, predictable | May check several SSTables (Bloom filters help) |
| Best for | Read-heavy, mixed OLTP | **Write-heavy** (logs, messages, metrics) |

## Object storage (S3-style)

- Flat namespace of `bucket/key → bytes + metadata`. Practically infinite, very durable (11 nines), cheap. It's **not** a filesystem and **not** for low-latency small random updates.
- **Upload pattern:** the client asks the API for a **pre-signed URL** and uploads directly to S3, so app servers never proxy big files.
- Large files use **multipart upload** (parallel, resumable).
- **Storage tiers:** hot (standard) → infrequent access → archive (Glacier). Use lifecycle rules.
- Serve via **CDN** with signed URLs for private content.

## Data modeling tips for NoSQL (Cassandra/DynamoDB)

1. List the queries first, then design one table per query pattern (denormalize).
2. **Partition key** = what you look up by (spreads the data). **Sort/clustering key** = how rows are ordered within the partition (enables range queries).
   - Messages: partition `conversation_id`, clustering `message_id DESC` (time-sortable id).
3. Avoid unbounded partitions: add a time bucket (`conversation_id + month`).

## Interview Q&A

**Q: Your single Postgres primary is at 80% CPU. What now?**
1. Find slow queries (EXPLAIN) and add indexes.
2. Add a cache for hot reads.
3. Move reads to replicas.
4. Use connection pooling (PgBouncer).
5. Scale up vertically.
6. Archive old data or partition tables.
7. Shard by a good key as a last resort.

**Q: How do you do a schema migration on a huge table without downtime?**
Expand/contract: add the nullable column → deploy code writing both → backfill in batches → switch reads → remove the old column. Use online tools (gh-ost, pg_repack) for big rewrites.

**Q: How do you generate unique IDs across shards?**
- UUIDv4: no coordination, but random, so it hurts index locality.
- **UUIDv7 / ULID:** time-ordered.
- **Snowflake:** 64-bit = time + machine + sequence. Sortable and compact (see `04_low_level_design/SnowflakeIdGenerator.java`).
- DB ticket server / ID ranges per server.

**Q: How do you keep a search index in sync with the DB?**
Change data capture (Debezium → Kafka → indexer) or the transactional outbox pattern. Accept eventual consistency, and rebuild from the source of truth if needed.
