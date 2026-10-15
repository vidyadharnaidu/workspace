package saga.choreography;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PATTERN: SAGA - CHOREOGRAPHY style  (event driven, no coordinator)
 *
 * The happy path, as events:
 *   OrderCreated   -> payment service charges  -> PaymentCompleted
 *   PaymentCompleted -> inventory reserves     -> StockReserved
 *   StockReserved  -> order service confirms   -> OrderConfirmed
 *
 * The failure path is just more events, flowing backwards:
 *   StockUnavailable -> payment service refunds -> PaymentRefunded -> order cancels
 *
 * TRICK TO REMEMBER: nobody is in charge. Each service knows only "when I see X, I do Y
 *                    and publish Z". That is the strength (loose coupling) and the
 *                    weakness (no single place shows you the whole flow).
 *
 * TRICK: every listener must be IDEMPOTENT. Kafka is at-least-once, so the same event
 *        WILL arrive twice. See 11.
 *
 * TRICK: publishing the event and writing the DB row must not drift apart -> see 10, outbox.
 *
 * Dependency: spring-kafka
 */
public class ChoreographedSaga {

    // ---------- events: past tense, immutable, carry the ids ----------
    public record OrderCreated(String orderId, String sku, int qty, double amount) { }
    public record PaymentCompleted(String orderId, String paymentId) { }
    public record PaymentFailed(String orderId, String reason) { }
    public record StockReserved(String orderId, String reservationId) { }
    public record StockUnavailable(String orderId, String sku) { }
    public record PaymentRefunded(String orderId, String paymentId) { }

    // ---------- service 1: payments ----------
    @Service
    static class PaymentService {

        private final KafkaTemplate<String, Object> kafka;
        private final PaymentRepo repo;

        PaymentService(KafkaTemplate<String, Object> kafka, PaymentRepo repo) {
            this.kafka = kafka;
            this.repo = repo;
        }

        @KafkaListener(topics = "orders", groupId = "payment-service")
        @Transactional
        void on(OrderCreated event) {
            if (repo.alreadyProcessed(event.orderId())) return;     // idempotency guard

            try {
                String paymentId = repo.charge(event.orderId(), event.amount());
                kafka.send("payments", event.orderId(), new PaymentCompleted(event.orderId(), paymentId));
            } catch (Exception e) {
                kafka.send("payments", event.orderId(), new PaymentFailed(event.orderId(), e.getMessage()));
            }
        }

        /** The compensation, triggered by an event rather than by a coordinator. */
        @KafkaListener(topics = "inventory", groupId = "payment-service")
        @Transactional
        void on(StockUnavailable event) {
            String paymentId = repo.findPaymentId(event.orderId());
            repo.refund(paymentId);
            kafka.send("payments", event.orderId(), new PaymentRefunded(event.orderId(), paymentId));
        }

        interface PaymentRepo {
            boolean alreadyProcessed(String orderId);
            String charge(String orderId, double amount);
            String findPaymentId(String orderId);
            void refund(String paymentId);
        }
    }

    // ---------- service 2: inventory ----------
    @Service
    static class InventoryService {

        private final KafkaTemplate<String, Object> kafka;
        private final StockRepo repo;

        InventoryService(KafkaTemplate<String, Object> kafka, StockRepo repo) {
            this.kafka = kafka;
            this.repo = repo;
        }

        @KafkaListener(topics = "payments", groupId = "inventory-service")
        @Transactional
        void on(PaymentCompleted event) {
            var order = repo.findOrder(event.orderId());
            if (repo.available(order.sku()) < order.qty()) {
                kafka.send("inventory", event.orderId(), new StockUnavailable(event.orderId(), order.sku()));
                return;
            }
            String reservationId = repo.reserve(order.sku(), order.qty());
            kafka.send("inventory", event.orderId(), new StockReserved(event.orderId(), reservationId));
        }

        interface StockRepo {
            OrderLine findOrder(String orderId);
            int available(String sku);
            String reserve(String sku, int qty);
            record OrderLine(String sku, int qty) { }
        }
    }

    // ---------- service 3: orders, which started it and now finishes it ----------
    @Service
    static class OrderService {

        private final KafkaTemplate<String, Object> kafka;
        private final OrderRepo repo;

        OrderService(KafkaTemplate<String, Object> kafka, OrderRepo repo) {
            this.kafka = kafka;
            this.repo = repo;
        }

        @Transactional
        public String place(String sku, int qty, double amount) {
            String orderId = repo.savePending(sku, qty, amount);
            // In real code this send goes through the OUTBOX (see 10), not straight to Kafka.
            kafka.send("orders", orderId, new OrderCreated(orderId, sku, qty, amount));
            return orderId;
        }

        @KafkaListener(topics = "inventory", groupId = "order-service")
        void on(StockReserved event) { repo.confirm(event.orderId()); }

        @KafkaListener(topics = "payments", groupId = "order-service")
        void on(PaymentFailed event) { repo.cancel(event.orderId(), event.reason()); }

        @KafkaListener(topics = "payments", groupId = "order-service")
        void on(PaymentRefunded event) { repo.cancel(event.orderId(), "out of stock, refunded"); }

        interface OrderRepo {
            String savePending(String sku, int qty, double amount);
            void confirm(String orderId);
            void cancel(String orderId, String reason);
        }
    }
}
