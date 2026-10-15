package design;

import java.util.*;

/**
 * Must-know OOP/design snippets:
 *  1. Thread-safe Singleton (enum + holder idiom)
 *  2. Immutable class
 *  3. equals/hashCode contract (why HashMap "loses" keys)
 *  4. Comparable vs Comparator
 */
public class DesignBasics {

    // 1a. Enum singleton - simplest, thread-safe, serialization-safe
    enum Config {
        INSTANCE;
        String env = "prod";
    }

    // 1b. Lazy holder idiom - JVM class loading guarantees thread safety
    static class Registry {
        private Registry() {}
        private static class Holder { static final Registry INSTANCE = new Registry(); }
        static Registry getInstance() { return Holder.INSTANCE; }
    }

    // 2. Immutable class: final class, private final fields, no setters, defensive copies
    static final class Person {
        private final String name;
        private final List<String> skills;

        Person(String name, List<String> skills) {
            this.name = name;
            this.skills = List.copyOf(skills);      // defensive copy in
        }

        String name() { return name; }
        List<String> skills() { return skills; }    // already unmodifiable

        Person withName(String newName) { return new Person(newName, skills); } // "setter" returns new object
    }
    // (Java 16+: a record is the short answer -> record Person(String name, List<String> skills) {})

    // 3. Key WITHOUT equals/hashCode vs record (has them)
    static class BadKey { final int id; BadKey(int id) { this.id = id; } }
    record GoodKey(int id) {}

    // 4. Comparable = natural order (inside class); Comparator = any order (outside)
    record Student(String name, int grade) implements Comparable<Student> {
        public int compareTo(Student o) { return name.compareTo(o.name); }
    }

    public static void main(String[] args) {
        System.out.println("1. " + (Config.INSTANCE == Config.INSTANCE) + " " + (Registry.getInstance() == Registry.getInstance()));

        List<String> skills = new ArrayList<>(List.of("java"));
        Person p = new Person("Ann", skills);
        skills.add("hacked");
        System.out.println("2. " + p.skills() + " " + p.withName("Bob").name() + " " + p.name());

        Map<Object, String> map = new HashMap<>();
        map.put(new BadKey(1), "bad");
        map.put(new GoodKey(1), "good");
        System.out.println("3. BadKey found? " + map.containsKey(new BadKey(1)) + ", GoodKey found? " + map.containsKey(new GoodKey(1)));

        List<Student> students = new ArrayList<>(List.of(new Student("Zed", 90), new Student("Amy", 85), new Student("Bob", 95)));
        Collections.sort(students);
        System.out.println("4. by name:  " + students);
        students.sort(Comparator.comparingInt(Student::grade).reversed());
        System.out.println("4. by grade: " + students);
    }
}
