package arrays;

import java.util.*;
import java.util.stream.*;

/**
 * Median problems.
 *
 * TRICK (static list): SORT, odd -> middle, even -> average of two middles.
 * TRICK (two sorted arrays): MERGE them (IntStream.concat + sorted) then same rule.
 * TRICK (running / stream of numbers): TWO HEAPS.
 *        low = MAX-heap (smaller half), high = MIN-heap (bigger half).
 *        Add to low -> move low's top to high -> if high bigger, move back.
 *        Median = low.peek() (odd) or avg of both tops (even).
 */
public class Median {

    // [5, 3, 1, 4, 2] -> 3.0 ; [4, 1, 3, 2] -> 2.5
    static double median(int[] nums) {
        int[] s = Arrays.stream(nums).sorted().toArray();
        int n = s.length;
        return n % 2 == 1 ? s[n / 2] : (s[n / 2 - 1] + s[n / 2]) / 2.0;
    }

    // Median of two sorted arrays (simple O((m+n) log) version - easy to remember)
    static double medianOfTwo(int[] a, int[] b) {
        return median(IntStream.concat(Arrays.stream(a), Arrays.stream(b)).toArray());
    }

    // Running median (LeetCode 295: MedianFinder)
    static class MedianFinder {
        private final PriorityQueue<Integer> low = new PriorityQueue<>(Comparator.reverseOrder()); // max-heap
        private final PriorityQueue<Integer> high = new PriorityQueue<>();                         // min-heap

        void add(int num) {
            low.offer(num);
            high.offer(low.poll());
            if (high.size() > low.size()) low.offer(high.poll());
        }

        double median() {
            return low.size() > high.size() ? low.peek() : (low.peek() + high.peek()) / 2.0;
        }
    }

    public static void main(String[] args) {
        System.out.println(median(new int[]{5, 3, 1, 4, 2}));                    // 3.0
        System.out.println(median(new int[]{4, 1, 3, 2}));                       // 2.5
        System.out.println(medianOfTwo(new int[]{1, 3}, new int[]{2}));          // 2.0
        System.out.println(medianOfTwo(new int[]{1, 2}, new int[]{3, 4}));       // 2.5

        MedianFinder mf = new MedianFinder();
        for (int x : new int[]{5, 15, 1, 3}) {
            mf.add(x);
            System.out.println("added " + x + " -> median " + mf.median());      // 5.0, 10.0, 5.0, 4.0
        }
    }
}
