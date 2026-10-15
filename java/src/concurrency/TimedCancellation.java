package concurrency;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * "Let a thread process for 10 minutes, then cancel it."
 *
 * The interview question behind it: HOW DO YOU STOP A RUNNING THREAD?
 * Answer: you cannot force it. Thread.stop() is removed - it released locks mid-update and
 * left objects corrupted. Every real answer is COOPERATIVE: you ask, the task checks.
 *
 * Five ways, in the order you should reach for them:
 *   1. Deadline inside the task   - the task knows its own budget and stops cleanly
 *   2. Future.get(timeout) + cancel(true)  - the caller waits at most N, then interrupts
 *   3. ScheduledExecutorService   - fire-and-forget: schedule the cancel up front
 *   4. awaitTermination + shutdownNow      - for a whole pool of tasks
 *   5. AtomicBoolean stop flag    - when the work is not interruptible
 *
 * TRICK: cancel(true) does NOT kill anything. It sets the interrupt flag. If the task never
 *        blocks and never checks, it runs to completion regardless.
 * TRICK: cancel(false) is weaker still - it only stops a task that has not STARTED yet.
 * TRICK: catching InterruptedException CLEARS the flag. Restore it, or return immediately.
 * TRICK: the deadline must be an ABSOLUTE instant (nanoTime + budget), computed once.
 *        Re-deriving "10 minutes from now" inside a loop means it never expires.
 *
 * Run with no arguments for a fast demo (the 10 minutes is scaled to 600ms).
 * Run with `real` to use the actual 10 minutes.
 */
public class TimedCancellation {

    /** The real budget. The demo scales it down so the file stays runnable. */
    static final Duration REAL_BUDGET = Duration.ofMinutes(10);

    static Duration budget = Duration.ofMillis(600);

    public static void main(String[] args) throws Exception {
        boolean real = args.length > 0 && args[0].equals("real");
        if (real) budget = REAL_BUDGET;
        System.out.println("budget = " + budget.toMillis() + "ms" + (real ? " (the real 10 minutes)" : " (demo scale for 10 minutes)"));
        System.out.println();

        approach1_deadlineInsideTheTask();
        approach2_futureGetWithTimeout();
        approach3_scheduledCancel();
        approach4_poolShutdownNow();
        approach5_stopFlagForUninterruptibleWork();
        pitfall_taskThatIgnoresInterrupt();
    }

    // ========================= 1. Deadline inside the task =========================
    /**
     * The cleanest one: the task owns its budget, so it stops at a safe point, finishes its
     * current item, and reports what it got through. Nothing is interrupted mid-write.
     * Prefer this whenever you control the task's code.
     */
    static void approach1_deadlineInsideTheTask() throws Exception {
        long deadline = System.nanoTime() + budget.toNanos();      // ABSOLUTE, computed once

        Callable<Long> job = () -> {
            long processed = 0;
            while (System.nanoTime() < deadline) {
                if (Thread.currentThread().isInterrupted()) break;  // still honour interrupts
                processOneItem();
                processed++;
            }
            return processed;                                      // a clean, partial result
        };

        ExecutorService pool = Executors.newSingleThreadExecutor();
        long processed = pool.submit(job).get();
        shutdown(pool);
        System.out.println("1. deadline in the task    : stopped itself after " + processed
                + " items, no interrupt needed");
    }

    // ========================= 2. Future.get(timeout) + cancel =========================
    /**
     * The caller-side version, for when the task does NOT know about any budget.
     * get(timeout) throws TimeoutException; the cancel(true) after it is what actually
     * interrupts the thread. Forgetting that cancel leaves the task running forever.
     */
    static void approach2_futureGetWithTimeout() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        AtomicLong processed = new AtomicLong();

        Future<?> future = pool.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                processOneItem();
                processed.incrementAndGet();
            }
            return null;
        });

        try {
            future.get(budget.toMillis(), TimeUnit.MILLISECONDS);   // wait AT MOST the budget
            System.out.println("2. get(timeout) + cancel   : finished on its own");
        } catch (TimeoutException e) {
            boolean cancelled = future.cancel(true);                // true = interrupt if running
            System.out.println("2. get(timeout) + cancel   : timed out, cancel returned " + cancelled
                    + ", processed " + processed.get() + " items");
        } catch (ExecutionException e) {
            System.out.println("2. task threw: " + e.getCause());   // the REAL exception is the cause
        } finally {
            shutdown(pool);
        }
    }

    // ========================= 3. Schedule the cancellation up front =========================
    /**
     * Fire and forget: submit the work, and in the same breath schedule its cancellation.
     * The main thread is free in between - no one sits blocked on a get().
     * This is the natural shape for "start it, and kill it in 10 minutes".
     */
    static void approach3_scheduledCancel() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
        AtomicLong processed = new AtomicLong();

        Future<?> future = worker.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                processOneItem();
                processed.incrementAndGet();
            }
        });

        ScheduledFuture<?> killSwitch = timer.schedule(
                () -> future.cancel(true), budget.toMillis(), TimeUnit.MILLISECONDS);

        // ... main thread does other things here ...

        try {
            future.get();                                    // just to observe the outcome
        } catch (CancellationException expected) {
            System.out.println("3. scheduled kill switch   : cancelled on schedule after "
                    + processed.get() + " items");
        } finally {
            killSwitch.cancel(false);                        // if the job finished early, cancel the timer
            shutdown(worker);
            shutdown(timer);
        }
    }

    // ========================= 4. Whole pool: awaitTermination + shutdownNow =========================
    /**
     * For N tasks instead of one. THE standard shutdown idiom:
     *   shutdown()            stop accepting new work, let the current work finish
     *   awaitTermination(T)   give it T to wind down
     *   shutdownNow()         out of time - interrupt everything still running
     */
    static void approach4_poolShutdownNow() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(3);
        AtomicLong processed = new AtomicLong();

        for (int i = 0; i < 3; i++) {
            pool.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    processOneItem();
                    processed.incrementAndGet();
                }
            });
        }

        pool.shutdown();
        if (!pool.awaitTermination(budget.toMillis(), TimeUnit.MILLISECONDS)) {
            var neverStarted = pool.shutdownNow();            // returns the tasks still queued
            boolean stopped = pool.awaitTermination(2, TimeUnit.SECONDS);
            System.out.println("4. pool shutdownNow        : budget expired, interrupted the workers"
                    + " (queued and never started: " + neverStarted.size() + ", all stopped: " + stopped
                    + ", processed " + processed.get() + ")");
        }
    }

    // ========================= 5. A stop flag, for work that cannot be interrupted =========================
    /**
     * Interruption only unblocks sleep/wait/join/BlockingQueue/InterruptibleChannel.
     * A tight CPU loop, or a legacy blocking socket read, ignores it completely.
     * Then you need an explicit flag the task polls.
     *
     * TRICK: the flag must be volatile or an Atomic, or the worker may never see the write.
     * TRICK: for a blocking socket that ignores interrupts, the only real cancel is to
     *        CLOSE the socket from another thread - the read then throws.
     */
    static void approach5_stopFlagForUninterruptibleWork() throws Exception {
        AtomicBoolean stop = new AtomicBoolean(false);
        AtomicLong processed = new AtomicLong();

        Thread worker = new Thread(() -> {
            while (!stop.get()) {                             // the only exit
                burnCpu();
                processed.incrementAndGet();
            }
        }, "uninterruptible-worker");
        worker.start();

        Thread.sleep(budget.toMillis());
        stop.set(true);                                       // ask it to stop
        worker.join(2_000);                                   // and WAIT for it - do not assume

        System.out.println("5. explicit stop flag      : worker alive = " + worker.isAlive()
                + ", processed " + processed.get() + " chunks");
    }

    // ========================= The pitfall to be able to name =========================
    /**
     * A task that swallows InterruptedException without restoring the flag becomes
     * uncancellable. This is the single most common concurrency bug in real code.
     */
    static void pitfall_taskThatIgnoresInterrupt() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        AtomicLong loops = new AtomicLong();

        Future<?> future = pool.submit(() -> {
            while (true) {
                try {
                    Thread.sleep(20);
                    loops.incrementAndGet();
                } catch (InterruptedException e) {
                    // WRONG: swallowed, flag cleared, loop continues. The task is now immortal.
                    // RIGHT: Thread.currentThread().interrupt(); return;
                }
                if (loops.get() > 1_000) return;              // safety net so the demo ends
            }
        });

        Thread.sleep(100);
        future.cancel(true);
        Thread.sleep(100);

        System.out.println("6. PITFALL                 : future.isCancelled() = " + future.isCancelled()
                + " but the thread kept looping (" + loops.get() + " and counting)"
                + " - swallowing InterruptedException makes a task uncancellable");

        pool.shutdownNow();
    }

    // ---------- helpers ----------

    /** One unit of interruptible work. */
    static void processOneItem() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();               // restore, so the loop condition sees it
        }
    }

    /** CPU work that interruption cannot touch. */
    static void burnCpu() {
        long x = 0;
        for (int i = 0; i < 200_000; i++) x += i;
        if (x == Long.MIN_VALUE) System.out.print("");        // stop the JIT eliding the loop
    }

    static void shutdown(ExecutorService pool) throws InterruptedException {
        pool.shutdown();
        if (!pool.awaitTermination(2, TimeUnit.SECONDS)) {
            pool.shutdownNow();
            pool.awaitTermination(2, TimeUnit.SECONDS);
        }
    }
}
