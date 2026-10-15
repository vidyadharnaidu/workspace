"""
Math, number and bit-manipulation problems.

Run: python python/13_math_bits/math_and_bits.py

REMEMBER:
  * Digits: n % 10 = last, n // 10 drops it, rev = rev * 10 + digit.
  * Only loop while i * i <= n when looking for divisors/primes.
  * Python ints are unbounded (no overflow) and pow(a, b, mod) is fast modular exponentiation.
  * Bits: x & (x - 1) clears the lowest set bit; a ^ a = 0; int.bit_count() counts 1s (3.10+).
  * Use math: gcd, lcm, isqrt, comb, perm, factorial, prod.
"""
import math
import random
from functools import reduce
from operator import xor


def check(label, actual, expected):
    assert actual == expected, f"{label}: expected {expected!r}, got {actual!r}"
    print(f"{label:<28} {actual!r}")


def sieve(n):
    """All primes up to n."""
    is_prime = [True] * (n + 1)
    is_prime[0:2] = [False, False]
    for i in range(2, math.isqrt(n) + 1):
        if is_prime[i]:
            is_prime[i * i::i] = [False] * len(is_prime[i * i::i])   # cross out multiples from i*i
    return [i for i, prime in enumerate(is_prime) if prime]


def is_prime(n):
    return n > 1 and all(n % i for i in range(2, math.isqrt(n) + 1))


def prime_factors(n):
    factors = []
    p = 2
    while p * p <= n:
        while n % p == 0:
            factors.append(p)
            n //= p
        p += 1
    if n > 1:
        factors.append(n)                     # what's left is a prime > sqrt
    return factors


def fizzbuzz(n):
    return ["FizzBuzz" if i % 15 == 0 else "Fizz" if i % 3 == 0 else "Buzz" if i % 5 == 0 else str(i)
            for i in range(1, n + 1)]


def fibonacci_list(n):
    result, a, b = [], 0, 1
    for _ in range(n):
        result.append(a)
        a, b = b, a + b
    return result


def is_palindrome_number(x):
    if x < 0 or (x % 10 == 0 and x != 0):
        return False
    reversed_half = 0
    while x > reversed_half:                  # only reverse half the digits
        x, reversed_half = x // 10, reversed_half * 10 + x % 10
    return x in (reversed_half, reversed_half // 10)


def reverse_integer(x):
    """32-bit overflow returns 0 (Python ints don't overflow, so check explicitly)."""
    sign = -1 if x < 0 else 1
    reversed_value = sign * int(str(abs(x))[::-1])
    return 0 if not -2**31 <= reversed_value <= 2**31 - 1 else reversed_value


def sum_of_digits(n):
    return sum(int(d) for d in str(abs(n)))


def digital_root(n):
    return 0 if n == 0 else 1 + (n - 1) % 9


def is_armstrong(n):
    digits = str(n)
    return sum(int(d) ** len(digits) for d in digits) == n


def is_perfect(n):
    if n <= 1:
        return False
    total = 1
    for i in range(2, math.isqrt(n) + 1):
        if n % i == 0:
            total += i + (n // i if i != n // i else 0)
    return total == n


def trailing_zeros_factorial(n):
    """Count factors of 5: n//5 + n//25 + ..."""
    count = 0
    while n:
        n //= 5
        count += n
    return count


def title_to_number(title):
    return reduce(lambda acc, ch: acc * 26 + ord(ch) - ord("A") + 1, title, 0)


def number_to_title(n):
    letters = []
    while n:
        n, rem = divmod(n - 1, 26)            # -1 because there is no "zero" digit in Excel
        letters.append(chr(ord("A") + rem))
    return "".join(reversed(letters))


ROMAN_VALUES = {"I": 1, "V": 5, "X": 10, "L": 50, "C": 100, "D": 500, "M": 1000}


def roman_to_int(s):
    total = 0
    for i, ch in enumerate(s):
        value = ROMAN_VALUES[ch]
        total += -value if i + 1 < len(s) and value < ROMAN_VALUES[s[i + 1]] else value
    return total


def int_to_roman(num):
    pairs = [(1000, "M"), (900, "CM"), (500, "D"), (400, "CD"), (100, "C"), (90, "XC"),
             (50, "L"), (40, "XL"), (10, "X"), (9, "IX"), (5, "V"), (4, "IV"), (1, "I")]
    parts = []
    for value, symbol in pairs:
        count, num = divmod(num, value)
        parts.append(symbol * count)
    return "".join(parts)


def add_binary(a, b):
    return bin(int(a, 2) + int(b, 2))[2:]     # or do it manually with a carry


def multiply_strings(a, b):
    result = [0] * (len(a) + len(b))
    for i in reversed(range(len(a))):
        for j in reversed(range(len(b))):
            total = int(a[i]) * int(b[j]) + result[i + j + 1]
            result[i + j + 1] = total % 10
            result[i + j] += total // 10      # digit i*j lands at positions i+j and i+j+1
    return "".join(map(str, result)).lstrip("0") or "0"


def plus_one(digits):
    number = int("".join(map(str, digits))) + 1
    return [int(d) for d in str(number)]


def is_happy(n):
    seen = set()
    while n != 1 and n not in seen:
        seen.add(n)
        n = sum(int(d) ** 2 for d in str(n))
    return n == 1


def to_base(n, base):
    if n == 0:
        return "0"
    digits = "0123456789abcdefghijklmnopqrstuvwxyz"
    out = []
    while n:
        n, rem = divmod(n, base)
        out.append(digits[rem])
    return "".join(reversed(out))


def gcd_of_strings(a, b):
    return a[:math.gcd(len(a), len(b))] if a + b == b + a else ""


def is_leap_year(year):
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def clock_angle(hour, minutes):
    diff = abs((30 * (hour % 12) + 0.5 * minutes) - 6 * minutes)
    return min(diff, 360 - diff)


def rectangles_overlap(a, b):
    """Rectangles as (x1, y1, x2, y2): must overlap on BOTH axes."""
    return a[0] < b[2] and b[0] < a[2] and a[1] < b[3] and b[1] < a[3]


def pythagorean_triplets(n):
    return [(a, b, math.isqrt(a * a + b * b))
            for a in range(1, n + 1) for b in range(a, n + 1)
            if math.isqrt(a * a + b * b) ** 2 == a * a + b * b and math.isqrt(a * a + b * b) <= n]


def shuffle(items, rng):
    """Fisher-Yates (what random.shuffle does)."""
    result = list(items)
    for i in range(len(result) - 1, 0, -1):
        j = rng.randint(0, i)
        result[i], result[j] = result[j], result[i]
    return result


# ---------------------------------------------------------------- bit manipulation

def single_number(nums):
    return reduce(xor, nums)                  # pairs cancel out


def is_power_of_two(n):
    return n > 0 and n & (n - 1) == 0         # exactly one bit set


def count_set_bits(n):
    count = 0
    while n:
        n &= n - 1                            # clears the lowest set bit
        count += 1
    return count


def counting_bits(n):
    bits = [0] * (n + 1)
    for i in range(1, n + 1):
        bits[i] = bits[i >> 1] + (i & 1)
    return bits


def hamming_distance(x, y):
    return (x ^ y).bit_count()


def swap_without_temp(a, b):
    a, b = b, a                               # Python way; the XOR trick: a ^= b; b ^= a; a ^= b
    return a, b


if __name__ == "__main__":
    check("sieve(30)", sieve(30), [2, 3, 5, 7, 11, 13, 17, 19, 23, 29])
    check("is_prime", (is_prime(97), is_prime(91)), (True, False))
    check("prime_factors(360)", prime_factors(360), [2, 2, 2, 3, 3, 5])
    check("fizzbuzz(15) last", fizzbuzz(15)[-3:], ["13", "14", "FizzBuzz"])
    check("fibonacci_list(10)", fibonacci_list(10), [0, 1, 1, 2, 3, 5, 8, 13, 21, 34])
    check("math.gcd / math.lcm", (math.gcd(48, 18), math.lcm(4, 6)), (6, 12))
    check("pow(2, 100, 1e9+7)", pow(2, 100, 1_000_000_007), 976371285)
    check("math.factorial / comb", (math.factorial(10), math.comb(52, 5)), (3628800, 2598960))
    check("is_palindrome_number", [is_palindrome_number(x) for x in (121, -121, 10)], [True, False, False])
    check("reverse_integer", (reverse_integer(-123), reverse_integer(1534236469)), (-321, 0))
    check("sum_of_digits / digital_root", (sum_of_digits(1234), digital_root(38)), (10, 2))
    check("is_armstrong", (is_armstrong(153), is_armstrong(154)), (True, False))
    check("is_perfect(28)", is_perfect(28), True)
    check("trailing_zeros_factorial(25)", trailing_zeros_factorial(25), 6)
    check("excel column", (title_to_number("AB"), number_to_title(28), number_to_title(701)), (28, "AB", "ZY"))
    check("roman", (roman_to_int("MCMXCIV"), int_to_roman(1994)), (1994, "MCMXCIV"))
    check("add_binary", add_binary("1010", "1011"), "10101")
    check("multiply_strings", (multiply_strings("123", "456"), multiply_strings("0", "52")), ("56088", "0"))
    check("plus_one", (plus_one([1, 2, 9]), plus_one([9, 9])), ([1, 3, 0], [1, 0, 0]))
    check("is_happy", (is_happy(19), is_happy(2)), (True, False))
    check("to_base", (to_base(255, 2), to_base(255, 16)), ("11111111", "ff"))
    check("gcd_of_strings", (gcd_of_strings("ABCABC", "ABC"), gcd_of_strings("LEET", "CODE")), ("ABC", ""))
    check("is_leap_year", [is_leap_year(y) for y in (2024, 1900, 2000)], [True, False, True])
    check("clock_angle(3, 30)", clock_angle(3, 30), 75.0)
    check("rectangles_overlap", rectangles_overlap((0, 0, 2, 2), (1, 1, 3, 3)), True)
    check("pythagorean_triplets(20)", pythagorean_triplets(20),
          [(3, 4, 5), (5, 12, 13), (6, 8, 10), (8, 15, 17), (9, 12, 15), (12, 16, 20)])

    shuffled = shuffle([1, 2, 3, 4, 5], random.Random(42))
    check("shuffle keeps elements", sorted(shuffled), [1, 2, 3, 4, 5])

    check("single_number (XOR)", single_number([4, 1, 2, 1, 2]), 4)
    check("is_power_of_two", (is_power_of_two(16), is_power_of_two(18)), (True, False))
    check("count_set_bits(11)", (count_set_bits(11), (11).bit_count()), (3, 3))
    check("counting_bits(5)", counting_bits(5), [0, 1, 1, 2, 1, 2])
    check("hamming_distance(1, 4)", hamming_distance(1, 4), 2)
    check("swap_without_temp", swap_without_temp(5, 9), (9, 5))

    print("\nall checks passed")
