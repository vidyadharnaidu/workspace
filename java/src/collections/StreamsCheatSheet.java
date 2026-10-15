package collections;

import java.util.*;
import java.util.function.Function;
import java.util.stream.*;

import static java.util.stream.Collectors.*;

/**
 * Stream one-liners that show up in almost every Java interview.
 */
public class StreamsCheatSheet {

    public static void main(String[] args) {
        List<Integer> nums = List.of(5, 3, 8, 1, 9, 2, 8, 4);
        List<String> words = List.of("apple", "banana", "cherry", "avocado", "blueberry", "kiwi");

        // Filter / map / reduce
        System.out.println("evens          " + nums.stream().filter(n -> n % 2 == 0).toList());
        System.out.println("squares        " + nums.stream().map(n -> n * n).toList());
        System.out.println("sum            " + nums.stream().mapToInt(Integer::intValue).sum());
        System.out.println("sum (reduce)   " + nums.stream().reduce(0, Integer::sum));
        System.out.println("max            " + nums.stream().max(Integer::compare).get());
        System.out.println("average        " + nums.stream().mapToInt(i -> i).average().orElse(0));

        // Sorting
        System.out.println("sorted desc    " + nums.stream().sorted(Comparator.reverseOrder()).toList());
        System.out.println("by length      " + words.stream().sorted(Comparator.comparing(String::length).thenComparing(Function.identity())).toList());

        // Grouping / partitioning
        System.out.println("group by 1st   " + words.stream().collect(groupingBy(w -> w.charAt(0))));
        System.out.println("group by len   " + words.stream().collect(groupingBy(String::length, TreeMap::new, counting())));
        System.out.println("partition even " + nums.stream().collect(partitioningBy(n -> n % 2 == 0)));

        // To map (word -> length)
        System.out.println("toMap          " + words.stream().collect(toMap(Function.identity(), String::length)));

        // Joining
        System.out.println("joining        " + words.stream().map(String::toUpperCase).collect(joining(", ", "[", "]")));

        // flatMap: list of lists -> flat list
        List<List<Integer>> nested = List.of(List.of(1, 2), List.of(3, 4), List.of(5));
        System.out.println("flatMap        " + nested.stream().flatMap(List::stream).toList());

        // Longest word
        System.out.println("longest word   " + words.stream().max(Comparator.comparingInt(String::length)).get());

        // Starts with / anyMatch / allMatch
        System.out.println("starts with a  " + words.stream().filter(w -> w.startsWith("a")).toList());
        System.out.println("anyMatch >8    " + nums.stream().anyMatch(n -> n > 8));

        // Frequency of each character in a string
        System.out.println("char freq      " + "banana".chars().mapToObj(c -> (char) c).collect(groupingBy(Function.identity(), counting())));

        // Convert int[] <-> List<Integer>
        int[] arr = {3, 1, 2};
        List<Integer> list = Arrays.stream(arr).boxed().toList();
        int[] back = list.stream().mapToInt(Integer::intValue).toArray();
        System.out.println("array<->list   " + list + " " + Arrays.toString(back));

        // Last element, skip/limit (pagination)
        System.out.println("last element   " + nums.stream().reduce((a, b) -> b).get());
        System.out.println("page 2 (size3) " + nums.stream().skip(3).limit(3).toList());

        // Sort a map by value desc
        Map<String, Integer> scores = Map.of("amy", 80, "bob", 95, "cat", 70);
        System.out.println("map by value   " + scores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new)));

        // Parallel sum
        System.out.println("parallel sum   " + LongStream.rangeClosed(1, 1_000_000).parallel().sum());
    }
}
