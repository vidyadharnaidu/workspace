import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Snowflake-style 64-bit unique, time-sortable IDs (Twitter Snowflake).
 *
 *   java system-design/04_low_level_design/SnowflakeIdGenerator.java
 *
 *   | 1 bit sign (0) | 41 bits ms since custom epoch (~69 years) | 10 bits machine id (1024) | 12 bits sequence (4096/ms) |
 *
 * TRICKS:
 *  - id = (timestamp << 22) | (machineId << 12) | sequence
 *  - same millisecond -> sequence++ ; sequence overflow -> wait for next millisecond
 *  - clock moved backwards -> refuse (or wait), otherwise duplicates are possible
 *  - No coordination between machines (only unique machine ids), sortable by time, fits in a BIGINT index.
 */
public class SnowflakeIdGenerator {

    static final long EPOCH = Instant.parse("2024-01-01T00:00:00Z").toEpochMilli();
    static final int MACHINE_BITS = 10, SEQUENCE_BITS = 12;
    static final long MAX_MACHINE = (1L << MACHINE_BITS) - 1;   // 1023
    static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1; // 4095

    private final long machineId;
    private long lastTimestamp = -1;
    private long sequence = 0;

    SnowflakeIdGenerator(long machineId) {
        if (machineId < 0 || machineId > MAX_MACHINE) throw new IllegalArgumentException("machineId 0..1023");
        this.machineId = machineId;
    }

    synchronized long nextId() {
        long now = System.currentTimeMillis();
        if (now < lastTimestamp) throw new IllegalStateException("clock moved backwards by " + (lastTimestamp - now) + " ms");
        if (now == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {                              // 4096 ids used this ms -> wait for the next ms
                while (now <= lastTimestamp) now = System.currentTimeMillis();
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = now;
        return ((now - EPOCH) << (MACHINE_BITS + SEQUENCE_BITS)) | (machineId << SEQUENCE_BITS) | sequence;
    }

    static String decode(long id) {
        long ts = (id >>> (MACHINE_BITS + SEQUENCE_BITS)) + EPOCH;
        long machine = (id >>> SEQUENCE_BITS) & MAX_MACHINE;
        long seq = id & SEQUENCE_MASK;
        return "time=" + Instant.ofEpochMilli(ts) + " machine=" + machine + " sequence=" + seq;
    }

    public static void main(String[] args) throws Exception {
        SnowflakeIdGenerator machine1 = new SnowflakeIdGenerator(1);
        SnowflakeIdGenerator machine2 = new SnowflakeIdGenerator(2);

        long id = machine1.nextId();
        System.out.println("id      = " + id);
        System.out.println("decoded = " + decode(id));

        // 8 threads, 2 "machines", 400,000 ids -> all unique
        Set<Long> ids = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < 8; t++) {
            SnowflakeIdGenerator gen = t % 2 == 0 ? machine1 : machine2;
            futures.add(pool.submit(() -> {
                for (int i = 0; i < 50_000; i++) ids.add(gen.nextId());
            }));
        }
        for (Future<?> f : futures) f.get();
        pool.shutdown();
        System.out.println("\ngenerated 400000 ids from 8 threads on 2 machines -> unique: " + ids.size());

        // time-sortable: ids generated later are bigger
        long a = machine1.nextId();
        Thread.sleep(2);
        long b = machine2.nextId();
        System.out.println("later id is larger (sortable by time): " + (b > a));
    }
}
