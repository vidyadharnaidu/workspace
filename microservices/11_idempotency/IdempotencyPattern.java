package idempotency;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * PATTERN: IDEMPOTENT RECEIVER / IDEMPOTENCY KEY
 *
 * PROBLEM: retries are everywhere - the gateway retries, Resilience4j retries, Kafka
 *          redelivers, the user double-clicks. "Charge 50 EUR" must happen ONCE even if
 *          the request arrives five times.
 * SOLUTION: the caller sends a stable key; the server records it and returns the FIRST
 *           result for every repeat.
 *
 * TRICK TO REMEMBER: idempotent = doing it twice has the same effect as doing it once.
 *                    GET/PUT/DELETE are naturally idempotent. POST is not - that is
 *                    exactly why POST needs an Idempotency-Key header.
 *
 * TRICK: the key must come from the CLIENT (a UUID it generates before sending), not from
 *        the server, or a retry generates a new key and you charge twice.
 *
 * TRICK: the unique index IS the lock. Let the database reject the duplicate; do not
 *        "check then insert", which races.
 */
public class IdempotencyPattern {

    // ============ 1. HTTP: the Idempotency-Key header (how Stripe does it) ============

    @RestController
    @RequestMapping("/payments")
    static class PaymentController {

        private final IdempotentPaymentService service;

        PaymentController(IdempotentPaymentService service) { this.service = service; }

        @PostMapping
        ResponseEntity<Receipt> pay(@RequestHeader("Idempotency-Key") String key,
                                    @RequestBody PayRequest request) {
            return service.pay(key, request);
        }
    }

    @Service
    static class IdempotentPaymentService {

        private final IdempotencyRepository keys;
        private final PaymentGateway gateway;

        IdempotentPaymentService(IdempotencyRepository keys, PaymentGateway gateway) {
            this.keys = keys;
            this.gateway = gateway;
        }

        @Transactional
        ResponseEntity<Receipt> pay(String key, PayRequest request) {

            // Replay: same key seen before -> return the stored response, do not charge again.
            Optional<IdempotencyRecord> existing = keys.findById(key);
            if (existing.isPresent()) {
                IdempotencyRecord record = existing.get();

                // Guard against key reuse with a DIFFERENT body - that is a client bug, so 422.
                if (!record.requestHash.equals(request.hash())) {
                    return ResponseEntity.unprocessableEntity().build();
                }
                if (record.response == null) {
                    return ResponseEntity.status(409).build();   // first call still in flight
                }
                return ResponseEntity.ok(record.toReceipt());
            }

            // Claim the key FIRST. The unique PK makes two concurrent requests race safely:
            // exactly one insert wins, the loser gets 409 and retries into the replay branch.
            try {
                keys.saveAndFlush(IdempotencyRecord.claim(key, request.hash()));
            } catch (DataIntegrityViolationException concurrentDuplicate) {
                return ResponseEntity.status(409).build();
            }

            Receipt receipt = gateway.charge(request.orderId(), request.amount());

            keys.save(IdempotencyRecord.complete(key, request.hash(), receipt));
            return ResponseEntity.ok(receipt);
        }
    }

    static class IdempotencyRecord {
        String key;                  // @Id -> the unique index that does the work
        String requestHash;
        String response;             // serialized first response
        Instant createdAt = Instant.now();
        Instant expiresAt;           // keys are not kept forever; 24h is a common TTL

        static IdempotencyRecord claim(String key, String hash) {
            IdempotencyRecord r = new IdempotencyRecord();
            r.key = key; r.requestHash = hash;
            r.expiresAt = Instant.now().plus(Duration.ofHours(24));
            return r;
        }

        static IdempotencyRecord complete(String key, String hash, Receipt receipt) {
            IdempotencyRecord r = claim(key, hash);
            r.response = receipt.toString();
            return r;
        }

        Receipt toReceipt() { return new Receipt("replayed", 0); }
    }

    interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, String> { }

    // ============ 2. MESSAGING: the inbox / dedup table ============

    /**
     * Kafka is at-least-once: rebalances and redeliveries WILL hand you the same event twice.
     * The consumer is responsible for exactly-once EFFECT.
     */
    @Service
    static class IdempotentConsumer {

        private final ProcessedRepository processed;
        private final OrderService orders;

        IdempotentConsumer(ProcessedRepository processed, OrderService orders) {
            this.processed = processed;
            this.orders = orders;
        }

        /**
         * TRICK: the dedup insert and the business change are in the SAME transaction.
         * Split them and a crash in between reprocesses, or silently drops, the message.
         */
        @KafkaListener(topics = "orders", groupId = "shipping-service")
        @Transactional
        void on(OrderCreated event, @Header(KafkaHeaders.RECEIVED_KEY) String messageId) {
            try {
                processed.saveAndFlush(new ProcessedMessage(messageId));
            } catch (DataIntegrityViolationException alreadySeen) {
                return;                                    // duplicate delivery, skip silently
            }
            orders.ship(event.orderId());
        }
    }

    static class ProcessedMessage {
        String messageId;                 // @Id
        Instant at = Instant.now();
        ProcessedMessage(String messageId) { this.messageId = messageId; }
    }

    interface ProcessedRepository extends JpaRepository<ProcessedMessage, String> { }

    // ============ 3. Cheap version: Redis SETNX ============

    @Service
    static class RedisIdempotency {

        private final StringRedisTemplate redis;

        RedisIdempotency(StringRedisTemplate redis) { this.redis = redis; }

        /** setIfAbsent is atomic - true means "I am the first", false means "duplicate". */
        boolean firstTime(String key) {
            Boolean claimed = redis.opsForValue()
                    .setIfAbsent("idem:" + key, "1", Duration.ofHours(24));
            return Boolean.TRUE.equals(claimed);
        }
    }

    // ============ 4. Natural idempotency: design it away ============

    /**
     * The best idempotency needs no key at all - make the operation naturally repeatable.
     *   bad : UPDATE account SET balance = balance - 50      (twice = wrong)
     *   good: INSERT INTO ledger (txn_id, delta) VALUES (?, -50)  with txn_id unique,
     *         then balance = SUM(delta). Replays collapse on the unique key.
     *   good: state transitions guarded by the current state -
     *         UPDATE orders SET status='SHIPPED' WHERE id=? AND status='PAID'
     *         The second run updates 0 rows, which is exactly right.
     */
    interface OrderService { void ship(String orderId); }

    record PayRequest(String orderId, double amount) {
        String hash() { return Integer.toHexString((orderId + amount).hashCode()); }
    }

    record Receipt(String id, double amount) { }

    interface PaymentGateway { Receipt charge(String orderId, double amount); }

    record OrderCreated(String orderId) { }
}
