import java.util.*;
import java.util.stream.IntStream;

/** PATTERN: sorted (or half-sorted) input -> halve the search space each step. */
public class SearchSort {

    // HOOK: plain binary search; l is where the value belongs when not found.
    static int searchInsert(int[] a, int target) {
        int l = 0, r = a.length - 1;
        while (l <= r) {
            int m = l + (r - l) / 2;
            if (a[m] == target) return m;
            if (a[m] < target) l = m + 1; else r = m - 1;
        }
        return l;
    }

    // HOOK: one half is always sorted - decide if target lies inside it.
    static int searchRotated(int[] a, int target) {
        int l = 0, r = a.length - 1;
        while (l <= r) {
            int m = l + (r - l) / 2;
            if (a[m] == target) return m;
            if (a[l] <= a[m]) {                                  // left half sorted
                if (target >= a[l] && target < a[m]) r = m - 1; else l = m + 1;
            } else {                                             // right half sorted
                if (target > a[m] && target <= a[r]) l = m + 1; else r = m - 1;
            }
        }
        return -1;
    }

    // HOOK: compare mid to RIGHT end; a[m] > a[r] means the min is to the right.
    static int findMin(int[] a) {
        int l = 0, r = a.length - 1;
        while (l < r) {
            int m = l + (r - l) / 2;
            if (a[m] > a[r]) l = m + 1; else r = m;
        }
        return a[l];
    }

    // HOOK: binary search the CUT in the shorter array; maxLeft <= minRight.
    static double findMedianSortedArrays(int[] a, int b[]) {
        if (a.length > b.length) return findMedianSortedArrays(b, a);
        int m = a.length, n = b.length, half = (m + n + 1) / 2;
        int l = 0, r = m;
        while (l <= r) {
            int i = (l + r) / 2, j = half - i;
            int aL = i == 0 ? Integer.MIN_VALUE : a[i - 1];
            int aR = i == m ? Integer.MAX_VALUE : a[i];
            int bL = j == 0 ? Integer.MIN_VALUE : b[j - 1];
            int bR = j == n ? Integer.MAX_VALUE : b[j];
            if (aL <= bR && bL <= aR)
                return ((m + n) % 2 == 1)
                        ? Math.max(aL, bL)
                        : (Math.max(aL, bL) + Math.min(aR, bR)) / 2.0;
            if (aL > bR) r = i - 1; else l = i + 1;
        }
        return 0;
    }

    // HOOK: min-heap of size k; its root is the kth largest.
    static int findKthLargest(int[] a, int k) {
        PriorityQueue<Integer> pq = new PriorityQueue<>();
        for (int x : a) {
            pq.offer(x);
            if (pq.size() > k) pq.poll();
        }
        return pq.peek();
    }

    // HOOK: bucket sort - n-1 buckets, so the max gap must straddle two buckets.
    static int maximumGap(int[] a) {
        int n = a.length;
        if (n < 2) return 0;
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int x : a) { min = Math.min(min, x); max = Math.max(max, x); }
        if (min == max) return 0;
        int size = Math.max(1, (max - min) / (n - 1)), count = (max - min) / size + 1;
        int[] lo = new int[count], hi = new int[count];
        Arrays.fill(lo, Integer.MAX_VALUE);
        Arrays.fill(hi, Integer.MIN_VALUE);
        for (int x : a) {
            int b = (x - min) / size;
            lo[b] = Math.min(lo[b], x);
            hi[b] = Math.max(hi[b], x);
        }
        int gap = 0, prev = min;
        for (int i = 0; i < count; i++) {
            if (lo[i] == Integer.MAX_VALUE) continue;            // empty bucket
            gap = Math.max(gap, lo[i] - prev);
            prev = hi[i];
        }
        return gap;
    }

    // HOOK: compare against a sorted copy, count mismatched positions.
    static int heightChecker(int[] h) {
        int[] s = h.clone();
        Arrays.sort(s);
        return (int) IntStream.range(0, h.length).filter(i -> h[i] != s[i]).count();
    }

    // HOOK: sort desc; x is special when a[x-1] >= x and (x==n or a[x] < x).
    static int specialArray(int[] a) {
        Arrays.sort(a);
        int n = a.length;
        for (int x = 1; x <= n; x++)
            if (a[n - x] >= x && (x == n || a[n - x - 1] < x)) return x;
        return -1;
    }

    // HOOK: a rotated-sorted array has at most ONE drop, counting the wrap-around.
    static boolean checkIfSortedAndRotated(int[] a) {
        int n = a.length, drops = 0;
        for (int i = 0; i < n; i++) if (a[i] > a[(i + 1) % n]) drops++;
        return drops <= 1;
    }

    // HOOK: distinct, sort descending, take 3; fewer than 3 -> return the max.
    static int thirdMax(int[] a) {
        List<Integer> top = Arrays.stream(a).distinct().boxed()
                .sorted(Comparator.reverseOrder()).limit(3).toList();
        return top.size() < 3 ? top.get(0) : top.get(2);
    }

    public static void main(String[] args) {
        System.out.println("searchInsert      = " + searchInsert(new int[]{1, 3, 5, 6}, 5));
        System.out.println("searchInsert miss = " + searchInsert(new int[]{1, 3, 5, 6}, 2));
        System.out.println("searchRotated     = " + searchRotated(new int[]{4, 5, 6, 7, 0, 1, 2}, 0));
        System.out.println("findMin rotated   = " + findMin(new int[]{4, 5, 6, 7, 0, 1, 2}));
        System.out.println("median            = " + findMedianSortedArrays(new int[]{1, 3}, new int[]{2}));
        System.out.println("median even       = " + findMedianSortedArrays(new int[]{1, 2}, new int[]{3, 4}));
        System.out.println("kthLargest k=2    = " + findKthLargest(new int[]{3, 2, 1, 5, 6, 4}, 2));
        System.out.println("maximumGap        = " + maximumGap(new int[]{3, 6, 9, 1}));
        System.out.println("heightChecker     = " + heightChecker(new int[]{1, 1, 4, 2, 1, 3}));
        System.out.println("specialArray      = " + specialArray(new int[]{0, 4, 3, 0, 4}));
        System.out.println("sortedAndRotated  = " + checkIfSortedAndRotated(new int[]{3, 4, 5, 1, 2}));
        System.out.println("thirdMax          = " + thirdMax(new int[]{2, 2, 3, 1}));
    }
}
