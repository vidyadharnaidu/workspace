# Reliability, Observability & Security

## Reliability patterns

| Pattern | Problem | Idea | Remember |
|---|---|---|---|
| **Timeouts** | A slow dependency ties up all threads | Every network call has a deadline | No timeout = eventual outage |
| **Retries + exponential backoff + jitter** | Transient errors | Retry after 100ms, 200ms, 400ms… ± random | Retry **idempotent** operations only; cap attempts; retry budgets prevent retry storms |
| **Circuit breaker** | Hammering a dead service makes things worse | CLOSED → (too many failures) → OPEN (fail fast) → wait → HALF_OPEN (try a few) → CLOSED or OPEN | See `04_low_level_design/CircuitBreaker.java` |
| **Bulkhead** | One slow dependency exhausts shared resources | Separate thread/connection pools per dependency | Like watertight compartments in a ship |
| **Rate limiting / load shedding** | Overload | Reject excess early (429/503), prioritize important traffic | Better to serve 80% than crash for 100% |
| **Fallback / graceful degradation** | Dependency down | Cached/default response (e.g. non-personalized recommendations) | Decide per feature |
| **Idempotency keys** | Retries causing duplicates | Store key → result; repeat returns the stored result | Essential for payments and orders |
| **Health checks** | Route away from bad instances | Liveness (restart me) vs readiness (don't send traffic yet) | Kubernetes probes |
| **Redundancy across failure domains** | Hardware/zone/region failure | N+1 or N+2 capacity across availability zones; multi-region for critical systems | Test failover regularly (chaos engineering) |
| **Graceful shutdown / draining** | Deploys drop requests | Stop accepting → finish in-flight → exit | |

### Disaster recovery vocabulary

- **RPO (Recovery Point Objective):** how much data you can lose, e.g. 5 minutes. This drives backup/replication frequency.
- **RTO (Recovery Time Objective):** how long you can be down, e.g. 1 hour. This drives the failover setup.
- **Strategies (cheap → expensive):** backup & restore → pilot light → warm standby → active-active multi-region.

## Observability: the 3 pillars

| Pillar | What | Tools |
|---|---|---|
| **Metrics** | Numbers over time: QPS, error rate, latency p50/p99, CPU, queue lag | Prometheus + Grafana, Datadog, CloudWatch |
| **Logs** | Structured (JSON) events with a request id | ELK/OpenSearch, Loki, Splunk |
| **Traces** | One request's path across services with timings | OpenTelemetry, Jaeger, Zipkin |

**What to monitor (memorize these acronyms)**
- **RED** for services: **R**ate, **E**rrors, **D**uration.
- **USE** for resources: **U**tilization, **S**aturation, **E**rrors.
- **Golden signals** (Google SRE): latency, traffic, errors, saturation.
- **SLI → SLO → SLA:** the measured indicator (p99 latency) → the internal target (p99 < 300ms for 99.9% of minutes) → the contract with customers (credits if broken). **Error budget** = 100% − SLO.
- **Alert on symptoms** users feel (error rate, latency), not every CPU blip. Link each alert to a runbook.

## Security checklist for designs

- **In transit:** TLS everywhere (mTLS between services in zero-trust setups).
- **At rest:** encryption (KMS-managed keys); separate secrets management (Vault, AWS Secrets Manager). Never put secrets in code.
- **AuthN/AuthZ:** OAuth2/OIDC, short-lived tokens, least privilege, server-side authorization checks on every request.
- **Input:** validate everything; parameterized SQL (no injection); output encoding (no XSS); CSRF tokens for cookie sessions.
- **Abuse:** rate limiting, CAPTCHA, WAF, DDoS protection at the CDN/edge.
- **Sensitive data:** PII minimization, tokenization for card data (PCI DSS: never store raw card numbers), masking in logs, GDPR delete/export.
- **Audit logs** for sensitive actions (append-only).
- **File uploads:** size/type limits, virus scanning, pre-signed URLs, serve from a separate domain.

## Interview Q&A

**Q: A downstream service is slow and your service starts timing out everywhere. Why, and what's the fix?**
Threads block waiting on the slow calls, the pool gets exhausted, and every endpoint fails (cascading failure). Fixes: shorter timeouts, a circuit breaker, a bulkhead (separate pool), a fallback response, and load shedding.

**Q: Why add jitter to retries?**
Without it, all clients retry at the same instants, causing synchronized waves (thundering herd) that keep the service down.

**Q: How would you know your system is healthy after a deploy?**
Canary release with automated comparison of RED metrics against the baseline, plus alerts on SLO burn rate and a quick rollback path.
