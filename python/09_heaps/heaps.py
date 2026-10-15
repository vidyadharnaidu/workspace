"""
Heap / priority queue problems (heapq).

Run: python python/09_heaps/heaps.py

REMEMBER:
  * heapq is a MIN-heap of a plain list. Max-heap: push -value (or tuples with negated keys).
  * "K largest"  -> min-heap of size k (pop when len > k); "K smallest/closest" -> max-heap of size k.
  * Tuples are compared element by element: push (priority, tiebreaker, item) so items never need __lt__.
  * Running median -> two heaps (max-heap of the low half, min-heap of the high half).
  * heapq.nlargest/nsmallest for one-off queries; heapq.merge to merge sorted iterables lazily.
"""
import heapq
from collections import Counter


def check(label, actual, expected):
    assert actual == expected, f"{label}: expected {expected!r}, got {actual!r}"
    print(f"{label:<28} {actual!r}")


def kth_largest(nums, k):
    heap = []
    for n in nums:
        heapq.heappush(heap, n)
        if len(heap) > k:
            heapq.heappop(heap)          # drop the smallest; the top is the kth largest
    return heap[0]


class KthLargestStream:
    def __init__(self, k, nums=()):
        self.k = k
        self.heap = list(nums)
        heapq.heapify(self.heap)
        while len(self.heap) > k:
            heapq.heappop(self.heap)

    def add(self, value):
        heapq.heappush(self.heap, value)
        if len(self.heap) > self.k:
            heapq.heappop(self.heap)
        return self.heap[0]


def top_k_frequent(nums, k):
    counts = Counter(nums)
    return [n for n, _ in heapq.nlargest(k, counts.items(), key=lambda kv: kv[1])]


def k_closest_points(points, k):
    return heapq.nsmallest(k, points, key=lambda p: p[0] ** 2 + p[1] ** 2)


def merge_k_sorted(lists):
    return list(heapq.merge(*lists))     # lazy, O(total log k)


def kth_smallest_in_matrix(matrix, k):
    heap = [(row[0], r, 0) for r, row in enumerate(matrix)]
    heapq.heapify(heap)
    value = None
    for _ in range(k):
        value, r, c = heapq.heappop(heap)
        if c + 1 < len(matrix[0]):
            heapq.heappush(heap, (matrix[r][c + 1], r, c + 1))
    return value


def min_meeting_rooms(meetings):
    ends = []                            # min-heap of end times
    for start, end in sorted(meetings):
        if ends and ends[0] <= start:
            heapq.heappop(ends)          # a room freed up, reuse it
        heapq.heappush(ends, end)
    return len(ends)


def last_stone_weight(stones):
    heap = [-s for s in stones]          # max-heap via negation
    heapq.heapify(heap)
    while len(heap) > 1:
        first, second = -heapq.heappop(heap), -heapq.heappop(heap)
        if first != second:
            heapq.heappush(heap, -(first - second))
    return -heap[0] if heap else 0


def connect_ropes_cost(ropes):
    heap = list(ropes)
    heapq.heapify(heap)
    cost = 0
    while len(heap) > 1:
        joined = heapq.heappop(heap) + heapq.heappop(heap)   # always join the two shortest
        cost += joined
        heapq.heappush(heap, joined)
    return cost


def sort_k_sorted(nums, k):
    """Each element is at most k positions away from its sorted place."""
    heap, result = [], []
    for n in nums:
        heapq.heappush(heap, n)
        if len(heap) > k:
            result.append(heapq.heappop(heap))
    while heap:
        result.append(heapq.heappop(heap))
    return result


class MedianFinder:
    def __init__(self):
        self.low = []      # max-heap (negated): the smaller half
        self.high = []     # min-heap: the larger half

    def add(self, num):
        heapq.heappush(self.low, -num)
        heapq.heappush(self.high, -heapq.heappop(self.low))     # balance values
        if len(self.high) > len(self.low):
            heapq.heappush(self.low, -heapq.heappop(self.high))  # balance sizes
    def median(self):
        if len(self.low) > len(self.high):
            return float(-self.low[0])
        return (-self.low[0] + self.high[0]) / 2


def reorganize_string(s):
    """No two adjacent characters equal, or '' if impossible."""
    counts = Counter(s)
    heap = [(-count, ch) for ch, count in counts.items()]
    heapq.heapify(heap)
    result, held = [], None
    while heap:
        count, ch = heapq.heappop(heap)
        result.append(ch)
        if held:
            heapq.heappush(heap, held)       # put back the previous char one round later
        held = (count + 1, ch) if count + 1 < 0 else None
    return "".join(result) if len(result) == len(s) else ""


def task_scheduler(tasks, cooldown):
    counts = Counter(tasks)
    most = max(counts.values())
    with_most = sum(1 for c in counts.values() if c == most)
    return max(len(tasks), (most - 1) * (cooldown + 1) + with_most)


if __name__ == "__main__":
    check("kth_largest", kth_largest([3, 2, 1, 5, 6, 4], 2), 5)

    stream = KthLargestStream(3, [4, 5, 8, 2])
    check("KthLargestStream", [stream.add(v) for v in (3, 5, 10, 9, 4)], [4, 5, 5, 8, 8])

    check("top_k_frequent", top_k_frequent([1, 1, 1, 2, 2, 3], 2), [1, 2])
    check("k_closest_points", k_closest_points([(3, 3), (5, -1), (-2, 4)], 2), [(3, 3), (-2, 4)])
    check("merge_k_sorted", merge_k_sorted([[1, 4, 5], [1, 3, 4], [2, 6]]), [1, 1, 2, 3, 4, 4, 5, 6])
    check("kth_smallest_in_matrix", kth_smallest_in_matrix([[1, 5, 9], [10, 11, 13], [12, 13, 15]], 8), 13)
    check("min_meeting_rooms", min_meeting_rooms([(0, 30), (5, 10), (15, 20)]), 2)
    check("last_stone_weight", last_stone_weight([2, 7, 4, 1, 8, 1]), 1)
    check("connect_ropes_cost", connect_ropes_cost([4, 3, 2, 6]), 29)
    check("sort_k_sorted", sort_k_sorted([6, 5, 3, 2, 8, 10, 9], 3), [2, 3, 5, 6, 8, 9, 10])

    finder = MedianFinder()
    medians = []
    for value in (5, 15, 1, 3):
        finder.add(value)
        medians.append(finder.median())
    check("MedianFinder", medians, [5.0, 10.0, 5.0, 4.0])

    check("reorganize_string", (reorganize_string("aab"), reorganize_string("aaab")), ("aba", ""))
    check("task_scheduler", task_scheduler(["A", "A", "A", "B", "B", "B"], 2), 8)

    print("\nall checks passed")
