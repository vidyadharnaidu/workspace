package saga.orchestration;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PATTERN: SAGA - ORCHESTRATION style
 *
 * PROBLEM: "place order" spans order + payment + inventory + shipping, each with its own
 *          database. There is no distributed transaction, so you cannot just roll back.
 * SOLUTION: a sequence of LOCAL transactions. If step N fails, run the COMPENSATING
 *           action of steps N-1 .. 1, in reverse.
 *
 * TRICK TO REMEMBER: a saga gives you EVENTUAL consistency, not atomicity. There IS a
 *                    window where the payment succeeded and the stock did not.
 *                    Compensation is a SEMANTIC undo (refund), never a rollback.
 *
 * ORCHESTRATION (this file): one coordinator tells everyone what to do.
 *   + the whole flow is readable in one class, easy to debug
 *   - the coordinator is a dependency hub and can become a mini-monolith
 *
 * CHOREOGRAPHY (see 07): services react to each other's events, no coordinator.
 *
 * Rule of thumb: 2-4 steps -> choreography. More, or complex branching -> orchestration.
 *
 * Production versions: Camunda / Temporal / AWS Step Functions keep the state machine
 * durable so a crash mid-saga resumes instead of stranding the order.
 */
@Service
public class OrderSagaOrchestrator {

    private final OrderRepository orders;
    private final PaymentClient payments;
    private final InventoryClient inventory;
    private final ShippingClient shipping;

    public OrderSagaOrchestrator(OrderRepository orders, PaymentClient payments,
                                 InventoryClient inventory, ShippingClient shipping) {
        this.orders = orders;
        this.payments = payments;
        this.inventory = inventory;
        this.shipping = shipping;
    }

    /**
     * Each step is its own transaction in its own service.
     * The try/catch ladder IS the saga - undo exactly what already succeeded.
     */
    public SagaResult placeOrder(OrderRequest request) {
        String orderId = orders.createPending(request);        // step 1 (local tx, state = PENDING)
        String paymentId = null;
        String reservationId = null;

        try {
            paymentId = payments.charge(orderId, request.amount(), orderId);        // step 2
            reservationId = inventory.reserve(request.sku(), request.qty(), orderId); // step 3
            shipping.schedule(orderId, request.address());                          // step 4

            orders.markConfirmed(orderId);
            return SagaResult.ok(orderId);

        } catch (Exception failure) {
            // Compensate in REVERSE order, and never let a compensation throw.
            compensate(orderId, paymentId, reservationId, failure);
            orders.markCancelled(orderId, failure.getMessage());
            return SagaResult.failed(orderId, failure.getMessage());
        }
    }

    private void compensate(String orderId, String paymentId, String reservationId, Exception cause) {
        safely(() -> {
            if (reservationId != null) inventory.release(reservationId);   // undo step 3
        });
        safely(() -> {
            if (paymentId != null) payments.refund(paymentId);             // undo step 2
        });
        // Step 1 needs no undo: the order row just becomes CANCELLED, which is good audit history.
    }

    /**
     * TRICK: a failed compensation is the worst case - you are now inconsistent and silent.
     * Swallow, log loudly, and push to a dead-letter queue for a human or a retry job.
     */
    private void safely(Runnable compensation) {
        try {
            compensation.run();
        } catch (Exception e) {
            // log.error("COMPENSATION FAILED - manual intervention required", e);
            // deadLetterQueue.send(...)
        }
    }

    /** State machine version - what a durable orchestrator persists after every step. */
    enum SagaStep { STARTED, PAYMENT_DONE, STOCK_RESERVED, SHIPPING_SCHEDULED, COMPLETED, COMPENSATING, CANCELLED }

    record OrderRequest(String customerId, String sku, int qty, double amount, String address) { }

    record SagaResult(String orderId, boolean success, String message) {
        static SagaResult ok(String id) { return new SagaResult(id, true, "confirmed"); }
        static SagaResult failed(String id, String why) { return new SagaResult(id, false, why); }
    }

    interface OrderRepository {
        @Transactional String createPending(OrderRequest request);
        @Transactional void markConfirmed(String orderId);
        @Transactional void markCancelled(String orderId, String reason);
    }

    /** Every call takes the orderId as the idempotency key - retries must not double charge. */
    interface PaymentClient {
        String charge(String orderId, double amount, String idempotencyKey);
        void refund(String paymentId);
    }

    interface InventoryClient {
        String reserve(String sku, int qty, String idempotencyKey);
        void release(String reservationId);
    }

    interface ShippingClient {
        void schedule(String orderId, String address);
    }
}
