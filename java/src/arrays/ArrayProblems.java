package arrays;

import java.util.*;
import java.util.stream.*;

/**
 * Top array questions with a one-line TRICK each.
 */
public class ArrayProblems {

    // TWO SUM: [2,7,11,15], 9 -> [0,1]
    // TRICK: HashMap value->index. For each x, look for (target - x).
    static int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> seen = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            Integer j = seen.get(target - nums[i]);
            if (j != null) return new int[]{j, i};
            seen.put(nums[i], i);
        }
        return new int[0];
    }

    // SECOND HIGHEST: [5,9,3,9,7] -> 7
    // TRICK: distinct -> sort desc -> skip 1
    static OptionalInt secondHighest(int[] nums) {
        return Arrays.stream(nums).boxed().distinct()
                .sorted(Comparator.reverseOrder())
                .skip(1).mapToInt(Integer::intValue).findFirst();
    }

    // KTH LARGEST: [3,2,1,5,6,4], k=2 -> 5
    // TRICK: MIN-heap of size k; its top is the answer.
    static int kthLargest(int[] nums, int k) {
        PriorityQueue<Integer> heap = new PriorityQueue<>();
        for (int x : nums) {
            heap.offer(x);
            if (heap.size() > k) heap.poll();
        }
        return heap.peek();
    }

    // MISSING NUMBER in 0..n: [3,0,1] -> 2
    // TRICK: expected sum n(n+1)/2 minus actual sum
    static int missingNumber(int[] nums) {
        int n = nums.length;
        return n * (n + 1) / 2 - Arrays.stream(nums).sum();
    }

    // MAX SUBARRAY SUM (Kadane): [-2,1,-3,4,-1,2,1,-5,4] -> 6
    // TRICK: cur = max(x, cur + x)  -> "start fresh or extend"
    static int maxSubArray(int[] nums) {
        int cur = nums[0], best = nums[0];
        for (int i = 1; i < nums.length; i++) {
            cur = Math.max(nums[i], cur + nums[i]);
            best = Math.max(best, cur);
        }
        return best;
    }

    // BEST TIME TO BUY/SELL STOCK: [7,1,5,3,6,4] -> 5
    // TRICK: track min price so far; profit = price - min
    static int maxProfit(int[] prices) {
        int min = Integer.MAX_VALUE, best = 0;
        for (int p : prices) {
            min = Math.min(min, p);
            best = Math.max(best, p - min);
        }
        return best;
    }

    // MOVE ZEROS: [0,1,0,3,12] -> [1,3,12,0,0]
    // TRICK: write pointer for non-zeros, fill rest with 0
    static void moveZeros(int[] nums) {
        int w = 0;
        for (int x : nums) if (x != 0) nums[w++] = x;
        while (w < nums.length) nums[w++] = 0;
    }

    // ROTATE RIGHT by k: [1,2,3,4,5,6,7], 3 -> [5,6,7,1,2,3,4]
    // TRICK: reverse all, reverse first k, reverse rest
    static void rotate(int[] nums, int k) {
        k %= nums.length;
        reverse(nums, 0, nums.length - 1);
        reverse(nums, 0, k - 1);
        reverse(nums, k, nums.length - 1);
    }

    static void reverse(int[] a, int i, int j) {
        while (i < j) { int t = a[i]; a[i++] = a[j]; a[j--] = t; }
    }

    // MERGE INTERVALS: [[1,3],[2,6],[8,10],[15,18]] -> [[1,6],[8,10],[15,18]]
    // TRICK: sort by start; if overlaps last merged -> extend end, else add
    static List<int[]> mergeIntervals(int[][] intervals) {
        List<int[]> res = new ArrayList<>();
        Arrays.stream(intervals).sorted(Comparator.comparingInt(a -> a[0])).forEach(cur -> {
            if (!res.isEmpty() && res.get(res.size() - 1)[1] >= cur[0])
                res.get(res.size() - 1)[1] = Math.max(res.get(res.size() - 1)[1], cur[1]);
            else
                res.add(cur.clone());
        });
        return res;
    }

    // INTERSECTION of two arrays: [1,2,2,1], [2,2] -> [2]
    static List<Integer> intersection(int[] a, int[] b) {
        Set<Integer> setB = Arrays.stream(b).boxed().collect(Collectors.toSet());
        return Arrays.stream(a).boxed().distinct().filter(setB::contains).toList();
    }

    // PRODUCT EXCEPT SELF: [1,2,3,4] -> [24,12,8,6]
    // TRICK: left-products pass, then right-products pass
    static int[] productExceptSelf(int[] nums) {
        int n = nums.length;
        int[] res = new int[n];
        res[0] = 1;
        for (int i = 1; i < n; i++) res[i] = res[i - 1] * nums[i - 1];
        for (int i = n - 1, right = 1; i >= 0; i--) {
            res[i] *= right;
            right *= nums[i];
        }
        return res;
    }

    // BINARY SEARCH: sorted array, return index or -1
    // TRICK: lo<=hi, mid = lo + (hi-lo)/2
    static int binarySearch(int[] a, int target) {
        int lo = 0, hi = a.length - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (a[mid] == target) return mid;
            if (a[mid] < target) lo = mid + 1;
            else hi = mid - 1;
        }
        return -1;
    }

    public static void main(String[] args) {
        System.out.println("twoSum        " + Arrays.toString(twoSum(new int[]{2, 7, 11, 15}, 9)));
        System.out.println("secondHighest " + secondHighest(new int[]{5, 9, 3, 9, 7}).getAsInt());
        System.out.println("kthLargest    " + kthLargest(new int[]{3, 2, 1, 5, 6, 4}, 2));
        System.out.println("missingNumber " + missingNumber(new int[]{3, 0, 1}));
        System.out.println("maxSubArray   " + maxSubArray(new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4}));
        System.out.println("maxProfit     " + maxProfit(new int[]{7, 1, 5, 3, 6, 4}));

        int[] z = {0, 1, 0, 3, 12};
        moveZeros(z);
        System.out.println("moveZeros     " + Arrays.toString(z));

        int[] r = {1, 2, 3, 4, 5, 6, 7};
        rotate(r, 3);
        System.out.println("rotate        " + Arrays.toString(r));

        System.out.println("mergeIntervals " + mergeIntervals(new int[][]{{1, 3}, {2, 6}, {8, 10}, {15, 18}})
                .stream().map(Arrays::toString).collect(Collectors.joining(" ")));
        System.out.println("intersection  " + intersection(new int[]{1, 2, 2, 1}, new int[]{2, 2}));
        System.out.println("productExcept " + Arrays.toString(productExceptSelf(new int[]{1, 2, 3, 4})));
        System.out.println("binarySearch  " + binarySearch(new int[]{1, 3, 5, 7, 9, 11}, 7));
    }
}
