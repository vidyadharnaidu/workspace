package strings;

import java.util.stream.*;

/**
 * Longest Palindromic Substring: "babad" -> "bab", "cbbd" -> "bb"
 *
 * TRICK: EXPAND AROUND CENTER. Every palindrome has a center:
 *        one char (odd length, "aba") or between two chars (even length, "abba").
 *        Try all 2n-1 centers, keep the longest. O(n^2) time, O(1) space.
 */
public class LongestPalindrome {

    static String longestPalindrome(String s) {
        String best = "";
        for (int c = 0; c < s.length(); c++) {
            String odd = expand(s, c, c);
            String even = expand(s, c, c + 1);
            if (odd.length() > best.length()) best = odd;
            if (even.length() > best.length()) best = even;
        }
        return best;
    }

    static String expand(String s, int l, int r) {
        while (l >= 0 && r < s.length() && s.charAt(l) == s.charAt(r)) {
            l--;
            r++;
        }
        return s.substring(l + 1, r);
    }

    // Bonus: is the whole string a palindrome? (one-liners)
    static boolean isPalindrome(String s) {
        return new StringBuilder(s).reverse().toString().equals(s);
    }

    static boolean isPalindromeStream(String s) {
        return IntStream.range(0, s.length() / 2)
                .allMatch(i -> s.charAt(i) == s.charAt(s.length() - 1 - i));
    }

    // Bonus: "A man, a plan, a canal: Panama" -> true (ignore case & non-letters)
    static boolean isSentencePalindrome(String s) {
        String clean = s.toLowerCase().replaceAll("[^a-z0-9]", "");
        return isPalindrome(clean);
    }

    public static void main(String[] args) {
        System.out.println(longestPalindrome("babad"));      // bab
        System.out.println(longestPalindrome("cbbd"));       // bb
        System.out.println(longestPalindrome("forgeeksskeegfor")); // geeksskeeg
        System.out.println(isPalindrome("racecar"));         // true
        System.out.println(isPalindromeStream("hello"));     // false
        System.out.println(isSentencePalindrome("A man, a plan, a canal: Panama")); // true
    }
}
