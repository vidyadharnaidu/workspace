"""
Array / hashing interview problems.

Run: python python/03_arrays_hashing/array_problems.py

REMEMBER:
  * "Have I seen this before / how many times?" -> dict or Counter (O(1) lookups).
  * Kadane: cur = max(x, cur + x). Prefix sums answer "subarray sum = k".
  * set() lookups are O(1); `in list` is O(n) and turns loops into O(n^2).
"""
from collections import Counter, defaultdict


def check(label, actual, expected):
    assert actual == expected, f"{label}: expected {expected!r}, got {actual!r}"
    print(f"{label:<30} {actual!r}")


def two_sum(nums, target):
    seen = {}                                   # value -> index
    for i, n in enumerate(nums):
        if target - n in seen:
            return [seen[target - n], i]
        seen[n] = i
    return []


def contains_duplicate(nums):
    return len(set(nums)) != len(nums)


def find_duplicates(nums):
    return [n for n, count in Counter(nums).items() if count > 1]


def remove_duplicates_keep_order(items):
    return list(dict.fromkeys(items))


def missing_number(nums):
    n = len(nums)
    return n * (n + 1) // 2 - sum(nums)


def second_largest(nums):
    distinct = sorted(set(nums), reverse=True)
    return distinct[1] if len(distinct) > 1 else None


def move_zeros(nums):                           # in place, keeps order
    write = 0
    for i, n in enumerate(nums):
        if n != 0:
            nums[write], nums[i] = nums[i], nums[write]
            write += 1
    return nums


def max_subarray(nums):                         # Kadane
    best = cur = nums[0]
    for n in nums[1:]:
        cur = max(n, cur + n)                   # start fresh or extend
        best = max(best, cur)
    return best


def max_profit(prices):
    best, cheapest = 0, float("inf")
    for p in prices:
        cheapest = min(cheapest, p)
        best = max(best, p - cheapest)
    return best


def product_except_self(nums):
    result = [1] * len(nums)
    left = 1
    for i, n in enumerate(nums):                # products of everything to the left
        result[i], left = left, left * n
    right = 1
    for i in range(len(nums) - 1, -1, -1):      # multiply by everything to the right
        result[i] *= right
        right *= nums[i]
    return result


def rotate(nums, k):
    k %= len(nums)
    return nums[-k:] + nums[:-k] if k else nums[:]


def merge_intervals(intervals):
    merged = []
    for start, end in sorted(intervals):
        if merged and start <= merged[-1][1]:
            merged[-1][1] = max(merged[-1][1], end)
        else:
            merged.append([start, end])
    return merged


def top_k_frequent(nums, k):
    return [n for n, _ in Counter(nums).most_common(k)]


def majority_element(nums):                     # Boyer-Moore voting, O(1) memory
    count, candidate = 0, None
    for n in nums:
        if count == 0:
            candidate = n
        count += 1 if n == candidate else -1
    return candidate


def intersection(a, b):
    return sorted(set(a) & set(b))


def intersection_with_counts(a, b):
    return sorted((Counter(a) & Counter(b)).elements())


def longest_consecutive(nums):
    values, best = set(nums), 0
    for n in values:
        if n - 1 not in values:                 # only start counting at the beginning of a run
            length = 1
            while n + length in values:
                length += 1
            best = max(best, length)
    return best


def subarray_sum_equals_k(nums, k):
    prefix_counts, total, found = defaultdict(int, {0: 1}), 0, 0
    for n in nums:
        total += n
        found += prefix_counts[total - k]
        prefix_counts[total] += 1
    return found


def group_by_key(records, key):
    groups = defaultdict(list)
    for record in records:
        groups[record[key]].append(record)
    return dict(groups)


def chunk(items, size):
    return [items[i:i + size] for i in range(0, len(items), size)]


def flatten(nested):
    for item in nested:
        if isinstance(item, (list, tuple)):
            yield from flatten(item)
        else:
            yield item


def rotate_matrix(matrix):                      # 90 degrees clockwise
    return [list(row) for row in zip(*matrix[::-1])]


def spiral_order(matrix):
    result, m = [], [row[:] for row in matrix]
    while m:
        result += m.pop(0)                      # take the top row
        m = [list(row) for row in zip(*m)][::-1]  # rotate the rest counter-clockwise
    return result


if __name__ == "__main__":
    check("two_sum", two_sum([2, 7, 11, 15], 9), [0, 1])
    check("contains_duplicate", contains_duplicate([1, 2, 3, 1]), True)
    check("find_duplicates", find_duplicates([1, 2, 3, 2, 4, 1, 1]), [1, 2])
    check("remove_duplicates_keep_order", remove_duplicates_keep_order([3, 1, 3, 2, 1]), [3, 1, 2])
    check("missing_number", missing_number([3, 0, 1]), 2)
    check("second_largest", second_largest([5, 9, 3, 9, 7]), 7)
    check("move_zeros", move_zeros([0, 1, 0, 3, 12]), [1, 3, 12, 0, 0])
    check("max_subarray", max_subarray([-2, 1, -3, 4, -1, 2, 1, -5, 4]), 6)
    check("max_profit", max_profit([7, 1, 5, 3, 6, 4]), 5)
    check("product_except_self", product_except_self([1, 2, 3, 4]), [24, 12, 8, 6])
    check("rotate", rotate([1, 2, 3, 4, 5, 6, 7], 3), [5, 6, 7, 1, 2, 3, 4])
    check("merge_intervals", merge_intervals([[1, 3], [2, 6], [8, 10], [15, 18]]), [[1, 6], [8, 10], [15, 18]])
    check("top_k_frequent", top_k_frequent([1, 1, 1, 2, 2, 3], 2), [1, 2])
    check("majority_element", majority_element([2, 2, 1, 1, 1, 2, 2]), 2)
    check("intersection", intersection([1, 2, 2, 1], [2, 2, 3]), [2])
    check("intersection_with_counts", intersection_with_counts([1, 2, 2, 1], [2, 2, 3]), [2, 2])
    check("longest_consecutive", longest_consecutive([100, 4, 200, 1, 3, 2]), 4)
    check("subarray_sum_equals_k", subarray_sum_equals_k([1, 2, 3], 3), 2)
    check("chunk", chunk([1, 2, 3, 4, 5], 2), [[1, 2], [3, 4], [5]])
    check("flatten", list(flatten([1, [2, [3, (4, 5)]], 6])), [1, 2, 3, 4, 5, 6])

    people = [{"name": "ann", "dept": "it"}, {"name": "bob", "dept": "hr"}, {"name": "cat", "dept": "it"}]
    check("group_by_key", {k: [p["name"] for p in v] for k, v in group_by_key(people, "dept").items()},
          {"it": ["ann", "cat"], "hr": ["bob"]})

    check("rotate_matrix", rotate_matrix([[1, 2, 3], [4, 5, 6], [7, 8, 9]]), [[7, 4, 1], [8, 5, 2], [9, 6, 3]])
    check("spiral_order", spiral_order([[1, 2, 3], [4, 5, 6], [7, 8, 9]]), [1, 2, 3, 6, 9, 8, 7, 4, 5])

    print("\nall checks passed")
