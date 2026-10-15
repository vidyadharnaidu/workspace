package arrays;

import java.util.*;
import java.util.stream.*;

/**
 * 3Sum (LeetCode 15): find all unique triplets that sum to 0.
 *
 * TRICK: SORT + fix one number + TWO POINTERS on the rest.
 *        sum < 0 -> left++ ; sum > 0 -> right-- ; sum == 0 -> add, move both.
 *        Skip duplicates for i, left and right to keep triplets unique.
 * Time O(n^2), Space O(1) extra (besides output).
 */
public class ThreeSum {

    // [-1, 0, 1, 2, -1, -4] -> [[-1, -1, 2], [-1, 0, 1]]
    static List<List<Integer>> threeSum(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        Arrays.sort(nums);

        for (int i = 0; i < nums.length - 2; i++) {
            if (i > 0 && nums[i] == nums[i - 1]) continue; // skip duplicate first number

            int left = i + 1, right = nums.length - 1;
            while (left < right) {
                int sum = nums[i] + nums[left] + nums[right];
                if (sum < 0) {
                    left++;
                } else if (sum > 0) {
                    right--;
                } else {
                    result.add(Arrays.asList(nums[i], nums[left], nums[right]));
                    while (left < right && nums[left] == nums[left + 1]) left++;   // skip duplicates
                    while (left < right && nums[right] == nums[right - 1]) right--;
                    left++;
                    right--;
                }
            }
        }
        return result;
    }

    // STREAM version (easy to remember, O(n^3)): try every i < j < k,
    // keep sum == 0, sort each triplet, distinct() removes duplicates.
    static List<List<Integer>> threeSumStream(int[] nums) {
        int n = nums.length;
        return IntStream.range(0, n).boxed()
                .flatMap(i -> IntStream.range(i + 1, n).boxed()
                        .flatMap(j -> IntStream.range(j + 1, n)
                                .filter(k -> nums[i] + nums[j] + nums[k] == 0)
                                .mapToObj(k -> Stream.of(nums[i], nums[j], nums[k]).sorted().toList())))
                .distinct()
                .toList();
    }

    public static void main(String[] args) {
        System.out.println(threeSumStream(new int[]{-1, 0, 1, 2, -1, -4})); // [[-1, 0, 1], [-1, -1, 2]]

        System.out.println(threeSum(new int[]{-1, 0, 1, 2, -1, -4})); // [[-1, -1, 2], [-1, 0, 1]]
        System.out.println(threeSum(new int[]{0, 0, 0, 0}));          // [[0, 0, 0]]
        System.out.println(threeSum(new int[]{1, 2, 3}));             // []
    }
}
