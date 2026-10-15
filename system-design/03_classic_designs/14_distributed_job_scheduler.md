# Design a Distributed Job Scheduler (cron at scale / delayed jobs)

> **Remember:** *"Jobs stored in a DB indexed by next_run_time; scheduler nodes claim due jobs safely (SELECT … FOR UPDATE SKIP LOCKED or partition ownership), push them to a queue, workers run them under a lease with heartbeats, at-least-once + idempotent jobs, retries with backoff, DLQ."*

## 1. Requirements

**Functional**
- Submit jobs: run once at a time T, run after a delay, or recurring (cron expression)
- Execute the job's task (HTTP callback, container/command, message to a queue)
- Retries with a policy, timeouts, cancel/pause, view status and history

**Non-functional**
- Jobs run **on time** (within seconds) and **not lost**; at-least-once execution
- Avoid running the same job instance twice concurrently
- Scale to millions of scheduled jobs and thousands of executions/s; no single point of failure

## 2. Estimates
- 100M scheduled jobs stored; 10k executions/s at peak (e.g. midnight cron spikes)
- Each job row ~1 KB → ~100 GB of metadata (sharded SQL or a KV store with a time index)

## 3. API
```
POST   /v1/jobs     { name, schedule: "0 2 * * *" | runAt | delaySeconds, target: {type:"http", url, payload},
                      retryPolicy: {maxAttempts: 5, backoff: "exponential"}, timeoutSeconds, idempotencyKey }
GET    /v1/jobs/{id}                 ;  GET /v1/jobs/{id}/runs?cursor=
POST   /v1/jobs/{id}/pause | resume  ;  DELETE /v1/jobs/{id}
```

## 4. Data model
```
jobs      (job_id, owner, schedule, target, retry_policy, timeout, status: ACTIVE|PAUSED|DELETED,
           next_run_time, partition_id)          INDEX (partition_id, next_run_time) WHERE status='ACTIVE'
job_runs  (run_id, job_id, scheduled_time, attempt, status: QUEUED|RUNNING|SUCCEEDED|FAILED|TIMED_OUT,
           worker_id, lease_expires_at, started_at, finished_at, error)
           UNIQUE (job_id, scheduled_time, attempt)          -- prevents duplicate run creation
```

## 5. High-level design

```
 Clients -> [Job API] -> [Jobs DB (sharded by partition_id)]
                                  ^
                                  | every ~1s: claim due jobs for MY partitions
                     [Scheduler nodes x N]  (partition ownership via etcd/ZooKeeper leases)
                                  |  for each due job (in a transaction):
                                  |    insert job_run(QUEUED), compute next_run_time from cron, update job
                                  v
                          [Queue: Kafka/SQS]  (priority / per-tenant topics)
                                  |
                                  v
                          [Worker pool x M]  -- lease + heartbeat --> job_runs(RUNNING, lease_expires_at)
                                  |  execute target (HTTP call / container) with timeout
                                  |  success -> SUCCEEDED ; failure -> retry with backoff (delayed re-enqueue) -> DLQ after max
                                  v
                          [Lease reaper] : runs whose lease expired (worker died) -> re-enqueue attempt+1
 [Metrics/alerts]: schedule lag (now - scheduled_time), failure rate, DLQ size
```

## 6. Deep dives

### Claiming due jobs safely (no double scheduling)
- **Option 1: DB row locking** ⭐ (simple, works well to ~thousands/s per shard):
```sql
SELECT job_id FROM jobs
WHERE partition_id = ANY(:my_partitions) AND status = 'ACTIVE' AND next_run_time <= now()
ORDER BY next_run_time
LIMIT 500
FOR UPDATE SKIP LOCKED;      -- concurrent schedulers skip rows another one is handling
```
- **Option 2: partition ownership.** Split jobs into P partitions. A coordinator (etcd lease) assigns each partition to exactly one scheduler node. On node failure, the lease expires and the partition is reassigned.
- Creating the `job_run` has a UNIQUE (job_id, scheduled_time) constraint, so even a duplicate claim can't create two runs.

### Execution guarantees
- **At-least-once:** a worker may crash after doing the work but before marking success, so the lease expires and the job runs again. Job targets **must be idempotent** (pass `run_id` / idempotency key to the target).
- **Leases + heartbeats:** workers extend `lease_expires_at` while running. If heartbeats stop, the reaper re-queues the run.
- **Fencing:** a stale worker whose lease expired must not mark the newer attempt's result. Check `attempt`/lease token on update.

### Timing precision & spikes
- Polling every 1 s gives ~1 s precision. For sub-second precision, load the next few seconds of jobs into an in-memory **timing wheel / delay queue** per scheduler node.
- "Everyone uses `0 0 * * *`" → spike at midnight. Add jitter where allowed, pre-scale workers, and use per-tenant quotas so one tenant can't starve others.

### Recurring jobs
- After claiming, compute the next occurrence from the cron expression (in the job's timezone, DST-aware) and update `next_run_time` in the same transaction.
- Missed runs after an outage follow a **misfire policy** per job: run once now, run all missed, or skip.

## 7. Trade-offs
- DB polling (simple, strong guarantees) vs a pure queue with delayed messages (SQS delay max 15 min, Kafka has no native delays) vs a time-bucketed store for very large scale.
- Exactly-once isn't achievable end to end. At-least-once + idempotency is the standard answer.
- A central scheduler with leader election (simple, one active) vs partitioned schedulers (scales horizontally).

## 8. Follow-up questions
- **Job dependencies (run B after A)?** Workflow engine / DAG: on A success, enqueue B (Airflow, Temporal, Step Functions).
- **Long-running jobs (hours)?** Checkpointing + heartbeats, workers on autoscaling containers, cancel via a flag checked by the job.
- **Prevent overlapping runs of the same recurring job?** A per-job concurrency limit: skip/queue a new run if a RUNNING run exists (checked atomically).
