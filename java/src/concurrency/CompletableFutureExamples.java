package concurrency;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

/**
 * CompletableFuture: the async toolkit interviewers ask about.
 *
 *  1. supplyAsync vs runAsync
 *  2. thenApply vs thenCompose vs thenCombine      <- the classic question
 *  3. allOf: wait for everything (and collect the results)
 *  4. anyOf: first one wins
 *  5. Error handling: exceptionally / handle / whenComplete
 *  6. Timeouts: orTimeout / completeOnTimeout
 *  7. Sequential vs parallel: the whole point
 *  8. ...Async and which thread actually runs your callback
 *  9. Manual completion
 * 10. Cancellation
 *
 * TRICK - the three "then"s:
 *   thenApply   (T -> U)                      map      - a plain value out
 *   thenCompose (T -> CompletableFuture<U>)   flatMap  - avoids CF<CF<U>>
 *   thenCombine (CF<U>, (T,U) -> V)           zip      - two INDEPENDENT futures
 *
 * TRICK: join() is get() without the checked exception. Prefer join() in streams.
 * TRICK: the default executor is ForkJoinPool.commonPool() - sized to CPUs and meant for
 *        CPU work. For blocking I/O, always pass your own pool or you starve everything.
 */
public class CompletableFutureExamples {

    static final ExecutorService IO_POOL = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws Exception {

        // 1. supplyAsync returns a value; runAsync returns nothing (CF<Void>)
        CompletableFuture<String> supply = CompletableFuture.supplyAsync(() -> "value", IO_POOL);
        CompletableFuture<Void> run = CompletableFuture.runAsync(() -> { /* side effect */ }, IO_POOL);
        System.out.println("1.  supplyAsync = " + supply.join() + ", runAsync = " + run.join());

        // 2. thenApply vs thenCompose vs thenCombine
        CompletableFuture<Integer> apply = CompletableFuture
                .supplyAsync(() -> 21, IO_POOL)
                .thenApply(n -> n * 2);                                  // map

        CompletableFuture<String> compose = CompletableFuture
                .supplyAsync(() -> "user-7", IO_POOL)
                .thenCompose(id -> loadProfile(id));                     // flatMap: no nested future

        CompletableFuture<String> combine = CompletableFuture
                .supplyAsync(() -> 100.0, IO_POOL)
                .thenCombine(CompletableFuture.supplyAsync(() -> 0.2, IO_POOL),
                        (price, tax) -> "total " + price * (1 + tax));   // zip two independents

        System.out.println("2.  thenApply=" + apply.join() + " thenCompose=" + compose.join() + " " + combine.join());

        // 3. allOf: waits for all, but returns Void - you re-join each future to get the values
        List<CompletableFuture<Integer>> futures = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> slow(i, 100), IO_POOL))
                .toList();

        CompletableFuture<List<Integer>> all = CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream().map(CompletableFuture::join).toList());
        //                                        ^ safe: allOf already completed them

        long start = System.currentTimeMillis();
        System.out.println("3.  allOf = " + all.join() + " in " + elapsed(start) + "ms (5 x 100ms in parallel)");

        // 4. anyOf: the fastest replica wins. Returns Object, so you cast.
        CompletableFuture<Object> fastest = CompletableFuture.anyOf(
                CompletableFuture.supplyAsync(() -> slow("slow-replica", 300), IO_POOL),
                CompletableFuture.supplyAsync(() -> slow("fast-replica", 50), IO_POOL));
        System.out.println("4.  anyOf = " + fastest.join());

        // 5. Error handling
        CompletableFuture<String> failing = CompletableFuture.supplyAsync(() -> {
            throw new IllegalStateException("service down");
        });

        // exceptionally: recover, only runs on failure
        System.out.println("5a. exceptionally = " + failing.exceptionally(ex -> "fallback (" + cause(ex) + ")").join());

        // handle: runs on BOTH paths, sees (value, throwable) - one of them is null
        System.out.println("5b. handle        = " + failing.handle((v, ex) -> ex != null ? "recovered" : v).join());

        // whenComplete: peek without changing the result (logging, metrics). Still fails after.
        String peeked = failing
                .whenComplete((v, ex) -> System.out.println("5c. whenComplete saw " + cause(ex)))
                .exceptionally(ex -> "still failed")
                .join();
        System.out.println("5c. result after whenComplete = " + peeked);

        // TRICK: an exception inside the chain skips every downstream stage until a handler.
        String skipped = CompletableFuture.supplyAsync(() -> "a")
                .thenApply(s -> { throw new RuntimeException("boom"); })
                .thenApply(s -> "never runs: " + s)            // skipped entirely
                .exceptionally(ex -> "caught at the end")
                .join();
        System.out.println("5d. downstream stages skipped -> " + skipped);

        // 6. Timeouts (Java 9+)
        String timedOut = CompletableFuture.supplyAsync(() -> slow("late", 500), IO_POOL)
                .completeOnTimeout("default value", 100, TimeUnit.MILLISECONDS)   // substitute
                .join();
        System.out.println("6a. completeOnTimeout = " + timedOut);

        String orTimeout = CompletableFuture.supplyAsync(() -> slow("late", 500), IO_POOL)
                .orTimeout(100, TimeUnit.MILLISECONDS)                            // fail instead
                .exceptionally(ex -> "TimeoutException -> fallback")
                .join();
        System.out.println("6b. orTimeout         = " + orTimeout);

        // 7. Sequential vs parallel, the reason this class exists
        start = System.currentTimeMillis();
        String seq = loadProfile("u1").join() + loadProfile("u2").join();     // 2 x 150ms
        long sequential = elapsed(start);

        start = System.currentTimeMillis();
        CompletableFuture<String> p1 = loadProfile("u1"), p2 = loadProfile("u2");
        String par = p1.join() + p2.join();                                  // both already running
        System.out.println("7.  sequential " + sequential + "ms vs parallel " + elapsed(start)
                + "ms  (same result: " + seq.equals(par) + ")");

        // 8. Which thread runs the callback?
        CompletableFuture.supplyAsync(() -> "x", IO_POOL)
                .thenApply(s -> report("8a. thenApply      runs on"))        // often the completing thread
                .thenApplyAsync(s -> report("8b. thenApplyAsync runs on"), IO_POOL)  // always the given pool
                .join();

        // 9. Manual completion: bridging a callback API into a future
        CompletableFuture<String> manual = new CompletableFuture<>();
        new Thread(() -> { sleep(50); manual.complete("completed by hand"); }).start();
        System.out.println("9.  " + manual.join());

        // 10. Cancellation. NOTE: cancel does NOT interrupt the running supplier -
        //     it only makes the future fail fast for whoever is waiting. See TimedCancellation.
        CompletableFuture<String> cancellable = CompletableFuture.supplyAsync(() -> slow("work", 500), IO_POOL);
        System.out.println("10. cancelled = " + cancellable.cancel(true)
                + ", isCancelled = " + cancellable.isCancelled());

        IO_POOL.shutdown();
    }

    // ---------- helpers ----------
    static CompletableFuture<String> loadProfile(String userId) {
        return CompletableFuture.supplyAsync(() -> slow("profile:" + userId, 150), IO_POOL);
    }

    static <T> T slow(T value, long ms) { sleep(ms); return value; }

    static String report(String label) {
        System.out.println(label + " " + Thread.currentThread().getName());
        return label;
    }

    static String cause(Throwable ex) {
        return ex == null ? "nothing" : ex.getCause().getMessage();   // CompletionException wraps yours
    }

    static long elapsed(long start) { return System.currentTimeMillis() - start; }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
