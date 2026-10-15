package concurrency;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Producer-Consumer with MATCHING COUNTERS (produced == consumed).
 *
 * TRICK: BlockingQueue does all the waiting for you (put blocks when full, take blocks when empty).
 *        AtomicInteger for counters (thread-safe, no locks).
 *        POISON PILL: after producers finish, put one pill PER consumer so each one stops.
 */
public class ProducerConsumer {

    static final int POISON = -1;

    public static void main(String[] args) throws Exception {
        int producers = 3, consumers = 2, itemsPerProducer = 1000;

        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);
        AtomicInteger produced = new AtomicInteger();
        AtomicInteger consumed = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(producers + consumers);

        // Consumers: take until poison pill
        IntStream.range(0, consumers).forEach(c -> pool.submit(() -> {
            try {
                while (queue.take() != POISON) consumed.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));

        // Producers: put N items each
        List<Future<?>> producerTasks = IntStream.range(0, producers)
                .<Future<?>>mapToObj(p -> pool.submit(() -> {
                    try {
                        for (int i = 0; i < itemsPerProducer; i++) {
                            queue.put(i);
                            produced.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }))
                .toList();

        for (Future<?> f : producerTasks) f.get();          // wait for all producers
        for (int i = 0; i < consumers; i++) queue.put(POISON); // stop every consumer

        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("Produced: " + produced.get());
        System.out.println("Consumed: " + consumed.get());
        System.out.println("Match:    " + (produced.get() == consumed.get()));
    }
}
