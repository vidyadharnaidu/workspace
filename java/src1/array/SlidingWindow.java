import java.util.*;

/** PATTERN: expand right always, shrink left while the window is invalid. */
public class SlidingWindow {

    // HOOK: fixed window - add right, drop left, no inner loop.
    static int maxSumFixedK(int[] a, int k) {
        int sum = 0;
        for (int i = 0; i < k; i++) sum += a[i];
        int best = sum;
        for (int i = k; i < a.length; i++) {
            sum += a[i] - a[i - k];
            best = Math.max(best, sum);
        }
        return best;
    }

    // HOOK: variable window, shrink WHILE valid to find the shortest one.
    static int minSubArrayLen(int target, int[] a) {
        int l = 0, sum = 0, best = Integer.MAX_VALUE;
        for (int r = 0; r < a.length; r++) {
            sum += a[r];
            while (sum >= target) {
                best = Math.min(best, r - l + 1);
                sum -= a[l++];
            }
        }
        return best == Integer.MAX_VALUE ? 0 : best;
    }

    // HOOK: fixed window max sum / k.
    static double findMaxAverage(int[] a, int k) {
        return maxSumFixedK(a, k) / (double) k;
    }

    // HOOK: longest window holding at most k zeros (each zero = one flip).
    static int longestOnes(int[] a, int k) {
        int l = 0, zeros = 0, best = 0;
        for (int r = 0; r < a.length; r++) {
            if (a[r] == 0) zeros++;
            while (zeros > k) if (a[l++] == 0) zeros--;
            best = Math.max(best, r - l + 1);
        }
        return best;
    }

    // HOOK: monotonic decreasing deque of INDICES; front is always the max.
    static int[] maxSlidingWindow(int[] a, int k) {
        Deque<Integer> dq = new ArrayDeque<>();
        int[] res = new int[a.length - k + 1];
        for (int i = 0; i < a.length; i++) {
            while (!dq.isEmpty() && dq.peekFirst() <= i - k) dq.pollFirst();
            while (!dq.isEmpty() && a[dq.peekLast()] <= a[i]) dq.pollLast();
            dq.offerLast(i);
            if (i >= k - 1) res[i - k + 1] = a[dq.peekFirst()];
        }
        return res;
    }

    // HOOK: exactly K = atMost(K) - atMost(K-1). The universal trick.
    static int subarraysWithKDistinct(int[] a, int k) {
        return atMostDistinct(a, k) - atMostDistinct(a, k - 1);
    }

    static int atMostDistinct(int[] a, int k) {
        Map<Integer, Integer> cnt = new HashMap<>();
        int l = 0, res = 0;
        for (int r = 0; r < a.length; r++) {
            cnt.merge(a[r], 1, Integer::sum);
            while (cnt.size() > k)
                if (cnt.merge(a[l++], -1, Integer::sum) == 0) cnt.remove(a[l - 1]);
            res += r - l + 1;       // every window ending at r is valid
        }
        return res;
    }

    // HOOK: avg >= t  <=>  sum >= k*t. Avoid division entirely.
    static int numOfSubarrays(int[] a, int k, int threshold) {
        int need = k * threshold, sum = 0, res = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i];
            if (i >= k) sum -= a[i - k];
            if (i >= k - 1 && sum >= need) res++;
        }
        return res;
    }

    // HOOK: binary array, sum == goal = atMostSum(goal) - atMostSum(goal-1).
    static int numSubarraysWithSum(int[] a, int goal) {
        return atMostSum(a, goal) - atMostSum(a, goal - 1);
    }

    static int atMostSum(int[] a, int goal) {
        if (goal < 0) return 0;
        int l = 0, sum = 0, res = 0;
        for (int r = 0; r < a.length; r++) {
            sum += a[r];
            while (sum > goal) sum -= a[l++];
            res += r - l + 1;
        }
        return res;
    }

    // HOOK: sort, then window where cost to raise all to a[r] fits in k.
    static int maxFrequency(int[] a, int k) {
        Arrays.sort(a);
        long sum = 0;
        int l = 0, best = 1;
        for (int r = 0; r < a.length; r++) {
            sum += a[r];
            while ((long) a[r] * (r - l + 1) - sum > k) sum -= a[l++];
            best = Math.max(best, r - l + 1);
        }
        return best;
    }

    public static void main(String[] args) {
        System.out.println("maxSumFixedK      = " + maxSumFixedK(new int[]{2, 1, 5, 1, 3, 2}, 3));
        System.out.println("minSubArrayLen    = " + minSubArrayLen(7, new int[]{2, 3, 1, 2, 4, 3}));
        System.out.println("findMaxAverage    = " + findMaxAverage(new int[]{1, 12, -5, -6, 50, 3}, 4));
        System.out.println("longestOnes k=2   = " + longestOnes(new int[]{1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0}, 2));
        System.out.println("maxSlidingWindow  = " + Arrays.toString(maxSlidingWindow(new int[]{1, 3, -1, -3, 5, 3, 6, 7}, 3)));
        System.out.println("kDistinct k=2     = " + subarraysWithKDistinct(new int[]{1, 2, 1, 2, 3}, 2));
        System.out.println("numOfSubarrays    = " + numOfSubarrays(new int[]{2, 2, 2, 2, 5, 5, 5, 8}, 3, 4));
        System.out.println("subarraysSum=2    = " + numSubarraysWithSum(new int[]{1, 0, 1, 0, 1}, 2));
        System.out.println("maxFrequency k=5  = " + maxFrequency(new int[]{1, 2, 4}, 5));
    }
}
