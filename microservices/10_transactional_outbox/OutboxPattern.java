package outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * PATTERN: TRANSACTIONAL OUTBOX
 *
 * PROBLEM - the dual write. This code is broken and it looks fine:
 *
 *     @Transactional
 *     void placeOrder(...) {
 *         orderRepo.save(order);              // database
 *         kafka.send("orders", event);        // message broker
 *     }
 *
 *   The DB commit and the Kafka send are two separate systems with no shared transaction.
 *   Crash in between -> either an order nobody was told about, or an event for an order
 *   that was rolled back. There is no ordering of the two that fixes it.
 *
 * SOLUTION: write the event INTO THE SAME DATABASE, in the SAME transaction, as a row in
 *           an `outbox` table. A separate process reads that table and publishes. Now the
 *           only commit is the local one, which is atomic.
 *
 * TRICK TO REMEMBER: "never write to two systems in one transaction - write to one, and
 *                    let something else forward it."
 *
 * TRICK: this gives AT-LEAST-ONCE delivery (the publisher can crash after sending, before
 *        marking sent). So every consumer must be idempotent - see 11.
 *
 * Two ways to drain the outbox:
 *   1. POLLING PUBLISHER (this file) - a @Scheduled job. Simple, no extra infra, some latency.
 *   2. CDC / log tailing (Debezium)  - reads the DB write-ahead log, near zero latency,
 *                                      no polling load, but another component to run.
 *                                      This is what most production systems end up using.
 */
public class OutboxPattern {

    // ---------- the outbox row ----------
    static class OutboxEvent {
        String id = UUID.randomUUID().toString();
        String aggregateType;     // "Order"
        String aggregateId;       // used as the Kafka key -> per-order ordering
        String type;              // "OrderCreated"
        String payload;           // JSON
        Instant createdAt = Instant.now();
        Instant sentAt;           // null = still pending
        int attempts;
    }

    interface OutboxRepository extends JpaRepository<OutboxEvent, String> {

        /**
         * FOR UPDATE SKIP LOCKED lets several publisher instances drain the same table
         * without stepping on each other - this one line is the whole scaling story.
         */
        @Query(value = """
                SELECT * FROM outbox_event
                WHERE sent_at IS NULL
                ORDER BY created_at
                LIMIT 100
                FOR UPDATE SKIP LOCKED
                """, nativeQuery = true)
        List<OutboxEvent> lockBatch();

        @Modifying
        @Query("UPDATE OutboxEvent o SET o.sentAt = :now WHERE o.id IN :ids")
        void markSent(List<String> ids, Instant now);
    }

    // ---------- the write side: one transaction, one system ----------
    @Service
    static class OrderService {

        private final OrderRepository orders;
        private final OutboxRepository outbox;
        private final ObjectMapper json;

        OrderService(OrderRepository orders, OutboxRepository outbox, ObjectMapper json) {
            this.orders = orders;
            this.outbox = outbox;
            this.json = json;
        }

        @Transactional
        public String placeOrder(String customerId, String sku, int qty) {
            Order order = orders.save(new Order(customerId, sku, qty));

            // Same transaction, same database -> commits or rolls back together. No dual write.
            outbox.save(toOutbox(order));

            return order.id;
        }

        private OutboxEvent toOutbox(Order order) {
            OutboxEvent e = new OutboxEvent();
            e.aggregateType = "Order";
            e.aggregateId = order.id;
            e.type = "OrderCreated";
            try {
                e.payload = json.writeValueAsString(order);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
            return e;
        }
    }

    // ---------- the relay: drains the outbox ----------
    @Service
    static class OutboxPublisher {

        private final OutboxRepository outbox;
        private final KafkaTemplate<String, String> kafka;

        OutboxPublisher(OutboxRepository outbox, KafkaTemplate<String, String> kafka) {
            this.outbox = outbox;
            this.kafka = kafka;
        }

        @Scheduled(fixedDelay = 500)
        @Transactional
        public void publishPending() {
            List<OutboxEvent> batch = outbox.lockBatch();
            if (batch.isEmpty()) return;

            List<String> sent = batch.stream()
                    .filter(this::send)
                    .map(e -> e.id)
                    .toList();

            outbox.markSent(sent, Instant.now());
            // Not marked = retried next tick. A row that keeps failing goes to a DLQ table
            // after N attempts so one poison message cannot block the queue.
        }

        private boolean send(OutboxEvent e) {
            try {
                // key = aggregateId -> all events for one order land on one partition, in order
                kafka.send("orders", e.aggregateId, e.payload).get();
                return true;
            } catch (Exception ex) {
                e.attempts++;
                return false;
            }
        }
    }

    // ---------- the INBOX, the mirror image on the consumer ----------
    /**
     * Same idea in reverse: record the processed message id in the SAME transaction as the
     * business change, so a redelivery is detected and skipped. See 11 for the full version.
     */
    interface InboxRepository extends JpaRepository<ProcessedMessage, String> { }

    static class ProcessedMessage {
        String messageId;
        Instant processedAt;
    }

    static class Order {
        String id = UUID.randomUUID().toString();
        String customerId, sku;
        int qty;
        Order(String customerId, String sku, int qty) {
            this.customerId = customerId; this.sku = sku; this.qty = qty;
        }
    }

    interface OrderRepository extends JpaRepository<Order, String> { }
}
