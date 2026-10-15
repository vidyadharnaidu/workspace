"""
Sorting: the algorithms interviewers ask you to implement, and the sorting Python gives you for free.

Run: python python/14_sorting/sorting.py

REMEMBER:
  * sorted() / list.sort() use Timsort: O(n log n), STABLE (equal items keep their original order).
  * Sort by several keys: key=lambda x: (x.dept, -x.salary). For a stable multi-pass, sort by the LAST key first.
  * Quicksort O(n log n) average / O(n^2) worst, in place. Mergesort O(n log n) always, needs O(n) space, stable.
  * "kth largest" doesn't need a full sort: quickselect O(n) average, or a heap.
"""
import random
from functools import cmp_to_key


def check(label, actual, expected):
    assert actual == expected, f"{label}: expected {expected!r}, got {actual!r}"
    print(f"{label:<30} {actual!r}")


def bubble_sort(nums):
    nums = list(nums)
    for i in range(len(nums) - 1):
        swapped = False
        for j in range(len(nums) - 1 - i):        # the largest bubbles to the end each pass
            if nums[j] > nums[j + 1]:
                nums[j], nums[j + 1] = nums[j + 1], nums[j]
                swapped = True
        if not swapped:                            # already sorted -> O(n) best case
            break
    return nums


def insertion_sort(nums):
    nums = list(nums)
    for i in range(1, len(nums)):
        current, j = nums[i], i - 1
        while j >= 0 and nums[j] > current:        # shift bigger items right
            nums[j + 1] = nums[j]
            j -= 1
        nums[j + 1] = current
    return nums


def selection_sort(nums):
    nums = list(nums)
    for i in range(len(nums)):
        smallest = min(range(i, len(nums)), key=nums.__getitem__)
        nums[i], nums[smallest] = nums[smallest], nums[i]
    return nums


def merge_sort(nums):
    if len(nums) <= 1:
        return list(nums)
    mid = len(nums) // 2
    left, right = merge_sort(nums[:mid]), merge_sort(nums[mid:])
    return merge(left, right)


def merge(left, right):
    result, i, j = [], 0, 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:                    # <= keeps it STABLE
            result.append(left[i])
            i += 1
        else:
            result.append(right[j])
            j += 1
    return result + left[i:] + right[j:]


def quick_sort(nums):
    """Readable version (extra memory). Interviewers accept this, then ask for in-place."""
    if len(nums) <= 1:
        return list(nums)
    pivot = nums[len(nums) // 2]
    smaller = [n for n in nums if n < pivot]
    equal = [n for n in nums if n == pivot]
    larger = [n for n in nums if n > pivot]
    return quick_sort(smaller) + equal + quick_sort(larger)


def quick_sort_in_place(nums, lo=0, hi=None):
    nums = list(nums) if hi is None else nums
    hi = len(nums) - 1 if hi is None else hi
    if lo < hi:
        p = partition(nums, lo, hi)
        quick_sort_in_place(nums, lo, p - 1)
        quick_sort_in_place(nums, p + 1, hi)
    return nums


def partition(nums, lo, hi):
    """Lomuto: pivot = last element; everything smaller goes left."""
    pivot, i = nums[hi], lo
    for j in range(lo, hi):
        if nums[j] < pivot:
            nums[i], nums[j] = nums[j], nums[i]
            i += 1
    nums[i], nums[hi] = nums[hi], nums[i]
    return i


def counting_sort(nums):
    """O(n + k) for small integer ranges."""
    if not nums:
        return []
    low, high = min(nums), max(nums)
    counts = [0] * (high - low + 1)
    for n in nums:
        counts[n - low] += 1
    return [value + low for value, count in enumerate(counts) for _ in range(count)]


def quickselect(nums, k):
    """kth LARGEST, average O(n)."""
    nums = list(nums)
    target = len(nums) - k                        # index in ascending order
    lo, hi = 0, len(nums) - 1
    while True:
        p = partition(nums, lo, hi)
        if p == target:
            return nums[p]
        if p < target:
            lo = p + 1
        else:
            hi = p - 1


def sort_colors(nums):
    """Dutch national flag: sort 0s, 1s, 2s in one pass, no counting."""
    nums = list(nums)
    low, mid, high = 0, 0, len(nums) - 1
    while mid <= high:
        if nums[mid] == 0:
            nums[low], nums[mid] = nums[mid], nums[low]
            low, mid = low + 1, mid + 1
        elif nums[mid] == 2:
            nums[mid], nums[high] = nums[high], nums[mid]
            high -= 1                              # don't advance mid: the swapped-in value is unknown
        else:
            mid += 1
    return nums


def largest_number(nums):
    """Arrange numbers to form the largest number: custom comparator."""
    def compare(a, b):
        return (int(b + a) > int(a + b)) - (int(b + a) < int(a + b))
    return "".join(sorted(map(str, nums), key=cmp_to_key(compare))).lstrip("0") or "0"


def merge_sorted_lists(a, b):
    return merge(a, b)


if __name__ == "__main__":
    data = [5, 2, 9, 1, 5, 6, -3]
    expected = sorted(data)
    check("bubble_sort", bubble_sort(data), expected)
    check("insertion_sort", insertion_sort(data), expected)
    check("selection_sort", selection_sort(data), expected)
    check("merge_sort", merge_sort(data), expected)
    check("quick_sort", quick_sort(data), expected)
    check("quick_sort_in_place", quick_sort_in_place(data), expected)
    check("counting_sort", counting_sort([4, 2, 2, 8, 3, 3, 1]), [1, 2, 2, 3, 3, 4, 8])

    random_data = [random.Random(7).randint(0, 1000) for _ in range(200)]
    check("all sorts agree (200 items)",
          merge_sort(random_data) == quick_sort(random_data) == sorted(random_data), True)

    check("quickselect 2nd largest", quickselect([3, 2, 1, 5, 6, 4], 2), 5)
    check("sort_colors", sort_colors([2, 0, 2, 1, 1, 0]), [0, 0, 1, 1, 2, 2])
    check("largest_number", largest_number([3, 30, 34, 5, 9]), "9534330")
    check("merge_sorted_lists", merge_sorted_lists([1, 4, 7], [2, 3, 9]), [1, 2, 3, 4, 7, 9])

    # ---- sorting with Python's built-ins
    people = [("ann", "it", 120), ("bob", "hr", 95), ("cat", "it", 120), ("dan", "hr", 130)]
    check("sort by dept, salary desc", sorted(people, key=lambda p: (p[1], -p[2])),
          [("dan", "hr", 130), ("bob", "hr", 95), ("ann", "it", 120), ("cat", "it", 120)])
    check("stable: equal keys keep order", [p[0] for p in sorted(people, key=lambda p: p[2])],
          ["bob", "ann", "cat", "dan"])
    check("sort strings case-insensitively", sorted(["b", "A", "c"], key=str.lower), ["A", "b", "c"])
    check("sort dict by value desc", sorted({"a": 2, "b": 5, "c": 1}.items(), key=lambda kv: -kv[1]),
          [("b", 5), ("a", 2), ("c", 1)])
    check("sort with None last", sorted([3, None, 1], key=lambda x: (x is None, x)), [1, 3, None])

    print("\nall checks passed")
