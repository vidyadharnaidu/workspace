package collections;

import java.util.*;
import java.util.function.BinaryOperator;

import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.*;

/**
 * THE most asked Java 8 streams question: Employee queries.
 *
 * TRICK: "by department" = groupingBy(Employee::dept, <downstream collector>)
 *        highest  -> maxBy(comparing...)            (returns Optional)
 *        average  -> averagingDouble(...)
 *        count    -> counting()
 *        names    -> mapping(Employee::name, toList())
 *        Nth highest -> distinct salaries, sort DESC, skip(n-1), findFirst
 */
public class EmployeeQueries {

    record Employee(String name, String dept, double salary, int age, String gender) {}

    public static void main(String[] args) {
        List<Employee> emps = List.of(
                new Employee("Alice", "IT", 120000, 30, "F"),
                new Employee("Bob", "IT", 95000, 45, "M"),
                new Employee("Carol", "HR", 70000, 28, "F"),
                new Employee("Dan", "HR", 82000, 35, "M"),
                new Employee("Eve", "Sales", 60000, 25, "F"),
                new Employee("Frank", "Sales", 99000, 50, "M"),
                new Employee("Grace", "IT", 120000, 38, "F"));

        // 1. Highest paid employee per department
        Map<String, Optional<Employee>> highestByDept = emps.stream()
                .collect(groupingBy(Employee::dept, maxBy(comparingDouble(Employee::salary))));
        highestByDept.forEach((d, e) -> System.out.println("1. " + d + " -> " + e.map(Employee::name).orElse("-")));

        // 1b. Same, without Optional (collectingAndThen unwraps it)
        Map<String, String> highestName = emps.stream()
                .collect(groupingBy(Employee::dept, TreeMap::new,
                        collectingAndThen(maxBy(comparingDouble(Employee::salary)), o -> o.get().name())));
        System.out.println("1b. " + highestName);

        // 1c. Same, with toMap + BinaryOperator.maxBy (very easy to remember)
        Map<String, Employee> viaToMap = emps.stream()
                .collect(toMap(Employee::dept, e -> e, BinaryOperator.maxBy(comparingDouble(Employee::salary))));
        System.out.println("1c. " + viaToMap.values().stream().map(Employee::name).toList());

        // 2. Highest paid overall
        emps.stream().max(comparingDouble(Employee::salary)).ifPresent(e -> System.out.println("2. " + e));

        // 3. Second (Nth) highest DISTINCT salary
        int n = 2;
        emps.stream().map(Employee::salary).distinct()
                .sorted(Comparator.reverseOrder())
                .skip(n - 1).findFirst()
                .ifPresent(s -> System.out.println("3. 2nd highest salary = " + s));

        // 4. Average salary per department
        System.out.println("4. " + emps.stream().collect(groupingBy(Employee::dept, averagingDouble(Employee::salary))));

        // 5. Count employees per department
        System.out.println("5. " + emps.stream().collect(groupingBy(Employee::dept, counting())));

        // 6. Names per department
        System.out.println("6. " + emps.stream().collect(groupingBy(Employee::dept, mapping(Employee::name, toList()))));

        // 7. Total salary per department
        System.out.println("7. " + emps.stream().collect(groupingBy(Employee::dept, summingDouble(Employee::salary))));

        // 8. Partition: salary > 90k
        System.out.println("8. " + emps.stream().collect(partitioningBy(e -> e.salary() > 90000, mapping(Employee::name, toList()))));

        // 9. Sort by salary DESC, then name ASC
        System.out.println("9. " + emps.stream()
                .sorted(comparingDouble(Employee::salary).reversed().thenComparing(Employee::name))
                .map(Employee::name).toList());

        // 10. Top 3 earners
        System.out.println("10. " + emps.stream()
                .sorted(comparingDouble(Employee::salary).reversed())
                .limit(3).map(Employee::name).toList());

        // 11. Department with most employees
        emps.stream().collect(groupingBy(Employee::dept, counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue())
                .ifPresent(e -> System.out.println("11. " + e.getKey()));

        // 12. Salary statistics (min, max, avg, sum, count in one go)
        DoubleSummaryStatistics stats = emps.stream().mapToDouble(Employee::salary).summaryStatistics();
        System.out.println("12. " + stats);

        // 13. Count by gender
        System.out.println("13. " + emps.stream().collect(groupingBy(Employee::gender, counting())));

        // 14. Youngest employee per department
        System.out.println("14. " + emps.stream().collect(groupingBy(Employee::dept,
                collectingAndThen(minBy(Comparator.comparingInt(Employee::age)), o -> o.get().name()))));

        // 15. Give IT a 10% raise (records are immutable -> map to new objects)
        System.out.println("15. " + emps.stream()
                .map(e -> e.dept().equals("IT") ? new Employee(e.name(), e.dept(), e.salary() * 1.1, e.age(), e.gender()) : e)
                .filter(e -> e.dept().equals("IT")).map(e -> e.name() + "=" + e.salary()).toList());

        // 16. Comma separated names
        System.out.println("16. " + emps.stream().map(Employee::name).collect(joining(", ", "[", "]")));
    }

}
