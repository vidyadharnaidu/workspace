package collections;

import java.util.*;
import java.util.function.Function;
import java.util.stream.*;

/**
 * Top K frequent words in a text / collection.
 *
 * TRICK: 3 steps -> SPLIT -> GROUP+COUNT -> SORT by count desc (tie: alphabetical) -> LIMIT k
 */
public class FrequentWords {

    static List<Map.Entry<String, Long>> topK(Collection<String> lines, int k) {
        return lines.stream()
                .flatMap(line -> Arrays.stream(line.toLowerCase().split("\\W+")))
                .filter(w -> !w.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(k)
                .toList();
    }

    // Most frequent single word
    static Optional<String> mostFrequent(List<String> words) {
        return words.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    // Top K frequent NUMBERS: [1,1,1,2,2,3], k=2 -> [1, 2]
    static List<Integer> topKNumbers(int[] nums, int k) {
        return Arrays.stream(nums).boxed()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .limit(k)
                .map(Map.Entry::getKey)
                .toList();
    }

    public static void main(String[] args) {
        List<String> text = List.of(
                "the quick brown fox jumps over the lazy dog",
                "The dog barks, the fox runs!",
                "a quick dog");

        topK(text, 3).forEach(e -> System.out.println(e.getKey() + " = " + e.getValue()));
        // the = 4, dog = 3, fox = 2

        System.out.println(mostFrequent(List.of("apple", "banana", "apple", "cherry"))); // Optional[apple]
        System.out.println(topKNumbers(new int[]{1, 1, 1, 2, 2, 3}, 2));                   // [1, 2]
    }
}
