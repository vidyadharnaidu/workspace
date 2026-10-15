# Design a Distributed Key-Value Store (Dynamo / Cassandra style)

> **Remember:** *"Consistent hashing with virtual nodes for partitioning, N replicas with tunable R/W quorums, vector clocks or last-write-wins for conflicts, hinted handoff + Merkle-tree anti-entropy for repair, gossip for membership, and an LSM-tree storage engine."*

## 1. Requirements

**Functional**
- `put(key, value)`, `get(key)`, `delete(key)`; values up to ~1 MB

**Non-functional**
- Scales horizontally to petabytes, with add/remove nodes online
- **Highly available** for writes even during failures/partitions (an AP design; consistency tunable)
- Low latency (single-digit ms), durable

## 2. Core techniques (the interview is about these)

| Problem | Technique |
|---|---|
| Split data across nodes | **Consistent hashing** with **virtual nodes** |
| Survive node failure | **Replication** to N successor nodes on the ring (across racks/zones) |
| Tune consistency vs latency | **Quorum**: write to W, read from R, with R + W > N for overlap |
| Concurrent writes conflict | **Vector clocks** (detect) or **last-write-wins** timestamps (simple, may lose updates) |
| Temporary node failure | **Sloppy quorum + hinted handoff** |
| Permanent divergence | **Anti-entropy** with **Merkle trees** |
| Who is alive | **Gossip protocol** + phi-accrual failure detection |
| Fast writes on disk | **LSM tree**: commit log → memtable → SSTables + compaction + Bloom filters |

## 3. High-level design

```
            Client
              |
              v
     [ Any node = coordinator ]  (or client-side partition-aware driver)
              |
   hash(key) -> position on ring -> first N distinct physical nodes clockwise
              |
      +-------+--------+
      v       v        v
   [Node B] [Node C] [Node D]      write succeeds when W acks; read returns after R replies
      |
   each node: commit log (WAL) -> memtable -> flush -> SSTables (+ Bloom filter, index) -> compaction
```

```
Consistent hash ring with virtual nodes (each physical node appears many times):

        A1 ---- C2 ---- B1
       /                  \
     D2                    A2        key k -> hash -> walk clockwise -> A2 (primary), then next distinct
      |                     |        physical nodes for replicas: B?, C?
     C1                    D1
       \                  /
        B2 ---- D3 ---- C3
```
→ Code: `04_low_level_design/ConsistentHashing.java` (shows only ~1/N of keys move when a node is added).

## 4. Deep dives

### Quorums
- N = 3 is typical.
  - **W = 2, R = 2:** R + W > N, so a read overlaps the latest write. That's strong-ish consistency with one node allowed down.
  - **W = 1, R = 1:** fastest and most available, but reads may be stale (eventual).
  - **W = 3:** most durable, but a single slow node blocks writes.
- **Read repair:** if replicas return different versions during a read, write the newest back to stale replicas.

### Conflict resolution
- **Last-write-wins (LWW):** keep the highest timestamp. Simple, but clock skew can drop writes (Cassandra's approach).
- **Vector clocks:** `[(A,3),(B,1)]` per value. If neither clock dominates the other, the writes were concurrent, so keep both "siblings" and let the client merge them (Dynamo shopping cart). **CRDTs** merge automatically for counters and sets.

### Handling failures
- **Hinted handoff:** if a replica is down, another node accepts the write with a *hint* and forwards it when the replica returns (sloppy quorum keeps writes available).
- **Merkle trees:** each node builds a hash tree over its key ranges. Comparing roots and descending only into mismatching branches lets replicas sync differences with minimal data transfer.
- **Gossip:** nodes periodically exchange membership/heartbeat tables with random peers, so failures are detected in seconds without a central master.

### Storage engine (write path)
1. Append to the **commit log** (durability) → 2. update the **memtable** (sorted, in memory) → 3. ack.
4. When the memtable is full, flush to an immutable **SSTable** file.
5. **Read:** memtable → newest SSTables, skipping files whose **Bloom filter** says "not present".
6. **Compaction** merges SSTables and drops overwritten/deleted (tombstoned) data.

## 5. Trade-offs
- AP with tunable consistency (Dynamo/Cassandra) vs CP with consensus (etcd, Spanner-like with Raft per shard): choose by use case.
- Virtual nodes give a smoother load spread and faster rebalancing, at the cost of more metadata.
- LSM means great writes but read/space amplification, and compaction tuning matters.

## 6. Follow-up questions
- **How do you add a node?** It takes over virtual-node ranges and streams data for those ranges from their current owners. Only neighbors are affected.
- **Deletes?** Write a **tombstone** (a delete marker) replicated like a write, and purge it after a grace period (otherwise a stale replica could "resurrect" the data).
- **Hot keys?** Client-side cache, key splitting, or a dedicated cache tier.
- **Range queries?** Hash partitioning can't do them efficiently. Use range partitioning (with split/merge of ranges, like Bigtable/HBase) or a clustering key within a partition (Cassandra).
