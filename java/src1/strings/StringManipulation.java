import java.util.*;
import java.util.stream.*;

/** PATTERN: split/join, index cursors, and building with StringBuilder. */
public class StringManipulation {

    // HOOK: split on whitespace runs, reverse the list view, join with one space.
    static String reverseWords(String s) {
        String[] w = s.trim().split("\\s+");
        Collections.reverse(Arrays.asList(w));
        return String.join(" ", w);
    }

    // HOOK: trim, then everything after the last space.
    static int lengthOfLastWord(String s) {
        String t = s.trim();
        return t.length() - t.lastIndexOf(' ') - 1;
    }

    // HOOK: shrink the candidate prefix until every word starts with it.
    static String longestCommonPrefix(String[] words) {
        String p = words[0];
        for (String w : words)
            while (!w.startsWith(p)) p = p.substring(0, p.length() - 1);
        return p;
    }

    // HOOK: one StringBuilder per row; bounce the row index at the top and bottom.
    static String zigzagConvert(String s, int numRows) {
        if (numRows == 1) return s;
        var rows = IntStream.range(0, numRows).mapToObj(i -> new StringBuilder()).toList();
        int row = 0, step = 1;
        for (char c : s.toCharArray()) {
            rows.get(row).append(c);
            if (row == 0) step = 1;
            else if (row == numRows - 1) step = -1;
            row += step;
        }
        return rows.stream().map(StringBuilder::toString).collect(Collectors.joining());
    }

    // HOOK: walk the string once, emitting a space each time the next index matches.
    static String addSpaces(String s, int[] spaces) {
        StringBuilder sb = new StringBuilder();
        int k = 0;
        for (int i = 0; i < s.length(); i++) {
            if (k < spaces.length && spaces[k] == i) { sb.append(' '); k++; }
            sb.append(s.charAt(i));
        }
        return sb.toString();
    }

    // HOOK: vowel -> word+"ma", else move first letter to the back +"ma"; add i+1 a's.
    static String toGoatLatin(String sentence) {
        String[] words = sentence.split(" ");
        return IntStream.range(0, words.length)
                .mapToObj(i -> {
                    String w = words[i];
                    String base = "aeiouAEIOU".indexOf(w.charAt(0)) >= 0 ? w : w.substring(1) + w.charAt(0);
                    return base + "ma" + "a".repeat(i + 1);
                })
                .collect(Collectors.joining(" "));
    }

    // HOOK: collapse all shifts into one net left-rotation, then rotate once.
    static String stringShift(String s, int[][] shift) {
        int n = s.length(), net = 0;
        for (int[] sh : shift) net += (sh[0] == 0 ? sh[1] : -sh[1]);
        net = ((net % n) + n) % n;
        return s.substring(net) + s.substring(0, net);
    }

    // HOOK: two pointers; swap only when BOTH sides are letters.
    static String reverseOnlyLetters(String s) {
        char[] c = s.toCharArray();
        int l = 0, r = c.length - 1;
        while (l < r) {
            if (!Character.isLetter(c[l])) l++;
            else if (!Character.isLetter(c[r])) r--;
            else { char t = c[l]; c[l++] = c[r]; c[r--] = t; }
        }
        return new String(c);
    }

    // HOOK: a common divisor exists iff s+t equals t+s; its length is gcd of lengths.
    static String gcdOfStrings(String s, String t) {
        if (!(s + t).equals(t + s)) return "";
        return s.substring(0, gcd(s.length(), t.length()));
    }

    static int gcd(int a, int b) { return b == 0 ? a : gcd(b, a % b); }

    // HOOK: type each string out with a StringBuilder stack, then compare.
    static boolean backspaceCompare(String s, String t) {
        return typed(s).equals(typed(t));
    }

    static String typed(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c != '#') sb.append(c);
            else if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    // HOOK: longest word that is a SUBSEQUENCE of s; tie broken lexicographically.
    static String findLongestWord(String s, List<String> dict) {
        return dict.stream()
                .filter(w -> isSubsequence(w, s))
                .max(Comparator.comparingInt(String::length).thenComparing(Comparator.reverseOrder()))
                .orElse("");
    }

    static boolean isSubsequence(String w, String s) {
        int i = 0;
        for (int j = 0; j < s.length() && i < w.length(); j++)
            if (w.charAt(i) == s.charAt(j)) i++;
        return i == w.length();
    }

    // HOOK: the answer always starts at the largest char; race two candidates forward.
    static String lastSubstring(String s) {
        int i = 0, j = 1, k = 0, n = s.length();
        while (j + k < n) {
            if (s.charAt(i + k) == s.charAt(j + k)) { k++; continue; }
            if (s.charAt(i + k) < s.charAt(j + k)) { i = Math.max(i + k + 1, j); j = i + 1; }
            else j = j + k + 1;
            k = 0;
        }
        return s.substring(i);
    }

    public static void main(String[] args) {
        System.out.println("reverseWords      = '" + reverseWords("  the sky   is blue  ") + "'");
        System.out.println("lengthOfLastWord  = " + lengthOfLastWord("Hello World  "));
        System.out.println("longestCommonPref = " + longestCommonPrefix(new String[]{"flower", "flow", "flight"}));
        System.out.println("zigzag rows=3     = " + zigzagConvert("PAYPALISHIRING", 3));
        System.out.println("addSpaces         = " + addSpaces("LeetcodeHelpsMeLearn", new int[]{8, 13, 15}));
        System.out.println("goatLatin         = " + toGoatLatin("I speak Goat Latin"));
        System.out.println("stringShift       = " + stringShift("abcdefg", new int[][]{{1, 1}, {1, 1}, {0, 2}, {1, 3}}));
        System.out.println("reverseOnlyLetter = " + reverseOnlyLetters("a-bC-dEf-ghIj"));
        System.out.println("gcdOfStrings      = " + gcdOfStrings("ABCABC", "ABC"));
        System.out.println("backspaceCompare  = " + backspaceCompare("ab#c", "ad#c"));
        System.out.println("findLongestWord   = " + findLongestWord("abpcplea", List.of("ale", "apple", "monkey", "plea")));
        System.out.println("lastSubstring     = " + lastSubstring("leetcode"));
    }
}
