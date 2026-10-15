package streams;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.*;

/**
 * 20 classic Java Stream interview questions (Q1..Q20), one method each.
 *
 * TRICK cheat-sheet:
 *   dedupe + sort      -> distinct().sorted(reverseOrder())
 *   2 buckets          -> partitioningBy(predicate)          (Map<Boolean, List<T>>)
 *   N buckets          -> groupingBy(key, downstream)
 *   frequency          -> groupingBy(identity(), counting())
 *   flatten            -> flatMap(List::stream)
 *   top N              -> sorted(comparator.reversed()).limit(N)
 *   max per group      -> groupingBy(key, collectingAndThen(maxBy(..), Optional::get))
 *   join strings       -> collect(joining(", "))
 *   intersection       -> filter(set::contains)   (set for O(1) lookup)
 *   fast sum           -> mapToDouble(..).sum()  (primitive stream, no boxing) / parallel for huge data
 */
public class StreamQuestions {

    record Employee(String name, String dept, double salary) {}

    record Transaction(String id, String category, BigDecimal amount) {}

    static final List<Employee> EMPS = List.of(
            new Employee("Alice", "IT", 120000),
            new Employee("Bob", "IT", 95000),
            new Employee("Carol", "HR", 70000),
            new Employee("Dan", "HR", 82000),
            new Employee("Eve", "Sales", 60000),
            new Employee("Frank", "Sales", 99000),
            new Employee("Grace", "IT", 110000));

    public static void main(String[] args) {
        List<Integer> nums = List.of(5, 3, 8, 3, 1, 8, 9, 2, 5, 7);
        List<String> words = List.of("apple", "bat", "cat", "banana", "apple", "dog", "cat", "apple");

        System.out.println("Q1  " + q1RemoveDuplicatesSortDesc(nums));
        System.out.println("Q2  " + q2OddSquares(nums));
        System.out.println("Q3  " + q3SecondAndThird(nums));
        System.out.println("Q4  " + q4PartitionEvenOdd(nums));
        System.out.println("Q5  " + q5HighestSalary(EMPS));
        System.out.println("Q6  " + q6GroupByLength(words));
        System.out.println("Q7  " + q7WordFrequency(words));
        System.out.println("Q8  " + q8Flatten(List.of(List.of(1, 2), List.of(3, 4), List.of(5))));
        System.out.println("Q9  " + q9TotalSalaryByDept(EMPS));
        System.out.println("Q10 " + q10Top3Paid(EMPS));
        System.out.println("Q11 " + q11CountByDept(EMPS));
        System.out.println("Q12 " + q12TotalPerCategory(List.of(
                new Transaction("t1", "Food", new BigDecimal("12.50")),
                new Transaction("t2", "Travel", new BigDecimal("300.00")),
                new Transaction("t3", "Food", new BigDecimal("7.25")),
                new Transaction("t4", "Bills", new BigDecimal("99.99")),
                new Transaction("t5", "Travel", new BigDecimal("45.00")))));
        System.out.println("Q13 " + q13AvgSalaryByDept(EMPS));
        System.out.println("Q14 " + q14HighestPaidByDept(EMPS));
        System.out.println("Q15 " + q15NamesCsv(EMPS));
        System.out.println("Q16 " + q16CommonElements(List.of(1, 2, 3, 4, 5, 5), List.of(4, 5, 6, 7, 5)));
        System.out.println("Q17 " + q17FlattenDistinct(List.of(List.of(1, 2, 3), List.of(3, 4), List.of(4, 5, 1))));
        System.out.print("Q18 ");
        q18PrintNamesParallel(EMPS);
        System.out.println();
        q19SpotTheBug();
        System.out.println("Q20 " + q20TotalSalaryFast(EMPS));
    }

    // Q1. Remove duplicates & sort in descending order
    static List<Integer> q1RemoveDuplicatesSortDesc(List<Integer> nums) {
        return nums.stream()
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    // Q2. Filter odd numbers & square them
    static List<Integer> q2OddSquares(List<Integer> nums) {
        return nums.stream()
                .filter(n -> n % 2 != 0)      // n % 2 != 0 (not == 1) so negatives work
                .map(n -> n * n)
                .toList();
    }

    // Q3. Find the 2nd and 3rd elements (skip the 1st, take the next 2)
    //     Variant often asked: 2nd & 3rd *highest* distinct -> distinct().sorted(reverseOrder()).skip(1).limit(2)
    static List<Integer> q3SecondAndThird(List<Integer> nums) {
        return nums.stream()
                .skip(1)
                .limit(2)
                .toList();
    }

    // Q4. Partition a list into even and odd -> {false=[odds], true=[evens]}
    static Map<Boolean, List<Integer>> q4PartitionEvenOdd(List<Integer> nums) {
        return nums.stream()
                .collect(partitioningBy(n -> n % 2 == 0));
    }

    // Q5. Find the employee with the highest salary
    static Optional<Employee> q5HighestSalary(List<Employee> emps) {
        return emps.stream()
                .max(comparingDouble(Employee::salary));
    }

    // Q6. Group words by their length
    static Map<Integer, List<String>> q6GroupByLength(List<String> words) {
        return words.stream()
                .collect(groupingBy(String::length));
    }

    // Q7. Count word frequency in a list (LinkedHashMap keeps first-seen order)
    static Map<String, Long> q7WordFrequency(List<String> words) {
        return words.stream()
                .collect(groupingBy(Function.identity(), LinkedHashMap::new, counting()));
    }

    // Q8. Flatten a list of lists
    static <T> List<T> q8Flatten(List<List<T>> lists) {
        return lists.stream()
                .flatMap(List::stream)
                .toList();
    }

    // Q9. Find the total salary by department
    static Map<String, Double> q9TotalSalaryByDept(List<Employee> emps) {
        return emps.stream()
                .collect(groupingBy(Employee::dept, summingDouble(Employee::salary)));
    }

    // Q10. Get the top 3 highest-paid employees
    static List<String> q10Top3Paid(List<Employee> emps) {
        return emps.stream()
                .sorted(comparingDouble(Employee::salary).reversed())
                .limit(3)
                .map(Employee::name)
                .toList();
    }

    // Q11. Count how many employees are in each department
    static Map<String, Long> q11CountByDept(List<Employee> emps) {
        return emps.stream()
                .collect(groupingBy(Employee::dept, counting()));
    }

    // Q12. Find total transaction amount per category (BigDecimal for money -> reducing)
    static Map<String, BigDecimal> q12TotalPerCategory(List<Transaction> txns) {
        return txns.stream()
                .collect(groupingBy(Transaction::category,
                        reducing(BigDecimal.ZERO, Transaction::amount, BigDecimal::add)));
    }

    // Q13. Find the average salary of employees in each department
    static Map<String, Double> q13AvgSalaryByDept(List<Employee> emps) {
        return emps.stream()
                .collect(groupingBy(Employee::dept, averagingDouble(Employee::salary)));
    }

    // Q14. Find the highest paid employee in each department
    //      collectingAndThen unwraps the Optional (safe: a group is never empty)
    static Map<String, String> q14HighestPaidByDept(List<Employee> emps) {
        return emps.stream()
                .collect(groupingBy(Employee::dept,
                        collectingAndThen(maxBy(comparingDouble(Employee::salary)),
                                e -> e.get().name())));
    }

    // Q15. Convert a list of employee names into a comma-separated string
    static String q15NamesCsv(List<Employee> emps) {
        return emps.stream()
                .map(Employee::name)
                .collect(joining(", "));
    }

    // Q16. Find common elements between two lists (Set -> O(n+m) instead of O(n*m))
    static List<Integer> q16CommonElements(List<Integer> a, List<Integer> b) {
        Set<Integer> setB = new HashSet<>(b);
        return a.stream()
                .filter(setB::contains)
                .distinct()
                .toList();
    }

    // Q17. Flatten a list of lists & remove duplicates
    static <T> List<T> q17FlattenDistinct(List<List<T>> lists) {
        return lists.stream()
                .flatMap(List::stream)
                .distinct()
                .toList();
    }

    // Q18. Print all employee names as fast as possible (parallel streams)
    //      forEach on a parallel stream = NO order guarantee (fastest).
    //      forEachOrdered keeps encounter order but loses most of the parallel benefit.
    //      Note: for small lists / I/O-bound println, parallel is usually SLOWER (fork-join overhead,
    //      System.out is synchronized). Parallel pays off for large, CPU-bound work.
    static void q18PrintNamesParallel(List<Employee> emps) {
        emps.parallelStream()
                .map(Employee::name)
                .forEach(n -> System.out.print(n + " "));
    }

    // Q19. Spot the bug — what is wrong with this code?
    //
    //      List<String> names = new ArrayList<>();
    //      emps.parallelStream().map(Employee::name).forEach(names::add);   // BUG
    //
    //      BUG: side effect on a non-thread-safe ArrayList from multiple threads ->
    //           lost elements, nulls, or ArrayIndexOutOfBoundsException. Result size is non-deterministic.
    //      FIX: let the stream build the collection: .collect(toList()) / .toList()
    //
    //      Other classic "spot the bug" answers:
    //        - reusing a stream:   s.forEach(..); s.count();   -> IllegalStateException (stream already operated upon)
    //        - no terminal op:     list.stream().peek(System.out::println);  -> nothing runs (streams are lazy)
    //        - toMap with duplicate keys -> IllegalStateException; pass a merge fn: toMap(k, v, (a, b) -> a)
    //        - modifying the source list inside forEach -> ConcurrentModificationException
    static void q19SpotTheBug() {
        List<Integer> source = Stream.iterate(0, i -> i + 1).limit(100_000).toList();

        List<Integer> buggy = new ArrayList<>();
        try {
            source.parallelStream().forEach(buggy::add);
            System.out.println("Q19 buggy size = " + buggy.size() + " (expected 100000)");
        } catch (RuntimeException e) {
            System.out.println("Q19 buggy threw " + e.getClass().getSimpleName());
        }

        List<Integer> fixed = source.parallelStream().collect(Collectors.toList());
        System.out.println("Q19 fixed size = " + fixed.size());
    }

    // Q20. Compute total salary of all employees as fast as possible
    //      mapToDouble -> primitive DoubleStream: no boxing, sum() is built in.
    //      parallel() helps only for large lists (rule of thumb: N * cost-per-element > ~10k ops).
    static double q20TotalSalaryFast(List<Employee> emps) {
        return emps.parallelStream()
                .mapToDouble(Employee::salary)
                .sum();
    }
}
