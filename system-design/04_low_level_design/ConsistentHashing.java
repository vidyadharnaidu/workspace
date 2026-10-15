import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Consistent hashing ring with virtual nodes.
 *
 *   java system-design/04_low_level_design/ConsistentHashing.java
 *
 * TRICKS:
 *  - Put nodes AND keys on the same hash ring (TreeMap<hash, node>).
 *  - A key belongs to the first node clockwise: ring.ceilingEntry(hash(key)), wrap to firstEntry().
 *  - VIRTUAL NODES: each physical node is placed many times ("A#0", "A#1", ...) -> even spread.
 *  - Adding a node moves only ~1/N of keys.  hash % N would move almost ALL keys.
 *  - Replicas = walk clockwise and collect the next N DISTINCT physical nodes.
 */
public class ConsistentHashing {

    static class HashRing {
        private final TreeMap<Long, String> ring = new TreeMap<>();
        private final int virtualNodes;

        HashRing(int virtualNodes) { this.virtualNodes = virtualNodes; }

        void addNode(String node) {
            for (int i = 0; i < virtualNodes; i++) ring.put(hash(node + "#" + i), node);
        }

        void removeNode(String node) {
            for (int i = 0; i < virtualNodes; i++) ring.remove(hash(node + "#" + i));
        }

        String nodeFor(String key) {
            Map.Entry<Long, String> e = ring.ceilingEntry(hash(key));
            return e != null ? e.getValue() : ring.firstEntry().getValue(); // wrap around the ring
        }

        List<String> replicasFor(String key, int n) {
            LinkedHashSet<String> nodes = new LinkedHashSet<>();
            long h = hash(key);
            for (String node : ring.tailMap(h, true).values()) { if (nodes.size() == n) break; nodes.add(node); }
            for (String node : ring.headMap(h, false).values()) { if (nodes.size() == n) break; nodes.add(node); }
            return new ArrayList<>(nodes);
        }
    }

    // First 8 bytes of MD5 as a long -> well-distributed positions on the ring
    static long hash(String s) {
        try {
            byte[] d = MessageDigest.getInstance("MD5").digest(s.getBytes(StandardCharsets.UTF_8));
            long h = 0;
            for (int i = 0; i < 8; i++) h = (h << 8) | (d[i] & 0xff);
            return h;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void main(String[] args) {
        int keys = 100_000;
        List<String> keyList = new ArrayList<>();
        for (int i = 0; i < keys; i++) keyList.add("user:" + i);

        // 1. Distribution with and without virtual nodes
        for (int vnodes : new int[]{1, 200}) {
            HashRing ring = new HashRing(vnodes);
            List.of("A", "B", "C").forEach(ring::addNode);
            Map<String, Integer> counts = new TreeMap<>();
            keyList.forEach(k -> counts.merge(ring.nodeFor(k), 1, Integer::sum));
            System.out.printf("virtual nodes=%-3d distribution of %d keys: %s%n", vnodes, keys, counts);
        }

        // 2. Add a 4th node: how many keys move?
        HashRing ring = new HashRing(200);
        List.of("A", "B", "C").forEach(ring::addNode);
        Map<String, String> before = new HashMap<>();
        keyList.forEach(k -> before.put(k, ring.nodeFor(k)));
        ring.addNode("D");
        long moved = keyList.stream().filter(k -> !before.get(k).equals(ring.nodeFor(k))).count();
        System.out.printf("%nConsistent hashing, 3 -> 4 nodes: %.1f%% of keys moved (ideal 25%%)%n", 100.0 * moved / keys);

        long movedModulo = keyList.stream()
                .filter(k -> Math.floorMod(k.hashCode(), 3) != Math.floorMod(k.hashCode(), 4))
                .count();
        System.out.printf("hash %% N,          3 -> 4 nodes: %.1f%% of keys moved%n", 100.0 * movedModulo / keys);

        // 3. Replicas and node removal
        System.out.println("\nreplicas for user:42 (N=3): " + ring.replicasFor("user:42", 3));
        String owner = ring.nodeFor("user:42");
        ring.removeNode(owner);
        System.out.println("after removing " + owner + ", user:42 -> " + ring.nodeFor("user:42")
                + " (the next replica takes over)");
    }
}
