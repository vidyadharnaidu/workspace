# Load Balancers, API Gateways, Reverse Proxies, CDN, DNS

## Load balancer (LB)

Spreads traffic across servers, removes unhealthy ones, and gives a single entry point.

| Layer | Looks at | Pros | Examples |
|---|---|---|---|
| **L4** (transport) | IP + port | Very fast, protocol-agnostic | AWS NLB, HAProxy (TCP mode) |
| **L7** (application) | HTTP path, headers, cookies | Path routing, TLS termination, sticky sessions, retries | AWS ALB, NGINX, Envoy |

**Algorithms**

| Algorithm | Use when |
|---|---|
| Round robin / weighted round robin | Servers are similar / have different sizes |
| **Least connections** | Requests have very different durations (WebSockets, uploads) |
| Least response time | Latency-sensitive traffic |
| **IP hash / consistent hashing** | Need the same client → same server (cache locality, sticky sessions) |

**Remember**
- **Health checks** (active pings plus passive error counting) take bad instances out of rotation.
- **Sticky sessions** are a smell. Prefer stateless servers with the session in Redis or a JWT.
- **The LB itself must not be a SPOF:** run an active-passive pair (floating IP) or use a managed LB, with DNS pointing to multiple LBs.
- **TLS termination at the LB** saves app CPU. Re-encrypt to the backend if required.

## API gateway

A specialized L7 proxy in front of microservices. It handles:

- Authentication/authorization (JWT validation, API keys)
- **Rate limiting** and quotas
- Routing and API versioning, request/response transformation
- Aggregation (one client call → several services; the BFF pattern, "backend for frontend")
- Logging, metrics, tracing headers

Examples: Kong, AWS API Gateway, Apigee, Envoy/Istio ingress.

## Reverse proxy vs forward proxy

- **Reverse proxy** sits in front of *servers* (clients don't know which server answered): NGINX, Envoy.
- **Forward proxy** sits in front of *clients* (servers don't know the real client): corporate proxies, VPN-like egress.

## CDN (content delivery network)

Edge servers near users cache content.

| | Pull CDN | Push CDN |
|---|---|---|
| How | Edge fetches from origin on first request, then caches | You upload content to the CDN ahead of time |
| Good for | Most websites, lots of content | Big, rarely changing files (installers, video libraries) |

**Remember**
- **What to cache:** static assets, images, video segments, and even API responses with short TTLs.
- **Cache busting:** versioned file names (`app.3f9a1c.js`) instead of waiting for the TTL to expire.
- **Invalidation** is slow and costly, so prefer versioned URLs.
- **Signed URLs / tokens** protect private content (paid videos).
- CDNs also absorb DDoS attacks and terminate TLS close to the user.

## DNS

- Resolves a name to an IP. Responses are cached by TTL at every layer, so changes propagate slowly.
- **GeoDNS / latency-based routing** sends users to the nearest region.
- **DNS failover** via health checks, but clients cache records, so use low TTLs (30–60 s) for failover records.

## Interview Q&A

**Q: How do you handle a server dying mid-request?**
The LB health check removes it. The client or LB retries idempotent requests on another server. Use connection draining when deploying.

**Q: L4 or L7 for a WebSocket chat service?**
Either works. L7 can route by path and understands the HTTP upgrade. Use **least-connections** because connections are long-lived. You can also use L4 for raw performance.

**Q: How do you do a zero-downtime deploy?**
Rolling or blue-green behind the LB: drain connections, deploy, health check, shift traffic. Use a canary with a small traffic percentage first, and keep DB migrations backward compatible (expand → migrate → contract).

**Q: How does a global app route users to the closest region?**
GeoDNS or anycast IPs, with a CDN for static content and regional LBs per region.
