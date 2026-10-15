package concurrency;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread questions, beyond the basics in ThreadBasics:
 *  1. Four ways to create one (Thread, Runnable, Callable+Future, executor)
 *  2. start() vs run()
 *  3. join(): wait for another thread
 *  4. Interruption: the only cooperative way to stop a thread
 *  5. Daemon threads
 *  6. Deadlock, and the lock-ordering fix
 *  7. volatile: visibility, not atomicity
 *  8. ThreadLocal
 *  9. Uncaught exception handler
 * 10. The executor shutdown idiom
 *
 * TRICK: run() just calls the method on the CURRENT thread. Only start() spawns one.
 * TRICK: Thread.stop() is gone. You interrupt, and the thread has to check the flag.
 */
public class ThreadLifecycle {

    // 7. Without volatile the JIT may hoist the read out of the loop -> infinite spin.
    static volatile boolean running = true;

    static final ThreadLocal<String> USER = ThreadLocal.withInitial(() -> "anonymous");

    public static void main(String[] args) throws Exception {
        // 1a. Subclass Thread
        Thread t1 = new Thread() {
            @Override public void run() { System.out.println("1a. subclass on " + Thread.currentThread().getName()); }
        };

        // 1b. Runnable: preferred, it leaves your class free to extend something else
        Thread t2 = new Thread(() -> System.out.println("1b. runnable on " + Thread.currentThread().getName()), "worker-2");

        // 2. start() vs run()
        t1.start();
        t2.start();
        t2.join();                       // 3. main blocks until worker-2 finishes
        new Thread(() -> System.out.println("2.  run() executes on " + Thread.currentThread().getName())).run();
        t1.join();

        // 1c. Callable + Future: the only form that returns a value or reports an exception
        ExecutorService pool = Executors.newFixedThreadPool(4);
        Future<Integer> future = pool.submit(() -> 6 * 7);
        System.out.println("1c. future.get()   = " + future.get());                    // blocks
        System.out.println("1c. timed get      = " + pool.submit(() -> "ok").get(1, TimeUnit.SECONDS));

        // 4. Interruption. A blocking call throws InterruptedException; a busy loop polls the flag.
        Thread sleeper = new Thread(() -> {
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                // TRICK: catching InterruptedException CLEARS the flag, so restore it.
                Thread.currentThread().interrupt();
                System.out.println("4.  interrupted while sleeping, flag restored = "
                        + Thread.currentThread().isInterrupted());
            }
        });
        sleeper.start();
        sleeper.interrupt();
        sleeper.join();

        // 5. Daemon thread: the JVM exits without waiting for it.
        Thread daemon = new Thread(() -> { while (true) { /* housekeeping */ } });
        daemon.setDaemon(true);          // must be set BEFORE start()
        daemon.start();
        System.out.println("5.  daemon alive = " + daemon.isAlive() + ", the JVM will not wait for it");

        // 6. Deadlock
        deadlockDemo();

        // 7. volatile stop flag
        Thread spinner = new Thread(() -> {
            long n = 0;
            while (running) n++;
            System.out.println("7.  volatile flag stopped the spin after " + n + " iterations");
        });
        spinner.start();
        Thread.sleep(50);
        running = false;
        spinner.join();

        // 8. ThreadLocal: one value per thread, no locking needed
        USER.set("main-user");
        Thread other = new Thread(() -> System.out.println("8.  other thread sees " + USER.get()));
        other.start(); other.join();
        System.out.println("8.  main thread sees " + USER.get());
        USER.remove();                   // TRICK: always remove in pooled threads, or you leak

        // 9. Uncaught exception handler
        Thread boom = new Thread(() -> { throw new IllegalStateException("kaboom"); }, "boom");
        boom.setUncaughtExceptionHandler((th, ex) -> System.out.println("9.  " + th.getName() + " died with " + ex.getMessage()));
        boom.start(); boom.join();

        // 10.
        shutdown(pool);
    }

    /** TRICK: the fix is a global lock ORDER - everyone takes A then B, never B then A. */
    static void deadlockDemo() throws InterruptedException {
        // The broken pair, on their own locks.
        Object a = new Object(), b = new Object();
        AtomicLong deadlocking = new AtomicLong();
        Thread d1 = new Thread(() -> { synchronized (a) { sleep(20); synchronized (b) { deadlocking.incrementAndGet(); } } }, "d1");
        Thread d2 = new Thread(() -> { synchronized (b) { sleep(20); synchronized (a) { deadlocking.incrementAndGet(); } } }, "d2");
        d1.setDaemon(true); d2.setDaemon(true);          // daemon, so the stuck pair cannot hang this demo
        d1.start(); d2.start();
        d1.join(300); d2.join(300);
        System.out.println("6.  opposite lock order -> deadlocked = " + (deadlocking.get() < 2)
                + "  (d1=" + d1.getState() + ", d2=" + d2.getState() + ")");
        // Those two threads are stuck holding a and b forever, so the fixed demo needs FRESH locks -
        // which is the practical lesson: one deadlock poisons those locks for every later caller.

        Object x = new Object(), y = new Object();
        AtomicLong completed = new AtomicLong();
        Runnable xThenY = () -> { synchronized (x) { sleep(20); synchronized (y) { completed.incrementAndGet(); } } };
        Thread f1 = new Thread(xThenY), f2 = new Thread(xThenY);   // SAME order in both
        f1.start(); f2.start(); f1.join(); f2.join();
        System.out.println("6.  same lock order     -> both finished, completions = " + completed.get());
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /** The shutdown idiom every interviewer wants to hear. */
    static void shutdown(ExecutorService pool) throws InterruptedException {
        pool.shutdown();                                     // stop accepting, finish what is queued
        if (!pool.awaitTermination(2, TimeUnit.SECONDS)) {
            pool.shutdownNow();                              // interrupt the running tasks
            pool.awaitTermination(2, TimeUnit.SECONDS);
        }
        System.out.println("10. pool terminated = " + pool.isTerminated());
    }
}
