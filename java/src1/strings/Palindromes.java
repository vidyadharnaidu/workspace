import java.util.*;

/** PATTERN: expand around a center, or close in from both ends. */
public class Palindromes {

    // HOOK: strip junk, lowercase, compare with its reverse. (O(n) space, one line.)
    static boolean isPalindrome(String s) {
        String t = s.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
        return t.equals(new StringBuilder(t).reverse().toString());
    }

    // HOOK: on the first mismatch, try skipping the left char OR the right char.
    static boolean validPalindrome2(String s) {
        int l = 0, r = s.length() - 1;
        while (l < r) {
            if (s.charAt(l) != s.charAt(r))
                return isRange(s, l + 1, r) || isRange(s, l, r - 1);
            l++; r--;
        }
        return true;
    }

    // The O(1)-space palindrome check. Use this shape if asked for no extra space.
    static boolean isRange(String s, int l, int r) {
        while (l < r) if (s.charAt(l++) != s.charAt(r--)) return false;
        return true;
    }

    // HOOK: 2n-1 centers - n single chars plus n-1 gaps. Expand each.
    static String longestPalindrome(String s) {
        int bs = 0, bl = 0;
        for (int i = 0; i < s.length(); i++) {
            int len = Math.max(expand(s, i, i), expand(s, i, i + 1));
            if (len > bl) { bl = len; bs = i - (len - 1) / 2; }
        }
        return s.substring(bs, bs + bl);
    }

    static int expand(String s, int l, int r) {
        while (l >= 0 && r < s.length() && s.charAt(l) == s.charAt(r)) { l--; r++; }
        return r - l - 1;
    }

    // HOOK: same centers, but COUNT every successful expansion instead of the longest.
    static int countSubstrings(String s) {
        int res = 0;
        for (int i = 0; i < s.length(); i++) {
            res += countFrom(s, i, i);
            res += countFrom(s, i, i + 1);
        }
        return res;
    }

    static int countFrom(String s, int l, int r) {
        int c = 0;
        while (l >= 0 && r < s.length() && s.charAt(l--) == s.charAt(r++)) c++;
        return c;
    }

    // HOOK: a permutation can be a palindrome iff at most one char has odd count.
    static boolean canPermutePalindrome(String s) {
        Set<Character> odd = new HashSet<>();
        for (char c : s.toCharArray()) if (!odd.add(c)) odd.remove(c);
        return odd.size() <= 1;
    }

    // HOOK: greedily drop the first non-'a' in the first half to 'a', else last char -> 'b'.
    static String breakPalindrome(String p) {
        int n = p.length();
        if (n < 2) return "";
        char[] c = p.toCharArray();
        for (int i = 0; i < n / 2; i++)
            if (c[i] != 'a') { c[i] = 'a'; return new String(c); }
        c[n - 1] = 'b';                                           // string was all 'a'
        return new String(c);
    }

    public static void main(String[] args) {
        System.out.println("isPalindrome      = " + isPalindrome("A man, a plan, a canal: Panama"));
        System.out.println("validPalindrome2  = " + validPalindrome2("abca"));
        System.out.println("longestPalindrome = " + longestPalindrome("babad"));
        System.out.println("longest even      = " + longestPalindrome("cbbd"));
        System.out.println("countSubstrings   = " + countSubstrings("aaa"));
        System.out.println("canPermute        = " + canPermutePalindrome("carerac"));
        System.out.println("breakPalindrome   = " + breakPalindrome("abccba"));
        System.out.println("breakPalindrome a = " + breakPalindrome("aa"));
    }
}
