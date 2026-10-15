"""
Stack and queue problems.

Run: python python/06_stacks_queues/stacks_queues.py

REMEMBER:
  * A Python list IS a stack: append / pop. For a queue use collections.deque (popleft is O(1)).
  * MONOTONIC STACK ("next greater", spans, histogram): keep indexes; while stack and a[stack[-1]] < a[i]: pop and answer.
  * Nesting problems (brackets, "3[a]", calculators): push the state on '(' and pop on ')'.
"""
from collections import deque


def check(label, actual, expected):
    assert actual == expected, f"{label}: expected {expected!r}, got {actual!r}"
    print(f"{label:<28} {actual!r}")


class MinStack:
    """push/pop/top/get_min all O(1): store (value, min so far)."""

    def __init__(self):
        self._stack = []

    def push(self, x):
        self._stack.append((x, x if not self._stack else min(x, self._stack[-1][1])))

    def pop(self):
        return self._stack.pop()[0]

    def top(self):
        return self._stack[-1][0]

    def get_min(self):
        return self._stack[-1][1]


class QueueWithTwoStacks:
    """Amortized O(1): pour `in` into `out` only when `out` is empty."""

    def __init__(self):
        self._in, self._out = [], []

    def push(self, x):
        self._in.append(x)

    def pop(self):
        self.peek()
        return self._out.pop()

    def peek(self):
        if not self._out:
            while self._in:
                self._out.append(self._in.pop())
        return self._out[-1]

    def empty(self):
        return not self._in and not self._out


class StackWithOneQueue:
    """After adding x, rotate the older items behind it."""

    def __init__(self):
        self._q = deque()

    def push(self, x):
        self._q.append(x)
        for _ in range(len(self._q) - 1):
            self._q.append(self._q.popleft())

    def pop(self):
        return self._q.popleft()

    def top(self):
        return self._q[0]


class CircularQueue:
    def __init__(self, capacity):
        self._data = [None] * capacity
        self._head = self._size = 0

    def enqueue(self, value):
        if self.is_full():
            return False
        self._data[(self._head + self._size) % len(self._data)] = value
        self._size += 1
        return True

    def dequeue(self):
        if self.is_empty():
            return False
        self._head = (self._head + 1) % len(self._data)
        self._size -= 1
        return True

    def front(self):
        return None if self.is_empty() else self._data[self._head]

    def rear(self):
        return None if self.is_empty() else self._data[(self._head + self._size - 1) % len(self._data)]

    def is_empty(self):
        return self._size == 0

    def is_full(self):
        return self._size == len(self._data)


class MovingAverage:
    def __init__(self, size):
        self._window, self._sum = deque(maxlen=size), 0.0

    def next(self, value):
        if len(self._window) == self._window.maxlen:
            self._sum -= self._window[0]          # the oldest is about to be dropped by maxlen
        self._window.append(value)
        self._sum += value
        return self._sum / len(self._window)


class RecentCounter:
    """Requests in the last 3000 ms."""

    def __init__(self):
        self._times = deque()

    def ping(self, t):
        self._times.append(t)
        while self._times[0] < t - 3000:
            self._times.popleft()
        return len(self._times)


def eval_rpn(tokens):
    stack = []
    ops = {"+": lambda a, b: a + b, "-": lambda a, b: a - b,
           "*": lambda a, b: a * b, "/": lambda a, b: int(a / b)}   # int() truncates toward zero
    for token in tokens:
        if token in ops:
            b, a = stack.pop(), stack.pop()
            stack.append(ops[token](a, b))
        else:
            stack.append(int(token))
    return stack.pop()


def calculate(expression):
    """Basic calculator: + - and parentheses."""
    stack, result, sign, number = [], 0, 1, 0
    for ch in expression:
        if ch.isdigit():
            number = number * 10 + int(ch)
        elif ch in "+-":
            result += sign * number
            number, sign = 0, 1 if ch == "+" else -1
        elif ch == "(":
            stack.append(result)
            stack.append(sign)
            result, sign = 0, 1
        elif ch == ")":
            result += sign * number
            number = 0
            result = result * stack.pop() + stack.pop()   # saved sign, then saved result
    return result + sign * number


def decode_string(s):
    """'3[a2[c]]' -> 'accaccacc'"""
    counts, parts, current, k = [], [], "", 0
    for ch in s:
        if ch.isdigit():
            k = k * 10 + int(ch)
        elif ch == "[":
            counts.append(k)
            parts.append(current)
            current, k = "", 0
        elif ch == "]":
            current = parts.pop() + current * counts.pop()
        else:
            current += ch
    return current


def simplify_path(path):
    stack = []
    for part in path.split("/"):
        if part == "..":
            if stack:
                stack.pop()
        elif part and part != ".":
            stack.append(part)
    return "/" + "/".join(stack)


def next_greater(nums):
    result, stack = [-1] * len(nums), []          # stack holds indexes waiting for a bigger value
    for i, n in enumerate(nums):
        while stack and nums[stack[-1]] < n:
            result[stack.pop()] = n
        stack.append(i)
    return result


def next_greater_circular(nums):
    n = len(nums)
    result, stack = [-1] * n, []
    for i in range(2 * n):                        # go around twice
        while stack and nums[stack[-1]] < nums[i % n]:
            result[stack.pop()] = nums[i % n]
        if i < n:
            stack.append(i)
    return result


def daily_temperatures(temps):
    result, stack = [0] * len(temps), []
    for i, t in enumerate(temps):
        while stack and temps[stack[-1]] < t:
            j = stack.pop()
            result[j] = i - j                     # distance, not the value
        stack.append(i)
    return result


def stock_spans(prices):
    spans, stack = [], []                         # stack of (price, span)
    for price in prices:
        span = 1
        while stack and stack[-1][0] <= price:
            span += stack.pop()[1]                # absorb smaller/equal spans
        stack.append((price, span))
        spans.append(span)
    return spans


def largest_rectangle(heights):
    stack, best = [], 0                           # increasing stack of indexes
    for i, h in enumerate(heights + [0]):         # sentinel 0 flushes the stack
        while stack and heights[stack[-1]] >= h:
            height = heights[stack.pop()]
            left = stack[-1] if stack else -1
            best = max(best, height * (i - left - 1))
        stack.append(i)
    return best


def remove_adjacent_duplicates(s):
    stack = []
    for ch in s:
        if stack and stack[-1] == ch:
            stack.pop()
        else:
            stack.append(ch)
    return "".join(stack)


def remove_k_digits(num, k):
    stack = []
    for digit in num:
        while k and stack and stack[-1] > digit:  # monotonic increasing -> smallest number
            stack.pop()
            k -= 1
        stack.append(digit)
    return "".join(stack[:len(stack) - k]).lstrip("0") or "0"


def asteroid_collision(asteroids):
    stack = []
    for a in asteroids:
        alive = True
        while alive and a < 0 and stack and stack[-1] > 0:   # only right-moving vs incoming left-moving collide
            if stack[-1] < -a:
                stack.pop()                                      # the one on the stack explodes, keep checking
            else:
                if stack[-1] == -a:
                    stack.pop()                                  # equal size: both explode
                alive = False
        if alive:
            stack.append(a)
    return stack


def backspace_compare(a, b):
    def typed(s):
        out = []
        for ch in s:
            if ch == "#":
                if out:
                    out.pop()
            else:
                out.append(ch)
        return out
    return typed(a) == typed(b)


if __name__ == "__main__":
    ms = MinStack()
    for v in (-2, 0, -3):
        ms.push(v)
    check("MinStack get_min", ms.get_min(), -3)
    ms.pop()
    check("MinStack after pop", (ms.top(), ms.get_min()), (0, -2))

    q = QueueWithTwoStacks()
    q.push(1), q.push(2)
    check("QueueWithTwoStacks", (q.peek(), q.pop(), q.pop(), q.empty()), (1, 1, 2, True))

    st = StackWithOneQueue()
    for v in (1, 2, 3):
        st.push(v)
    check("StackWithOneQueue", (st.top(), st.pop(), st.pop()), (3, 3, 2))

    cq = CircularQueue(3)
    check("CircularQueue fill", [cq.enqueue(v) for v in (1, 2, 3, 4)], [True, True, True, False])
    cq.dequeue()
    cq.enqueue(4)
    check("CircularQueue wrap", (cq.front(), cq.rear()), (2, 4))

    ma = MovingAverage(3)
    check("MovingAverage", [round(ma.next(v), 2) for v in (1, 10, 3, 5)], [1.0, 5.5, 4.67, 6.0])

    rc = RecentCounter()
    check("RecentCounter", [rc.ping(t) for t in (1, 100, 3001, 3002)], [1, 2, 3, 3])

    check("eval_rpn", (eval_rpn(["2", "1", "+", "3", "*"]), eval_rpn(["4", "13", "5", "/", "+"])), (9, 6))
    check("calculate", (calculate("(1+(4+5+2)-3)+(6+8)"), calculate("2-(5-6)")), (23, 3))
    check("decode_string", decode_string("3[a2[c]]"), "accaccacc")
    check("simplify_path", (simplify_path("/a/./b/../../c/"), simplify_path("/../")), ("/c", "/"))
    check("next_greater", next_greater([2, 1, 2, 4, 3]), [4, 2, 4, -1, -1])
    check("next_greater_circular", next_greater_circular([1, 2, 1]), [2, -1, 2])
    check("daily_temperatures", daily_temperatures([73, 74, 75, 71, 69, 72, 76, 73]), [1, 1, 4, 2, 1, 1, 0, 0])
    check("stock_spans", stock_spans([100, 80, 60, 70, 60, 75, 85]), [1, 1, 1, 2, 1, 4, 6])
    check("largest_rectangle", largest_rectangle([2, 1, 5, 6, 2, 3]), 10)
    check("remove_adjacent_duplicates", remove_adjacent_duplicates("abbaca"), "ca")
    check("remove_k_digits", (remove_k_digits("1432219", 3), remove_k_digits("10200", 1), remove_k_digits("10", 2)),
          ("1219", "200", "0"))
    check("asteroid_collision", (asteroid_collision([5, 10, -5]), asteroid_collision([8, -8]), asteroid_collision([10, 2, -5])),
          ([5, 10], [], [10]))
    check("backspace_compare", (backspace_compare("ab#c", "ad#c"), backspace_compare("a#c", "b")), (True, False))

    print("\nall checks passed")
