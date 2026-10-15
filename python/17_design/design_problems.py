"""
"Design a class" questions (LLD in Python).

Run: python python/17_design/design_problems.py

REMEMBER:
  * LRU cache -> OrderedDict (move_to_end + popitem(last=False)); for a function, just use @lru_cache.
  * Expiring / TTL data -> store (value, expires_at) and check lazily on read.
  * Anything time-based -> inject a clock function so tests are deterministic (no sleeping in tests).
  * "Last N seconds" -> deque of timestamps, pop from the left while too old.
  * Thread safety -> wrap the critical section in `with self._lock`.
"""
import bisect
import threading
from collections import OrderedDict, defaultdict, deque


def check(label, actual, expected):
    assert actual == expected, f"{label}: expected {expected!r}, got {actual!r}"
    print(f"{label:<32} {actual!r}")


class LRUCache:
    """get/put in O(1). OrderedDict = dict + doubly linked list, which is exactly what LRU needs."""

    def __init__(self, capacity):
        self.capacity = capacity
        self._data = OrderedDict()

    def get(self, key, default=None):
        if key not in self._data:
            return default
        self._data.move_to_end(key)                  # mark as most recently used
        return self._data[key]

    def put(self, key, value):
        if key in self._data:
            self._data.move_to_end(key)
        self._data[key] = value
        if len(self._data) > self.capacity:
            self._data.popitem(last=False)           # evict the least recently used

    def keys(self):
        return list(self._data)


class TTLCache:
    """Cache where entries expire. Lazy expiry on read + optional purge."""

    def __init__(self, ttl_seconds, clock):
        self.ttl = ttl_seconds
        self.clock = clock
        self._data = {}

    def put(self, key, value):
        self._data[key] = (value, self.clock() + self.ttl)

    def get(self, key, default=None):
        entry = self._data.get(key)
        if entry is None:
            return default
        value, expires_at = entry
        if self.clock() >= expires_at:
            del self._data[key]                      # expired: drop it
            return default
        return value

    def __len__(self):
        return sum(1 for _, expires_at in self._data.values() if self.clock() < expires_at)


class TokenBucketRateLimiter:
    """Allows bursts up to `capacity`, refills at `rate` tokens/second."""

    def __init__(self, capacity, refill_per_second, clock):
        self.capacity = capacity
        self.refill_per_second = refill_per_second
        self.clock = clock
        self.tokens = float(capacity)
        self.updated_at = clock()
        self._lock = threading.Lock()

    def allow(self):
        with self._lock:
            now = self.clock()
            self.tokens = min(self.capacity, self.tokens + (now - self.updated_at) * self.refill_per_second)
            self.updated_at = now
            if self.tokens >= 1:
                self.tokens -= 1
                return True
            return False


class HitCounter:
    """Hits in the last 300 seconds."""

    def __init__(self, window=300):
        self.window = window
        self._hits = deque()

    def hit(self, timestamp):
        self._hits.append(timestamp)

    def get_hits(self, timestamp):
        while self._hits and self._hits[0] <= timestamp - self.window:
            self._hits.popleft()
        return len(self._hits)


class TimeMap:
    """set(key, value, timestamp) / get(key, timestamp) -> the value at or before that time (binary search)."""

    def __init__(self):
        self._times = defaultdict(list)
        self._values = defaultdict(list)

    def set(self, key, value, timestamp):
        self._times[key].append(timestamp)           # timestamps arrive in increasing order
        self._values[key].append(value)

    def get(self, key, timestamp):
        i = bisect.bisect_right(self._times[key], timestamp) - 1
        return self._values[key][i] if i >= 0 else ""


class EventEmitter:
    """Observer pattern: subscribe callbacks to named events."""

    def __init__(self):
        self._handlers = defaultdict(list)

    def on(self, event, handler):
        self._handlers[event].append(handler)
        return lambda: self._handlers[event].remove(handler)      # unsubscribe function

    def emit(self, event, *args, **kwargs):
        for handler in list(self._handlers[event]):
            handler(*args, **kwargs)
        return len(self._handlers[event])


class BankAccount:
    """Thread-safe account: every mutation happens inside the lock."""

    def __init__(self, account_id, balance=0):
        self.id = account_id
        self._balance = balance
        self._lock = threading.Lock()

    @property
    def balance(self):
        with self._lock:
            return self._balance

    def deposit(self, amount):
        with self._lock:
            self._balance += amount

    def withdraw(self, amount):
        with self._lock:
            if self._balance < amount:
                return False                          # insufficient funds
            self._balance -= amount
            return True


class PeekingIterator:
    """Wrap any iterator and add peek() without consuming."""

    _SENTINEL = object()

    def __init__(self, iterable):
        self._it = iter(iterable)
        self._next = next(self._it, self._SENTINEL)

    def peek(self):
        return None if self._next is self._SENTINEL else self._next

    def has_next(self):
        return self._next is not self._SENTINEL

    def __next__(self):
        if not self.has_next():
            raise StopIteration
        current, self._next = self._next, next(self._it, self._SENTINEL)
        return current

    def __iter__(self):
        return self


class NestedIterator:
    """Flatten [[1, 1], 2, [1, 1]] lazily using a stack."""

    def __init__(self, nested):
        self._stack = list(reversed(nested))

    def __iter__(self):
        return self

    def __next__(self):
        while self._stack:
            top = self._stack.pop()
            if isinstance(top, list):
                self._stack.extend(reversed(top))
            else:
                return top
        raise StopIteration


class ConsistentHashRing:
    """Virtual nodes on a sorted ring; adding a node moves only ~1/N of the keys."""

    def __init__(self, nodes=(), virtual_nodes=100):
        self.virtual_nodes = virtual_nodes
        self._ring = []            # sorted hashes
        self._nodes = {}           # hash -> node
        for node in nodes:
            self.add(node)

    @staticmethod
    def _hash(key):
        import hashlib
        return int(hashlib.md5(str(key).encode()).hexdigest()[:16], 16)

    def add(self, node):
        for i in range(self.virtual_nodes):
            h = self._hash(f"{node}#{i}")
            bisect.insort(self._ring, h)
            self._nodes[h] = node

    def remove(self, node):
        for i in range(self.virtual_nodes):
            h = self._hash(f"{node}#{i}")
            self._ring.remove(h)
            del self._nodes[h]

    def node_for(self, key):
        if not self._ring:
            return None
        i = bisect.bisect(self._ring, self._hash(key)) % len(self._ring)    # wrap around
        return self._nodes[self._ring[i]]


if __name__ == "__main__":
    cache = LRUCache(2)
    cache.put("a", 1)
    cache.put("b", 2)
    cache.get("a")                      # "a" is now most recently used
    cache.put("c", 3)                   # evicts "b"
    check("LRUCache eviction order", cache.keys(), ["a", "c"])
    check("LRUCache evicted key gone", cache.get("b"), None)

    now = [100.0]
    ttl = TTLCache(ttl_seconds=10, clock=lambda: now[0])
    ttl.put("session", "user42")
    check("TTLCache before expiry", ttl.get("session"), "user42")
    now[0] += 11
    check("TTLCache after expiry", (ttl.get("session"), len(ttl)), (None, 0))

    now[0] = 0.0
    limiter = TokenBucketRateLimiter(capacity=5, refill_per_second=1, clock=lambda: now[0])
    check("rate limiter burst of 7", sum(limiter.allow() for _ in range(7)), 5)
    now[0] += 2
    check("rate limiter after 2s", sum(limiter.allow() for _ in range(3)), 2)

    counter = HitCounter()
    for t in (1, 2, 3, 300):
        counter.hit(t)
    check("HitCounter at t=300", counter.get_hits(300), 4)
    check("HitCounter at t=301", counter.get_hits(301), 3)

    time_map = TimeMap()
    time_map.set("price", "10", 1)
    time_map.set("price", "20", 4)
    check("TimeMap lookups", [time_map.get("price", t) for t in (0, 1, 3, 4, 9)], ["", "10", "10", "20", "20"])

    emitter = EventEmitter()
    received = []
    unsubscribe = emitter.on("order", received.append)
    emitter.on("order", lambda o: received.append(f"audit:{o}"))
    emitter.emit("order", "A1")
    unsubscribe()
    emitter.emit("order", "A2")
    check("EventEmitter", received, ["A1", "audit:A1", "audit:A2"])

    account = BankAccount("acc-1", 100)
    account.deposit(50)
    check("BankAccount ops", (account.withdraw(200), account.withdraw(120), account.balance), (False, True, 30))

    it = PeekingIterator([1, 2, 3])
    check("PeekingIterator", (it.peek(), next(it), it.peek(), list(it)), (1, 1, 2, [2, 3]))
    check("NestedIterator", list(NestedIterator([[1, 1], 2, [1, [3, 4]]])), [1, 1, 2, 1, 3, 4])

    ring = ConsistentHashRing(["A", "B", "C"])
    keys = [f"user:{i}" for i in range(3000)]
    before = {k: ring.node_for(k) for k in keys}
    ring.add("D")
    moved = sum(1 for k in keys if before[k] != ring.node_for(k))
    print(f"{'consistent hashing 3->4 nodes':<32} {100 * moved / len(keys):.1f}% of keys moved (ideal 25%)")
    check("most keys stayed put", moved < len(keys) * 0.4, True)

    print("\nall checks passed")
