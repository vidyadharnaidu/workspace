import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.LongSupplier;

/**
 * Four rate limiting algorithms with a FAKE CLOCK so results are deterministic.
 *
 *   java system-design/04_low_level_design/RateLimiters.java
 *
 * TRICKS:
 *  - TOKEN BUCKET: tokens refill continuously at `rate`, capped at `capacity`; each request spends 1 -> allows bursts.
 *  - FIXED WINDOW: counter per floor(now / window) -> simplest, but 2x burst at window boundaries.
 *  - SLIDING WINDOW LOG: keep timestamps, drop those older than the window -> exact, memory heavy.
 *  - SLIDING WINDOW COUNTER: estimate = prevCount * (1 - elapsed%) + currCount -> near-exact, 2 counters.
 *  In production the state lives in Redis and the check runs atomically (Lua script).
 */
public class RateLimiters {

    interface RateLimiter {
        boolean allow();
    }

    static class FakeClock implements LongSupplier {
        private long nowMs;

        public long getAsLong() { return nowMs; }
        void set(long ms) { nowMs = ms; }
        void advance(long ms) { nowMs += ms; }
    }

    // ---------------------------------------------------------------- token bucket
    static class TokenBucket implements RateLimiter {
        private final long capacity;
        private final double refillPerMs;
        private final LongSupplier clock;
        private double tokens;
        private long lastRefill;

        TokenBucket(long capacity, double refillPerSecond, LongSupplier clock) {
            this.capacity = capacity;
            this.refillPerMs = refillPerSecond / 1000.0;
            this.clock = clock;
            this.tokens = capacity;
            this.lastRefill = clock.getAsLong();
        }

        public synchronized boolean allow() {
            long now = clock.getAsLong();
            tokens = Math.min(capacity, tokens + (now - lastRefill) * refillPerMs); // refill lazily on each call
            lastRefill = now;
            if (tokens >= 1) {
                tokens -= 1;
                return true;
            }
            return false;
        }
    }

    // ---------------------------------------------------------------- fixed window counter
    static class FixedWindow implements RateLimiter {
        private final int limit;
        private final long windowMs;
        private final LongSupplier clock;
        private long currentWindow = -1;
        private int count;

        FixedWindow(int limit, long windowMs, LongSupplier clock) {
            this.limit = limit;
            this.windowMs = windowMs;
            this.clock = clock;
        }

        public synchronized boolean allow() {
            long window = clock.getAsLong() / windowMs;
            if (window != currentWindow) {
                currentWindow = window;
                count = 0;
            }
            if (count < limit) {
                count++;
                return true;
            }
            return false;
        }
    }

    // ---------------------------------------------------------------- sliding window log
    static class SlidingWindowLog implements RateLimiter {
        private final int limit;
        private final long windowMs;
        private final LongSupplier clock;
        private final Deque<Long> timestamps = new ArrayDeque<>();

        SlidingWindowLog(int limit, long windowMs, LongSupplier clock) {
            this.limit = limit;
            this.windowMs = windowMs;
            this.clock = clock;
        }

        public synchronized boolean allow() {
            long now = clock.getAsLong();
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= now - windowMs) timestamps.pollFirst();
            if (timestamps.size() < limit) {
                timestamps.addLast(now);
                return true;
            }
            return false;
        }
    }

    // ---------------------------------------------------------------- sliding window counter
    static class SlidingWindowCounter implements RateLimiter {
        private final int limit;
        private final long windowMs;
        private final LongSupplier clock;
        private long currentWindow = -1;
        private int currentCount, previousCount;

        SlidingWindowCounter(int limit, long windowMs, LongSupplier clock) {
            this.limit = limit;
            this.windowMs = windowMs;
            this.clock = clock;
        }

        public synchronized boolean allow() {
            long now = clock.getAsLong();
            long window = now / windowMs;
            if (window != currentWindow) {
                previousCount = (window == currentWindow + 1) ? currentCount : 0; // older windows don't count
                currentCount = 0;
                currentWindow = window;
            }
            double elapsedFraction = (now % windowMs) / (double) windowMs;
            double estimate = previousCount * (1 - elapsedFraction) + currentCount;
            if (estimate < limit) {
                currentCount++;
                return true;
            }
            return false;
        }
    }

    static int fire(RateLimiter limiter, int requests) {
        int allowed = 0;
        for (int i = 0; i < requests; i++) if (limiter.allow()) allowed++;
        return allowed;
    }

    public static void main(String[] args) {
        // 1. Token bucket: capacity 5, refill 1 token/second
        FakeClock clock = new FakeClock();
        TokenBucket bucket = new TokenBucket(5, 1.0, clock);
        System.out.println("TokenBucket  t=0s   burst of 7  -> allowed " + fire(bucket, 7));   // 5 (burst up to capacity)
        clock.advance(2000);
        System.out.println("TokenBucket  t=2s   3 requests  -> allowed " + fire(bucket, 3));   // 2 (refilled 2 tokens)
        clock.advance(10_000);
        System.out.println("TokenBucket  t=12s  10 requests -> allowed " + fire(bucket, 10));  // 5 (capped at capacity)

        // 2. The window BOUNDARY problem: limit 5 per 1000 ms, 5 requests at t=999 and 5 more at t=1000
        System.out.println();
        System.out.println("Limit 5 / second. 5 requests at t=999ms, then 5 at t=1000ms (10 requests in 2 ms):");
        for (String name : new String[]{"FixedWindow", "SlidingWindowLog", "SlidingWindowCounter"}) {
            FakeClock c = new FakeClock();
            RateLimiter limiter = switch (name) {
                case "FixedWindow" -> new FixedWindow(5, 1000, c);
                case "SlidingWindowLog" -> new SlidingWindowLog(5, 1000, c);
                default -> new SlidingWindowCounter(5, 1000, c);
            };
            c.set(999);
            int first = fire(limiter, 5);
            c.set(1000);
            int second = fire(limiter, 5);
            System.out.printf("  %-21s allowed %d + %d = %d%s%n", name, first, second, first + second,
                    first + second > 5 ? "   <- boundary burst!" : "");
        }

        // 3. Sliding window log recovers exactly one window later
        FakeClock c = new FakeClock();
        SlidingWindowLog log = new SlidingWindowLog(3, 1000, c);
        System.out.println();
        System.out.println("SlidingWindowLog limit 3/s: t=0 -> " + fire(log, 5));   // 3
        c.set(500);
        System.out.println("                            t=500 -> " + fire(log, 5)); // 0
        c.set(1000);
        System.out.println("                            t=1000 -> " + fire(log, 5)); // 3 (t=0 entries expired)
    }
}
