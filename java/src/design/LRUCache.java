package design;

import java.util.*;

/**
 * LRU Cache (LeetCode 146).
 *
 * TRICK: LinkedHashMap(capacity, 0.75f, accessOrder = TRUE) + override removeEldestEntry.
 *        That's it - 5 lines. (If they forbid it: HashMap + doubly linked list.)
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {

    private final int capacity;

    public LRUCache(int capacity) {
        super(capacity, 0.75f, true);
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }

    public static void main(String[] args) {
        LRUCache<Integer, String> cache = new LRUCache<>(2);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.get(1);            // 1 is now most recently used
        cache.put(3, "three");   // evicts 2
        System.out.println(cache);                 // {1=one, 3=three}
        System.out.println(cache.containsKey(2));  // false
    }
}
