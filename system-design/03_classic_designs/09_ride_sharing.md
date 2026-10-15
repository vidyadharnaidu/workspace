# Design a Ride-Sharing Service (Uber / Lyft)

> **Remember:** *"Drivers stream locations every few seconds into a geospatial index (geohash/H3 in Redis), matching finds nearby available drivers and offers the ride to one at a time with an atomic lock, and trips are a state machine."*

## 1. Requirements

**Functional**
- Riders request a ride (pickup, destination) and see a price estimate and ETA
- Match the rider with a nearby available driver; the driver accepts or declines
- Live location tracking of the driver during pickup and trip
- Trip lifecycle and payment at the end; ratings

**Non-functional**
- Matching in seconds; location updates near real-time
- **A driver must never be assigned to two trips** (strong consistency on assignment)
- Highly available per city/region; handle huge location write volume

## 2. Estimates
- 1M drivers online at peak, each sending a location every 4 s → **250k location writes/s**
- 20M rides/day → ~230 ride requests/s (peaks ×3–5 in rush hour / events)
- Location data is ephemeral (only the latest per driver matters for matching) → in-memory; history goes to cheap storage

## 3. API
```
Rider:   POST /v1/rides/estimate  { pickup, dropoff } -> { price, eta }
         POST /v1/rides           { pickup, dropoff, estimateId } -> { rideId, status: MATCHING }
         WS   ride updates: driver assigned, driver location, trip status
Driver:  WS   location updates { lat, lng, heading, ts } every ~4 s
         <-   ride offer { rideId, pickup, expiresIn: 15s }
         POST /v1/rides/{id}/accept | decline | arrived | start | complete
```

## 4. Data model
```
drivers        (driver_id, vehicle, rating, status: OFFLINE|AVAILABLE|OFFERED|ON_TRIP, ride_id, version)
driver_geo     Redis GEO / in-memory index: cell -> set of available driver_ids (+ latest location per driver)
rides          (ride_id, rider_id, driver_id, status, pickup, dropoff, price, timestamps)   -- SQL, sharded by city
location_log   (driver_id, ts, lat, lng) -> Kafka -> cold storage (analytics, disputes, ETA models)
```

## 5. High-level design

```
 Driver app ==WS==> [Location Gateway] -> [Location Service] -> update [Geo Index (Redis GEO / H3 cells)]
                                                        \-> [Kafka: locations] -> history, ETA ML, surge calc

 Rider app --request--> [Ride Service] --(create ride MATCHING)--> [Rides DB]
                              |
                              v
                        [Matching Service]
                          1. nearby available drivers from Geo Index (expand radius if none)
                          2. rank by ETA (Maps/ETA service), rating, acceptance rate
                          3. lock driver (AVAILABLE -> OFFERED, atomic) and send offer via [Driver WS gateway]
                          4. accept -> ride ASSIGNED; decline/timeout -> unlock, try next driver
                              |
               [Pricing/Surge Service]   [Payments Service (async, at trip end)]   [Notifications]
```

## 6. Deep dives

### Geospatial indexing ("find drivers near me")
| Index | Idea | Notes |
|---|---|---|
| **Geohash** | Encode lat/lng into a string; shared prefix ≈ nearby | Search own cell + 8 neighbors; Redis GEO uses geohash internally (`GEOSEARCH`) |
| **Quadtree** | Recursively split areas with > N drivers into 4 | Adapts to density (downtown vs rural); in-memory, rebuilt often |
| **H3 (Uber)** | Hexagonal cells at multiple resolutions | Neighbors equidistant; great for surge per cell |

Location updates overwrite the latest position. **Shard the geo index by city/region**, since matching is local.

### Never double-assign a driver
- **Atomic conditional update:** `UPDATE drivers SET status='OFFERED', ride_id=? WHERE driver_id=? AND status='AVAILABLE'` (1 row = locked). Or Redis `SET driver_lock:{id} ride NX EX 15`.
- Offer one driver at a time with a timeout (~15 s). On decline/timeout, release and offer the next. Some systems offer to a few drivers concurrently with first-accept-wins using the same atomic check.

### Trip state machine
`REQUESTED → MATCHING → DRIVER_ASSIGNED → DRIVER_ARRIVED → IN_PROGRESS → COMPLETED` (or `CANCELLED` from several states). Transitions are validated server-side, and each is an event (Kafka) for billing, notifications and analytics.

### Live tracking
- The driver location stream is forwarded to the rider's WebSocket (only for their assigned driver). The client interpolates between updates for smooth movement.

### Surge pricing
- A streaming job computes demand (requests) vs supply (available drivers) per H3 cell every minute. The multiplier is stored in the cache and read by the estimate API.

## 7. Trade-offs
- Location updates are AP (the latest value wins, losing one update is fine). Assignment and payment are CP.
- Update frequency trades battery/network cost against accuracy (adaptive: slower when idle).
- Regional isolation: each city/region is an independent cell, which limits the blast radius.

## 8. Follow-up questions
- **What if the matching service crashes mid-offer?** The lock has a TTL, and the ride stays in MATCHING. A recovery worker picks up stale MATCHING rides and retries.
- **ETA calculation?** Road graph + live traffic (a routing engine with contraction hierarchies), corrected with ML from historical trips.
- **Payments?** Authorize an estimate at request time and capture the final fare after the trip (see `11_payment_system.md`).
