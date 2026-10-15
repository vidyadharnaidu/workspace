"""
Python language fundamentals: the "trick questions" interviewers ask.

Run: python python/01_python_basics/language_fundamentals.py

REMEMBER:
  * Default arguments are evaluated ONCE (at def time) -> never use a mutable default ([] or {}).
  * `==` compares values, `is` compares identity (same object). Use `is` only for None/True/False.
  * Assignment never copies. copy.copy = shallow (inner objects shared), copy.deepcopy = full copy.
  * Closures capture VARIABLES, not values (late binding) -> lambda i=i: i
  * Scope lookup order LEGB: Local -> Enclosing -> Global -> Built-in. Use nonlocal / global to rebind.
"""
import copy
import math
from decimal import Decimal


def check(label, actual, expected):
    assert actual == expected, f"{label}: expected {expected!r}, got {actual!r}"
    print(f"{label:<34} {actual!r}")


# Q1. Mutable default argument trap
def append_bad(item, items=[]):          # the SAME list is reused across calls
    items.append(item)
    return items


def append_good(item, items=None):       # fix: use None as the sentinel
    if items is None:
        items = []
    items.append(item)
    return items


# Q2. Late binding closures
def make_lambdas_bad():
    return [lambda: i for i in range(3)]          # all see the final i


def make_lambdas_good():
    return [lambda i=i: i for i in range(3)]      # default arg freezes the current value


# Q3. nonlocal counter (closure with state)
def make_counter():
    count = 0

    def increment():
        nonlocal count            # without this: UnboundLocalError
        count += 1
        return count

    return increment


# Q4. *args, **kwargs, keyword-only and positional-only parameters
def full_signature(a, b=2, *args, c, d=4, **kwargs):
    return a, b, args, c, d, kwargs


def pos_and_kw_only(a, /, b, *, c):   # a: positional only, c: keyword only
    return a + b + c


# Q5. match statement (structural pattern matching, 3.10+)
def describe(value):
    match value:
        case (0, 0):
            return "origin"
        case (x, 0):
            return f"on x-axis at {x}"
        case {"type": "circle", "radius": r}:
            return f"circle r={r}"
        case [first, *rest]:
            return f"list starting with {first}, {len(rest)} more"
        case str() as s if s.isdigit():
            return "numeric string"
        case _:
            return "something else"


if __name__ == "__main__":
    # Q1
    append_bad(1)
    check("Q1 mutable default (bad)", append_bad(2), [1, 2])
    append_good(1)
    check("Q1 None default (good)", append_good(2), [2])

    # Q2
    check("Q2 late binding (bad)", [f() for f in make_lambdas_bad()], [2, 2, 2])
    check("Q2 i=i default (good)", [f() for f in make_lambdas_good()], [0, 1, 2])

    # Q3
    counter = make_counter()
    counter()
    check("Q3 closure counter", counter(), 2)

    # Q4
    check("Q4 full signature", full_signature(1, 3, 5, 6, c=7, e=8), (1, 3, (5, 6), 7, 4, {"e": 8}))
    check("Q4 positional/keyword only", pos_and_kw_only(1, b=2, c=3), 6)
    check("Q4 unpack into call", max(*[4, 9, 2]), 9)
    check("Q4 merge dicts", {**{"a": 1, "b": 2}, **{"b": 3}} | {"c": 4}, {"a": 1, "b": 3, "c": 4})

    # Q5
    check("Q5 match tuple", describe((5, 0)), "on x-axis at 5")
    check("Q5 match dict", describe({"type": "circle", "radius": 2}), "circle r=2")
    check("Q5 match list", describe([1, 2, 3]), "list starting with 1, 2 more")
    check("Q5 match guard", describe("42"), "numeric string")

    # Q6. == vs is
    a, b = [1, 2, 3], [1, 2, 3]
    c = a
    check("Q6 a == b / a is b / c is a", (a == b, a is b, c is a), (True, False, True))

    # Q7. Shallow vs deep copy
    original = [[1, 2], [3, 4]]
    shallow, deep = copy.copy(original), copy.deepcopy(original)   # shallow also: original[:] or list(original)
    original[0].append(99)
    check("Q7 shallow copy shares inner", shallow[0], [1, 2, 99])
    check("Q7 deep copy independent", deep[0], [1, 2])
    grid_bad = [[0] * 2] * 2          # TRAP: 2 references to the SAME row
    grid_bad[0][0] = 1
    grid_good = [[0] * 2 for _ in range(2)]
    grid_good[0][0] = 1
    check("Q7 [[0]*2]*2 trap", grid_bad, [[1, 0], [1, 0]])
    check("Q7 list comprehension grid", grid_good, [[1, 0], [0, 0]])

    # Q8. Mutable vs immutable
    t = (1, [2, 3])
    t[1].append(4)                    # the tuple can't change, the list inside can
    check("Q8 tuple containing list", t, (1, [2, 3, 4]))
    try:
        {[1, 2]: "x"}
        raise AssertionError("lists should not be hashable")
    except TypeError:
        check("Q8 list as dict key", "TypeError: unhashable", "TypeError: unhashable")

    # Q9. Unpacking and swapping
    first, *middle, last = [1, 2, 3, 4, 5]
    check("Q9 star unpacking", (first, middle, last), (1, [2, 3, 4], 5))
    x, y = 1, 2
    x, y = y, x
    check("Q9 swap without temp", (x, y), (2, 1))

    # Q10. Comprehensions and generator expressions
    matrix = [[1, 2, 3], [4, 5, 6]]
    check("Q10 list comp with filter", [n * n for n in range(6) if n % 2 == 0], [0, 4, 16])
    check("Q10 dict comp", {w: len(w) for w in ["hi", "hello"]}, {"hi": 2, "hello": 5})
    check("Q10 flatten (nested for)", [v for row in matrix for v in row], [1, 2, 3, 4, 5, 6])
    check("Q10 transpose with zip(*m)", [list(col) for col in zip(*matrix)], [[1, 4], [2, 5], [3, 6]])
    check("Q10 generator (lazy, no list)", sum(n * n for n in range(1_000_000)), 333332833333500000)

    # Q11. Truthiness and short-circuit values
    check("Q11 all falsy values", any([0, 0.0, "", [], {}, set(), None, False]), False)
    check("Q11 `or` returns a value", 0 or "default", "default")
    check("Q11 `and` returns a value", [] and "never", [])

    # Q12. Numbers: floats, integer division, rounding, big ints
    check("Q12 0.1 + 0.2 == 0.3", 0.1 + 0.2 == 0.3, False)
    check("Q12 math.isclose", math.isclose(0.1 + 0.2, 0.3), True)
    check("Q12 Decimal for money", Decimal("0.1") + Decimal("0.2"), Decimal("0.3"))
    check("Q12 floor division -7 // 2", -7 // 2, -4)           # floors toward -infinity
    check("Q12 int(-7 / 2) truncates", int(-7 / 2), -3)
    check("Q12 banker's rounding", (round(2.5), round(3.5)), (2, 4))
    check("Q12 arbitrary big ints", 2 ** 100, 1267650600228229401496703205376)

    # Q13. f-strings
    name, price = "pen", 1.5
    check("Q13 f-string formats", f"{name:>5}|{price:.2f}|{1234567:,}|{0.256:.1%}", "  pen|1.50|1,234,567|25.6%")
    check("Q13 f-string debug =", f"{name=}", "name='pen'")

    # Q14. Walrus operator
    data = [3, 1, 4, 1, 5]
    check("Q14 walrus in comprehension", [y for v in data if (y := v * 10) > 20], [30, 40, 50])

    # Q15. String immutability and joining
    parts = []
    for word in ["a", "b", "c"]:
        parts.append(word)                 # build a list, join once: O(n) instead of O(n^2) +=
    check("Q15 join instead of +=", "-".join(parts), "a-b-c")

    print("\nall checks passed")
