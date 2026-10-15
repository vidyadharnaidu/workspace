package concurrency;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * CountDownLatch, CyclicBarrier and Phaser - the "wait for other threads" trio.
 *
 * TRICK - the one-line difference:
 *   CountDownLatch  ONE-SHOT.  Counts DOWN to zero and stays there. Waiters and counters
 *                   are DIFFERENT threads: the main thread waits, workers count down.
 *   CyclicBarrier   REUSABLE.  N parties all call await() and all block until the last one
 *                   arrives, then they ALL continue - and the barrier RESETS.
 *   Phaser          a barrier where parties can JOIN and LEAVE between rounds.
 *
 * TRICK: "latch = gate that opens once, barrier = meeting point you can use every round."
 * TRICK: a latch cannot be reset. If you need another round, you need a new latch - or a barrier.
 * TRICK: ALWAYS use await(timeout). A barrier whose party died hangs the rest forever.
 */
public class LatchAndBarrier {

    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);

        // ================= 1. CountDownLatch: wait for N workers to finish =================
        CountDownLatch finished = new CountDownLatch(4);
        IntStream.rangeClosed(1, 4).forEach(i -> pool.submit(() -> {
            try {
                Thread.sleep(50L * i);
                System.out.println("1. worker " + i + " done");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                finished.countDown();            // ALWAYS in finally, or a crashed worker hangs main
            }
        }));
        boolean allDone = finished.await(2, TimeUnit.SECONDS);   // timed: never await() forever
        System.out.println("1. main resumed, all finished = " + allDone + ", count = " + finished.getCount());

        // ================= 2. CountDownLatch as a STARTING GUN =================
        // The other direction: one latch of 1 holds N threads until you fire it.
        // This is how you write a load test - everyone starts at the same instant.
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch racers = new CountDownLatch(5);
        AtomicInteger startedTogether = new AtomicInteger();

        IntStream.rangeClosed(1, 5).forEach(i -> pool.submit(() -> {
            try {
                startGun.await();                // all 5 park here
                startedTogether.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                racers.countDown();
            }
        }));
        Thread.sleep(50);
        System.out.println("2. before the gun, started = " + startedTogether.get());
        startGun.countDown();                    // fire: all 5 released at once
        racers.await(2, TimeUnit.SECONDS);
        System.out.println("2. after the gun,  started = " + startedTogether.get());

        // ================= 3. CyclicBarrier: N threads meet each round =================
        int parties = 3;
        AtomicInteger round = new AtomicInteger();

        // The barrier ACTION runs on the last thread to arrive, once per round, before
        // anyone is released - the natural place to merge the round's results.
        CyclicBarrier barrier = new CyclicBarrier(parties,
                () -> System.out.println("3. --- barrier tripped, end of round " + round.incrementAndGet() + " ---"));

        CountDownLatch simulationDone = new CountDownLatch(parties);
        IntStream.rangeClosed(1, parties).forEach(id -> pool.submit(() -> {
            try {
                for (int r = 1; r <= 3; r++) {                // three rounds, ONE barrier
                    Thread.sleep(20L * id);                   // uneven work
                    System.out.println("3. worker " + id + " finished round " + r + ", waiting at barrier");
                    barrier.await(2, TimeUnit.SECONDS);       // nobody proceeds until all 3 arrive
                }
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            } finally {
                simulationDone.countDown();
            }
        }));
        simulationDone.await(5, TimeUnit.SECONDS);
        System.out.println("3. barrier reused for " + round.get() + " rounds, parties = " + barrier.getParties());

        // ================= 4. A broken barrier =================
        // TRICK: if one party times out, is interrupted, or throws, the barrier BREAKS and
        // every other waiter gets BrokenBarrierException. It is all-or-nothing by design.
        CyclicBarrier fragile = new CyclicBarrier(2);
        Thread waiter = new Thread(() -> {
            try {
                fragile.await();
            } catch (BrokenBarrierException e) {
                System.out.println("4. other party got BrokenBarrierException");
            } catch (InterruptedException e) {
                System.out.println("4. this waiter was interrupted");
            }
        });
        waiter.start();
        Thread.sleep(50);
        waiter.interrupt();                      // one party dies -> the barrier is now broken
        waiter.join();
        System.out.println("4. barrier isBroken = " + fragile.isBroken() + "  (reset() repairs it)");
        fragile.reset();

        // ================= 5. Phaser: a barrier with a changing number of parties =================
        Phaser phaser = new Phaser(1);           // "1" = this main thread registers itself
        IntStream.rangeClosed(1, 3).forEach(i -> {
            phaser.register();                   // parties can join AFTER construction - a barrier cannot do this
            pool.submit(() -> {
                for (int p = 0; p < 2; p++) {
                    sleep(20L * i);
                    phaser.arriveAndAwaitAdvance();      // same meaning as barrier.await()
                }
                phaser.arriveAndDeregister();            // ...and leave early. Also impossible with a barrier.
            });
        });

        phaser.arriveAndAwaitAdvance();          // main takes part in phase 0
        System.out.println("5. phase " + phaser.getPhase() + " complete, registered parties = " + phaser.getRegisteredParties());
        phaser.arriveAndDeregister();            // main drops out; the workers finish on their own

        // ================= 6. Which one do I use? =================
        System.out.println("""
                6. Choosing:
                   - "main waits for N workers to finish"         -> CountDownLatch(N)
                   - "N threads all start at the same moment"     -> CountDownLatch(1) as a start gun
                   - "N threads sync up every round"              -> CyclicBarrier(N)
                   - "same, but threads join and leave"           -> Phaser
                   - "at most N at a time"                        -> Semaphore(N)
                   - "run tasks, collect results"                 -> invokeAll / CompletableFuture.allOf""");

        pool.shutdown();
        pool.awaitTermination(3, TimeUnit.SECONDS);
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
