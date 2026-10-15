import java.util.*;

/**
 * PATTERN: dp[i][j] compares prefixes s[0..i) and t[0..j).
 * Chars match -> take the diagonal. Mismatch -> best of the neighbours.
 */
public class DpTwoStrings {

    // HOOK: match -> 1 + diagonal; else max(up, left).
    static int longestCommonSubsequence(String s, String t) {
        int m = s.length(), n = t.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++)
            for (int j = 1; j <= n; j++)
                dp[i][j] = s.charAt(i - 1) == t.charAt(j - 1)
                        ? dp[i - 1][j - 1] + 1
                        : Math.max(dp[i - 1][j], dp[i][j - 1]);
        return dp[m][n];
    }

    // HOOK: match -> diagonal free; else 1 + min(replace, delete, insert).
    static int minDistance(String s, String t) {
        int m = s.length(), n = t.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;                // delete everything
        for (int j = 0; j <= n; j++) dp[0][j] = j;                // insert everything
        for (int i = 1; i <= m; i++)
            for (int j = 1; j <= n; j++)
                dp[i][j] = s.charAt(i - 1) == t.charAt(j - 1)
                        ? dp[i - 1][j - 1]
                        : 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
        return dp[m][n];
    }

    // HOOK: count how many ways t appears in s. Match -> diagonal + up; else up.
    static int numDistinct(String s, String t) {
        int m = s.length(), n = t.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = 1;                // empty t matches once
        for (int i = 1; i <= m; i++)
            for (int j = 1; j <= n; j++)
                dp[i][j] = dp[i - 1][j] + (s.charAt(i - 1) == t.charAt(j - 1) ? dp[i - 1][j - 1] : 0);
        return dp[m][n];
    }

    // HOOK: dp[i][j] = "s3's first i+j chars are an interleave"; take from s1 OR s2.
    static boolean isInterleave(String s1, String s2, String s3) {
        int m = s1.length(), n = s2.length();
        if (m + n != s3.length()) return false;
        boolean[][] dp = new boolean[m + 1][n + 1];
        dp[0][0] = true;
        for (int i = 0; i <= m; i++)
            for (int j = 0; j <= n; j++) {
                if (i > 0 && dp[i - 1][j] && s1.charAt(i - 1) == s3.charAt(i + j - 1)) dp[i][j] = true;
                if (j > 0 && dp[i][j - 1] && s2.charAt(j - 1) == s3.charAt(i + j - 1)) dp[i][j] = true;
            }
        return dp[m][n];
    }

    // HOOK: '*' means ZERO of the previous char (skip 2) OR one more (if it matches).
    static boolean isMatch(String s, String p) {
        int m = s.length(), n = p.length();
        boolean[][] dp = new boolean[m + 1][n + 1];
        dp[0][0] = true;
        for (int j = 2; j <= n; j++)                              // patterns like a*b*c*
            if (p.charAt(j - 1) == '*') dp[0][j] = dp[0][j - 2];
        for (int i = 1; i <= m; i++)
            for (int j = 1; j <= n; j++) {
                char pc = p.charAt(j - 1);
                if (pc == '*') {
                    dp[i][j] = dp[i][j - 2]                        // zero occurrences
                            || (matches(s.charAt(i - 1), p.charAt(j - 2)) && dp[i - 1][j]);
                } else {
                    dp[i][j] = matches(s.charAt(i - 1), pc) && dp[i - 1][j - 1];
                }
            }
        return dp[m][n];
    }

    static boolean matches(char sc, char pc) { return pc == '.' || pc == sc; }

    public static void main(String[] args) {
        System.out.println("LCS               = " + longestCommonSubsequence("abcde", "ace"));
        System.out.println("editDistance      = " + minDistance("horse", "ros"));
        System.out.println("numDistinct       = " + numDistinct("rabbbit", "rabbit"));
        System.out.println("isInterleave      = " + isInterleave("aabcc", "dbbca", "aadbbcbcac"));
        System.out.println("isInterleave bad  = " + isInterleave("aabcc", "dbbca", "aadbbbaccc"));
        System.out.println("regex a* on aa    = " + isMatch("aa", "a*"));
        System.out.println("regex .* on ab    = " + isMatch("ab", ".*"));
        System.out.println("regex c*a*b mismatch = " + isMatch("mississippi", "mis*is*p*."));
    }
}
