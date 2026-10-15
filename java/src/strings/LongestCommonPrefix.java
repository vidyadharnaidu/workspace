package strings;

import java.util.*;
import java.util.stream.*;

/**
 * Longest Common Prefix: ["flower","flow","flight"] -> "fl"
 * Shortest Unique Prefix: ["zebra","dog","duck","dove"] -> z, dog, du, dov
 *
 * TRICK (LCP): SORT, then compare only FIRST and LAST word (they differ the most).
 * TRICK (Unique): COUNT every prefix, then pick the first prefix whose count == 1.
 */
public class LongestCommonPrefix {

    static String longestCommonPrefix(String[] words) {
        if (words == null || words.length == 0) return "";
        String[] s = words.clone();
        Arrays.sort(s);
        String first = s[0], last = s[s.length - 1];
        int len = (int) IntStream.range(0, first.length())
                .takeWhile(i -> first.charAt(i) == last.charAt(i))
                .count();
        return first.substring(0, len);
    }

    static Stream<String> prefixes(String w) {
        return IntStream.rangeClosed(1, w.length()).mapToObj(i -> w.substring(0, i));
    }

    static Map<String, String> shortestUniquePrefix(List<String> words) {
        Map<String, Long> counts = words.stream()
                .flatMap(LongestCommonPrefix::prefixes)
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()));

        return words.stream().collect(Collectors.toMap(
                w -> w,
                w -> prefixes(w).filter(p -> counts.get(p) == 1).findFirst().orElse(w),
                (a, b) -> a, LinkedHashMap::new));
    }

    public static void main(String[] args) {
        System.out.println(longestCommonPrefix(new String[]{"flower", "flow", "flight"})); // fl
        System.out.println(longestCommonPrefix(new String[]{"dog", "racecar", "car"}));    // ""
        System.out.println(shortestUniquePrefix(List.of("zebra", "dog", "duck", "dove")));
        // {zebra=z, dog=dog, duck=du, dove=dov}
    }
}
