package concurrency;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Semaphore: a counter of PERMITS. acquire() takes one (blocking if none left),
 * release() gives one back.
 *
 *  1. Rate limiting / connection pool: at most N at once
 *  2. tryAcquire: do not block, fail fast (with and without a timeout)
 *  3. Binary semaphore (1 permit) vs a lock
 *  4. Bounded resource pool, the real-world use
 *  5. Fair vs unfair
 *  6. Using it as a signal between threads
 *
 * TRICK: a semaphore has NO OWNER. Thread A can acquire and thread B can release - that
 *        is exactly what a lock forbids, and it is why a semaphore can signal across
 *        threads while a lock cannot.
 *
 * TRICK: always release in a finally. A leaked permit is permanent, and the pool shrinks
 *        silently until everything hangs.
 *
 * TRICK vs the others:
 *   Semaphore      N permits, reusable, no owner
 *   CountDownLatch counts DOWN to zero once, then it is done forever
 *   CyclicBarrier  waits for N parties, then RESETS and can be used again
 */
public class SemaphoreExamples {

    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(12);

        // 1. At most 3 concurrent calls, whatever the pool size is.
        Semaphore limiter = new Semaphore(3);
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();

        CountDownLatch done = new CountDownLatch(9);
        IntStream.rangeClosed(1, 9).forEach(i -> pool.submit(() -> {
            try {
                limiter.acquire();                       // blocks while 3 are already running
                try {
                    peak.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
                    Thread.sleep(60);                    // "the call"
                } finally {
                    inFlight.decrementAndGet();
                    limiter.release();                   // ALWAYS in finally
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        }));
        done.await();
        System.out.println("1. 9 tasks, 3 permits -> peak concurrency was " + peak.get() + " (never above 3)");

        // 2. tryAcquire: shed load instead of queueing
        Semaphore tiny = new Semaphore(1);
        tiny.acquire();
        System.out.println("2a. tryAcquire with no permits = " + tiny.tryAcquire());                    // false, instantly
        System.out.println("2b. tryAcquire waiting 50ms    = " + tiny.tryAcquire(50, TimeUnit.MILLISECONDS));
        tiny.release();
        System.out.println("2c. tryAcquire after release   = " + tiny.tryAcquire());

        // 3. Binary semaphore = mutual exclusion... with one crucial difference
        Semaphore binary = new Semaphore(1);
        binary.acquire();
        Thread releaser = new Thread(binary::release);   // a DIFFERENT thread releases it
        releaser.start(); releaser.join();
        System.out.println("3. released by another thread, permits = " + binary.availablePermits()
                + " (a ReentrantLock would have thrown IllegalMonitorStateException)");

        // 4. The classic: a bounded object pool
        ConnectionPool connections = new ConnectionPool(2);
        CountDownLatch poolDone = new CountDownLatch(6);
        IntStream.rangeClosed(1, 6).forEach(i -> pool.submit(() -> {
            try {
                String conn = connections.borrow();
                Thread.sleep(30);
                connections.giveBack(conn);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                poolDone.countDown();
            }
        }));
        poolDone.await();
        System.out.println("4. pool of 2 served 6 borrowers, max concurrent = " + connections.peak());

        // 5. Fairness: new Semaphore(n, true) hands permits out FIFO.
        //    Unfair (the default) is faster but can starve a waiter. Fair when latency must be predictable.
        Semaphore fair = new Semaphore(1, true);
        System.out.println("5. fair semaphore is FIFO; unfair is faster but may starve a thread");

        // 6. Semaphore as a SIGNAL: start with 0 permits and let another thread hand one over.
        Semaphore signal = new Semaphore(0);
        new Thread(() -> { sleep(50); System.out.println("6. producer signals"); signal.release(); }).start();
        signal.acquire();                                 // blocks until the release above
        System.out.println("6. consumer woke up after the signal");

        // 7. Draining and multi-permit acquire
        Semaphore batch = new Semaphore(5);
        batch.acquire(3);                                 // take 3 at once, e.g. a task needing 3 slots
        System.out.println("7a. after acquire(3), available = " + batch.availablePermits());
        System.out.println("7b. drainPermits() took " + batch.drainPermits() + ", available = " + batch.availablePermits());

        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);
    }

    /** A bounded pool: the semaphore guards the COUNT, a queue holds the objects. */
    static class ConnectionPool {
        private final Semaphore permits;
        private final BlockingQueue<String> idle = new LinkedBlockingQueue<>();
        private final AtomicInteger borrowed = new AtomicInteger();
        private final AtomicInteger peak = new AtomicInteger();

        ConnectionPool(int size) {
            this.permits = new Semaphore(size);
            IntStream.rangeClosed(1, size).forEach(i -> idle.add("conn-" + i));
        }

        String borrow() throws InterruptedException {
            permits.acquire();                            // wait for a free slot
            String conn = idle.take();                    // then take the actual object
            peak.accumulateAndGet(borrowed.incrementAndGet(), Math::max);
            return conn;
        }

        void giveBack(String conn) {
            borrowed.decrementAndGet();
            idle.offer(conn);
            permits.release();                            // release LAST, after the object is back
        }

        int peak() { return peak.get(); }
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
