# Microservices Design Patterns — Spring Boot 3 / Spring Cloud

Teaching snippets, one folder per pattern. Each file starts with **PROBLEM → SOLUTION → TRICK TO REMEMBER**,
so you can read the header alone and still answer the interview question.

These are *not* a runnable app — they reference real Spring beans but have no `main` wiring.
`pom.xml` lists every dependency they assume. To actually run one, paste it into a project from
[start.spring.io](https://start.spring.io) with the matching starters.

| # | Pattern | File | The one line to remember |
|---|---|---|---|
| 01 | API Gateway | `01_api_gateway/` | One entry point owns routing + cross-cutting. Business logic there = distributed monolith |
| 02 | Service Discovery | `02_service_discovery/` | Client-side (Eureka): caller picks. Server-side (k8s DNS): a proxy picks. On k8s you rarely need Eureka |
| 03 | Externalized Config | `03_config_server/` | URL is `/{application}/{profile}/{label}`; `@RefreshScope` + `/actuator/refresh` = no restart |
| 04 | Declarative client + LB | `04_feign_load_balancing/` | `lb://NAME` resolves at call time, so scaling needs no redeploy |
| 05 | Circuit breaker & friends | `05_resilience_circuit_breaker/` | CLOSED → OPEN (fail instantly, no network hop) → HALF_OPEN (probe). Slow calls count as failures |
| 06 | Saga — orchestration | `06_saga_orchestration/` | Local transactions + compensations in reverse. A semantic undo (refund), never a rollback |
| 07 | Saga — choreography | `07_saga_choreography/` | Nobody is in charge; services react to events. Loose coupling, no single view of the flow |
| 08 | CQRS | `08_cqrs/` | Two models, often two databases. The read side is eventually consistent — the UI must cope |
| 09 | Event Sourcing | `09_event_sourcing/` | Events are the truth; state is a replay. Append a correction, never edit history. Snapshot every N |
| 10 | Transactional Outbox | `10_transactional_outbox/` | Never write to DB **and** broker in one transaction. Write one row; let a relay forward it |
| 11 | Idempotency | `11_idempotency/` | The client generates the key. The unique index *is* the lock — don't check-then-insert |
| 12 | Observability | `12_observability_tracing/` | Trace = whole request, span = one unit. Liveness ≠ readiness: never probe a dependency in liveness |
| 13 | BFF + API composition | `13_bff_aggregator/` | Fan out in **parallel**, and degrade on optional dependencies instead of failing the page |
| 14 | Token-based security | `14_security_jwt/` | Gateway does coarse checks, each service does ownership. Short-lived tokens — JWTs can't be un-issued |
| 15 | Strangler Fig | `15_strangler_fig/` | Facade first, then move one capability at a time. The hard part is the shared database |

## How they fit together

```
                 ┌──────────────┐
   mobile ─────▶ │  BFF (13)    │──┐
   web    ─────▶ │  Gateway (01)│  │  lb://  ┌──────────────┐
                 └──────┬───────┘  ├────────▶│ Discovery(02)│
                        │ JWT (14) │         └──────────────┘
                        ▼          │
        ┌───────────────────────────────────────┐
        │ order-service    payment-service ...  │◀── Config server (03)
        │  Feign (04) + Resilience4j (05)       │
        │  own DB  ──▶ outbox (10) ──▶ Kafka    │
        └───────────────────────────────────────┘
                  │ events                 │
                  ▼                        ▼
            Saga (06/07)            Projections → CQRS read model (08)
                  │
        every consumer is idempotent (11), everything is traced (12)
```

## The decisions interviewers actually probe

| Question | The answer that lands |
|---|---|
| Orchestration or choreography? | 2–4 steps → choreography. More steps, branching, or you need one place to debug → orchestration |
| How do you keep DB and Kafka in sync? | You don't do both in one transaction. Outbox table + relay (or Debezium CDC) |
| Exactly-once delivery? | Doesn't exist end to end. At-least-once delivery + idempotent consumers = exactly-once *effect* |
| Why not a distributed transaction (2PC)? | It locks across services, needs every participant up, and doesn't scale. Sagas trade atomicity for availability |
| Database per service — but I need a join | Compose in the BFF (13), or build a read model from events (08). Never reach into another service's DB |
| Retry is safe, right? | Only for idempotent operations. Retrying a charge without an idempotency key charges twice |
| Circuit breaker on a 400? | No. Business rejections must be in `ignore-exceptions` or the breaker trips on valid "no" answers |
| How much do you trace? | 1–10% sampling in prod; 100% costs more than the service |
| Where do I start breaking up the monolith? | Least coupled, highest value first (notifications, search). Facade before extraction |

## Anti-patterns worth naming out loud

- **Distributed monolith** — services that must deploy together. You paid the network cost and kept the coupling.
- **Shared database** — two services writing the same table. Now neither can change its schema.
- **Chatty services** — one request causing 30 downstream calls. Aggregate, cache, or redraw the boundary.
- **Nano-services** — a service per entity. The coordination costs more than the code it holds.
- **No correlation id** — debugging becomes archaeology across six log files.
- **Synchronous chains** — A→B→C→D over HTTP. Availability multiplies: four 99.9% services ≈ 99.6%.
