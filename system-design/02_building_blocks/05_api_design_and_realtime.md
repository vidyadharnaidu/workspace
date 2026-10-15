# API Design & Real-Time Communication

## REST vs gRPC vs GraphQL

| | REST ⭐ | gRPC | GraphQL |
|---|---|---|---|
| Format | JSON over HTTP | Protobuf (binary) over HTTP/2 | JSON; client specifies fields |
| Best for | Public APIs, CRUD, browsers | **Internal service-to-service**, low latency, streaming | Many clients needing different shapes of data (mobile vs web) |
| Pros | Simple, cacheable (HTTP caching), universal | Fast, typed contracts, bi-directional streaming | No over/under-fetching, one endpoint |
| Cons | Over/under-fetching, many round trips | Not browser-native, harder to debug | Caching is harder, N+1 queries on the server, query cost limits needed |

## REST design checklist

```
GET    /v1/users/{id}                  read
GET    /v1/users/{id}/orders?status=paid&limit=20&cursor=abc   list + filter + paginate
POST   /v1/orders                      create      (Idempotency-Key header!)
PUT    /v1/orders/{id}                 replace     (idempotent)
PATCH  /v1/orders/{id}                 partial update
DELETE /v1/orders/{id}                 delete      (idempotent)
```

- **Nouns, not verbs.** Plural resources, nested only one level.
- **Status codes:**
  - 200 OK, 201 Created, 204 No Content
  - 400 bad input, 401 not authenticated, 403 not allowed, 404 not found, 409 conflict
  - 422 validation, **429 rate limited**, 500 server error, 503 unavailable
- **Versioning:** `/v1/` in the path (simple) or a header.
- **Idempotency:** GET/PUT/DELETE are idempotent by definition. Make POST safe to retry with an `Idempotency-Key` header: the server stores key → response.
- **Errors:** a consistent body such as `{ "error": { "code": "CARD_DECLINED", "message": "...", "requestId": "..." } }`.

### Pagination

| Type | How | Pros | Cons |
|---|---|---|---|
| Offset | `?page=3&size=20` → `OFFSET 40 LIMIT 20` | Simple, jump to page N | Slow for deep pages; duplicates/skips when data changes |
| **Cursor / keyset** ⭐ | `?cursor=<last_id>&limit=20` → `WHERE id < last_id ORDER BY id DESC LIMIT 20` | Fast at any depth, stable under inserts | No "jump to page 57" |

Feeds, timelines and chat history should **always** use cursor pagination.

## Authentication & authorization (short)

- **Session cookie** (state in the server/Redis) vs **JWT** (signed and stateless, but hard to revoke, so keep it short-lived and use refresh tokens).
- **OAuth 2.0 / OIDC** for "Login with Google" and third-party access. Service-to-service: mTLS or signed tokens.
- **Authorization:** RBAC (roles) or ABAC (attributes). Check on the server, always.

## Real-time options

| Technique | Direction | How it works | Use for |
|---|---|---|---|
| Short polling | Client → server repeatedly | Request every N seconds | Rare updates, simplest |
| **Long polling** | Client waits | Server holds the request until data or timeout, then the client re-requests | Notifications when WebSockets aren't possible |
| **Server-Sent Events (SSE)** | Server → client | One long HTTP response streaming events; auto-reconnect | Live scores, stock tickers, AI token streaming, notifications |
| **WebSockets** ⭐ | Both ways | Upgraded persistent TCP connection | Chat, multiplayer games, collaborative editing, live location |
| Webhooks | Server → *another server* | HTTP callback when an event happens | Payment provider notifies you, GitHub events |

### Scaling WebSockets

```
Clients ==WS==> [ LB (least connections) ] ==> [ Gateway 1 ] [ Gateway 2 ] [ Gateway 3 ]   (hold connections only)
                                                     |            |            |
                                   connection registry in Redis: user_id -> gateway_id
                                                     |
                                        Pub/Sub (Redis/Kafka) to route messages to the right gateway
```

- Gateways are "dumb" connection holders. Business logic lives in stateless services behind them.
- A **registry** maps user → gateway so any service can deliver a message.
- **Heartbeats** detect dead connections, and presence (online/offline) comes from them.
- When a gateway dies, clients reconnect (with exponential backoff + jitter) and **sync missed messages** using their last-seen message id.

## Webhooks done right (as sender and receiver)

- **Sender:** sign payloads (HMAC), retry with backoff, send an event id, and let receivers fetch the latest state.
- **Receiver:** verify the signature, respond 2xx fast, process async, **dedupe by event id** (at-least-once delivery), and don't trust the order.

## Interview Q&A

**Q: Design the API for "like a post".**
`PUT /v1/posts/{id}/likes/me` (idempotent: liking twice is still one like) and `DELETE /v1/posts/{id}/likes/me`. Counts are served from a cache and updated asynchronously.

**Q: How do you stream ChatGPT-style responses?**
SSE (or WebSocket) streaming tokens as they are generated. Clients render incrementally.

**Q: Why use cursor pagination for a feed?**
New posts arrive constantly. With offsets, page 2 would repeat or skip items, and deep offsets are slow.
