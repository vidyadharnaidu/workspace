package concurrency;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Popular threading questions:
 *  1. Race condition: int++ vs AtomicInteger vs synchronized
 *  2. Print odd/even alternately with two threads
 *  3. CompletableFuture: run tasks in parallel and combine
 *  4. ExecutorService invokeAll
 *  5. CountDownLatch: wait for N workers
 *
 * TRICK: count++ is READ-ADD-WRITE (3 steps) -> not atomic -> lost updates.
 */
public class ThreadBasics {

    static int unsafeCounter = 0;
    static int syncCounter = 0;

    static synchronized void incrementSync() { syncCounter++; }

    public static void main(String[] args) throws Exception {
        // 1. Race condition
        AtomicInteger atomic = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Callable<Void>> tasks = IntStream.range(0, 8).<Callable<Void>>mapToObj(t -> () -> {
            for (int i = 0; i < 100_000; i++) {
                unsafeCounter++;
                incrementSync();
                atomic.incrementAndGet();
            }
            return null;
        }).toList();
        pool.invokeAll(tasks); // 4. invokeAll waits for all
        System.out.println("1. unsafe=" + unsafeCounter + " sync=" + syncCounter + " atomic=" + atomic.get() + " (expected 800000)");

        // 2. Odd/Even printer
        OddEvenPrinter printer = new OddEvenPrinter(10);
        Thread odd = new Thread(() -> printer.print(true));
        Thread even = new Thread(() -> printer.print(false));
        odd.start(); even.start();
        odd.join(); even.join();
        System.out.println();

        // 3. CompletableFuture: parallel + combine
        CompletableFuture<Integer> price = CompletableFuture.supplyAsync(() -> 100, pool);
        CompletableFuture<Double> tax = CompletableFuture.supplyAsync(() -> 0.2, pool);
        String total = price.thenCombine(tax, (p, t) -> p * (1 + t))
                .thenApply(v -> "3. total = " + v)
                .exceptionally(ex -> "failed: " + ex.getMessage())
                .join();
        System.out.println(total);

        // 5. CountDownLatch
        CountDownLatch latch = new CountDownLatch(3);
        IntStream.range(0, 3).forEach(i -> pool.submit(() -> {
            System.out.println("5. worker " + i + " done");
            latch.countDown();
        }));
        latch.await();
        System.out.println("5. all workers done");

        pool.shutdown();
    }

    // TRICK: shared lock + boolean "whose turn"; each thread waits while it's not its turn.
    static class OddEvenPrinter {
        private final int max;
        private int current = 1;

        OddEvenPrinter(int max) { this.max = max; }

        synchronized void print(boolean oddThread) {
            while (current <= max) {
                if ((current % 2 == 1) == oddThread) {
                    System.out.print((oddThread ? "O:" : "E:") + current++ + " ");
                    notifyAll();
                } else {
                    try { wait(); } catch (InterruptedException e) { return; }
                }
            }
            notifyAll();
        }
    }
}
