import java.util.*;

/** PATTERN: sum[i..j] = pre[j+1] - pre[i]. Range updates use a diff array. */
public class PrefixSum {

    // HOOK: pre[i+1] = pre[i] + a[i]. Everything below is a variation of this.
    static int[] prefix(int[] a) {
        int[] pre = new int[a.length + 1];
        for (int i = 0; i < a.length; i++) pre[i + 1] = pre[i] + a[i];
        return pre;
    }

    // HOOK: left == total - left - a[i].
    static int pivotIndex(int[] a) {
        int total = 0, left = 0;
        for (int x : a) total += x;
        for (int i = 0; i < a.length; i++) {
            if (left == total - left - a[i]) return i;
            left += a[i];
        }
        return -1;
    }

    // HOOK: count how many earlier prefixes equal (running - k). Seed map with 0->1.
    static int subarraySum(int[] a, int k) {
        Map<Integer, Integer> seen = new HashMap<>();
        seen.put(0, 1);
        int running = 0, res = 0;
        for (int x : a) {
            running += x;
            res += seen.getOrDefault(running - k, 0);
            seen.merge(running, 1, Integer::sum);
        }
        return res;
    }

    // HOOK: longest one -> store the FIRST index of each prefix sum.
    static int maxSubArrayLen(int[] a, int k) {
        Map<Integer, Integer> first = new HashMap<>();
        first.put(0, -1);
        int running = 0, best = 0;
        for (int i = 0; i < a.length; i++) {
            running += a[i];
            if (first.containsKey(running - k)) best = Math.max(best, i - first.get(running - k));
            first.putIfAbsent(running, i);
        }
        return best;
    }

    // HOOK: prefix product on the left, suffix product on the right. No division.
    static int[] productExceptSelf(int[] a) {
        int n = a.length;
        int[] res = new int[n];
        res[0] = 1;
        for (int i = 1; i < n; i++) res[i] = res[i - 1] * a[i - 1];
        int right = 1;
        for (int i = n - 1; i >= 0; i--) {
            res[i] *= right;
            right *= a[i];
        }
        return res;
    }

    // HOOK: DIFF ARRAY - add at l, subtract at r+1, then prefix-sum it back.
    static int[] corpFlightBookings(int[][] bookings, int n) {
        int[] diff = new int[n + 1];
        for (int[] b : bookings) {
            diff[b[0] - 1] += b[2];
            diff[b[1]] -= b[2];
        }
        for (int i = 1; i < n; i++) diff[i] += diff[i - 1];
        return Arrays.copyOf(diff, n);
    }

    // HOOK: same diff array; every element must be covered at least nums[i] times.
    static boolean isZeroArray(int[] a, int[][] queries) {
        int n = a.length;
        int[] diff = new int[n + 1];
        for (int[] q : queries) {
            diff[q[0]]++;
            diff[q[1] + 1]--;
        }
        int cover = 0;
        for (int i = 0; i < n; i++) {
            cover += diff[i];
            if (cover < a[i]) return false;
        }
        return true;
    }

    // HOOK: valid split at i when leftSum >= total - leftSum.
    static int waysToSplitArray(int[] a) {
        long total = 0;
        for (int x : a) total += x;
        long left = 0;
        int res = 0;
        for (int i = 0; i < a.length - 1; i++) {
            left += a[i];
            if (left >= total - left) res++;
        }
        return res;
    }

    // HOOK: immutable range-sum queries in O(1) after O(n) setup.
    static class NumArray {
        private final int[] pre;

        NumArray(int[] a) { pre = prefix(a); }

        int sumRange(int i, int j) { return pre[j + 1] - pre[i]; }
    }

    public static void main(String[] args) {
        System.out.println("prefix            = " + Arrays.toString(prefix(new int[]{1, 2, 3, 4})));
        System.out.println("pivotIndex        = " + pivotIndex(new int[]{1, 7, 3, 6, 5, 6}));
        System.out.println("subarraySum k=2   = " + subarraySum(new int[]{1, 1, 1}, 2));
        System.out.println("maxSubArrayLen    = " + maxSubArrayLen(new int[]{1, -1, 5, -2, 3}, 3));
        System.out.println("productExceptSelf = " + Arrays.toString(productExceptSelf(new int[]{1, 2, 3, 4})));
        System.out.println("flightBookings    = " + Arrays.toString(
                corpFlightBookings(new int[][]{{1, 2, 10}, {2, 3, 20}, {2, 5, 25}}, 5)));
        System.out.println("isZeroArray       = " + isZeroArray(new int[]{1, 0, 1}, new int[][]{{0, 2}}));
        System.out.println("waysToSplitArray  = " + waysToSplitArray(new int[]{10, 4, -8, 7}));

        NumArray na = new NumArray(new int[]{-2, 0, 3, -5, 2, -1});
        System.out.println("sumRange(0,2)     = " + na.sumRange(0, 2));
        System.out.println("sumRange(2,5)     = " + na.sumRange(2, 5));
    }
}
