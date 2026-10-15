"""
Concurrency in Python: threads, processes, asyncio - and the GIL question.

Run: python python/15_concurrency/threads_processes_asyncio.py

THE INTERVIEW ANSWER (memorize this):
  The GIL lets only ONE thread run Python bytecode at a time, so:
    * I/O-BOUND work (network, disk, DB)  -> THREADS or ASYNCIO: the GIL is released while waiting.
    * CPU-BOUND work (math, parsing)      -> MULTIPROCESSING: separate interpreters, real parallelism.
  threading = preemptive (the OS switches any time) -> you still need locks for shared state.
  asyncio   = cooperative (switches only at `await`) -> one thread, thousands of sockets, no locks needed
              for ordinary code, but ONE blocking call freezes everything (use asyncio.to_thread).

REMEMBER:
  * count += 1 is read-modify-write -> not atomic -> use a Lock (or itertools.count / queue).
  * Producer/consumer -> queue.Queue does the waiting and locking for you; one sentinel PER consumer.
  * Always lock multiple resources in the SAME ORDER -> no deadlock.
  * concurrent.futures gives one API: ThreadPoolExecutor (I/O) and ProcessPoolExecutor (CPU).
"""
import asyncio
import queue
import threading
import time
from concurrent.futures import ProcessPoolExecutor, ThreadPoolExecutor, as_completed


def check(label, actual, expected):
    assert actual == expected, f"{label}: expected {expected!r}, got {actual!r}"
    print(f"{label:<34} {actual!r}")


# ---------------------------------------------------------------- 1. race condition and Lock

class Counter:
    def __init__(self):
        self.unsafe = 0
        self.safe = 0
        self._lock = threading.Lock()

    def increment(self):
        # This is what `self.unsafe += 1` does under the hood: READ, ADD, WRITE.
        # Written out (with a yield point between read and write) the lost updates show up every run.
        value = self.unsafe
        time.sleep(0)                        # hands the GIL to another thread
        self.unsafe = value + 1

        with self._lock:                     # atomic section: read-add-write can't be interleaved
            self.safe += 1


def race_condition_demo(threads=8, per_thread=2_000):
    counter = Counter()

    def work():
        for _ in range(per_thread):
            counter.increment()

    workers = [threading.Thread(target=work) for _ in range(threads)]
    for w in workers:
        w.start()
    for w in workers:
        w.join()
    return counter.unsafe, counter.safe, threads * per_thread


# ---------------------------------------------------------------- 2. producer / consumer with matching counters

def producer_consumer(producers=3, consumers=2, items_each=1000):
    work_queue = queue.Queue(maxsize=10)     # blocks when full / empty: no manual wait/notify
    produced = consumers_done = 0
    consumed = 0
    lock = threading.Lock()
    SENTINEL = object()

    def produce():
        nonlocal produced
        for i in range(items_each):
            work_queue.put(i)
            with lock:
                produced += 1

    def consume():
        nonlocal consumed
        while True:
            item = work_queue.get()
            if item is SENTINEL:
                work_queue.task_done()
                return
            with lock:
                consumed += 1
            work_queue.task_done()

    producer_threads = [threading.Thread(target=produce) for _ in range(producers)]
    consumer_threads = [threading.Thread(target=consume) for _ in range(consumers)]
    for t in producer_threads + consumer_threads:
        t.start()
    for t in producer_threads:
        t.join()                              # all items are in the queue now
    for _ in consumer_threads:
        work_queue.put(SENTINEL)              # one stop signal PER consumer
    for t in consumer_threads:
        t.join()
    return produced, consumed


# ---------------------------------------------------------------- 3. thread pool for I/O

def fetch(url, delay=0.05):
    time.sleep(delay)                         # pretend network call; sleep releases the GIL
    return f"{url}:200"


def fetch_all_threaded(urls):
    with ThreadPoolExecutor(max_workers=8) as pool:
        return list(pool.map(fetch, urls))    # map keeps input order


def fetch_all_as_completed(urls):
    with ThreadPoolExecutor(max_workers=8) as pool:
        futures = {pool.submit(fetch, url): url for url in urls}
        return sorted(f.result() for f in as_completed(futures))   # results as they finish


# ---------------------------------------------------------------- 4. process pool for CPU

def count_primes(limit):
    return sum(1 for n in range(2, limit) if all(n % d for d in range(2, int(n ** 0.5) + 1)))


# ---------------------------------------------------------------- 5. deadlock-free transfers

class Account:
    def __init__(self, account_id, balance):
        self.id = account_id
        self.balance = balance
        self.lock = threading.Lock()


def transfer(source, target, amount):
    """TRICK: always take locks in a fixed order (by id) so two opposite transfers can't deadlock."""
    first, second = (source, target) if source.id < target.id else (target, source)
    with first.lock, second.lock:
        if source.balance < amount:
            return False
        source.balance -= amount
        target.balance += amount
        return True


def transfer_demo(threads=8, transfers=2000):
    accounts = [Account(i, 1000) for i in range(4)]
    total_before = sum(a.balance for a in accounts)

    def work(seed):
        for i in range(transfers):
            a = accounts[(seed + i) % len(accounts)]
            b = accounts[(seed + i + 1) % len(accounts)]
            transfer(a, b, 10)

    workers = [threading.Thread(target=work, args=(t,)) for t in range(threads)]
    for w in workers:
        w.start()
    for w in workers:
        w.join()
    return total_before, sum(a.balance for a in accounts), all(a.balance >= 0 for a in accounts)


# ---------------------------------------------------------------- 6. asyncio

async def async_fetch(url, delay=0.05):
    await asyncio.sleep(delay)               # non-blocking; other tasks run meanwhile
    return f"{url}:200"


async def gather_demo(urls):
    return await asyncio.gather(*(async_fetch(u) for u in urls))


async def limited_concurrency(urls, limit=3):
    """Semaphore = at most `limit` requests in flight (rate limiting / politeness)."""
    semaphore = asyncio.Semaphore(limit)
    in_flight = peak = 0
    lock = asyncio.Lock()

    async def fetch_one(url):
        nonlocal in_flight, peak
        async with semaphore:
            async with lock:
                in_flight += 1
                peak = max(peak, in_flight)
            result = await async_fetch(url)
            async with lock:
                in_flight -= 1
            return result

    results = await asyncio.gather(*(fetch_one(u) for u in urls))
    return results, peak


async def timeout_demo():
    try:
        await asyncio.wait_for(async_fetch("slow", delay=1.0), timeout=0.05)
        return "no timeout"
    except asyncio.TimeoutError:
        return "timed out"


async def async_producer_consumer(items=20):
    q = asyncio.Queue(maxsize=5)
    consumed = []

    async def produce():
        for i in range(items):
            await q.put(i)
        await q.put(None)                    # one sentinel: a single consumer here

    async def consume():
        while True:
            item = await q.get()
            if item is None:
                return
            consumed.append(item)

    await asyncio.gather(produce(), consume())
    return len(consumed)


if __name__ == "__main__":
    unsafe, safe, expected = race_condition_demo()
    print(f"{'race: unsafe / safe / expected':<34} {unsafe} / {safe} / {expected}"
          f"{'   <- lost updates!' if unsafe != expected else '   (no lost updates this run)'}")
    check("lock keeps the counter correct", safe, expected)
    check("unlocked counter lost updates", unsafe < expected, True)

    produced, consumed = producer_consumer()
    check("producer/consumer counters match", (produced, consumed, produced == consumed), (3000, 3000, True))

    urls = [f"http://api/{i}" for i in range(16)]
    start = time.perf_counter()
    results = fetch_all_threaded(urls)
    threaded_seconds = time.perf_counter() - start
    check("thread pool results", len(results), 16)
    print(f"{'16 I/O calls x 50ms in threads':<34} {threaded_seconds:.2f}s (sequential would be ~0.80s)")
    check("threads beat sequential", threaded_seconds < 0.5, True)
    check("as_completed results", len(fetch_all_as_completed(urls)), 16)

    start = time.perf_counter()
    with ProcessPoolExecutor(max_workers=4) as pool:
        prime_counts = list(pool.map(count_primes, [20_000] * 4))
    print(f"{'4 CPU tasks in processes':<34} {time.perf_counter() - start:.2f}s")
    check("process pool results", prime_counts, [2262] * 4)

    before, after, no_negatives = transfer_demo()
    check("transfers: total conserved", (before, after, no_negatives), (4000, 4000, True))

    check("asyncio.gather", len(asyncio.run(gather_demo(urls))), 16)
    async_results, peak = asyncio.run(limited_concurrency(urls, limit=3))
    check("asyncio semaphore peak <= 3", (len(async_results), peak <= 3), (16, True))
    check("asyncio.wait_for timeout", asyncio.run(timeout_demo()), "timed out")
    check("asyncio.Queue producer/consumer", asyncio.run(async_producer_consumer()), 20)

    print("\nall checks passed")
