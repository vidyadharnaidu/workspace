"""
Two pointers and sliding window.

Run: python python/04_two_pointers_sliding_window/two_pointers_sliding_window.py

TEMPLATES:
  two pointers (sorted array):      left, right = 0, len(a)-1;  move the side that helps
  variable window:                  for right in range(n): add a[right]
                                        while invalid: remove a[left]; left += 1
                                        best = max(best, right - left + 1)
  fixed window of size k:           add a[i]; if i >= k: remove a[i-k]
  negatives break sliding windows -> use prefix sums instead.
"""
from collections import Counter, defaultdict, deque


def check(label, actual, expected):
    assert actual == expected, f"{label}: expected {expected!r}, got {actual!r}"
    print(f"{label:<30} {actual!r}")


def two_sum_sorted(nums, target):
    left, right = 0, len(nums) - 1
    while left < right:
        total = nums[left] + nums[right]
        if total == target:
            return [left, right]
        if total < target:
            left += 1                     # need a bigger sum
        else:
            right -= 1
    return []


def three_sum(nums):
    nums, result = sorted(nums), []
    for i, first in enumerate(nums):
        if i and first == nums[i - 1]:
            continue                      # skip duplicate anchors
        left, right = i + 1, len(nums) - 1
        while left < right:
            total = first + nums[left] + nums[right]
            if total < 0:
                left += 1
            elif total > 0:
                right -= 1
            else:
                result.append([first, nums[left], nums[right]])
                left += 1
                while left < right and nums[left] == nums[left - 1]:
                    left += 1
                right -= 1
    return result


def container_with_most_water(heights):
    left, right, best = 0, len(heights) - 1, 0
    while left < right:
        best = max(best, (right - left) * min(heights[left], heights[right]))
        if heights[left] < heights[right]:    # move the shorter side; it's the limit
            left += 1
        else:
            right -= 1
    return best


def trap_rain_water(heights):
    left, right = 0, len(heights) - 1
    left_max = right_max = water = 0
    while left < right:
        if heights[left] < heights[right]:
            left_max = max(left_max, heights[left])
            water += left_max - heights[left]
            left += 1
        else:
            right_max = max(right_max, heights[right])
            water += right_max - heights[right]
            right -= 1
    return water


def remove_duplicates_sorted(nums):       # in place, returns new length
    if not nums:
        return 0
    write = 1
    for read in range(1, len(nums)):
        if nums[read] != nums[write - 1]:
            nums[write] = nums[read]
            write += 1
    return write


def max_sum_of_k(nums, k):
    window = sum(nums[:k])
    best = window
    for i in range(k, len(nums)):
        window += nums[i] - nums[i - k]   # add the new, drop the one leaving
        best = max(best, window)
    return best


def min_subarray_len(target, nums):
    left = total = 0
    best = float("inf")
    for right, n in enumerate(nums):
        total += n
        while total >= target:            # shrink while still valid -> minimum
            best = min(best, right - left + 1)
            total -= nums[left]
            left += 1
    return 0 if best == float("inf") else best


def longest_k_distinct(s, k):
    counts, left, best = defaultdict(int), 0, 0
    for right, ch in enumerate(s):
        counts[ch] += 1
        while len(counts) > k:
            counts[s[left]] -= 1
            if counts[s[left]] == 0:
                del counts[s[left]]
            left += 1
        best = max(best, right - left + 1)
    return best


def character_replacement(s, k):
    counts, left, max_freq, best = defaultdict(int), 0, 0, 0
    for right, ch in enumerate(s):
        counts[ch] += 1
        max_freq = max(max_freq, counts[ch])
        while (right - left + 1) - max_freq > k:    # too many chars to replace
            counts[s[left]] -= 1
            left += 1
        best = max(best, right - left + 1)
    return best


def longest_ones(nums, k):                # max consecutive 1s if you may flip k zeros
    left = zeros = best = 0
    for right, n in enumerate(nums):
        zeros += n == 0
        while zeros > k:
            zeros -= nums[left] == 0
            left += 1
        best = max(best, right - left + 1)
    return best


def find_anagrams(s, p):
    need, window, result = Counter(p), Counter(), []
    for i, ch in enumerate(s):
        window[ch] += 1
        if i >= len(p):
            left = s[i - len(p)]
            window[left] -= 1
            if window[left] == 0:
                del window[left]
        if window == need:
            result.append(i - len(p) + 1)
    return result


def min_window(s, t):
    need = Counter(t)
    missing = len(t)
    best, start, left = float("inf"), 0, 0
    for right, ch in enumerate(s):
        if need[ch] > 0:
            missing -= 1
        need[ch] -= 1
        while missing == 0:               # valid -> shrink from the left
            if right - left + 1 < best:
                best, start = right - left + 1, left
            need[s[left]] += 1
            if need[s[left]] > 0:
                missing += 1
            left += 1
    return "" if best == float("inf") else s[start:start + best]


def max_sliding_window(nums, k):
    dq, result = deque(), []              # deque of INDEXES with decreasing values
    for i, n in enumerate(nums):
        if dq and dq[0] <= i - k:
            dq.popleft()                  # drop the index that left the window
        while dq and nums[dq[-1]] < n:
            dq.pop()                      # drop smaller values; they can never be the max
        dq.append(i)
        if i >= k - 1:
            result.append(nums[dq[0]])
    return result


if __name__ == "__main__":
    check("two_sum_sorted", two_sum_sorted([2, 7, 11, 15], 18), [1, 2])
    check("three_sum", three_sum([-1, 0, 1, 2, -1, -4]), [[-1, -1, 2], [-1, 0, 1]])
    check("container_with_most_water", container_with_most_water([1, 8, 6, 2, 5, 4, 8, 3, 7]), 49)
    check("trap_rain_water", trap_rain_water([0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1]), 6)

    nums = [1, 1, 2, 2, 3]
    length = remove_duplicates_sorted(nums)
    check("remove_duplicates_sorted", (length, nums[:length]), (3, [1, 2, 3]))

    check("max_sum_of_k", max_sum_of_k([2, 1, 5, 1, 3, 2], 3), 9)
    check("min_subarray_len", min_subarray_len(7, [2, 3, 1, 2, 4, 3]), 2)
    check("longest_k_distinct", longest_k_distinct("eceba", 2), 3)
    check("character_replacement", character_replacement("AABABBA", 1), 4)
    check("longest_ones", longest_ones([1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0], 2), 6)
    check("find_anagrams", find_anagrams("cbaebabacd", "abc"), [0, 6])
    check("min_window", min_window("ADOBECODEBANC", "ABC"), "BANC")
    check("max_sliding_window", max_sliding_window([1, 3, -1, -3, 5, 3, 6, 7], 3), [3, 3, 5, 5, 6, 7])

    print("\nall checks passed")
