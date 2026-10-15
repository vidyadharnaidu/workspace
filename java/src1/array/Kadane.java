import java.util.*;

/** PATTERN: at each index ask "extend the running best, or restart here?". */
public class Kadane {

    // HOOK: cur = max(a[i], cur + a[i]); best = max(best, cur).
    static int maxSubArray(int[] a) {
        int cur = a[0], best = a[0];
        for (int i = 1; i < a.length; i++) {
            cur = Math.max(a[i], cur + a[i]);
            best = Math.max(best, cur);
        }
        return best;
    }

    // HOOK: negatives flip sign, so carry BOTH a running max and a running min.
    static int maxProduct(int[] a) {
        int max = a[0], min = a[0], best = a[0];
        for (int i = 1; i < a.length; i++) {
            int x = a[i], pm = max;
            max = Math.max(x, Math.max(max * x, min * x));
            min = Math.min(x, Math.min(pm * x, min * x));
            best = Math.max(best, max);
        }
        return best;
    }

    // HOOK: circular best = total - minSubarray; all-negative case falls back to Kadane.
    static int maxSubarraySumCircular(int[] a) {
        int total = 0, curMax = 0, bestMax = a[0], curMin = 0, bestMin = a[0];
        for (int x : a) {
            total += x;
            curMax = Math.max(x, curMax + x);
            bestMax = Math.max(bestMax, curMax);
            curMin = Math.min(x, curMin + x);
            bestMin = Math.min(bestMin, curMin);
        }
        return bestMax < 0 ? bestMax : Math.max(bestMax, total - bestMin);
    }

    // HOOK: Kadane while also remembering where the current run started.
    static int[] maxSubArrayBounds(int[] a) {
        int cur = a[0], best = a[0], s = 0, bs = 0, be = 0;
        for (int i = 1; i < a.length; i++) {
            if (a[i] > cur + a[i]) { cur = a[i]; s = i; } else cur += a[i];
            if (cur > best) { best = cur; bs = s; be = i; }
        }
        return new int[]{best, bs, be};
    }

    public static void main(String[] args) {
        System.out.println("maxSubArray       = " + maxSubArray(new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4}));
        System.out.println("maxProduct        = " + maxProduct(new int[]{2, 3, -2, 4}));
        System.out.println("maxProduct neg    = " + maxProduct(new int[]{-2, 3, -4}));
        System.out.println("circular          = " + maxSubarraySumCircular(new int[]{5, -3, 5}));
        System.out.println("circular allNeg   = " + maxSubarraySumCircular(new int[]{-3, -2, -3}));
        System.out.println("bounds [sum,s,e]  = " + Arrays.toString(maxSubArrayBounds(new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4})));
    }
}
