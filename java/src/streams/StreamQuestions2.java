package streams;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.*;

/**
 * More frequently asked Java Stream interview questions (Q21..Q50).
 * Continues from {@link StreamQuestions}.
 *
 * TRICK cheat-sheet:
 *   chars of a String       -> s.chars().mapToObj(c -> (char) c)
 *   first non-repeated      -> groupingBy(identity(), LinkedHashMap::new, counting()) then filter count == 1
 *   duplicates              -> filter(x -> !seen.add(x))   or   frequency map, filter count > 1
 *   Nth highest             -> distinct().sorted(reverseOrder()).skip(n - 1).findFirst()
 *   sort map by value       -> entrySet().stream().sorted(Map.Entry.comparingByValue(reverseOrder()))
 *   list -> map             -> toMap(key, value, mergeFn, LinkedHashMap::new)
 *   number ranges           -> IntStream.rangeClosed(a, b)
 *   min/max/avg/sum at once -> summaryStatistics()
 *   multi-level sort        -> comparing(a).thenComparing(b, reverseOrder())
 */
public class StreamQuestions2 {

    record Employee(String name, String dept, double salary, int age) {}

    static final List<Employee> EMPS = List.of(
            new Employee("Alice", "IT", 120000, 30),
            new Employee("Bob", "IT", 95000, 45),
            new Employee("Carol", "HR", 70000, 28),
            new Employee("Dan", "HR", 82000, 35),
            new Employee("Eve", "Sales", 60000, 25),
            new Employee("Frank", "Sales", 99000, 50),
            new Employee("Grace", "IT", 110000, 38));

    public static void main(String[] args) {
        List<Integer> nums = List.of(5, 3, 8, 3, 1, 8, 9, 2, 5, 7);
        List<String> words = List.of("listen", "silent", "enlist", "google", "gooegl", "cat", "act", "tac", "dog");
        String text = "swiss programming";

        System.out.println("Q21 " + q21FirstNonRepeatedChar(text));
        System.out.println("Q22 " + q22FirstRepeatedChar(text));
        System.out.println("Q23 " + q23Duplicates(nums));
        System.out.println("Q24 " + q24NthHighestSalary(EMPS, 2));
        System.out.println("Q25 " + q25CharFrequency("banana"));
        System.out.println("Q26 " + q26ReverseEachWord("java streams are fun"));
        System.out.println("Q27 " + q27IsPalindrome("Madam") + " " + q27IsPalindrome("Java"));
        System.out.println("Q28 " + q28Statistics(nums));
        System.out.println("Q29 " + q29SortMapByValueDesc(Map.of("a", 3, "b", 9, "c", 1, "d", 5)));
        System.out.println("Q30 " + q30ListToMap(EMPS));
        System.out.println("Q31 " + q31LongestString(words));
        System.out.println("Q32 " + q32GroupAnagrams(words));
        System.out.println("Q33 " + q33NamesByDept(EMPS));
        System.out.println("Q34 " + q34SortByDeptThenSalaryDesc(EMPS));
        System.out.println("Q35 " + q35PartitionBySalary(EMPS, 90000));
        System.out.println("Q36 " + q36MergeSortedDistinct(List.of(5, 1, 3, 3), List.of(4, 1, 6, 5)));
        System.out.println("Q37 " + q37CountVowels("Java Stream Interview"));
        System.out.println("Q38 " + q38Fibonacci(10));
        System.out.println("Q39 " + q39Primes(1, 50));
        System.out.println("Q40 " + q40SumOfDigits(98765));
        System.out.println("Q41 " + q41MatchOps(nums));
        System.out.println("Q42 " + q42MostFrequent(nums));
        System.out.println("Q43 " + q43SecondHighestPerDept(EMPS));
        System.out.println("Q44 " + q44OldestAndYoungest(EMPS));
        System.out.println("Q45 " + q45TakeWhileDropWhile(List.of(1, 2, 3, 10, 4, 5)));
        System.out.println("Q46 " + q46CapitalizeWords("hello java stream world"));
        System.out.println("Q47 " + q47MissingNumber(List.of(1, 2, 4, 5, 6), 6));
        System.out.println("Q48 " + q48RemoveDuplicateChars("programming"));
        System.out.println("Q49 " + q49Product(List.of(1, 2, 3, 4, 5)));
        System.out.println("Q50 " + q50EvenIndexElements(List.of("a", "b", "c", "d", "e", "f")));
    }

    // Q21. First non-repeated character in a string
    //      LinkedHashMap keeps insertion order so the "first" one is found.
    static Optional<Character> q21FirstNonRepeatedChar(String s) {
        return s.chars()
                .mapToObj(c -> (char) c)
                .collect(groupingBy(Function.identity(), LinkedHashMap::new, counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() == 1)
                .map(Map.Entry::getKey)
                .findFirst();
    }

    // Q22. First repeated character in a string
    //      Set.add returns false when the element is already present.
    static Optional<Character> q22FirstRepeatedChar(String s) {
        Set<Character> seen = new HashSet<>();
        return s.chars()
                .mapToObj(c -> (char) c)
                .filter(c -> !seen.add(c))
                .findFirst();
    }

    // Q23. Find duplicate elements in a list
    static Set<Integer> q23Duplicates(List<Integer> nums) {
        Set<Integer> seen = new HashSet<>();
        return nums.stream()
                .filter(n -> !seen.add(n))
                .collect(toCollection(LinkedHashSet::new));
        // Stateless alternative (safe for parallel):
        // nums.stream().collect(groupingBy(identity(), counting()))
        //     .entrySet().stream().filter(e -> e.getValue() > 1).map(Map.Entry::getKey).collect(toSet());
    }

    // Q24. Find the Nth highest salary (distinct so ties don't count twice)
    static Optional<Double> q24NthHighestSalary(List<Employee> emps, int n) {
        return emps.stream()
                .map(Employee::salary)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .skip(n - 1)
                .findFirst();
    }

    // Q25. Count frequency of each character in a string
    static Map<Character, Long> q25CharFrequency(String s) {
        return s.chars()
                .mapToObj(c -> (char) c)
                .collect(groupingBy(Function.identity(), LinkedHashMap::new, counting()));
    }

    // Q26. Reverse each word of a sentence (word order kept)
    static String q26ReverseEachWord(String sentence) {
        return Arrays.stream(sentence.split("\\s+"))
                .map(w -> new StringBuilder(w).reverse().toString())
                .collect(joining(" "));
    }

    // Q27. Check if a string is a palindrome (case-insensitive)
    static boolean q27IsPalindrome(String s) {
        String t = s.toLowerCase();
        return IntStream.range(0, t.length() / 2)
                .allMatch(i -> t.charAt(i) == t.charAt(t.length() - 1 - i));
    }

    // Q28. Sum, min, max, average, count in ONE pass
    static IntSummaryStatistics q28Statistics(List<Integer> nums) {
        return nums.stream()
                .mapToInt(Integer::intValue)
                .summaryStatistics();
    }

    // Q29. Sort a map by value (descending)
    static Map<String, Integer> q29SortMapByValueDesc(Map<String, Integer> map) {
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));   // LinkedHashMap keeps the sorted order
    }

    // Q30. Convert a list to a map (name -> salary)
    //      Merge fn avoids IllegalStateException on duplicate keys.
    static Map<String, Double> q30ListToMap(List<Employee> emps) {
        return emps.stream()
                .collect(toMap(Employee::name, Employee::salary, (a, b) -> a, LinkedHashMap::new));
    }

    // Q31. Find the longest string
    static Optional<String> q31LongestString(List<String> words) {
        return words.stream()
                .max(comparing(String::length));
    }

    // Q32. Group anagrams (key = sorted characters)
    static Collection<List<String>> q32GroupAnagrams(List<String> words) {
        return words.stream()
                .collect(groupingBy(w -> w.chars().sorted()
                                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                                .toString(),
                        LinkedHashMap::new, toList()))
                .values();
    }

    // Q33. Employee names grouped by department
    static Map<String, List<String>> q33NamesByDept(List<Employee> emps) {
        return emps.stream()
                .collect(groupingBy(Employee::dept, TreeMap::new, mapping(Employee::name, toList())));
    }

    // Q34. Sort employees by department, then by salary descending
    static List<String> q34SortByDeptThenSalaryDesc(List<Employee> emps) {
        return emps.stream()
                .sorted(comparing(Employee::dept)
                        .thenComparing(Employee::salary, Comparator.reverseOrder()))
                .map(e -> e.dept() + ":" + e.name())
                .toList();
    }

    // Q35. Partition employees by salary threshold
    static Map<Boolean, List<String>> q35PartitionBySalary(List<Employee> emps, double threshold) {
        return emps.stream()
                .collect(partitioningBy(e -> e.salary() > threshold, mapping(Employee::name, toList())));
    }

    // Q36. Merge two lists, remove duplicates and sort
    static List<Integer> q36MergeSortedDistinct(List<Integer> a, List<Integer> b) {
        return Stream.concat(a.stream(), b.stream())
                .distinct()
                .sorted()
                .toList();
    }

    // Q37. Count vowels in a string
    static long q37CountVowels(String s) {
        return s.toLowerCase().chars()
                .filter(c -> "aeiou".indexOf(c) >= 0)
                .count();
    }

    // Q38. First N Fibonacci numbers (Stream.iterate over pairs)
    static List<Long> q38Fibonacci(int n) {
        return Stream.iterate(new long[]{0, 1}, f -> new long[]{f[1], f[0] + f[1]})
                .limit(n)
                .map(f -> f[0])
                .toList();
    }

    // Q39. Prime numbers in a range
    static List<Integer> q39Primes(int from, int to) {
        return IntStream.rangeClosed(from, to)
                .filter(StreamQuestions2::isPrime)
                .boxed()
                .toList();
    }

    static boolean isPrime(int n) {
        return n > 1 && IntStream.rangeClosed(2, (int) Math.sqrt(n)).noneMatch(d -> n % d == 0);
    }

    // Q40. Sum of digits of a number
    static int q40SumOfDigits(int n) {
        return String.valueOf(Math.abs(n)).chars()
                .map(Character::getNumericValue)
                .sum();
    }

    // Q41. anyMatch / allMatch / noneMatch (all short-circuit)
    static String q41MatchOps(List<Integer> nums) {
        boolean anyGt8 = nums.stream().anyMatch(n -> n > 8);
        boolean allPositive = nums.stream().allMatch(n -> n > 0);
        boolean noneNegative = nums.stream().noneMatch(n -> n < 0);
        return "any>8=" + anyGt8 + ", allPositive=" + allPositive + ", noneNegative=" + noneNegative;
    }

    // Q42. Most frequent element
    static Optional<Integer> q42MostFrequent(List<Integer> nums) {
        return nums.stream()
                .collect(groupingBy(Function.identity(), counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    // Q43. Second highest paid employee in each department
    static Map<String, Optional<String>> q43SecondHighestPerDept(List<Employee> emps) {
        return emps.stream()
                .collect(groupingBy(Employee::dept, TreeMap::new,
                        collectingAndThen(toList(), list -> list.stream()
                                .sorted(comparingDouble(Employee::salary).reversed())
                                .skip(1)
                                .findFirst()
                                .map(Employee::name))));
    }

    // Q44. Oldest and youngest employee
    static String q44OldestAndYoungest(List<Employee> emps) {
        Optional<Employee> oldest = emps.stream().max(comparing(Employee::age));
        Optional<Employee> youngest = emps.stream().min(comparing(Employee::age));
        return "oldest=" + oldest.map(Employee::name).orElse("-")
                + ", youngest=" + youngest.map(Employee::name).orElse("-");
    }

    // Q45. takeWhile vs dropWhile (Java 9+) — stop at the FIRST element failing the predicate,
    //      unlike filter which checks every element.
    static String q45TakeWhileDropWhile(List<Integer> nums) {
        List<Integer> take = nums.stream().takeWhile(n -> n < 5).toList();   // [1, 2, 3]
        List<Integer> drop = nums.stream().dropWhile(n -> n < 5).toList();   // [10, 4, 5]
        return "takeWhile=" + take + ", dropWhile=" + drop;
    }

    // Q46. Capitalize the first letter of each word
    static String q46CapitalizeWords(String s) {
        return Arrays.stream(s.split("\\s+"))
                .filter(w -> !w.isEmpty())
                .map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1))
                .collect(joining(" "));
    }

    // Q47. Find the missing number in 1..n
    static List<Integer> q47MissingNumber(List<Integer> nums, int n) {
        Set<Integer> present = new HashSet<>(nums);
        return IntStream.rangeClosed(1, n)
                .filter(i -> !present.contains(i))
                .boxed()
                .toList();
        // Single missing number shortcut: n * (n + 1) / 2 - nums.stream().mapToInt(i -> i).sum()
    }

    // Q48. Remove duplicate characters from a string (keep first occurrence)
    static String q48RemoveDuplicateChars(String s) {
        return s.chars()
                .distinct()
                .mapToObj(c -> String.valueOf((char) c))
                .collect(Collectors.joining());
    }

    // Q49. Product of all numbers (reduce with identity 1)
    static long q49Product(List<Integer> nums) {
        return nums.stream()
                .mapToLong(Integer::longValue)
                .reduce(1L, (a, b) -> a * b);
    }

    // Q50. Elements at even indexes (streams have no index -> stream over indexes)
    static List<String> q50EvenIndexElements(List<String> list) {
        return IntStream.range(0, list.size())
                .filter(i -> i % 2 == 0)
                .mapToObj(list::get)
                .toList();
    }
}
