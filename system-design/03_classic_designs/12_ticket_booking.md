# Design a Ticket Booking System (Ticketmaster / BookMyShow / airline seats)

> **Remember:** *"Browse from cache/search; to buy, place a short-lived HOLD on seats with an atomic conditional update (or Redis SET NX EX), pay within the TTL, then confirm; expired holds are released; a virtual waiting room protects hot on-sales."*

## 1. Requirements

**Functional**
- Browse/search events, view the seat map with availability
- Select seats → hold them for ~10 minutes → pay → booking confirmed (ticket issued)
- Cancel/refund per policy

**Non-functional**
- **No double booking** (strong consistency for seat inventory)
- Handle massive spikes at on-sale time (millions of users for one concert), fairly
- Browsing is read-heavy and can be slightly stale; booking must be correct

## 2. Estimates
- A popular on-sale: 10M users arrive for 50k seats in minutes, so request spikes reach **hundreds of thousands/s** to the seat map but only 50k successful bookings. This is a demand-shaping problem.
- Normal day: ~1M bookings/day → ~12/s (easy)

## 3. API
```
GET  /v1/events?city=&date=&q=                  (search: Elasticsearch)
GET  /v1/events/{id}/seats                      -> seat map + status (cached, short TTL)
POST /v1/events/{id}/holds   { seatIds[] }      -> 201 { holdId, expiresAt }   | 409 seats unavailable
POST /v1/bookings            { holdId, paymentToken } (Idempotency-Key) -> { bookingId, status }
DELETE /v1/holds/{holdId}
```

## 4. Data model (SQL for inventory)
```
events    (event_id, venue_id, name, start_time, on_sale_at, status)
seats     (event_id, seat_id, section, row, number, price_tier,
           status: AVAILABLE|HELD|BOOKED, hold_id, hold_expires_at, booking_id, version)
           PK (event_id, seat_id)     -- one row per seat per event; shard by event_id
holds     (hold_id, user_id, event_id, seat_ids, expires_at, status)
bookings  (booking_id, user_id, event_id, seat_ids, payment_id, status, created_at)
```

## 5. High-level design

```
 Users --> [CDN: static event pages, seat map images]
   |
   v
 [Waiting Room / Queue Service]  (only for hot on-sales: admits N users/min with signed tokens)
   |
   v
 [API Gateway: auth, rate limit per user/IP, bot protection]
   |                 |                                   |
   v                 v                                   v
 [Search Svc]   [Inventory / Seat Svc] --read--> [Seat map cache (Redis)]
 (Elasticsearch)       |  hold/confirm (atomic)
                       v
                 [Inventory DB (SQL, sharded by event_id)]
                       ^
                       |  confirm
                 [Booking Svc] --> [Payment Svc / PSP] ; outbox -> [Kafka] -> tickets, emails, analytics
 [Hold Expiry worker]: releases HELD seats where hold_expires_at < now (plus lazy check on read)
```

## 6. Deep dives

### Holding seats without double booking (the key question)
**Option A: atomic conditional update in SQL** ⭐
```sql
UPDATE seats
SET status = 'HELD', hold_id = :hold, hold_expires_at = now() + interval '10 minutes'
WHERE event_id = :event AND seat_id IN (:seats)
  AND (status = 'AVAILABLE' OR (status = 'HELD' AND hold_expires_at < now()));
-- rows updated must equal number of seats requested, otherwise ROLLBACK -> 409
```
All seats are held together or none (one transaction). No separate "read then write" race.

**Option B: Redis holds.** `SET seat:{event}:{seat} {holdId} NX EX 600` per seat (use a Lua script for multi-seat all-or-nothing). Fast for spikes, but the DB still needs the final `BOOKED` update with a condition. Redis is the gate, SQL is the source of truth.

**Pessimistic** (`SELECT ... FOR UPDATE`) also works but holds DB locks longer. **Optimistic** (version column) is fine when conflicts are rare. For hot on-sales, conditional updates/Redis are simplest.

### Confirming
1. The user pays within the hold TTL (with an idempotency key).
2. On payment success: `UPDATE seats SET status='BOOKED', booking_id=? WHERE seat_id IN (...) AND hold_id = :hold AND status='HELD'`.
   - If the hold already expired and the seats went to someone else → refund automatically. This is rare; mitigate with a grace period before release.

### Surviving the on-sale spike
- **Virtual waiting room:** users get a random/fair queue position, and the queue admits users at the rate the inventory service can handle (token with expiry).
- Serve the seat map from the cache (a few seconds stale is OK). The hold request is the real check.
- Rate limits per account, CAPTCHAs, purchase limits per user (anti-bot/scalper).
- Pre-scale before the known on-sale time, and shard inventory by event.

## 7. Trade-offs
- Hold TTL: short (more seats free up, users rushed) vs long (friendlier, but inventory looks sold out).
- Strict consistency for seats vs eventual for browse/availability counts.
- General admission (no seat numbers): a counter decrement `UPDATE ... SET remaining = remaining - :n WHERE remaining >= :n` instead of per-seat rows.

## 8. Follow-up questions
- **Two users click the same seat at the same moment?** Only one conditional update succeeds (row-level atomicity). The other gets 409 and a refreshed seat map.
- **Show seats "being held by others" live?** Push seat status changes via WebSocket/SSE per event, throttled, or just short polling of the cached map.
- **Payment service down during an on-sale?** Extend holds briefly, queue confirmations, circuit breaker; never mark BOOKED without payment.
