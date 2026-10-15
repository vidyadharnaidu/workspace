import java.util.*;
import java.util.function.Function;
import java.util.stream.*;

/** PATTERN: canonical key (sorted string / count signature) or char->char mapping. */
public class AnagramsAndMaps {

    // HOOK: one int[26]; ++ for s, -- for t; every bucket must end at 0.
    static boolean isAnagram(String s, String t) {
        if (s.length() != t.length()) return false;
        int[] cnt = new int[26];
        for (int i = 0; i < s.length(); i++) { cnt[s.charAt(i) - 'a']++; cnt[t.charAt(i) - 'a']--; }
        return Arrays.stream(cnt).allMatch(c -> c == 0);
    }

    // HOOK: group by the sorted string. groupingBy IS the whole algorithm.
    static Collection<List<String>> groupAnagrams(String[] words) {
        return Arrays.stream(words)
                .collect(Collectors.groupingBy(AnagramsAndMaps::sorted))
                .values();
    }

    // Plain char[] sort beats the stream here - chars() gives ints, not chars.
    static String sorted(String w) {
        char[] c = w.toCharArray();
        Arrays.sort(c);
        return new String(c);
    }

    // HOOK: 26 distinct letters seen -> pangram.
    static boolean checkIfPangram(String s) {
        return s.toLowerCase().chars().distinct().count() == 26;
    }

    // HOOK: count, then the first index whose count is 1.
    static int firstUniqChar(String s) {
        int[] cnt = new int[26];
        for (char c : s.toCharArray()) cnt[c - 'a']++;
        return IntStream.range(0, s.length())
                .filter(i -> cnt[s.charAt(i) - 'a'] == 1)
                .findFirst().orElse(-1);
    }

    // HOOK: split on non-letters, count, drop banned, take the max entry.
    static String mostCommonWord(String paragraph, String[] banned) {
        Set<String> ban = Set.of(banned);
        return Arrays.stream(paragraph.toLowerCase().split("[^a-z]+"))
                .filter(w -> !w.isEmpty() && !ban.contains(w))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("");
    }

    // HOOK: keep the per-letter MINIMUM count across all words.
    static List<String> commonChars(String[] words) {
        int[] min = new int[26];
        Arrays.fill(min, Integer.MAX_VALUE);
        for (String w : words) {
            int[] cnt = new int[26];
            for (char c : w.toCharArray()) cnt[c - 'a']++;
            for (int i = 0; i < 26; i++) min[i] = Math.min(min[i], cnt[i]);
        }
        return IntStream.range(0, 26).boxed()
                .flatMap(i -> Collections.nCopies(min[i], String.valueOf((char) ('a' + i))).stream())
                .toList();
    }

    // HOOK: slide a 10-char window into a set; a second insert means repeated.
    static List<String> findRepeatedDnaSequences(String s) {
        Set<String> seen = new HashSet<>();
        return IntStream.rangeClosed(0, Math.max(0, s.length() - 10))
                .mapToObj(i -> s.substring(i, i + 10))
                .filter(w -> !seen.add(w))
                .distinct().toList();
    }

    // HOOK: length-prefix each chunk ("4#lint") so any payload is safe.
    static String encode(List<String> list) {
        return list.stream().map(s -> s.length() + "#" + s).collect(Collectors.joining());
    }

    static List<String> decode(String s) {
        List<String> res = new ArrayList<>();
        int i = 0;
        while (i < s.length()) {
            int j = s.indexOf('#', i);
            int len = Integer.parseInt(s.substring(i, j));
            res.add(s.substring(j + 1, j + 1 + len));
            i = j + 1 + len;
        }
        return res;
    }

    // HOOK: bijection needs BOTH directions checked.
    static boolean isIsomorphic(String s, String t) {
        Map<Character, Character> fwd = new HashMap<>(), back = new HashMap<>();
        for (int i = 0; i < s.length(); i++) {
            char a = s.charAt(i), b = t.charAt(i);
            if (fwd.putIfAbsent(a, b) != null && fwd.get(a) != b) return false;
            if (back.putIfAbsent(b, a) != null && back.get(b) != a) return false;
        }
        return true;
    }

    // HOOK: same bijection, but char <-> whole word.
    static boolean wordPattern(String pattern, String s) {
        String[] words = s.split(" ");
        if (words.length != pattern.length()) return false;
        Map<Character, String> fwd = new HashMap<>();
        Map<String, Character> back = new HashMap<>();
        for (int i = 0; i < words.length; i++) {
            char c = pattern.charAt(i);
            if (fwd.putIfAbsent(c, words[i]) != null && !fwd.get(c).equals(words[i])) return false;
            if (back.putIfAbsent(words[i], c) != null && back.get(words[i]) != c) return false;
        }
        return true;
    }

    // HOOK: count "xy" and "yx" mismatches; pairs cost 1 swap, one of each costs 2.
    static int minimumSwaps(String s1, String s2) {
        int xy = 0, yx = 0;
        for (int i = 0; i < s1.length(); i++) {
            if (s1.charAt(i) == 'x' && s2.charAt(i) == 'y') xy++;
            if (s1.charAt(i) == 'y' && s2.charAt(i) == 'x') yx++;
        }
        if ((xy + yx) % 2 != 0) return -1;
        return xy / 2 + yx / 2 + (xy % 2) * 2;
    }

    public static void main(String[] args) {
        System.out.println("isAnagram         = " + isAnagram("anagram", "nagaram"));
        System.out.println("groupAnagrams     = " + groupAnagrams(new String[]{"eat", "tea", "tan", "ate", "nat", "bat"}));
        System.out.println("isPangram         = " + checkIfPangram("thequickbrownfoxjumpsoverthelazydog"));
        System.out.println("firstUniqChar     = " + firstUniqChar("loveleetcode"));
        System.out.println("mostCommonWord    = " + mostCommonWord("Bob hit a ball, the hit BALL flew far after it was hit.", new String[]{"hit"}));
        System.out.println("commonChars       = " + commonChars(new String[]{"bella", "label", "roller"}));
        System.out.println("repeatedDna       = " + findRepeatedDnaSequences("AAAAACCCCCAAAAACCCCCCAAAAAGGGTTT"));

        String enc = encode(List.of("lint", "co#de", "love"));
        System.out.println("encode            = " + enc);
        System.out.println("decode            = " + decode(enc));

        System.out.println("isIsomorphic      = " + isIsomorphic("paper", "title"));
        System.out.println("isIsomorphic bad  = " + isIsomorphic("badc", "baba"));
        System.out.println("wordPattern       = " + wordPattern("abba", "dog cat cat dog"));
        System.out.println("minimumSwaps      = " + minimumSwaps("xxyyxyxyxx", "xyyxyxxxyx"));
    }
}
