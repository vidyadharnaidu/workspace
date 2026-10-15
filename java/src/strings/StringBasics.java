package strings;

import java.util.*;
import java.util.function.Function;
import java.util.stream.*;

/**
 * Classic "warm-up" string questions, all with streams.
 *
 * TRICK: s.chars() gives an IntStream -> mapToObj(c -> (char) c) -> groupingBy.
 *        Use LinkedHashMap when ORDER matters (first non-repeating etc.).
 */
public class StringBasics {

    // "hello" -> "olleh"
    static String reverse(String s) {
        return new StringBuilder(s).reverse().toString();
    }

    // "Java is fun" -> "fun is Java"
    static String reverseWords(String s) {
        List<String> words = Arrays.asList(s.trim().split("\\s+"));
        Collections.reverse(words);
        return String.join(" ", words);
    }

    // "programming" -> {p=1, r=2, o=1, g=2, a=1, m=2, i=1, n=1}
    static Map<Character, Long> charFrequency(String s) {
        return s.chars().mapToObj(c -> (char) c)
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
    }

    // "swiss" -> 'w'
    static Optional<Character> firstNonRepeating(String s) {
        return charFrequency(s).entrySet().stream()
                .filter(e -> e.getValue() == 1)
                .map(Map.Entry::getKey)
                .findFirst();
    }

    // "swiss" -> 's'
    static Optional<Character> firstRepeating(String s) {
        Set<Character> seen = new HashSet<>();
        return s.chars().mapToObj(c -> (char) c).filter(c -> !seen.add(c)).findFirst();
    }

    // "listen", "silent" -> true
    static boolean isAnagram(String a, String b) {
        return sortChars(a).equals(sortChars(b));
    }

    static String sortChars(String s) {
        char[] c = s.toLowerCase().toCharArray();
        Arrays.sort(c);
        return new String(c);
    }

    // ["eat","tea","tan","ate","nat","bat"] -> [[eat, tea, ate], [tan, nat], [bat]]
    static Collection<List<String>> groupAnagrams(List<String> words) {
        return words.stream()
                .collect(Collectors.groupingBy(StringBasics::sortChars, LinkedHashMap::new, Collectors.toList()))
                .values();
    }

    // "aaabbc" -> "a3b2c1"
    static String compress(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, count = 1; i < s.length(); i++, count++) {
            if (i + 1 == s.length() || s.charAt(i) != s.charAt(i + 1)) {
                sb.append(s.charAt(i)).append(count);
                count = 0;
            }
        }
        return sb.toString();
    }

    // "Hello World" -> 3
    static long countVowels(String s) {
        return s.toLowerCase().chars().filter(c -> "aeiou".indexOf(c) >= 0).count();
    }

    // "abcabcbb" -> 3 ("abc")   TRICK: SLIDING WINDOW, remember last index of each char
    static int longestUniqueSubstring(String s) {
        Map<Character, Integer> lastSeen = new HashMap<>();
        int best = 0;
        for (int left = 0, right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            if (lastSeen.containsKey(c)) left = Math.max(left, lastSeen.get(c) + 1);
            lastSeen.put(c, right);
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    public static void main(String[] args) {
        System.out.println(reverse("hello"));                        // olleh
        System.out.println(reverseWords("  Java is   fun "));        // fun is Java
        System.out.println(charFrequency("programming"));
        System.out.println(firstNonRepeating("swiss").orElse('-'));  // w
        System.out.println(firstRepeating("swiss").orElse('-'));     // s
        System.out.println(isAnagram("Listen", "Silent"));           // true
        System.out.println(groupAnagrams(List.of("eat", "tea", "tan", "ate", "nat", "bat")));
        System.out.println(compress("aaabbc"));                      // a3b2c1
        System.out.println(countVowels("Hello World"));              // 3
        System.out.println(longestUniqueSubstring("abcabcbb"));      // 3
        System.out.println(longestUniqueSubstring("pwwkew"));        // 3
    }
}
