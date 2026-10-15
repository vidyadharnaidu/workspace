package binarysearch;

import java.util.*;
import java.util.function.IntPredicate;

/**
 * Binary search problems.
 *
 * TWO TEMPLATES TO MEMORIZE:
 *
 *  A) EXACT MATCH  - while (lo <= hi), return mid when found, else -1
 *
 *  B) FIRST TRUE   - the one template that solves almost everything else:
 *       "find the smallest x in [lo, hi) where ok(x) is true" (ok is false...false,true...true)
 *
 *       while (lo < hi) {
 *           mid = lo + (hi - lo) / 2;
 *           if (ok(mid)) hi = mid;    // mid might be the answer, keep it
 *           else lo = mid + 1;        // mid is definitely not
 *       }
 *       return lo;
 *
 *  "BINARY SEARCH ON THE ANSWER": when the question is "minimum speed / capacity / max-load such that
 *  it's possible", search over the ANSWER values with ok(x) = "can we do it with x?"
 *
 *  Always: mid = lo + (hi - lo) / 2  (avoids int overflow)
 *  Built-in: Arrays.binarySearch(arr, key) / Collections.binarySearch(list, key)
 *            -> index if found, else -(insertionPoint) - 1
 */
public class BinarySearch {

    // TEMPLATE A: exact match
    static int search(int[] a, int target) {
        int lo = 0, hi = a.length - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (a[mid] == target) return mid;
            if (a[mid] < target) lo = mid + 1;
            else hi = mid - 1;
        }
        return -1;
    }

    // TEMPLATE B: first index/value in [lo, hi) where ok is true (returns hi if none)
    static int firstTrue(int lo, int hi, IntPredicate ok) {
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (ok.test(mid)) hi = mid;
            else lo = mid + 1;
        }
        return lo;
    }

    // 1. LOWER BOUND / SEARCH INSERT POSITION: [1,3,5,6], 5 -> 2; 2 -> 1; 7 -> 4
    static int lowerBound(int[] a, int target) {
        return firstTrue(0, a.length, i -> a[i] >= target);
    }

    // 2. FIRST AND LAST POSITION: [5,7,7,8,8,10], 8 -> [3, 4]; 6 -> [-1, -1]
    // TRICK: first = lowerBound(target), last = lowerBound(target + 1) - 1
    static int[] searchRange(int[] a, int target) {
        int first = lowerBound(a, target);
        if (first == a.length || a[first] != target) return new int[]{-1, -1};
        return new int[]{first, lowerBound(a, target + 1) - 1};
    }

    // 3. COUNT OCCURRENCES in sorted array: [1,2,2,2,3], 2 -> 3
    static int countOccurrences(int[] a, int target) {
        return lowerBound(a, target + 1) - lowerBound(a, target);
    }

    // 4. SEARCH IN ROTATED SORTED ARRAY: [4,5,6,7,0,1,2], 0 -> 4
    // TRICK: one half is ALWAYS sorted. Check if target is inside the sorted half; else go to the other.
    static int searchRotated(int[] a, int target) {
        int lo = 0, hi = a.length - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (a[mid] == target) return mid;
            if (a[lo] <= a[mid]) { // left half sorted
                if (a[lo] <= target && target < a[mid]) hi = mid - 1;
                else lo = mid + 1;
            } else {               // right half sorted
                if (a[mid] < target && target <= a[hi]) lo = mid + 1;
                else hi = mid - 1;
            }
        }
        return -1;
    }

    // 5. MINIMUM IN ROTATED SORTED ARRAY: [3,4,5,1,2] -> 1
    // TRICK: compare mid with the RIGHT end. mid > right -> min is to the right.
    static int findMinRotated(int[] a) {
        int lo = 0, hi = a.length - 1;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (a[mid] > a[hi]) lo = mid + 1;
            else hi = mid;
        }
        return a[lo];
    }

    // 6. FIND PEAK ELEMENT (index): [1,2,1,3,5,6,4] -> 5
    // TRICK: if going uphill (a[mid] < a[mid+1]), a peak is to the right
    static int findPeak(int[] a) {
        int lo = 0, hi = a.length - 1;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (a[mid] < a[mid + 1]) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    // 7. INTEGER SQRT: 8 -> 2, 16 -> 4
    // TRICK: use long for mid*mid; answer ends up in hi
    static int mySqrt(int x) {
        long lo = 0, hi = x;
        while (lo <= hi) {
            long mid = (lo + hi) / 2;
            if (mid * mid <= x) lo = mid + 1;
            else hi = mid - 1;
        }
        return (int) hi;
    }

    // 8. SEARCH 2D MATRIX (rows sorted, each row starts after previous ends)
    // TRICK: pretend it's one sorted array of m*n; index i -> matrix[i / cols][i % cols]
    static boolean searchMatrix(int[][] m, int target) {
        int cols = m[0].length, lo = 0, hi = m.length * cols - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            int v = m[mid / cols][mid % cols];
            if (v == target) return true;
            if (v < target) lo = mid + 1;
            else hi = mid - 1;
        }
        return false;
    }

    // 8b. SEARCH 2D MATRIX II (rows and columns sorted separately)
    // TRICK: start TOP-RIGHT. Too big -> go left; too small -> go down. O(m + n)
    static boolean searchMatrixII(int[][] m, int target) {
        int r = 0, c = m[0].length - 1;
        while (r < m.length && c >= 0) {
            if (m[r][c] == target) return true;
            if (m[r][c] > target) c--;
            else r++;
        }
        return false;
    }

    // 9. KOKO EATING BANANAS: piles [3,6,7,11], h=8 -> 4
    // BINARY SEARCH ON ANSWER: smallest speed where hours needed <= h
    static int minEatingSpeed(int[] piles, int h) {
        int max = Arrays.stream(piles).max().getAsInt();
        return firstTrue(1, max, speed ->
                Arrays.stream(piles).mapToLong(p -> (p + speed - 1) / speed).sum() <= h); // ceil(p / speed)
    }

    // 10. SHIP PACKAGES WITHIN D DAYS: [1..10], 5 days -> 15
    // BINARY SEARCH ON ANSWER: capacity range [max weight, total weight]
    static int shipWithinDays(int[] weights, int days) {
        int lo = Arrays.stream(weights).max().getAsInt();
        int hi = Arrays.stream(weights).sum();
        return firstTrue(lo, hi, cap -> daysNeeded(weights, cap) <= days);
    }

    static int daysNeeded(int[] weights, int capacity) {
        int days = 1, load = 0;
        for (int w : weights) {
            if (load + w > capacity) { days++; load = 0; }
            load += w;
        }
        return days;
    }

    // 11. SPLIT ARRAY LARGEST SUM: [7,2,5,10,8], k=2 -> 18
    // TRICK: it's the SAME problem as shipping packages (k parts = k days)
    static int splitArray(int[] nums, int k) {
        return shipWithinDays(nums, k);
    }

    // 12. KTH MISSING POSITIVE: [2,3,4,7,11], k=5 -> 9
    // TRICK: numbers missing before index i = a[i] - (i + 1). Find first i where that >= k; answer = i + k
    static int findKthPositive(int[] a, int k) {
        return firstTrue(0, a.length, i -> a[i] - (i + 1) >= k) + k;
    }

    // 13. SINGLE ELEMENT IN SORTED ARRAY (others appear twice): [1,1,2,3,3,4,4,8,8] -> 2
    // TRICK: before the single, pairs start at EVEN index. Search even indexes only.
    static int singleNonDuplicate(int[] a) {
        int pairs = a.length / 2;
        int i = firstTrue(0, pairs, p -> a[2 * p] != a[2 * p + 1]);
        return a[2 * i];
    }

    public static void main(String[] args) {
        int[] sorted = {1, 3, 5, 7, 9, 11};
        System.out.println("search             " + search(sorted, 7) + " " + search(sorted, 4));           // 3 -1
        System.out.println("Arrays.binarySearch" + " " + Arrays.binarySearch(sorted, 7) + " " + Arrays.binarySearch(sorted, 4)); // 3 -3

        int[] ins = {1, 3, 5, 6};
        System.out.println("searchInsert       " + lowerBound(ins, 5) + " " + lowerBound(ins, 2) + " " + lowerBound(ins, 7)); // 2 1 4
        System.out.println("searchRange        " + Arrays.toString(searchRange(new int[]{5, 7, 7, 8, 8, 10}, 8))
                + " " + Arrays.toString(searchRange(new int[]{5, 7, 7, 8, 8, 10}, 6)));                  // [3, 4] [-1, -1]
        System.out.println("countOccurrences   " + countOccurrences(new int[]{1, 2, 2, 2, 3}, 2));        // 3
        System.out.println("searchRotated      " + searchRotated(new int[]{4, 5, 6, 7, 0, 1, 2}, 0)
                + " " + searchRotated(new int[]{4, 5, 6, 7, 0, 1, 2}, 3));                               // 4 -1
        System.out.println("findMinRotated     " + findMinRotated(new int[]{3, 4, 5, 1, 2}));             // 1
        System.out.println("findPeak           " + findPeak(new int[]{1, 2, 1, 3, 5, 6, 4}));             // 5
        System.out.println("mySqrt             " + mySqrt(8) + " " + mySqrt(16) + " " + mySqrt(Integer.MAX_VALUE)); // 2 4 46340

        int[][] matrix = {{1, 3, 5, 7}, {10, 11, 16, 20}, {23, 30, 34, 60}};
        System.out.println("searchMatrix       " + searchMatrix(matrix, 3) + " " + searchMatrix(matrix, 13)); // true false
        int[][] matrix2 = {{1, 4, 7, 11}, {2, 5, 8, 12}, {3, 6, 9, 16}, {10, 13, 14, 17}};
        System.out.println("searchMatrixII     " + searchMatrixII(matrix2, 5) + " " + searchMatrixII(matrix2, 15)); // true false

        System.out.println("kokoBananas        " + minEatingSpeed(new int[]{3, 6, 7, 11}, 8));            // 4
        System.out.println("shipWithinDays     " + shipWithinDays(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, 5)); // 15
        System.out.println("splitArray         " + splitArray(new int[]{7, 2, 5, 10, 8}, 2));             // 18
        System.out.println("kthMissing         " + findKthPositive(new int[]{2, 3, 4, 7, 11}, 5));        // 9
        System.out.println("singleNonDuplicate " + singleNonDuplicate(new int[]{1, 1, 2, 3, 3, 4, 4, 8, 8})); // 2
    }
}
