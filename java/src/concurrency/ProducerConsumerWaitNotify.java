package concurrency;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Producer-Consumer the "old school" way: synchronized + wait/notifyAll.
 * Interviewers ask this to check you understand what BlockingQueue does internally.
 *
 * TRICK: always wait in a WHILE loop (spurious wake-ups), always notifyAll after changing state.
 */
public class ProducerConsumerWaitNotify {

    static class Buffer {
        private final Queue<Integer> items = new LinkedList<>();
        private final int capacity;
        private int produced, consumed; // guarded by "this"

        Buffer(int capacity) { this.capacity = capacity; }

        synchronized void put(int value) throws InterruptedException {
            while (items.size() == capacity) wait();
            items.add(value);
            produced++;
            notifyAll();
        }

        synchronized int take() throws InterruptedException {
            while (items.isEmpty()) wait();
            int value = items.poll();
            consumed++;
            notifyAll();
            return value;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Buffer buffer = new Buffer(5);
        int count = 20;

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= count; i++) buffer.put(i);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });

        Thread consumer = new Thread(() -> {
            try {
                long sum = 0;
                for (int i = 1; i <= count; i++) sum += buffer.take();
                System.out.println("Sum consumed = " + sum + " (expected " + count * (count + 1) / 2 + ")");
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();

        synchronized (buffer) {
            System.out.println("Produced " + buffer.produced + ", Consumed " + buffer.consumed
                    + ", Match = " + (buffer.produced == buffer.consumed));
        }
    }
}
