"""
Binary search, including "binary search on the answer" and the bisect module.

Run: python python/10_binary_search/binary_search.py

TWO TEMPLATES:
  A) EXACT:      while lo <= hi:  mid = (lo + hi) // 2 ... return -1 if not found
  B) FIRST TRUE: find the smallest x in [lo, hi) where ok(x) is True (False...False,True...True)
         while lo < hi:
             mid = (lo + hi) // 2
             if ok(mid): hi = mid        # mid might be the answer, keep it
             else:       lo = mid + 1    # mid is definitely not
         return lo
  "Binary search on the answer": ok(x) = "can we do it with x?" over the ANSWER range (speed, capacity...).
  Python has no int overflow, so (lo + hi) // 2 is safe. Use bisect for sorted lists.
"""
import bisect


def check(label, actual, expected):
    assert actual == expected, f"{label}: expected {expected!r}, got {actual!r}"
    print(f"{label:<28} {actual!r}")


def binary_search(nums, target):
    lo, hi = 0, len(nums) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if nums[mid] == target:
            return mid
        if nums[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1


def first_true(lo, hi, ok):
    """Smallest x in [lo, hi) with ok(x) True; returns hi if none."""
    while lo < hi:
        mid = (lo + hi) // 2
        if ok(mid):
            hi = mid
        else:
            lo = mid + 1
    return lo


def search_insert(nums, target):
    return first_true(0, len(nums), lambda i: nums[i] >= target)     # same as bisect.bisect_left


def search_range(nums, target):
    first = bisect.bisect_left(nums, target)
    if first == len(nums) or nums[first] != target:
        return [-1, -1]
    return [first, bisect.bisect_right(nums, target) - 1]


def count_occurrences(nums, target):
    return bisect.bisect_right(nums, target) - bisect.bisect_left(nums, target)


def search_rotated(nums, target):
    lo, hi = 0, len(nums) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if nums[mid] == target:
            return mid
        if nums[lo] <= nums[mid]:                  # left half is sorted
            if nums[lo] <= target < nums[mid]:
                hi = mid - 1
            else:
                lo = mid + 1
        else:                                      # right half is sorted
            if nums[mid] < target <= nums[hi]:
                lo = mid + 1
            else:
                hi = mid - 1
    return -1


def find_min_rotated(nums):
    lo, hi = 0, len(nums) - 1
    while lo < hi:
        mid = (lo + hi) // 2
        if nums[mid] > nums[hi]:                   # the minimum is to the right
            lo = mid + 1
        else:
            hi = mid
    return nums[lo]


def find_peak(nums):
    lo, hi = 0, len(nums) - 1
    while lo < hi:
        mid = (lo + hi) // 2
        if nums[mid] < nums[mid + 1]:              # going uphill -> a peak is to the right
            lo = mid + 1
        else:
            hi = mid
    return lo


def integer_sqrt(x):
    lo, hi = 0, x
    while lo <= hi:
        mid = (lo + hi) // 2
        if mid * mid <= x:
            lo = mid + 1
        else:
            hi = mid - 1
    return hi


def search_matrix(matrix, target):
    """Rows sorted, each row starts after the previous ends -> treat as one sorted list."""
    cols = len(matrix[0])
    lo, hi = 0, len(matrix) * cols - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        value = matrix[mid // cols][mid % cols]
        if value == target:
            return True
        if value < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return False


def search_matrix_ii(matrix, target):
    """Rows and columns sorted separately: start TOP-RIGHT, O(m + n)."""
    r, c = 0, len(matrix[0]) - 1
    while r < len(matrix) and c >= 0:
        if matrix[r][c] == target:
            return True
        if matrix[r][c] > target:
            c -= 1
        else:
            r += 1
    return False


def min_eating_speed(piles, hours):
    """Koko: smallest speed so all piles are eaten within `hours`."""
    def hours_needed(speed):
        return sum(-(-pile // speed) for pile in piles)      # ceil division
    return first_true(1, max(piles), lambda speed: hours_needed(speed) <= hours)


def ship_within_days(weights, days):
    def days_needed(capacity):
        used, load = 1, 0
        for w in weights:
            if load + w > capacity:
                used += 1
                load = 0
            load += w
        return used
    return first_true(max(weights), sum(weights), lambda cap: days_needed(cap) <= days)


def split_array_largest_sum(nums, k):
    return ship_within_days(nums, k)        # same problem: k parts = k days


def find_kth_missing_positive(nums, k):
    """Missing numbers before index i = nums[i] - (i + 1)."""
    return first_true(0, len(nums), lambda i: nums[i] - (i + 1) >= k) + k


def single_non_duplicate(nums):
    """Every element appears twice except one; pairs start at even indexes before it."""
    pairs = len(nums) // 2
    i = first_true(0, pairs, lambda p: nums[2 * p] != nums[2 * p + 1])
    return nums[2 * i]


if __name__ == "__main__":
    sorted_nums = [1, 3, 5, 7, 9, 11]
    check("binary_search", (binary_search(sorted_nums, 7), binary_search(sorted_nums, 4)), (3, -1))
    check("bisect_left / insort", (bisect.bisect_left(sorted_nums, 7), bisect.bisect_left(sorted_nums, 4)), (3, 2))
    check("search_insert", [search_insert([1, 3, 5, 6], t) for t in (5, 2, 7)], [2, 1, 4])
    check("search_range", (search_range([5, 7, 7, 8, 8, 10], 8), search_range([5, 7, 7, 8, 8, 10], 6)), ([3, 4], [-1, -1]))
    check("count_occurrences", count_occurrences([1, 2, 2, 2, 3], 2), 3)
    check("search_rotated", (search_rotated([4, 5, 6, 7, 0, 1, 2], 0), search_rotated([4, 5, 6, 7, 0, 1, 2], 3)), (4, -1))
    check("find_min_rotated", find_min_rotated([3, 4, 5, 1, 2]), 1)
    check("find_peak", find_peak([1, 2, 1, 3, 5, 6, 4]), 5)
    check("integer_sqrt", (integer_sqrt(8), integer_sqrt(16), integer_sqrt(2 ** 31 - 1)), (2, 4, 46340))

    matrix = [[1, 3, 5, 7], [10, 11, 16, 20], [23, 30, 34, 60]]
    check("search_matrix", (search_matrix(matrix, 3), search_matrix(matrix, 13)), (True, False))
    matrix2 = [[1, 4, 7, 11], [2, 5, 8, 12], [3, 6, 9, 16], [10, 13, 14, 17]]
    check("search_matrix_ii", (search_matrix_ii(matrix2, 5), search_matrix_ii(matrix2, 15)), (True, False))

    check("min_eating_speed", min_eating_speed([3, 6, 7, 11], 8), 4)
    check("ship_within_days", ship_within_days([1, 2, 3, 4, 5, 6, 7, 8, 9, 10], 5), 15)
    check("split_array_largest_sum", split_array_largest_sum([7, 2, 5, 10, 8], 2), 18)
    check("find_kth_missing_positive", find_kth_missing_positive([2, 3, 4, 7, 11], 5), 9)
    check("single_non_duplicate", single_non_duplicate([1, 1, 2, 3, 3, 4, 4, 8, 8]), 2)

    print("\nall checks passed")
