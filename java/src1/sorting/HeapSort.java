import java.util.*;
import java.util.stream.IntStream;

/**
 * PATTERN: an array IS a binary heap.
 *   children of i = 2i+1 and 2i+2,  parent of i = (i-1)/2
 * Two phases, one helper:
 *   1. BUILD  - siftDown every non-leaf, from the last one backwards  -> O(n)
 *   2. SORTDOWN - swap root (the max) to the end, shrink, siftDown    -> O(n log n)
 *
 *   O(n log n) worst case, in place, NOT stable.
 */
public class HeapSort {

    // HOOK: build a max-heap, then repeatedly move the root to the back.
    static void heapSort(int[] a) {
        int n = a.length;
        for (int i = n / 2 - 1; i >= 0; i--) siftDown(a, i, n);   // phase 1: heapify
        for (int end = n - 1; end > 0; end--) {                   // phase 2: sort down
            swap(a, 0, end);                                      // max goes to its final slot
            siftDown(a, 0, end);                                  // restore the shrunken heap
        }
    }

    // HOOK: sink a value until both children are smaller. The whole heap in 8 lines.
    static void siftDown(int[] a, int i, int n) {
        while (true) {
            int left = 2 * i + 1, right = left + 1, largest = i;
            if (left < n && a[left] > a[largest]) largest = left;
            if (right < n && a[right] > a[largest]) largest = right;
            if (largest == i) return;                             // heap property restored
            swap(a, i, largest);
            i = largest;
        }
    }

    // HOOK: in an interview, "heap" usually means PriorityQueue - say so, then show this.
    static int[] sortWithPriorityQueue(int[] a) {
        PriorityQueue<Integer> pq = new PriorityQueue<>();
        for (int x : a) pq.offer(x);
        return IntStream.range(0, a.length).map(i -> pq.poll()).toArray();
    }

    static void swap(int[] a, int i, int j) { int t = a[i]; a[i] = a[j]; a[j] = t; }

    static int[] sample() { return new int[]{5, 2, 9, 1, 5, 6, 0, -3}; }

    static String check(int[] a) {
        int[] expected = sample();
        Arrays.sort(expected);
        return Arrays.toString(a) + (Arrays.equals(a, expected) ? "  OK" : "  WRONG");
    }

    public static void main(String[] args) {
        int[] h = sample(); heapSort(h);
        System.out.println("heapSort      = " + check(h));
        System.out.println("viaPriorityQ  = " + check(sortWithPriorityQueue(sample())));
    }
}
