"""
Trie (prefix tree): insert, search, startswith, autocomplete, shortest unique prefix, word search II.

Run: python python/07_trees_tries/trie.py

REMEMBER:
  * Node = dict of children + an is_word flag (+ a count of words passing through, for prefix counts).
  * Everything is "walk the prefix character by character".
  * Use it for autocomplete, spell check, IP routing, and word games; a set() only answers exact matches.
"""
from collections import defaultdict


def check(label, actual, expected):
    assert actual == expected, f"{label}: expected {expected!r}, got {actual!r}"
    print(f"{label:<28} {actual!r}")


class TrieNode:
    __slots__ = ("children", "is_word", "count")

    def __init__(self):
        self.children = {}
        self.is_word = False
        self.count = 0          # how many inserted words pass through this node


class Trie:
    def __init__(self, words=()):
        self.root = TrieNode()
        for word in words:
            self.insert(word)

    def insert(self, word):
        node = self.root
        for ch in word:
            node = node.children.setdefault(ch, TrieNode())
            node.count += 1
        node.is_word = True

    def _walk(self, prefix):
        node = self.root
        for ch in prefix:
            node = node.children.get(ch)
            if node is None:
                return None
        return node

    def search(self, word):
        node = self._walk(word)
        return bool(node and node.is_word)

    def starts_with(self, prefix):
        return self._walk(prefix) is not None

    def count_with_prefix(self, prefix):
        node = self._walk(prefix)
        return node.count if node else 0

    def autocomplete(self, prefix, limit=None):
        node, results = self._walk(prefix), []

        def collect(n, path):
            if limit is not None and len(results) >= limit:
                return
            if n.is_word:
                results.append(path)
            for ch, child in sorted(n.children.items()):
                collect(child, path + ch)

        if node:
            collect(node, prefix)
        return results

    def shortest_unique_prefix(self, word):
        node, prefix = self.root, ""
        for ch in word:
            node = node.children[ch]
            prefix += ch
            if node.count == 1:          # only this word passes here
                break
        return prefix

    def delete(self, word):
        def remove(node, index):
            if index == len(word):
                if not node.is_word:
                    return False
                node.is_word = False
                return not node.children          # tell the parent to drop this node if it's now a leaf
            ch = word[index]
            child = node.children.get(ch)
            if not child or not remove(child, index + 1):
                return False
            del node.children[ch]
            return not node.children and not node.is_word

        return remove(self.root, 0)


def word_search_ii(board, words):
    """Find all words from the list that exist in the grid (classic Trie + DFS)."""
    trie = Trie(words)
    rows, cols, found = len(board), len(board[0]), set()

    def dfs(r, c, node, path):
        ch = board[r][c]
        child = node.children.get(ch)
        if not child:
            return
        if child.is_word:
            found.add(path + ch)
        board[r][c] = "#"                          # mark visited
        for dr, dc in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nr, nc = r + dr, c + dc
            if 0 <= nr < rows and 0 <= nc < cols and board[nr][nc] != "#":
                dfs(nr, nc, child, path + ch)
        board[r][c] = ch                           # restore

    for r in range(rows):
        for c in range(cols):
            dfs(r, c, trie.root, "")
    return sorted(found)


def longest_common_prefix_trie(words):
    trie = Trie(words)
    node, prefix = trie.root, ""
    while len(node.children) == 1 and not node.is_word:
        ch, node = next(iter(node.children.items()))
        prefix += ch
    return prefix


if __name__ == "__main__":
    trie = Trie(["apple", "app", "application", "apt", "banana", "band"])
    check("search 'app' / 'appl'", (trie.search("app"), trie.search("appl")), (True, False))
    check("starts_with 'appl'", trie.starts_with("appl"), True)
    check("count_with_prefix 'ap'", trie.count_with_prefix("ap"), 4)
    check("autocomplete 'app'", trie.autocomplete("app"), ["app", "apple", "application"])
    check("autocomplete 'ban'", trie.autocomplete("ban"), ["banana", "band"])
    check("autocomplete limit", trie.autocomplete("a", limit=2), ["app", "apple"])

    prefixes = Trie(["zebra", "dog", "duck", "dove"])
    check("shortest_unique_prefix", {w: prefixes.shortest_unique_prefix(w) for w in ["zebra", "dog", "duck", "dove"]},
          {"zebra": "z", "dog": "dog", "duck": "du", "dove": "dov"})

    trie.delete("app")
    check("after delete('app')", (trie.search("app"), trie.search("apple")), (False, True))

    board = [list("oaan"), list("etae"), list("ihkr"), list("iflv")]
    check("word_search_ii", word_search_ii(board, ["oath", "pea", "eat", "rain"]), ["eat", "oath"])
    check("longest_common_prefix_trie", longest_common_prefix_trie(["flower", "flow", "flight"]), "fl")

    print("\nall checks passed")
