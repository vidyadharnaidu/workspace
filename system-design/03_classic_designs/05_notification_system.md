# Design a Notification System (push, SMS, email, in-app)

> **Remember:** *"Validate → check preferences & dedupe → render template → per-channel priority queues → workers call providers with retries/backoff → DLQ → track delivery."*

## 1. Requirements

**Functional**
- Services send notifications to users over **push (iOS/Android), SMS, email, in-app**
- User preferences (opt-out per channel/type), quiet hours
- Templates with personalization; scheduled and bulk (marketing) sends
- Delivery tracking (sent, delivered, opened, failed)

**Non-functional**
- **At-least-once** delivery with no visible duplicates
- Transactional notifications (OTP, password reset) are fast (seconds) and prioritized over marketing
- Scales to spikes (a campaign to 50M users); provider outages don't lose notifications

## 2. Estimates
- 10M push + 1M SMS + 5M email per day ≈ 16M/day → ~200/s average, **bursty** (campaigns → tens of thousands/s)
- Providers impose rate limits (SMS/email per second), so we must throttle

## 3. API
```
POST /v1/notifications
{
  "idempotencyKey": "order-123-shipped",
  "userId": "u42",
  "type": "ORDER_SHIPPED",               // maps to template + default channels + priority
  "channels": ["push", "email"],         // optional override
  "data": { "orderId": "123", "eta": "Tue" },
  "sendAt": "optional ISO time"
}
-> 202 Accepted { notificationId }
GET /v1/users/{id}/notification-preferences  |  PUT ...
```

## 4. Data model
```
notification_types  (type, template_ids per channel, priority, default_channels)
templates           (template_id, channel, locale, subject, body with {{placeholders}})
user_preferences    (user_id, type/category, channel, enabled, quiet_hours, timezone)
devices             (user_id, device_token, platform, last_seen)       -- push tokens
notifications       (notification_id, idempotency_key UNIQUE, user_id, type, status, created_at)
delivery_log        (notification_id, channel, provider_msg_id, status, attempts, updated_at)
```

## 5. High-level design

```
[Order Svc] [Auth Svc] [Marketing Svc]
        \        |        /
         v       v       v
      [ Notification API ] -- validate, auth, idempotency check --> [Notifications DB]
                 |
                 v
      [ Kafka: notifications ] --> [ Processor ]
                                     1. load user preferences / quiet hours (cache)
                                     2. dedupe (idempotency key), per-user rate limit
                                     3. render template per channel & locale
                                     4. route by channel + priority
                                        |
          +-------------+---------------+----------------+
          v             v               v                v
   [push-high]    [sms-high]       [email-low]      [in-app]
          |             |               |                |
   [Push workers] [SMS workers]   [Email workers]  [In-app svc -> WebSocket/SSE + inbox table]
          |             |               |
        APNs/FCM      Twilio         SES/SendGrid
          |             |               |
          +-------------+---------------+--> status events / provider webhooks --> [Tracking svc] -> delivery_log
   failures after N retries --> [DLQ] -> alert / manual replay
 [Scheduler] -- due scheduled/bulk sends --> Kafka
```

## 6. Deep dives

### Reliability
- **Persist before enqueue**, or use the outbox pattern, so an accepted request is never lost.
- Workers ack the queue message only after the provider accepts it. Retry with **exponential backoff + jitter**, then send to the **DLQ**.
- **Provider failover:** a secondary SMS/email provider, with a circuit breaker per provider.

### No duplicates
- An `idempotencyKey` UNIQUE constraint at the API.
- Workers check `delivery_log` (notification_id + channel) before sending, because retries after a crash can still resend. Providers sometimes support idempotency keys too.

### Priority & throttling
- Separate queues/topics per priority so marketing floods can't delay OTPs.
- Token-bucket rate limits **per provider** (their quotas) and **per user** (e.g. max 3 marketing pushes/day).
- Honor quiet hours: delay non-urgent sends until the user's morning.

### Bulk campaigns
- The segment is computed offline (a list of user ids), then a batch job enqueues in chunks at a controlled rate.

### Push tokens
- Invalid-token responses from APNs/FCM mean deleting the token. Users with multiple devices get a send to each.

## 7. Trade-offs
- At-least-once + dedupe instead of exactly-once.
- Kafka (replay, high throughput) vs SQS (simple per-message retries/DLQ). Either is fine.
- In-app notifications stored in an inbox table are the source of truth. Push is best-effort.

## 8. Follow-up questions
- **How do you track opens/clicks?** Tracking pixel / redirect links for email, SDK events for push, all as events into analytics.
- **How do you handle timezones for "9 AM local" campaigns?** Bucket users by timezone and let the scheduler release each bucket at its local time.
- **How do you test templates safely?** Versioned templates, preview API, send to internal test users first.
