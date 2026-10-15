import java.util.*;

/**
 * PATTERN: 0/1 knapsack - each item used AT MOST once.
 * The 1-D form walks capacity BACKWARDS, which is the only difference
 * from the unbounded version in DpCoins (that one walks forwards).
 */
public class DpKnapsack01 {

    // HOOK: classic - max value within capacity. Backwards inner loop = use once.
    static int knapsack(int[] weight, int[] value, int capacity) {
        int[] dp = new int[capacity + 1];
        for (int i = 0; i < weight.length; i++)
            for (int c = capacity; c >= weight[i]; c--)
                dp[c] = Math.max(dp[c], dp[c - weight[i]] + value[i]);
        return dp[capacity];
    }

    // HOOK: reachable subset sums as a boolean dp; target is total/2.
    static boolean canPartition(int[] a) {
        int total = Arrays.stream(a).sum();
        if (total % 2 != 0) return false;
        int half = total / 2;
        boolean[] dp = new boolean[half + 1];
        dp[0] = true;
        for (int x : a)
            for (int s = half; s >= x; s--) dp[s] |= dp[s - x];
        return dp[half];
    }

    // HOOK: split stones into two piles; answer = total - 2 * (best pile <= total/2).
    static int lastStoneWeightII(int[] stones) {
        int total = Arrays.stream(stones).sum(), half = total / 2;
        boolean[] dp = new boolean[half + 1];
        dp[0] = true;
        for (int x : stones)
            for (int s = half; s >= x; s--) dp[s] |= dp[s - x];
        for (int s = half; s >= 0; s--) if (dp[s]) return total - 2 * s;
        return total;
    }

    // HOOK: dp indexed by REMAINDER mod 3, not by capacity. Same one-item-at-a-time shape.
    static int maxSumDivThree(int[] a) {
        int[] dp = {0, Integer.MIN_VALUE, Integer.MIN_VALUE};     // best sum with remainder r
        for (int x : a) {
            int[] next = dp.clone();
            for (int r = 0; r < 3; r++) {
                if (dp[r] == Integer.MIN_VALUE) continue;
                int s = dp[r] + x;
                next[s % 3] = Math.max(next[s % 3], s);
            }
            dp = next;
        }
        return dp[0];
    }

    public static void main(String[] args) {
        System.out.println("knapsack cap=5    = " + knapsack(new int[]{1, 3, 4}, new int[]{15, 20, 30}, 5));
        System.out.println("canPartition      = " + canPartition(new int[]{1, 5, 11, 5}));
        System.out.println("canPartition no   = " + canPartition(new int[]{1, 2, 3, 5}));
        System.out.println("lastStoneWeightII = " + lastStoneWeightII(new int[]{2, 7, 4, 1, 8, 1}));
        System.out.println("maxSumDivThree    = " + maxSumDivThree(new int[]{3, 6, 5, 1, 8}));
    }
}
