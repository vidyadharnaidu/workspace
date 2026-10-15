package slidingwindow;

import java.util.*;
import java.util.stream.*;

/**
 * Sliding window problems.
 *
 * TEMPLATE (variable window) - memorize this shape, almost every problem is a variation:
 *
 *     for (right = 0; right < n; right++) {
 *         add a[right] to window
 *         while (window is INVALID) remove a[left++]
 *         update best with (right - left + 1)
 *     }
 *
 * FIXED window of size k: add a[i], remove a[i - k] once i >= k.
 * Negative numbers? Sliding window breaks -> use PREFIX SUM + HashMap instead.
 */
public class SlidingWindow {

    // 1. MAX SUM OF K CONSECUTIVE: [2,1,5,1,3,2], k=3 -> 9
    // TRICK: fixed window, add the new element, subtract the one that falls out
    static int maxSumK(int[] a, int k) {
        int sum = 0, best = Integer.MIN_VALUE;
        for (int i = 0; i < a.length; i++) {
            sum += a[i];
            if (i >= k) sum -= a[i - k];
            if (i >= k - 1) best = Math.max(best, sum);
        }
        return best;
    }

    // 1b. Same with streams (brute force O(n*k), fine to show you know streams)
    static int maxSumKStream(int[] a, int k) {
        return IntStream.rangeClosed(0, a.length - k)
                .map(i -> Arrays.stream(a, i, i + k).sum())
                .max().orElse(0);
    }

    // 2. SMALLEST SUBARRAY WITH SUM >= target: target=7, [2,3,1,2,4,3] -> 2 ([4,3])
    // TRICK: shrink WHILE valid (we want the minimum)
    static int minSubArrayLen(int target, int[] a) {
        int left = 0, sum = 0, best = Integer.MAX_VALUE;
        for (int right = 0; right < a.length; right++) {
            sum += a[right];
            while (sum >= target) {
                best = Math.min(best, right - left + 1);
                sum -= a[left++];
            }
        }
        return best == Integer.MAX_VALUE ? 0 : best;
    }

    // 3. LONGEST SUBSTRING WITH AT MOST K DISTINCT CHARS: "eceba", k=2 -> 3 ("ece")
    // TRICK: map char->count; shrink while map.size() > k (remove key when count hits 0)
    static int longestKDistinct(String s, int k) {
        Map<Character, Integer> count = new HashMap<>();
        int left = 0, best = 0;
        for (int right = 0; right < s.length(); right++) {
            count.merge(s.charAt(right), 1, Integer::sum);
            while (count.size() > k) {
                char c = s.charAt(left++);
                if (count.merge(c, -1, Integer::sum) == 0) count.remove(c);
            }
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    // 4. LONGEST SUBSTRING WITHOUT REPEATING CHARS: "abcabcbb" -> 3
    // TRICK: Set as window; shrink while the new char is already inside
    static int longestUnique(String s) {
        Set<Character> window = new HashSet<>();
        int left = 0, best = 0;
        for (int right = 0; right < s.length(); right++) {
            while (!window.add(s.charAt(right))) window.remove(s.charAt(left++));
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    // 5. LONGEST REPEATING CHARACTER REPLACEMENT: "AABABBA", k=1 -> 4
    // TRICK: window is valid if (length - count of most frequent char) <= k
    static int characterReplacement(String s, int k) {
        int[] count = new int[26];
        int left = 0, maxFreq = 0, best = 0;
        for (int right = 0; right < s.length(); right++) {
            maxFreq = Math.max(maxFreq, ++count[s.charAt(right) - 'A']);
            while (right - left + 1 - maxFreq > k) count[s.charAt(left++) - 'A']--;
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    // 6. MAX CONSECUTIVE ONES (flip at most k zeros): [1,1,1,0,0,0,1,1,1,1,0], k=2 -> 6
    // TRICK: count zeros in window; shrink while zeros > k
    static int longestOnes(int[] a, int k) {
        int left = 0, zeros = 0, best = 0;
        for (int right = 0; right < a.length; right++) {
            if (a[right] == 0) zeros++;
            while (zeros > k) if (a[left++] == 0) zeros--;
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    // 7. FIND ALL ANAGRAMS (permutation in string): s="cbaebabacd", p="abc" -> [0, 6]
    // TRICK: fixed window of p.length(), compare two int[26] count arrays
    static List<Integer> findAnagrams(String s, String p) {
        int[] need = new int[26], window = new int[26];
        for (char c : p.toCharArray()) need[c - 'a']++;
        List<Integer> res = new ArrayList<>();
        int k = p.length();
        for (int i = 0; i < s.length(); i++) {
            window[s.charAt(i) - 'a']++;
            if (i >= k) window[s.charAt(i - k) - 'a']--;
            if (i >= k - 1 && Arrays.equals(need, window)) res.add(i - k + 1);
        }
        return res;
    }

    // 8. MINIMUM WINDOW SUBSTRING: s="ADOBECODEBANC", t="ABC" -> "BANC"
    // TRICK: need-map + "missing" counter. Expand until missing == 0, then shrink while still valid.
    static String minWindow(String s, String t) {
        Map<Character, Integer> need = new HashMap<>();
        for (char c : t.toCharArray()) need.merge(c, 1, Integer::sum);
        int missing = t.length(), left = 0, start = 0, bestLen = Integer.MAX_VALUE;

        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            if (need.getOrDefault(c, 0) > 0) missing--;
            need.merge(c, -1, Integer::sum);

            while (missing == 0) {
                if (right - left + 1 < bestLen) {
                    bestLen = right - left + 1;
                    start = left;
                }
                char out = s.charAt(left++);
                if (need.merge(out, 1, Integer::sum) > 0) missing++;
            }
        }
        return bestLen == Integer.MAX_VALUE ? "" : s.substring(start, start + bestLen);
    }

    // 9. SLIDING WINDOW MAXIMUM: [1,3,-1,-3,5,3,6,7], k=3 -> [3,3,5,5,6,7]
    // TRICK: Deque of INDEXES with DECREASING values. Front = max.
    //        Drop front if out of window; drop back while smaller than new value.
    static int[] maxSlidingWindow(int[] a, int k) {
        Deque<Integer> dq = new ArrayDeque<>();
        int[] res = new int[a.length - k + 1];
        for (int i = 0; i < a.length; i++) {
            if (!dq.isEmpty() && dq.peekFirst() <= i - k) dq.pollFirst();
            while (!dq.isEmpty() && a[dq.peekLast()] < a[i]) dq.pollLast();
            dq.offerLast(i);
            if (i >= k - 1) res[i - k + 1] = a[dq.peekFirst()];
        }
        return res;
    }

    // 10. SUBARRAY SUM EQUALS K (negatives allowed): [1,2,3], k=3 -> 2 ([1,2] and [3])
    // TRICK: PREFIX SUM. If (sum - k) was seen before, those subarrays end here.
    static int subarraySum(int[] a, int k) {
        Map<Integer, Integer> seen = new HashMap<>(Map.of(0, 1));
        int sum = 0, count = 0;
        for (int x : a) {
            sum += x;
            count += seen.getOrDefault(sum - k, 0);
            seen.merge(sum, 1, Integer::sum);
        }
        return count;
    }

    public static void main(String[] args) {
        System.out.println("maxSumK              " + maxSumK(new int[]{2, 1, 5, 1, 3, 2}, 3));            // 9
        System.out.println("maxSumKStream        " + maxSumKStream(new int[]{2, 1, 5, 1, 3, 2}, 3));      // 9
        System.out.println("minSubArrayLen       " + minSubArrayLen(7, new int[]{2, 3, 1, 2, 4, 3}));     // 2
        System.out.println("longestKDistinct     " + longestKDistinct("eceba", 2));                       // 3
        System.out.println("longestUnique        " + longestUnique("abcabcbb"));                          // 3
        System.out.println("characterReplacement " + characterReplacement("AABABBA", 1));                 // 4
        System.out.println("longestOnes          " + longestOnes(new int[]{1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0}, 2)); // 6
        System.out.println("findAnagrams         " + findAnagrams("cbaebabacd", "abc"));                  // [0, 6]
        System.out.println("minWindow            " + minWindow("ADOBECODEBANC", "ABC"));                  // BANC
        System.out.println("maxSlidingWindow     " + Arrays.toString(maxSlidingWindow(new int[]{1, 3, -1, -3, 5, 3, 6, 7}, 3))); // [3, 3, 5, 5, 6, 7]
        System.out.println("subarraySum          " + subarraySum(new int[]{1, 2, 3}, 3));                 // 2
    }
}
