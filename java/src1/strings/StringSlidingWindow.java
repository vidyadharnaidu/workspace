import java.util.*;

/** PATTERN: int[26] or int[128] counts + two pointers. Expand right, fix left. */
public class StringSlidingWindow {

    // HOOK: remember each char's last index; jump left PAST the previous copy.
    static int lengthOfLongestSubstring(String s) {
        int[] last = new int[128];
        Arrays.fill(last, -1);
        int l = 0, best = 0;
        for (int r = 0; r < s.length(); r++) {
            l = Math.max(l, last[s.charAt(r)] + 1);
            last[s.charAt(r)] = r;
            best = Math.max(best, r - l + 1);
        }
        return best;
    }

    // HOOK: window valid while (size - mostFrequentCount) <= k replacements.
    static int characterReplacement(String s, int k) {
        int[] cnt = new int[26];
        int l = 0, maxCount = 0, best = 0;
        for (int r = 0; r < s.length(); r++) {
            maxCount = Math.max(maxCount, ++cnt[s.charAt(r) - 'A']);
            while (r - l + 1 - maxCount > k) cnt[s.charAt(l++) - 'A']--;
            best = Math.max(best, r - l + 1);
        }
        return best;
    }

    // HOOK: map char->count; shrink while the map holds more than k keys.
    static int lengthOfLongestSubstringKDistinct(String s, int k) {
        Map<Character, Integer> cnt = new HashMap<>();
        int l = 0, best = 0;
        for (int r = 0; r < s.length(); r++) {
            cnt.merge(s.charAt(r), 1, Integer::sum);
            while (cnt.size() > k)
                if (cnt.merge(s.charAt(l++), -1, Integer::sum) == 0) cnt.remove(s.charAt(l - 1));
            best = Math.max(best, r - l + 1);
        }
        return best;
    }

    // HOOK: need[] + a "missing" counter; shrink only once the window covers t.
    static String minWindow(String s, String t) {
        int[] need = new int[128];
        for (char c : t.toCharArray()) need[c]++;
        int missing = t.length(), l = 0, bs = 0, bl = Integer.MAX_VALUE;
        for (int r = 0; r < s.length(); r++) {
            if (need[s.charAt(r)]-- > 0) missing--;               // char was actually needed
            while (missing == 0) {
                if (r - l + 1 < bl) { bl = r - l + 1; bs = l; }
                if (++need[s.charAt(l++)] > 0) missing++;
            }
        }
        return bl == Integer.MAX_VALUE ? "" : s.substring(bs, bs + bl);
    }

    // HOOK: fixed window of p.length(); compare the two count arrays.
    static List<Integer> findAnagrams(String s, String p) {
        List<Integer> res = new ArrayList<>();
        if (s.length() < p.length()) return res;
        int[] need = new int[26], win = new int[26];
        for (char c : p.toCharArray()) need[c - 'a']++;
        for (int i = 0; i < s.length(); i++) {
            win[s.charAt(i) - 'a']++;
            if (i >= p.length()) win[s.charAt(i - p.length()) - 'a']--;
            if (i >= p.length() - 1 && Arrays.equals(need, win)) res.add(i - p.length() + 1);
        }
        return res;
    }

    // HOOK: same fixed window as findAnagrams - just ask "does one exist?".
    static boolean checkInclusion(String s1, String s2) {
        return !findAnagrams(s2, s1).isEmpty();
    }

    // HOOK: fixed window, count vowels in, count vowels out.
    static int maxVowels(String s, int k) {
        int cur = 0, best = 0;
        for (int i = 0; i < s.length(); i++) {
            if (isVowel(s.charAt(i))) cur++;
            if (i >= k && isVowel(s.charAt(i - k))) cur--;
            if (i >= k - 1) best = Math.max(best, cur);
        }
        return best;
    }

    static boolean isVowel(char c) { return "aeiou".indexOf(c) >= 0; }

    // HOOK: window whose total |s[i]-t[i]| cost stays within budget.
    static int equalSubstring(String s, String t, int maxCost) {
        int l = 0, cost = 0, best = 0;
        for (int r = 0; r < s.length(); r++) {
            cost += Math.abs(s.charAt(r) - t.charAt(r));
            while (cost > maxCost) cost -= Math.abs(s.charAt(l) - t.charAt(l++));
            best = Math.max(best, r - l + 1);
        }
        return best;
    }

    // HOOK: fixed window of 3, valid when all three chars differ.
    static int countGoodSubstrings(String s) {
        int res = 0;
        for (int i = 0; i + 2 < s.length(); i++) {
            char a = s.charAt(i), b = s.charAt(i + 1), c = s.charAt(i + 2);
            if (a != b && b != c && a != c) res++;
        }
        return res;
    }

    // HOOK: every valid window contributes (r-l+1) substrings ending at r.
    static long countSubstringsAllDistinct(String s) {
        int[] last = new int[128];
        Arrays.fill(last, -1);
        int l = 0;
        long res = 0;
        for (int r = 0; r < s.length(); r++) {
            l = Math.max(l, last[s.charAt(r)] + 1);
            last[s.charAt(r)] = r;
            res += r - l + 1;
        }
        return res;
    }

    // HOOK: shrink while SOME char hits k; then l = number of valid starts.
    static int countKConstraintSubstrings(String s, int k) {
        int[] cnt = new int[128];
        int l = 0, res = 0;
        for (int r = 0; r < s.length(); r++) {
            cnt[s.charAt(r)]++;
            while (cnt[s.charAt(r)] >= k) cnt[s.charAt(l++)]--;
            res += l;                                             // starts 0..l-1 all valid
        }
        return res;
    }

    // HOOK: replace a window; valid when everything OUTSIDE it is already <= n/4.
    static int balancedString(String s) {
        int n = s.length(), k = n / 4;
        int[] cnt = new int[128];
        for (char c : s.toCharArray()) cnt[c]++;
        if (ok(cnt, k)) return 0;
        int l = 0, best = n;
        for (int r = 0; r < n; r++) {
            cnt[s.charAt(r)]--;                                   // char r is inside the window
            while (l <= r && ok(cnt, k)) {
                best = Math.min(best, r - l + 1);
                cnt[s.charAt(l++)]++;
            }
        }
        return best;
    }

    static boolean ok(int[] cnt, int k) {
        return cnt['Q'] <= k && cnt['W'] <= k && cnt['E'] <= k && cnt['R'] <= k;
    }

    public static void main(String[] args) {
        System.out.println("longestNoRepeat   = " + lengthOfLongestSubstring("abcabcbb"));
        System.out.println("charReplacement   = " + characterReplacement("AABABBA", 1));
        System.out.println("kDistinct k=2     = " + lengthOfLongestSubstringKDistinct("eceba", 2));
        System.out.println("minWindow         = " + minWindow("ADOBECODEBANC", "ABC"));
        System.out.println("findAnagrams      = " + findAnagrams("cbaebabacd", "abc"));
        System.out.println("checkInclusion    = " + checkInclusion("ab", "eidbaooo"));
        System.out.println("maxVowels k=3     = " + maxVowels("abciiidef", 3));
        System.out.println("equalSubstring    = " + equalSubstring("abcd", "bcdf", 3));
        System.out.println("countGoodSubstr   = " + countGoodSubstrings("xyzzaz"));
        System.out.println("allDistinctSubstr = " + countSubstringsAllDistinct("abc"));
        System.out.println("kConstraint k=2   = " + countKConstraintSubstrings("abacb", 2));
        System.out.println("balancedString    = " + balancedString("QQWE"));
    }
}
