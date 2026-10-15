import java.nio.charset.StandardCharsets;
import java.util.BitSet;

/**
 * Bloom filter: "definitely NOT in the set" or "PROBABLY in the set".
 * Used by: web crawlers (URL seen?), Cassandra/RocksDB (skip SSTables), cache penetration protection.
 *
 *   java system-design/04_low_level_design/BloomFilter.java
 *
 * TRICKS:
 *  - m bits, k hash functions. add: set k bits. contains: all k bits set?
 *  - No false negatives. False positives ~ (1 - e^(-kn/m))^k. No deletes (use a counting Bloom filter).
 *  - Size for n items and false-positive rate p:  m = -n ln p / (ln 2)^2,   k = (m / n) ln 2
 *    -> about 9.6 bits per item for 1%, 14.4 bits for 0.1%.
 *  - k hashes from 2 base hashes (double hashing): h_i = h1 + i * h2
 */
public class BloomFilter {

    private final BitSet bits;
    private final int m, k;

    BloomFilter(int expectedItems, double falsePositiveRate) {
        this.m = (int) Math.ceil(-expectedItems * Math.log(falsePositiveRate) / (Math.log(2) * Math.log(2)));
        this.k = Math.max(1, (int) Math.round((double) m / expectedItems * Math.log(2)));
        this.bits = new BitSet(m);
    }

    void add(String item) {
        long h = fnv1a64(item);
        int h1 = (int) h, h2 = (int) (h >>> 32);
        for (int i = 0; i < k; i++) bits.set(Math.floorMod(h1 + i * h2, m));
    }

    boolean mightContain(String item) {
        long h = fnv1a64(item);
        int h1 = (int) h, h2 = (int) (h >>> 32);
        for (int i = 0; i < k; i++)
            if (!bits.get(Math.floorMod(h1 + i * h2, m))) return false; // one zero bit -> definitely absent
        return true;
    }

    // 64-bit FNV-1a with a final avalanche mix
    static long fnv1a64(String s) {
        long h = 0xcbf29ce484222325L;
        for (byte b : s.getBytes(StandardCharsets.UTF_8)) {
            h ^= (b & 0xff);
            h *= 0x100000001b3L;
        }
        h ^= h >>> 33; h *= 0xff51afd7ed558ccdL; h ^= h >>> 33; h *= 0xc4ceb9fe1a85ec53L; h ^= h >>> 33;
        return h;
    }

    public static void main(String[] args) {
        int n = 100_000;
        BloomFilter filter = new BloomFilter(n, 0.01);
        System.out.printf("for %d items at 1%% false positives: m=%d bits (%.1f KB), k=%d hashes%n",
                n, filter.m, filter.m / 8.0 / 1024, filter.k);

        for (int i = 0; i < n; i++) filter.add("https://site.com/page/" + i);

        int falseNegatives = 0;
        for (int i = 0; i < n; i++) if (!filter.mightContain("https://site.com/page/" + i)) falseNegatives++;

        int falsePositives = 0, probes = 100_000;
        for (int i = 0; i < probes; i++) if (filter.mightContain("https://other.com/item/" + i)) falsePositives++;

        System.out.println("false negatives (must be 0): " + falseNegatives);
        System.out.printf("false positive rate: %.2f%% (target 1%%)%n", 100.0 * falsePositives / probes);
        System.out.println("vs storing 100k URLs in a HashSet: several MB");
    }
}
