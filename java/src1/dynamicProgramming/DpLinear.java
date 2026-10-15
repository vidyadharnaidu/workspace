import java.util.*;

/** PATTERN: dp[i] depends on dp[i-1] / dp[i-2]. Keep 2-3 rolling variables, not an array. */
public class DpLinear {

    // HOOK: two rolling variables. The ancestor of everything in this file.
    static int fib(int n) {
        int a = 0, b = 1;
        for (int i = 0; i < n; i++) { int t = a + b; a = b; b = t; }
        return a;
    }

    // HOOK: ways(n) = ways(n-1) + ways(n-2). It IS fib, shifted by one.
    static int climbStairs(int n) {
        int a = 1, b = 1;
        for (int i = 1; i < n; i++) { int t = a + b; a = b; b = t; }
        return b;
    }

    // HOOK: at each house choose rob(prev2 + x) or skip(prev1).
    static int rob(int[] a) {
        int prev2 = 0, prev1 = 0;
        for (int x : a) { int cur = Math.max(prev1, prev2 + x); prev2 = prev1; prev1 = cur; }
        return prev1;
    }

    // HOOK: circular -> the first and last are exclusive, so run rob() twice.
    static int rob2(int[] a) {
        if (a.length == 1) return a[0];
        return Math.max(rob(Arrays.copyOfRange(a, 0, a.length - 1)),
                        rob(Arrays.copyOfRange(a, 1, a.length)));
    }

    // HOOK: fib with validity gates - one digit must be 1-9, two digits must be 10-26.
    static int numDecodings(String s) {
        int prev2 = 1, prev1 = s.charAt(0) == '0' ? 0 : 1;
        for (int i = 1; i < s.length(); i++) {
            int cur = 0;
            if (s.charAt(i) != '0') cur += prev1;
            int two = Integer.parseInt(s.substring(i - 1, i + 1));
            if (two >= 10 && two <= 26) cur += prev2;
            prev2 = prev1;
            prev1 = cur;
        }
        return prev1;
    }

    // HOOK: dp[i] = sum of dp[i-j] over the runs of equal keys (7 and 9 allow 4).
    static int countTexts(String keys) {
        long MOD = 1_000_000_007L;
        int n = keys.length();
        long[] dp = new long[n + 1];
        dp[0] = 1;
        for (int i = 1; i <= n; i++) {
            char c = keys.charAt(i - 1);
            int max = (c == '7' || c == '9') ? 4 : 3;
            for (int j = 1; j <= max && i - j >= 0; j++) {
                if (keys.charAt(i - j) != c) break;               // run of equal keys ended
                dp[i] = (dp[i] + dp[i - j]) % MOD;
            }
        }
        return (int) dp[n];
    }

    public static void main(String[] args) {
        System.out.println("fib(10)           = " + fib(10));
        System.out.println("climbStairs(5)    = " + climbStairs(5));
        System.out.println("rob               = " + rob(new int[]{2, 7, 9, 3, 1}));
        System.out.println("rob2 circular     = " + rob2(new int[]{2, 3, 2}));
        System.out.println("numDecodings 226  = " + numDecodings("226"));
        System.out.println("numDecodings 06   = " + numDecodings("06"));
        System.out.println("countTexts 22233  = " + countTexts("22233"));
    }
}
