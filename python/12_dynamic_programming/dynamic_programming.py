"""
Dynamic programming.

Run: python python/12_dynamic_programming/dynamic_programming.py

4 STEPS FOR ANY DP:
  1. STATE:     what does dp[i] (or dp[i][j]) MEAN? Say it in words.
  2. CHOICE:    what are my options at step i -> the recurrence.
  3. BASE CASE: dp[0], empty string, amount 0...
  4. ANSWER:    dp[n]? max(dp)?

  TOP-DOWN  = recursion + @cache (write the brute force, add one decorator).
  BOTTOM-UP = fill a table with loops (no recursion limit).
  If dp[i] only needs dp[i-1] and dp[i-2], keep two variables instead of a list.

FAMILIES: 1D line (stairs, robber, LIS, word break) | unbounded coins | 0/1 knapsack (loop capacity backwards)
          two strings (LCS, edit distance -> (m+1) x (n+1) grid) | grid paths
"""
from functools import cache, lru_cache
from bisect import bisect_left


def check(label, actual, expected):
    assert actual == expected, f"{label}: expected {expected!r}, got {actual!r}"
    print(f"{label:<30} {actual!r}")


@cache                                    # this one decorator turns O(2^n) into O(n)
def fib(n):
    return n if n < 2 else fib(n - 1) + fib(n - 2)


def climb_stairs(n):
    a, b = 1, 1
    for _ in range(n - 1):
        a, b = b, a + b
    return b


def min_cost_climbing_stairs(cost):
    two_back = one_back = 0
    for i in range(2, len(cost) + 1):
        two_back, one_back = one_back, min(one_back + cost[i - 1], two_back + cost[i - 2])
    return one_back


def house_robber(houses):
    prev = cur = 0
    for money in houses:
        prev, cur = cur, max(cur, prev + money)     # skip this house, or rob it
    return cur


def coin_change(coins, amount):
    """Fewest coins; -1 if impossible."""
    dp = [0] + [float("inf")] * amount
    for a in range(1, amount + 1):
        dp[a] = min((dp[a - c] + 1 for c in coins if c <= a), default=float("inf"))
    return -1 if dp[amount] == float("inf") else dp[amount]


def coin_ways(coins, amount):
    """Number of combinations. COINS in the OUTER loop -> combinations, not permutations."""
    dp = [1] + [0] * amount
    for coin in coins:
        for a in range(coin, amount + 1):
            dp[a] += dp[a - coin]
    return dp[amount]


def longest_increasing_subsequence(nums):
    dp = [1] * len(nums)
    for i in range(len(nums)):
        for j in range(i):
            if nums[j] < nums[i]:
                dp[i] = max(dp[i], dp[j] + 1)
    return max(dp, default=0)


def lis_fast(nums):
    """O(n log n): tails[i] = smallest tail of an increasing subsequence of length i+1."""
    tails = []
    for n in nums:
        i = bisect_left(tails, n)
        if i == len(tails):
            tails.append(n)
        else:
            tails[i] = n
    return len(tails)


def longest_common_subsequence(a, b):
    dp = [[0] * (len(b) + 1) for _ in range(len(a) + 1)]
    for i in range(1, len(a) + 1):
        for j in range(1, len(b) + 1):
            dp[i][j] = dp[i - 1][j - 1] + 1 if a[i - 1] == b[j - 1] else max(dp[i - 1][j], dp[i][j - 1])
    return dp[-1][-1]


def edit_distance(a, b):
    @lru_cache(maxsize=None)
    def distance(i, j):
        if i == len(a):
            return len(b) - j                 # insert the rest
        if j == len(b):
            return len(a) - i                 # delete the rest
        if a[i] == b[j]:
            return distance(i + 1, j + 1)
        return 1 + min(distance(i + 1, j + 1),   # replace
                       distance(i + 1, j),       # delete
                       distance(i, j + 1))       # insert
    result = distance(0, 0)
    distance.cache_clear()
    return result


def knapsack(weights, values, capacity):
    """0/1 knapsack: each item once -> iterate capacity BACKWARDS."""
    dp = [0] * (capacity + 1)
    for weight, value in zip(weights, values):
        for c in range(capacity, weight - 1, -1):
            dp[c] = max(dp[c], dp[c - weight] + value)
    return dp[capacity]


def can_partition(nums):
    """Split into two equal-sum subsets -> boolean knapsack for sum/2."""
    total = sum(nums)
    if total % 2:
        return False
    target = total // 2
    dp = [True] + [False] * target
    for n in nums:
        for t in range(target, n - 1, -1):
            dp[t] = dp[t] or dp[t - n]
    return dp[target]


def unique_paths(m, n):
    ways = [1] * n
    for _ in range(1, m):
        for c in range(1, n):
            ways[c] += ways[c - 1]            # from the top + from the left
    return ways[-1]


def min_path_sum(grid):
    rows, cols = len(grid), len(grid[0])
    dp = [[0] * cols for _ in range(rows)]
    for r in range(rows):
        for c in range(cols):
            if r == c == 0:
                dp[r][c] = grid[r][c]
            elif r == 0:
                dp[r][c] = dp[r][c - 1] + grid[r][c]
            elif c == 0:
                dp[r][c] = dp[r - 1][c] + grid[r][c]
            else:
                dp[r][c] = min(dp[r - 1][c], dp[r][c - 1]) + grid[r][c]
    return dp[-1][-1]


def word_break(s, words):
    word_set = frozenset(words)

    @lru_cache(maxsize=None)
    def can_break(start):
        if start == len(s):
            return True
        return any(s[start:end] in word_set and can_break(end) for end in range(start + 1, len(s) + 1))

    result = can_break(0)
    can_break.cache_clear()
    return result


def num_decodings(s):
    """'226' -> 3 ways (A=1..Z=26)."""
    if not s or s[0] == "0":
        return 0
    two_back, one_back = 1, 1
    for i in range(1, len(s)):
        current = 0
        if s[i] != "0":
            current += one_back
        if 10 <= int(s[i - 1:i + 1]) <= 26:
            current += two_back
        two_back, one_back = one_back, current
    return one_back


def max_product_subarray(nums):
    """Track BOTH max and min: a negative number flips them."""
    best = high = low = nums[0]
    for n in nums[1:]:
        if n < 0:
            high, low = low, high
        high = max(n, high * n)
        low = min(n, low * n)
        best = max(best, high)
    return best


def longest_palindromic_subsequence(s):
    return longest_common_subsequence(s, s[::-1])     # LCS of the string and its reverse


if __name__ == "__main__":
    check("fib(100) with @cache", fib(100), 354224848179261915075)
    check("climb_stairs(5)", climb_stairs(5), 8)
    check("min_cost_climbing_stairs", min_cost_climbing_stairs([10, 15, 20]), 15)
    check("house_robber", house_robber([2, 7, 9, 3, 1]), 12)
    check("coin_change", (coin_change([1, 2, 5], 11), coin_change([2], 3)), (3, -1))
    check("coin_ways", coin_ways([1, 2, 5], 5), 4)
    check("longest_increasing_subseq", longest_increasing_subsequence([10, 9, 2, 5, 3, 7, 101, 18]), 4)
    check("lis_fast", lis_fast([10, 9, 2, 5, 3, 7, 101, 18]), 4)
    check("longest_common_subsequence", longest_common_subsequence("abcde", "ace"), 3)
    check("edit_distance", edit_distance("horse", "ros"), 3)
    check("knapsack", knapsack([1, 3, 4, 5], [1, 4, 5, 7], 7), 9)
    check("can_partition", (can_partition([1, 5, 11, 5]), can_partition([1, 2, 3, 5])), (True, False))
    check("unique_paths(3, 7)", unique_paths(3, 7), 28)
    check("min_path_sum", min_path_sum([[1, 3, 1], [1, 5, 1], [4, 2, 1]]), 7)
    check("word_break", (word_break("leetcode", ["leet", "code"]),
                         word_break("catsandog", ["cats", "dog", "sand", "and", "cat"])), (True, False))
    check("num_decodings", (num_decodings("226"), num_decodings("06")), (3, 0))
    check("max_product_subarray", max_product_subarray([2, 3, -2, 4]), 6)
    check("longest_palindromic_subseq", longest_palindromic_subsequence("bbbab"), 4)

    print("\nall checks passed")
