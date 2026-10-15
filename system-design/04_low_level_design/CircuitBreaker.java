import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Circuit breaker with a fake clock.
 *
 *   java system-design/04_low_level_design/CircuitBreaker.java
 *
 *   CLOSED  --(failures >= threshold)-->  OPEN  --(after openTimeout)-->  HALF_OPEN
 *     ^                                     ^                                 |
 *     |                                     +------(trial call fails)---------+
 *     +-------------------------(trial call succeeds)-------------------------+
 *
 * TRICKS:
 *  - CLOSED: calls pass; count consecutive failures.
 *  - OPEN: fail fast (return fallback) without calling the broken dependency -> it gets time to recover.
 *  - HALF_OPEN: let ONE trial call through; success closes, failure re-opens.
 *  - Always pair with timeouts, and give callers a fallback.
 */
public class CircuitBreaker {

    enum State { CLOSED, OPEN, HALF_OPEN }

    private final int failureThreshold;
    private final long openTimeoutMs;
    private final LongSupplier clock;
    private State state = State.CLOSED;
    private int consecutiveFailures;
    private long openedAt;

    CircuitBreaker(int failureThreshold, long openTimeoutMs, LongSupplier clock) {
        this.failureThreshold = failureThreshold;
        this.openTimeoutMs = openTimeoutMs;
        this.clock = clock;
    }

    synchronized <T> T call(Supplier<T> action, Supplier<T> fallback) {
        if (state == State.OPEN) {
            if (clock.getAsLong() - openedAt >= openTimeoutMs) state = State.HALF_OPEN; // time to test the waters
            else return fallback.get();                                                 // fail fast
        }
        try {
            T result = action.get();
            consecutiveFailures = 0;
            state = State.CLOSED;
            return result;
        } catch (RuntimeException e) {
            consecutiveFailures++;
            if (state == State.HALF_OPEN || consecutiveFailures >= failureThreshold) {
                state = State.OPEN;
                openedAt = clock.getAsLong();
            }
            return fallback.get();
        }
    }

    synchronized State state() { return state; }

    public static void main(String[] args) {
        long[] now = {0};
        boolean[] serviceUp = {true};
        int[] realCalls = {0};

        CircuitBreaker breaker = new CircuitBreaker(3, 5_000, () -> now[0]);
        Supplier<String> remoteCall = () -> {
            realCalls[0]++;
            if (!serviceUp[0]) throw new RuntimeException("503");
            return "fresh data";
        };
        Supplier<String> fallback = () -> "cached data";

        Runnable step = () -> {
            int before = realCalls[0];
            String result = breaker.call(remoteCall, fallback);
            System.out.printf("t=%5dms service=%-4s -> %-11s state=%-9s %s%n", now[0], serviceUp[0] ? "UP" : "DOWN",
                    result, breaker.state(), realCalls[0] > before ? "(called service)" : "(fail fast, no call)");
        };

        step.run();                                   // CLOSED, success
        serviceUp[0] = false;
        for (int i = 0; i < 5; i++) { now[0] += 100; step.run(); } // 3 failures -> OPEN, then fail fast
        now[0] += 5_000; step.run();                  // HALF_OPEN trial fails -> OPEN again
        serviceUp[0] = true;
        now[0] += 1_000; step.run();                  // still OPEN (timeout not reached)
        now[0] += 5_000; step.run();                  // HALF_OPEN trial succeeds -> CLOSED
        now[0] += 100; step.run();                    // CLOSED
        System.out.println("real calls to the service: " + realCalls[0] + " of 10 requests");
    }
}
