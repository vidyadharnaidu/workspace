package resilience;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * PATTERN: CIRCUIT BREAKER, RETRY, TIMEOUT, BULKHEAD, RATE LIMITER  (Resilience4j)
 *
 * PROBLEM: one slow downstream service holds every caller thread, the callers fill up,
 *          and the whole system falls over. That is a cascading failure.
 * SOLUTION: fail fast and stay inside a budget.
 *
 * TRICK TO REMEMBER - the three circuit states:
 *   CLOSED    calls pass, failures are counted
 *   OPEN      calls fail INSTANTLY with no network hop (this is what saves you)
 *   HALF_OPEN after waitDuration, let a few probe calls through; recover or re-open
 *
 * TRICK: order matters. Spring applies them outside-in:
 *   Retry ( CircuitBreaker ( RateLimiter ( TimeLimiter ( Bulkhead ( call ) ) ) ) )
 *   so a retry wraps the breaker - that is usually what you want.
 *
 * TRICK: retry only IDEMPOTENT operations. Retrying "charge the card" charges twice
 *        unless you send an idempotency key (see 11).
 *
 * Dependencies: spring-cloud-starter-circuitbreaker-resilience4j, spring-boot-starter-aop
 */
@Service
public class ResilientPaymentService {

    private final PaymentGatewayClient gateway;

    public ResilientPaymentService(PaymentGatewayClient gateway) {
        this.gateway = gateway;
    }

    /**
     * The everyday combination: retry the transient blips, break on sustained failure,
     * and always have a fallback.
     *
     * The fallback method must have the SAME signature plus a Throwable last parameter.
     */
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "payFallback")
    @Retry(name = "paymentGateway")
    public Receipt pay(String orderId, double amount) {
        return gateway.charge(orderId, amount);
    }

    private Receipt payFallback(String orderId, double amount, Throwable t) {
        // Degrade deliberately: queue it, or tell the user to try later. Never rethrow blindly.
        return Receipt.pending(orderId, "gateway unavailable: " + t.getClass().getSimpleName());
    }

    /**
     * BULKHEAD: cap the concurrent calls to this dependency so it cannot eat the whole pool.
     * Think of ship compartments - one floods, the ship stays up.
     */
    @Bulkhead(name = "paymentGateway", fallbackMethod = "busyFallback")
    public Receipt payLimited(String orderId, double amount) {
        return gateway.charge(orderId, amount);
    }

    private Receipt busyFallback(String orderId, double amount, Throwable t) {
        return Receipt.pending(orderId, "too many in-flight payments");
    }

    /** RATE LIMITER: cap the calls per second, usually to respect a partner's quota. */
    @RateLimiter(name = "paymentGateway")
    public Receipt payThrottled(String orderId, double amount) {
        return gateway.charge(orderId, amount);
    }

    /** TIME LIMITER only works on a CompletableFuture - it needs another thread to cancel. */
    @TimeLimiter(name = "paymentGateway")
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "asyncFallback")
    public CompletableFuture<Receipt> payAsync(String orderId, double amount) {
        return CompletableFuture.supplyAsync(() -> gateway.charge(orderId, amount));
    }

    private CompletableFuture<Receipt> asyncFallback(String orderId, double amount, Throwable t) {
        return CompletableFuture.completedFuture(Receipt.pending(orderId, "timed out"));
    }

    interface PaymentGatewayClient {
        Receipt charge(String orderId, double amount);
    }

    record Receipt(String orderId, String status, String note) {
        static Receipt pending(String orderId, String note) {
            return new Receipt(orderId, "PENDING", note);
        }
    }
}
