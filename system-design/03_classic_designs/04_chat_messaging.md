# Design a Chat App (WhatsApp / Messenger / Slack)

> **Remember:** *"WebSocket gateways + a user→gateway registry, messages stored per conversation with time-sortable ids, ack states sent→delivered→read, offline users get a push notification and sync on reconnect."*

## 1. Requirements

**Functional**
- 1:1 and group chat (groups up to ~500)
- Sent / delivered / read receipts
- Online presence / last seen
- Offline delivery (receive messages when back online), push notifications
- Media sharing (images, video)

**Non-functional**
- Real-time: low latency (< 200 ms)
- **Messages never lost**; order preserved within a conversation
- Highly available; multi-device sync

**Out of scope:** voice/video calls, stories

## 2. Estimates
- 500M DAU × 40 messages/day = 20B messages/day → **~230k messages/s** average
- Message ~100 bytes → 2 TB/day of text → wide-column store, sharded
- Concurrent connections: ~100M+ → **~1,000+ WebSocket gateway servers** (at ~100k connections each)

## 3. API
```
WebSocket (persistent, after auth):
  -> send     { clientMsgId, conversationId, text | mediaId }
  <- ack      { clientMsgId, messageId, serverTs }             (sent ✓)
  <- message  { messageId, conversationId, from, text, ts }
  -> delivered / read { conversationId, upToMessageId }        (✓✓ / blue ✓✓)
REST:
  GET  /v1/conversations/{id}/messages?before=<messageId>&limit=50   (history, cursor)
  POST /v1/media/upload-url  -> pre-signed S3 URL
```

## 4. Data model
```
messages      partition key: conversation_id   clustering key: message_id DESC (Snowflake -> time order)
              columns: sender_id, body/media_url, created_at          -> Cassandra/ScyllaDB (write-heavy)
conversations (conversation_id, type, member ids, last_message_id)
user_inbox    (user_id, conversation_id, last_read_message_id, unread_count)  -> list of chats screen
sessions      Redis: user_id -> {gateway_id, device ids}, TTL refreshed by heartbeat
```

## 5. High-level design

```
 Alice ==WS==> [Gateway 7] --send--> [Chat Service] --1. persist--> [Messages DB (Cassandra)]
                                          |
                                          |--2. lookup Bob's gateways--> [Session Registry (Redis)]
                                          |
                     +--------------------+--------------------+
                     | Bob online                               | Bob offline
                     v                                          v
          [Pub/Sub / direct RPC] -> [Gateway 3] ==WS==> Bob    [Kafka] -> [Push Notification Service] -> APNs / FCM
                                                                  (Bob syncs missed messages on reconnect)
 Media: client -> pre-signed upload -> [S3] -> [CDN];  message carries only the media URL
 Presence: heartbeats from gateways -> [Presence Service] (Redis, TTL) -> fan-out to contacts (throttled)
```

## 6. Deep dives

### Message flow & delivery guarantees
1. The client sends with a **clientMsgId** (an idempotency key, so a retry doesn't create a duplicate).
2. The server assigns a **messageId** (Snowflake), **persists first**, then acks the sender (single ✓).
3. The server routes to the recipient's gateway(s). The recipient's device acks, and the server marks it delivered (✓✓).
4. When opened, the recipient sends read up to messageId (read receipts are batched).
5. If a gateway or device is unreachable, the message stays stored. On reconnect the client calls **sync(lastMessageId)** per conversation, so nothing is lost.

### Ordering
- A global order is impossible and also unnecessary. You need order **per conversation**.
- Use time-sortable ids per conversation (or a per-conversation sequence number from the partition leader). Clients sort by id and dedupe by id.

### Group chat
- **Small groups (≤ ~500):** fan out on write. Look up members, deliver to each online member, and store once per conversation (not per member).
- **Huge channels (Slack/Telegram channels):** store once and let clients pull/stream (fan-out on read).

### Presence
- A heartbeat every ~30 s updates `last_seen` with a TTL in Redis. Missing heartbeats mean offline.
- Don't broadcast every change to every contact. Only subscribe to presence for chats currently open, and throttle updates.

### Multi-device
- Each device has its own connection and sync cursor. Messages fan out to all of a user's devices.
- End-to-end encryption (Signal protocol) encrypts per device. The server stores only ciphertext.

## 7. Bottlenecks & trade-offs
- **Gateway crash:** clients reconnect to another gateway (with backoff + jitter), the registry is updated, and clients sync missed messages.
- **Hot conversation partitions:** bucket the partition key by time (`conversation_id + month`).
- **Cassandra (AP)** is chosen for write throughput and availability. Ordering and idempotency are handled with ids.
- **Push notifications** are best-effort. The source of truth is the sync API.

## 8. Follow-up questions
- **How do you show unread counts?** A per-user inbox row updated asynchronously, recomputed from `last_read_message_id` if it drifts.
- **Search old messages?** Index asynchronously into Elasticsearch. With E2E encryption, search happens on-device only.
- **Message edits/deletes?** Store as new events referencing the original id. Clients apply them in order.
- **Why not HTTP polling?** Latency and huge wasted requests at 100M users. WebSockets keep one cheap idle connection.
