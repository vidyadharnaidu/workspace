package dp;

import java.util.*;
import java.util.stream.*;

/**
 * Dynamic programming problems.
 *
 * 4 STEPS FOR ANY DP:
 *   1. STATE:      what does dp[i] (or dp[i][j]) MEAN? Say it in words.
 *   2. CHOICE:     what are my options at step i? -> recurrence
 *   3. BASE CASE:  dp[0], empty string, 0 amount...
 *   4. ANSWER:     dp[n]? max of all dp[i]?
 *
 * TOP-DOWN  = recursion + memo (easy to write from brute force).
 * BOTTOM-UP = loop filling the table (no stack overflow).
 * If dp[i] only needs dp[i-1], dp[i-2] -> keep 2 variables instead of an array.
 *
 * FAMILIES:
 *   1D line        : climbing stairs, house robber, decode ways, word break, LIS
 *   Unbounded coins: coin change (loop amount OUTER for min; coins OUTER for number of ways)
 *   0/1 knapsack   : knapsack, partition subset -> iterate capacity BACKWARDS
 *   Two strings    : LCS, edit distance -> dp[i][j] over prefixes, table size (m+1) x (n+1)
 *   Grid           : unique paths, min path sum -> from top + from left
 *
 * (Kadane max subarray and stock buy/sell are also DP -> see arrays/ArrayProblems)
 */
public class DynamicProgramming {

    // 1. FIBONACCI - top-down with memo (shows recursion -> memo)
    static long fibMemo(int n, Map<Integer, Long> memo) {
        if (n < 2) return n;
        Long cached = memo.get(n);
        if (cached != null) return cached;
        long value = fibMemo(n - 1, memo) + fibMemo(n - 2, memo);
        memo.put(n, value);
        return value;
    }

    // 2. MIN COST CLIMBING STAIRS: [10,15,20] -> 15
    // dp[i] = cheapest cost to STAND on step i = min(dp[i-1] + cost[i-1], dp[i-2] + cost[i-2])
    static int minCostClimbingStairs(int[] cost) {
        int twoBack = 0, oneBack = 0;
        for (int i = 2; i <= cost.length; i++) {
            int cur = Math.min(oneBack + cost[i - 1], twoBack + cost[i - 2]);
            twoBack = oneBack;
            oneBack = cur;
        }
        return oneBack;
    }

    // 3. HOUSE ROBBER: [2,7,9,3,1] -> 12 (2 + 9 + 1)
    // CHOICE: skip this house (keep best so far) OR rob it (best two houses back + x)
    static int rob(int[] houses) {
        int prev = 0, cur = 0;
        for (int x : houses) {
            int next = Math.max(cur, prev + x);
            prev = cur;
            cur = next;
        }
        return cur;
    }

    // 4. COIN CHANGE (min coins): [1,2,5], 11 -> 3 (5+5+1); [2], 3 -> -1
    // dp[a] = min coins to make amount a = min over coins (dp[a - coin] + 1)
    static int coinChange(int[] coins, int amount) {
        int[] dp = new int[amount + 1];
        Arrays.fill(dp, amount + 1); // "infinity"
        dp[0] = 0;
        for (int a = 1; a <= amount; a++)
            for (int coin : coins)
                if (coin <= a) dp[a] = Math.min(dp[a], dp[a - coin] + 1);
        return dp[amount] > amount ? -1 : dp[amount];
    }

    // 5. COIN CHANGE II (number of ways): amount 5, [1,2,5] -> 4
    // TRICK: COINS in the OUTER loop -> counts combinations, not permutations
    static int coinWays(int[] coins, int amount) {
        int[] dp = new int[amount + 1];
        dp[0] = 1;
        for (int coin : coins)
            for (int a = coin; a <= amount; a++)
                dp[a] += dp[a - coin];
        return dp[amount];
    }

    // 6. LONGEST INCREASING SUBSEQUENCE: [10,9,2,5,3,7,101,18] -> 4 ([2,3,7,101])
    // O(n^2): dp[i] = longest LIS ending at i = 1 + max(dp[j]) for j < i with a[j] < a[i]
    static int lis(int[] a) {
        int[] dp = new int[a.length];
        Arrays.fill(dp, 1);
        for (int i = 0; i < a.length; i++)
            for (int j = 0; j < i; j++)
                if (a[j] < a[i]) dp[i] = Math.max(dp[i], dp[j] + 1);
        return Arrays.stream(dp).max().orElse(0);
    }

    // 6b. LIS O(n log n): tails[i] = smallest tail of an increasing subsequence of length i+1
    static int lisFast(int[] a) {
        int[] tails = new int[a.length];
        int size = 0;
        for (int x : a) {
            int i = Arrays.binarySearch(tails, 0, size, x);
            if (i < 0) i = -(i + 1); // insertion point
            tails[i] = x;
            if (i == size) size++;
        }
        return size;
    }

    // 7. LONGEST COMMON SUBSEQUENCE: "abcde", "ace" -> 3
    // same char -> diagonal + 1; different -> max(top, left)
    static int lcs(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 1; i <= a.length(); i++)
            for (int j = 1; j <= b.length(); j++)
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? dp[i - 1][j - 1] + 1
                        : Math.max(dp[i - 1][j], dp[i][j - 1]);
        return dp[a.length()][b.length()];
    }

    // 8. EDIT DISTANCE: "horse", "ros" -> 3
    // same char -> diagonal; different -> 1 + min(replace=diagonal, delete=top, insert=left)
    static int editDistance(String a, String b) {
        int m = a.length(), n = b.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        for (int i = 1; i <= m; i++)
            for (int j = 1; j <= n; j++)
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? dp[i - 1][j - 1]
                        : 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
        return dp[m][n];
    }

    // 9. 0/1 KNAPSACK: weights [1,3,4,5], values [1,4,5,7], capacity 7 -> 9 (items 3kg + 4kg)
    // dp[w] = best value with capacity w. Loop capacity BACKWARDS so each item is used once.
    static int knapsack(int[] weights, int[] values, int capacity) {
        int[] dp = new int[capacity + 1];
        for (int i = 0; i < weights.length; i++)
            for (int w = capacity; w >= weights[i]; w--)
                dp[w] = Math.max(dp[w], dp[w - weights[i]] + values[i]);
        return dp[capacity];
    }

    // 10. PARTITION EQUAL SUBSET SUM: [1,5,11,5] -> true ([1,5,5] and [11])
    // TRICK: it's knapsack with booleans. Can some subset reach sum/2?
    static boolean canPartition(int[] nums) {
        int sum = Arrays.stream(nums).sum();
        if (sum % 2 == 1) return false;
        int target = sum / 2;
        boolean[] dp = new boolean[target + 1];
        dp[0] = true;
        for (int x : nums)
            for (int t = target; t >= x; t--)
                dp[t] |= dp[t - x];
        return dp[target];
    }

    // 11. UNIQUE PATHS in m x n grid (only right/down): 3x7 -> 28
    // ways[c] = ways from top (old ways[c]) + ways from left (ways[c-1])
    static int uniquePaths(int m, int n) {
        int[] ways = new int[n];
        Arrays.fill(ways, 1);
        for (int r = 1; r < m; r++)
            for (int c = 1; c < n; c++)
                ways[c] += ways[c - 1];
        return ways[n - 1];
    }

    // 12. MIN PATH SUM in grid: [[1,3,1],[1,5,1],[4,2,1]] -> 7 (1-3-1-1-1)
    static int minPathSum(int[][] grid) {
        int m = grid.length, n = grid[0].length;
        int[][] dp = new int[m][n];
        for (int r = 0; r < m; r++)
            for (int c = 0; c < n; c++) {
                if (r == 0 && c == 0) dp[r][c] = grid[r][c];
                else if (r == 0) dp[r][c] = dp[r][c - 1] + grid[r][c];
                else if (c == 0) dp[r][c] = dp[r - 1][c] + grid[r][c];
                else dp[r][c] = Math.min(dp[r - 1][c], dp[r][c - 1]) + grid[r][c];
            }
        return dp[m - 1][n - 1];
    }

    // 13. WORD BREAK: "leetcode", [leet, code] -> true
    // dp[i] = s[0..i) can be split = some j < i where dp[j] && s[j..i) is a word
    static boolean wordBreak(String s, List<String> words) {
        Set<String> dict = new HashSet<>(words);
        boolean[] dp = new boolean[s.length() + 1];
        dp[0] = true;
        for (int i = 1; i <= s.length(); i++) {
            final int end = i;
            dp[i] = IntStream.range(0, i).anyMatch(j -> dp[j] && dict.contains(s.substring(j, end)));
        }
        return dp[s.length()];
    }

    // 14. DECODE WAYS: "226" -> 3 ("BZ", "VF", "BBF")  (A=1 ... Z=26)
    // ways(i) = ways(i-1) if last digit is 1-9  +  ways(i-2) if last two digits are 10-26
    static int numDecodings(String s) {
        if (s.isEmpty() || s.charAt(0) == '0') return 0;
        int twoBack = 1, oneBack = 1;
        for (int i = 2; i <= s.length(); i++) {
            int cur = 0;
            if (s.charAt(i - 1) != '0') cur += oneBack;
            int two = Integer.parseInt(s.substring(i - 2, i));
            if (two >= 10 && two <= 26) cur += twoBack;
            twoBack = oneBack;
            oneBack = cur;
        }
        return oneBack;
    }

    // 15. MAX PRODUCT SUBARRAY: [2,3,-2,4] -> 6
    // TRICK: track BOTH max and min (a negative flips min into max)
    static int maxProduct(int[] a) {
        int max = a[0], min = a[0], best = a[0];
        for (int i = 1; i < a.length; i++) {
            int x = a[i];
            if (x < 0) { int t = max; max = min; min = t; }
            max = Math.max(x, max * x);
            min = Math.min(x, min * x);
            best = Math.max(best, max);
        }
        return best;
    }

    // 16. LONGEST PALINDROMIC SUBSEQUENCE: "bbbab" -> 4 ("bbbb")
    // TRICK: it's LCS of the string and its reverse
    static int longestPalindromicSubsequence(String s) {
        return lcs(s, new StringBuilder(s).reverse().toString());
    }

    public static void main(String[] args) {
        System.out.println("fib(50)            " + fibMemo(50, new HashMap<>()));                         // 12586269025
        System.out.println("minCostStairs      " + minCostClimbingStairs(new int[]{10, 15, 20}));         // 15
        System.out.println("houseRobber        " + rob(new int[]{2, 7, 9, 3, 1}));                        // 12
        System.out.println("coinChange         " + coinChange(new int[]{1, 2, 5}, 11));                   // 3
        System.out.println("coinChange none    " + coinChange(new int[]{2}, 3));                          // -1
        System.out.println("coinWays           " + coinWays(new int[]{1, 2, 5}, 5));                      // 4
        System.out.println("LIS                " + lis(new int[]{10, 9, 2, 5, 3, 7, 101, 18}));           // 4
        System.out.println("LIS fast           " + lisFast(new int[]{10, 9, 2, 5, 3, 7, 101, 18}));       // 4
        System.out.println("LCS                " + lcs("abcde", "ace"));                                  // 3
        System.out.println("editDistance       " + editDistance("horse", "ros"));                         // 3
        System.out.println("knapsack           " + knapsack(new int[]{1, 3, 4, 5}, new int[]{1, 4, 5, 7}, 7)); // 9
        System.out.println("canPartition       " + canPartition(new int[]{1, 5, 11, 5}) + " " + canPartition(new int[]{1, 2, 3, 5})); // true false
        System.out.println("uniquePaths(3,7)   " + uniquePaths(3, 7));                                    // 28
        System.out.println("minPathSum         " + minPathSum(new int[][]{{1, 3, 1}, {1, 5, 1}, {4, 2, 1}})); // 7
        System.out.println("wordBreak          " + wordBreak("leetcode", List.of("leet", "code"))
                + " " + wordBreak("catsandog", List.of("cats", "dog", "sand", "and", "cat")));      // true false
        System.out.println("decodeWays         " + numDecodings("226") + " " + numDecodings("06"));      // 3 0
        System.out.println("maxProduct         " + maxProduct(new int[]{2, 3, -2, 4}));                   // 6
        System.out.println("longestPalSubseq   " + longestPalindromicSubsequence("bbbab"));               // 4
    }
}
