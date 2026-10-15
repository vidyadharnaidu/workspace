package collections;

import java.util.*;
import java.util.function.Function;
import java.util.stream.*;

/**
 * Find / remove duplicates.
 *
 * TRICK 1: Set.add() returns FALSE if already present -> filter(x -> !seen.add(x))
 * TRICK 2: groupingBy + counting -> keep entries with count > 1
 * TRICK 3: remove duplicates -> distinct() (keeps order) or new LinkedHashSet<>(list)
 */
public class Duplicates {

    // [1,2,3,2,4,1,1] -> [2, 1]
    static Set<Integer> findDuplicates(List<Integer> list) {
        Set<Integer> seen = new HashSet<>();
        return list.stream().filter(x -> !seen.add(x)).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // [1,2,3,2,4,1,1] -> {1=3, 2=2}
    static Map<Integer, Long> duplicateCounts(List<Integer> list) {
        return list.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // [1,2,3,2,4,1,1] -> [1, 2, 3, 4]
    static List<Integer> removeDuplicates(List<Integer> list) {
        return list.stream().distinct().toList();
    }

    // "programming" -> [r, m, g]
    static Set<Character> duplicateChars(String s) {
        Set<Character> seen = new HashSet<>();
        return s.chars().mapToObj(c -> (char) c)
                .filter(c -> !seen.add(c))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // Contains duplicate? (LeetCode 217)
    static boolean containsDuplicate(int[] nums) {
        return Arrays.stream(nums).distinct().count() != nums.length;
    }

    // Array 1..n with exactly one duplicate -> sum trick: sum - n(n-1)/2
    static int singleDuplicate(int[] nums) {
        int n = nums.length;
        return Arrays.stream(nums).sum() - (n - 1) * n / 2;
    }

    // Remove duplicates in-place from a SORTED array, return new length (LeetCode 26)
    static int removeDuplicatesSorted(int[] nums) {
        if (nums.length == 0) return 0;
        int k = 1;
        for (int i = 1; i < nums.length; i++)
            if (nums[i] != nums[k - 1]) nums[k++] = nums[i];
        return k;
    }

    public static void main(String[] args) {
        List<Integer> list = List.of(1, 2, 3, 2, 4, 1, 1);
        System.out.println(findDuplicates(list));                          // [2, 1]
        System.out.println(duplicateCounts(list));                         // {1=3, 2=2}
        System.out.println(removeDuplicates(list));                        // [1, 2, 3, 4]
        System.out.println(duplicateChars("programming"));                 // [r, m, g]
        System.out.println(containsDuplicate(new int[]{1, 2, 3, 1}));      // true
        System.out.println(singleDuplicate(new int[]{1, 3, 4, 2, 2}));     // 2

        int[] sorted = {1, 1, 2, 2, 2, 3};
        int k = removeDuplicatesSorted(sorted);
        System.out.println(k + " " + Arrays.toString(Arrays.copyOf(sorted, k))); // 3 [1, 2, 3]
    }
}
