import java.util.*;

/** PATTERN: walk consecutive equal chars as one "run", or use a stack for adjacency. */
public class StringRuns {

    // HOOK: running counter - same as previous? cur+1, else reset to 1.
    static int maxPower(String s) {
        int best = 1, cur = 1;
        for (int i = 1; i < s.length(); i++) {
            cur = s.charAt(i) == s.charAt(i - 1) ? cur + 1 : 1;
            best = Math.max(best, cur);
        }
        return best;
    }

    // HOOK: same running counter - each position adds `cur` new homogenous substrings.
    static int countHomogenous(String s) {
        long MOD = 1_000_000_007L, res = 0, cur = 0;
        for (int i = 0; i < s.length(); i++) {
            cur = (i > 0 && s.charAt(i) == s.charAt(i - 1)) ? cur + 1 : 1;
            res = (res + cur) % MOD;
        }
        return (int) res;
    }

    // HOOK: adjacent runs contribute min(prevRun, curRun) valid substrings.
    static int countBinarySubstrings(String s) {
        int prev = 0, cur = 1, res = 0;
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) == s.charAt(i - 1)) cur++;
            else { prev = cur; cur = 1; }
            if (prev >= cur) res++;
        }
        return res;
    }

    // HOOK: in-place write pointer; append the count only when the run is > 1.
    static int compress(char[] c) {
        int write = 0, i = 0;
        while (i < c.length) {
            char ch = c[i];
            int j = i;
            while (j < c.length && c[j] == ch) j++;
            c[write++] = ch;
            if (j - i > 1) for (char d : String.valueOf(j - i).toCharArray()) c[write++] = d;
            i = j;
        }
        return write;
    }

    // HOOK: each term describes the previous term's runs as "count digit".
    static String countAndSay(int n) {
        String s = "1";
        for (int t = 1; t < n; t++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); ) {
                int j = i;
                while (j < s.length() && s.charAt(j) == s.charAt(i)) j++;
                sb.append(j - i).append(s.charAt(i));
                i = j;
            }
            s = sb.toString();
        }
        return s;
    }

    // HOOK: StringBuilder as a stack - pop when the top equals the incoming char.
    static String removeAdjacentDuplicates(String s) {
        StringBuilder st = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (st.length() > 0 && st.charAt(st.length() - 1) == c) st.deleteCharAt(st.length() - 1);
            else st.append(c);
        }
        return st.toString();
    }

    // HOOK: same stack, but pop k-1 chars when a run of k forms.
    static String removeDuplicatesK(String s, int k) {
        Deque<int[]> st = new ArrayDeque<>();                     // {charCode, runLength}
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (!st.isEmpty() && st.peek()[0] == c) {
                if (++st.peek()[1] == k) st.pop();
            } else st.push(new int[]{c, 1});
        }
        for (int[] e : st) sb.append(String.valueOf((char) e[0]).repeat(e[1]));
        return sb.reverse().toString();
    }

    // HOOK: keep deleting the leftmost occurrence until indexOf returns -1.
    static String removeOccurrences(String s, String part) {
        int i;
        while ((i = s.indexOf(part)) != -1) s = s.substring(0, i) + s.substring(i + part.length());
        return s;
    }

    public static void main(String[] args) {
        System.out.println("maxPower          = " + maxPower("abbcccddddeeeeedcba"));
        System.out.println("countHomogenous   = " + countHomogenous("abbcccaa"));
        System.out.println("countBinarySubstr = " + countBinarySubstrings("00110011"));

        char[] c = {'a', 'a', 'b', 'b', 'c', 'c', 'c'};
        int len = compress(c);
        System.out.println("compress          = " + len + " " + new String(c, 0, len));

        System.out.println("countAndSay(5)    = " + countAndSay(5));
        System.out.println("removeAdjacentDup = " + removeAdjacentDuplicates("abbaca"));
        System.out.println("removeDuplicatesK = " + removeDuplicatesK("deeedbbcccbdaa", 3));
        System.out.println("removeOccurrences = " + removeOccurrences("daabcbaabcbc", "abc"));
    }
}
