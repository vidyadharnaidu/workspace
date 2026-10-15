"""
String interview problems, the Pythonic way.

Run: python python/02_strings/string_problems.py

REMEMBER:
  * s[::-1] reverses; Counter(s) counts; sorted(s) is the anagram key.
  * Strings are immutable: build a list and "".join(parts) instead of += in a loop.
  * Sliding window template: expand right, shrink while invalid, track best.
"""
import re
from collections import Counter, defaultdict
from itertools import groupby


def check(label, actual, expected):
    assert actual == expected, f"{label}: expected {expected!r}, got {actual!r}"
    print(f"{label:<30} {actual!r}")


def reverse_string(s):
    return s[::-1]


def reverse_words(s):
    return " ".join(reversed(s.split()))          # split() also collapses extra spaces


def is_palindrome(s):
    cleaned = [c.lower() for c in s if c.isalnum()]
    return cleaned == cleaned[::-1]


def is_anagram(a, b):
    return Counter(a.lower()) == Counter(b.lower())


def group_anagrams(words):
    groups = defaultdict(list)
    for word in words:
        groups["".join(sorted(word))].append(word)   # TRICK: sorted letters = key
    return list(groups.values())


def first_unique_char(s):
    counts = Counter(s)
    return next((c for c in s if counts[c] == 1), None)


def first_repeating_char(s):
    seen = set()
    for c in s:
        if c in seen:
            return c
        seen.add(c)
    return None


def char_frequency(s):
    return dict(Counter(s))


def longest_common_prefix(words):
    if not words:
        return ""
    first, last = min(words), max(words)          # TRICK: only the alphabetically first and last matter
    for i, ch in enumerate(first):
        if i == len(last) or ch != last[i]:
            return first[:i]
    return first


def shortest_unique_prefixes(words):
    counts = Counter(p for w in words for p in (w[:i] for i in range(1, len(w) + 1)))
    return {w: next((w[:i] for i in range(1, len(w) + 1) if counts[w[:i]] == 1), w) for w in words}


def longest_palindrome(s):
    def expand(left, right):
        while left >= 0 and right < len(s) and s[left] == s[right]:
            left, right = left - 1, right + 1
        return s[left + 1:right]

    best = ""
    for i in range(len(s)):
        best = max(best, expand(i, i), expand(i, i + 1), key=len)   # odd and even centers
    return best


def is_valid_parentheses(s):
    pairs = {")": "(", "]": "[", "}": "{"}
    stack = []
    for ch in s:
        if ch in pairs:
            if not stack or stack.pop() != pairs[ch]:
                return False
        else:
            stack.append(ch)
    return not stack


def longest_unique_substring(s):
    last_seen, left, best = {}, 0, 0
    for right, ch in enumerate(s):
        if ch in last_seen and last_seen[ch] >= left:
            left = last_seen[ch] + 1              # jump past the previous occurrence
        last_seen[ch] = right
        best = max(best, right - left + 1)
    return best


def compress(s):
    return "".join(f"{ch}{len(list(group))}" for ch, group in groupby(s))   # groupby = runs of equal chars


def count_vowels(s):
    return sum(ch in "aeiou" for ch in s.lower())


def is_rotation(a, b):
    return len(a) == len(b) and b in a + a        # TRICK: any rotation of a is a substring of a+a


def remove_duplicate_chars(s):
    return "".join(dict.fromkeys(s))              # dict preserves insertion order


def is_pangram(s):
    return set("abcdefghijklmnopqrstuvwxyz") <= set(s.lower())


def capitalize_words(s):
    return " ".join(w[:1].upper() + w[1:].lower() for w in s.split())   # "it's".title() -> "It'S" (wrong)


def word_frequencies(text, top=None):
    counts = Counter(re.findall(r"[a-z']+", text.lower()))
    return counts.most_common(top)


def most_common_word(text, banned=()):
    banned = {w.lower() for w in banned}
    counts = Counter(w for w in re.findall(r"[a-z]+", text.lower()) if w not in banned)
    return counts.most_common(1)[0][0]


def string_to_int(s):                             # simplified atoi
    match = re.match(r"\s*([+-]?\d+)", s)
    return max(-2**31, min(2**31 - 1, int(match.group(1)))) if match else 0


if __name__ == "__main__":
    check("reverse_string", reverse_string("hello"), "olleh")
    check("reverse_words", reverse_words("  Python is   fun "), "fun is Python")
    check("is_palindrome", is_palindrome("A man, a plan, a canal: Panama"), True)
    check("is_anagram", is_anagram("Listen", "Silent"), True)
    check("group_anagrams", group_anagrams(["eat", "tea", "tan", "ate", "nat", "bat"]),
          [["eat", "tea", "ate"], ["tan", "nat"], ["bat"]])
    check("first_unique_char", first_unique_char("swiss"), "w")
    check("first_repeating_char", first_repeating_char("swiss"), "s")
    check("char_frequency", char_frequency("aabc"), {"a": 2, "b": 1, "c": 1})
    check("longest_common_prefix", longest_common_prefix(["flower", "flow", "flight"]), "fl")
    check("longest_common_prefix none", longest_common_prefix(["dog", "racecar"]), "")
    check("shortest_unique_prefixes", shortest_unique_prefixes(["zebra", "dog", "duck", "dove"]),
          {"zebra": "z", "dog": "dog", "duck": "du", "dove": "dov"})
    check("longest_palindrome", longest_palindrome("forgeeksskeegfor"), "geeksskeeg")
    check("is_valid_parentheses", [is_valid_parentheses(s) for s in ("()[]{}", "([)]", "(")], [True, False, False])
    check("longest_unique_substring", longest_unique_substring("abcabcbb"), 3)
    check("compress", compress("aaabbc"), "a3b2c1")
    check("count_vowels", count_vowels("Hello World"), 3)
    check("is_rotation", is_rotation("waterbottle", "erbottlewat"), True)
    check("remove_duplicate_chars", remove_duplicate_chars("programming"), "progamin")
    check("is_pangram", is_pangram("The quick brown fox jumps over the lazy dog"), True)
    check("capitalize_words", capitalize_words("it's a WARM day"), "It's A Warm Day")
    check("word_frequencies", word_frequencies("the cat the hat the end", top=2), [("the", 3), ("cat", 1)])
    check("most_common_word", most_common_word("Bob hit a ball, the hit BALL flew", banned=["hit"]), "ball")
    check("string_to_int", [string_to_int("  -42abc"), string_to_int("words"), string_to_int("99999999999")],
          [-42, 0, 2147483647])

    print("\nall checks passed")
