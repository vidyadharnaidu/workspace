import java.util.*;

/** PATTERN: longest increasing subsequence - O(n^2) dp, or O(n log n) patience piles. */
public class DpSubsequence {

    // HOOK: dp[i] = LIS ending at i; look back at every smaller earlier value.
    static int lengthOfLIS(int[] a) {
        int[] dp = new int[a.length];
        Arrays.fill(dp, 1);
        int best = 1;
        for (int i = 1; i < a.length; i++) {
            for (int j = 0; j < i; j++)
                if (a[j] < a[i]) dp[i] = Math.max(dp[i], dp[j] + 1);
            best = Math.max(best, dp[i]);
        }
        return best;
    }

    // HOOK: tails[k] = smallest tail of an LIS of length k+1. Binary search, overwrite.
    static int lengthOfLISFast(int[] a) {
        int[] tails = new int[a.length];
        int len = 0;
        for (int x : a) {
            int i = Arrays.binarySearch(tails, 0, len, x);
            if (i < 0) i = -(i + 1);                              // insertion point
            tails[i] = x;
            if (i == len) len++;
        }
        return len;
    }

    // HOOK: sort width ASC, height DESC (so equal widths cannot nest), then LIS on height.
    static int maxEnvelopes(int[][] env) {
        Arrays.sort(env, (x, y) -> x[0] != y[0] ? x[0] - y[0] : y[1] - x[1]);
        int[] heights = Arrays.stream(env).mapToInt(e -> e[1]).toArray();
        return lengthOfLISFast(heights);
    }

    public static void main(String[] args) {
        int[] a = {10, 9, 2, 5, 3, 7, 101, 18};
        System.out.println("LIS n^2           = " + lengthOfLIS(a));
        System.out.println("LIS n log n       = " + lengthOfLISFast(a));
        System.out.println("russianDoll       = " + maxEnvelopes(new int[][]{{5, 4}, {6, 4}, {6, 7}, {2, 3}}));
    }
}
