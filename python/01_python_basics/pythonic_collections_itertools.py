"""
The standard-library toolbox that makes Python interview answers short.

Run: python python/01_python_basics/pythonic_collections_itertools.py

REMEMBER (what to reach for):
  counting            -> Counter           grouping         -> defaultdict(list)
  queue / sliding win -> deque             min/max heap     -> heapq (negate for max)
  sorted insert/search-> bisect            LRU order        -> OrderedDict / functools.lru_cache
  combinations        -> itertools         running totals   -> itertools.accumulate
  complexity: list append O(1), list.pop(0) O(n) (use deque), `x in list` O(n), `x in set/dict` O(1)
"""
import bisect
import heapq
import itertools
import operator
from collections import ChainMap, Counter, OrderedDict, defaultdict, deque, namedtuple
from functools import cmp_to_key, reduce


def check(label, actual, expected):
    assert actual == expected, f"{label}: expected {expected!r}, got {actual!r}"
    print(f"{label:<34} {actual!r}")


if __name__ == "__main__":
    # ---------------- Counter
    c = Counter("mississippi")
    check("Counter.most_common(2)", c.most_common(2), [("i", 4), ("s", 4)])
    check("Counter arithmetic", Counter(a=3, b=1) - Counter(a=1, b=2), Counter(a=2))
    check("Counter & (min counts)", Counter("aabbc") & Counter("abbbd"), Counter(a=1, b=2))
    check("Counter update from words", Counter("the cat the hat".split())["the"], 2)

    # ---------------- defaultdict
    groups = defaultdict(list)
    for word in ["apple", "avocado", "banana", "blueberry", "cherry"]:
        groups[word[0]].append(word)
    check("defaultdict(list) grouping", dict(groups), {"a": ["apple", "avocado"], "b": ["banana", "blueberry"], "c": ["cherry"]})
    graph = defaultdict(set)
    graph["a"].add("b")
    check("defaultdict(set)", graph["a"] | graph["missing"], {"b"})

    # ---------------- deque
    dq = deque([1, 2, 3], maxlen=3)
    dq.append(4)                      # maxlen drops from the left
    dq.appendleft(0)                  # ... and appendleft drops from the right
    check("deque maxlen", list(dq), [0, 2, 3])
    dq.rotate(1)
    check("deque rotate", list(dq), [3, 0, 2])

    # ---------------- OrderedDict / dict ordering
    od = OrderedDict.fromkeys("abc")
    od.move_to_end("a")
    check("OrderedDict.move_to_end", list(od), ["b", "c", "a"])
    check("dict keeps insertion order (3.7+)", list({"z": 1, "a": 2}), ["z", "a"])
    check("dict.fromkeys dedupe keep order", list(dict.fromkeys([3, 1, 3, 2, 1])), [3, 1, 2])

    # ---------------- namedtuple / ChainMap
    Point = namedtuple("Point", "x y")
    p = Point(1, 2)
    check("namedtuple fields", (p.x, p._replace(y=5)), (1, Point(1, 5)))
    config = ChainMap({"debug": True}, {"debug": False, "port": 80})
    check("ChainMap lookup order", (config["debug"], config["port"]), (True, 80))

    # ---------------- heapq
    nums = [5, 1, 8, 3, 9, 2]
    check("heapq.nsmallest / nlargest", (heapq.nsmallest(2, nums), heapq.nlargest(2, nums)), ([1, 2], [9, 8]))
    heap = []
    for n in nums:
        heapq.heappush(heap, -n)          # max-heap trick: push negatives
    check("max-heap via negation", -heapq.heappop(heap), 9)
    tasks = [(2, "write"), (1, "plan"), (3, "ship")]
    heapq.heapify(tasks)
    check("heap of tuples (priority)", heapq.heappop(tasks), (1, "plan"))
    check("heapq.merge sorted lists", list(heapq.merge([1, 4], [2, 3, 5])), [1, 2, 3, 4, 5])

    # ---------------- bisect
    scores = [10, 20, 20, 30]
    check("bisect_left / bisect_right", (bisect.bisect_left(scores, 20), bisect.bisect_right(scores, 20)), (1, 3))
    bisect.insort(scores, 25)
    check("bisect.insort keeps sorted", scores, [10, 20, 20, 25, 30])
    grades = "FDCBA"
    check("bisect for ranges (grades)", [grades[bisect.bisect([60, 70, 80, 90], s)] for s in (55, 75, 95)], ["F", "C", "A"])

    # ---------------- itertools
    check("combinations", list(itertools.combinations("abc", 2)), [("a", "b"), ("a", "c"), ("b", "c")])
    check("permutations count", len(list(itertools.permutations(range(4)))), 24)
    check("product (cartesian)", list(itertools.product("ab", [1, 2])), [("a", 1), ("a", 2), ("b", 1), ("b", 2)])
    check("accumulate (running sum)", list(itertools.accumulate([1, 2, 3, 4])), [1, 3, 6, 10])
    check("accumulate (running max)", list(itertools.accumulate([3, 1, 5, 2], max)), [3, 3, 5, 5])
    check("chain.from_iterable", list(itertools.chain.from_iterable([[1, 2], [3], []])), [1, 2, 3])
    check("groupby (needs sorted input)", [(k, len(list(g))) for k, g in itertools.groupby("aaabccaa")],
          [("a", 3), ("b", 1), ("c", 2), ("a", 2)])
    check("islice on infinite count", list(itertools.islice(itertools.count(10, 5), 3)), [10, 15, 20])
    check("pairwise (3.10+)", list(itertools.pairwise([1, 4, 9])), [(1, 4), (4, 9)])
    check("zip_longest", list(itertools.zip_longest("ab", "xyz", fillvalue="-")), [("a", "x"), ("b", "y"), ("-", "z")])

    # ---------------- functools / operator
    check("reduce with operator.mul", reduce(operator.mul, [1, 2, 3, 4]), 24)
    check("sorted by itemgetter", sorted([("b", 2), ("a", 2), ("c", 1)], key=operator.itemgetter(1, 0)),
          [("c", 1), ("a", 2), ("b", 2)])

    def compare_len_then_alpha(a, b):
        return (len(a) - len(b)) or ((a > b) - (a < b))

    check("cmp_to_key custom comparator", sorted(["bb", "a", "ab", "c"], key=cmp_to_key(compare_len_then_alpha)),
          ["a", "c", "ab", "bb"])

    # ---------------- built-ins that save lines
    people = [("ann", 31), ("bob", 25), ("cat", 31)]
    check("sort desc age, asc name", sorted(people, key=lambda p: (-p[1], p[0])), [("ann", 31), ("cat", 31), ("bob", 25)])
    check("max with key and default", (max(people, key=lambda p: p[1]), max([], default=None)), (("ann", 31), None))
    check("enumerate start=1", list(enumerate("ab", start=1)), [(1, "a"), (2, "b")])
    check("zip into dict", dict(zip(["a", "b"], [1, 2])), {"a": 1, "b": 2})
    check("any / all", (any(n > 8 for n in nums), all(n > 0 for n in nums)), (True, True))
    check("set operations", ({1, 2, 3} & {2, 3, 4}, {1, 2} | {3}, {1, 2, 3} - {2}, {1, 2} ^ {2, 3}),
          ({2, 3}, {1, 2, 3}, {1, 3}, {1, 3}))
    check("dict.get / setdefault", ({}.get("x", 0), {}.setdefault("k", []),), (0, []))
    check("divmod", divmod(17, 5), (3, 2))
    check("str methods", ("  hi ".strip(), "a,b,,c".split(","), "x".join("abc")), ("hi", ["a", "b", "", "c"], "axbxc"))

    print("\nall checks passed")
