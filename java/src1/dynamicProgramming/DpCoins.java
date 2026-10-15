import java.util.*;

/**
 * PATTERN: unbounded knapsack over a 1-D dp indexed by amount.
 * LOOP ORDER IS THE WHOLE TRICK:
 *   coins outside, amount inside -> COMBINATIONS (order does not matter)
 *   amount outside, coins inside -> PERMUTATIONS (order matters)
 */
public class DpCoins {

    // HOOK: dp[x] = fewest coins making x. Seed with "impossible" = amount+1.
    static int coinChange(int[] coins, int amount) {
        int[] dp = new int[amount + 1];
        Arrays.fill(dp, amount + 1);
        dp[0] = 0;
        for (int c : coins)
            for (int x = c; x <= amount; x++)
                dp[x] = Math.min(dp[x], dp[x - c] + 1);
        return dp[amount] > amount ? -1 : dp[amount];
    }

    // HOOK: COMBINATIONS - coins outer loop, so {1,2} and {2,1} count once.
    static int change(int amount, int[] coins) {
        int[] dp = new int[amount + 1];
        dp[0] = 1;
        for (int c : coins)
            for (int x = c; x <= amount; x++)
                dp[x] += dp[x - c];
        return dp[amount];
    }

    // HOOK: PERMUTATIONS - amount outer loop, so {1,2} and {2,1} count twice.
    static int combinationSum4(int[] nums, int target) {
        int[] dp = new int[target + 1];
        dp[0] = 1;
        for (int x = 1; x <= target; x++)
            for (int c : nums)
                if (c <= x) dp[x] += dp[x - c];
        return dp[target];
    }

    // HOOK: coinChange where the "coins" are the perfect squares 1,4,9,...
    static int numSquares(int n) {
        int[] dp = new int[n + 1];
        Arrays.fill(dp, Integer.MAX_VALUE);
        dp[0] = 0;
        for (int c = 1; c * c <= n; c++)
            for (int x = c * c; x <= n; x++)
                dp[x] = Math.min(dp[x], dp[x - c * c] + 1);
        return dp[n];
    }

    // HOOK: dp[i] = "prefix of length i is breakable"; look back for a matching word.
    static boolean wordBreak(String s, List<String> dict) {
        Set<String> words = new HashSet<>(dict);
        boolean[] dp = new boolean[s.length() + 1];
        dp[0] = true;
        for (int i = 1; i <= s.length(); i++)
            for (int j = 0; j < i; j++)
                if (dp[j] && words.contains(s.substring(j, i))) { dp[i] = true; break; }
        return dp[s.length()];
    }

    public static void main(String[] args) {
        System.out.println("coinChange 11     = " + coinChange(new int[]{1, 2, 5}, 11));
        System.out.println("coinChange imposs = " + coinChange(new int[]{2}, 3));
        System.out.println("change combos     = " + change(5, new int[]{1, 2, 5}));
        System.out.println("combinationSum4   = " + combinationSum4(new int[]{1, 2, 3}, 4));
        System.out.println("numSquares 12     = " + numSquares(12));
        System.out.println("wordBreak         = " + wordBreak("leetcode", List.of("leet", "code")));
        System.out.println("wordBreak false   = " + wordBreak("catsandog", List.of("cats", "dog", "sand", "and", "cat")));
    }
}
