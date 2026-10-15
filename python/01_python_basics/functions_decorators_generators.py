"""
Decorators, generators, iterators, context managers, closures.

Run: python python/01_python_basics/functions_decorators_generators.py

REMEMBER:
  * Decorator = function that takes a function and returns a wrapper. Always use @functools.wraps.
  * Decorator WITH arguments = 3 levels: factory(args) -> decorator(func) -> wrapper(*args, **kwargs).
  * Generator = function with `yield`; lazy, remembers its state, single pass. `yield from` delegates.
  * Iterator protocol = __iter__ returns self + __next__ raises StopIteration.
  * Context manager = __enter__/__exit__ or @contextlib.contextmanager with try/finally around yield.
"""
import contextlib
import functools
import itertools
import time


def check(label, actual, expected):
    assert actual == expected, f"{label}: expected {expected!r}, got {actual!r}"
    print(f"{label:<34} {actual!r}")


# ---------------------------------------------------------------- decorators

def timer(func):
    @functools.wraps(func)                       # keeps __name__, __doc__
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        try:
            return func(*args, **kwargs)
        finally:
            wrapper.last_seconds = time.perf_counter() - start
    return wrapper


def retry(times=3, exceptions=(Exception,)):     # decorator factory (takes arguments)
    def decorator(func):
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            for attempt in range(1, times + 1):
                try:
                    return func(*args, **kwargs)
                except exceptions:
                    if attempt == times:
                        raise
        return wrapper
    return decorator


def memoize(func):                               # what functools.lru_cache/cache does for you
    cache = {}

    @functools.wraps(func)
    def wrapper(*args):
        if args not in cache:
            cache[args] = func(*args)
        return cache[args]
    return wrapper


class CountCalls:                                # class-based decorator with state
    def __init__(self, func):
        functools.update_wrapper(self, func)
        self.func = func
        self.calls = 0

    def __call__(self, *args, **kwargs):
        self.calls += 1
        return self.func(*args, **kwargs)


@timer
def slow_add(a, b):
    """Adds slowly."""
    time.sleep(0.01)
    return a + b


attempts = {"n": 0}


@retry(times=3, exceptions=(ConnectionError,))
def flaky_call():
    attempts["n"] += 1
    if attempts["n"] < 3:
        raise ConnectionError("try again")
    return "ok"


@memoize
def fib(n):
    return n if n < 2 else fib(n - 1) + fib(n - 2)


@CountCalls
def greet(name):
    return f"hi {name}"


# ---------------------------------------------------------------- generators & iterators

def fibonacci():                                 # infinite generator
    a, b = 0, 1
    while True:
        yield a
        a, b = b, a + b


def flatten(nested):                             # recursive generator with yield from
    for item in nested:
        if isinstance(item, (list, tuple)):
            yield from flatten(item)
        else:
            yield item


def chunked(iterable, size):                     # works on any iterable, even huge files
    it = iter(iterable)
    while batch := list(itertools.islice(it, size)):
        yield batch


def read_lines_lazily(lines):                    # generator pipeline: nothing runs until consumed
    stripped = (line.strip() for line in lines)
    non_empty = (line for line in stripped if line and not line.startswith("#"))
    return (line.upper() for line in non_empty)


class Countdown:                                 # iterator protocol by hand
    def __init__(self, start):
        self.current = start

    def __iter__(self):
        return self

    def __next__(self):
        if self.current <= 0:
            raise StopIteration
        self.current -= 1
        return self.current + 1


def running_average():                           # generator that RECEIVES values with send()
    total = count = 0
    average = None
    while True:
        value = yield average
        total += value
        count += 1
        average = total / count


# ---------------------------------------------------------------- context managers

class Timer:
    def __enter__(self):
        self.start = time.perf_counter()
        return self

    def __exit__(self, exc_type, exc, tb):
        self.seconds = time.perf_counter() - self.start
        return False                             # False = don't swallow exceptions


@contextlib.contextmanager
def temporary_setting(settings, key, value):
    old = settings.get(key)
    settings[key] = value
    try:
        yield settings
    finally:                                     # always restores, even on exception
        settings[key] = old


# ---------------------------------------------------------------- closures, partial, lambda

def make_multiplier(n):
    return lambda x: x * n


if __name__ == "__main__":
    check("decorator keeps metadata", (slow_add.__name__, slow_add.__doc__), ("slow_add", "Adds slowly."))
    check("timer decorator result", slow_add(2, 3), 5)
    check("timer measured >= 0.01s", slow_add.last_seconds >= 0.01, True)
    check("retry succeeded on attempt", (flaky_call(), attempts["n"]), ("ok", 3))
    check("memoized fib(80)", fib(80), 23416728348467685)
    greet("a"), greet("b")
    check("class decorator call count", greet.calls, 2)

    check("infinite generator + islice", list(itertools.islice(fibonacci(), 10)), [0, 1, 1, 2, 3, 5, 8, 13, 21, 34])
    check("flatten with yield from", list(flatten([1, [2, [3, (4, 5)]], 6])), [1, 2, 3, 4, 5, 6])
    check("chunked", list(chunked(range(7), 3)), [[0, 1, 2], [3, 4, 5], [6]])
    check("lazy pipeline", list(read_lines_lazily(["a\n", "  \n", "# comment", " b "])), ["A", "B"])
    check("custom iterator", list(Countdown(3)), [3, 2, 1])

    gen = (x * x for x in range(3))
    list(gen)
    check("generators are single-pass", list(gen), [])

    avg = running_average()
    next(avg)                                    # prime the generator
    check("generator send()", [avg.send(10), avg.send(20), avg.send(60)], [10.0, 15.0, 30.0])

    with Timer() as t:
        time.sleep(0.01)
    check("context manager class", t.seconds >= 0.01, True)

    settings = {"debug": False}
    with temporary_setting(settings, "debug", True):
        check("inside contextmanager", settings["debug"], True)
    check("restored after with", settings["debug"], False)

    with contextlib.suppress(ZeroDivisionError):
        1 / 0
    check("contextlib.suppress", "no crash", "no crash")

    triple = make_multiplier(3)
    check("closure factory", triple(7), 21)
    check("functools.partial", functools.partial(int, base=2)("1010"), 10)
    check("functools.reduce", functools.reduce(lambda acc, x: acc * x, [1, 2, 3, 4], 1), 24)
    check("sorted with lambda key", sorted(["bb", "a", "ccc"], key=lambda s: -len(s)), ["ccc", "bb", "a"])

    print("\nall checks passed")
