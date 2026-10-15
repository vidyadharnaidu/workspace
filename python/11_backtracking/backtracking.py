"""
Backtracking problems.

Run: python python/11_backtracking/backtracking.py

TEMPLATE - choose, explore, un-choose:
    def backtrack(path, start):
        if goal(path):
            result.append(path[:])          # COPY the path!
            return
        for i in range(start, n):
            if not valid(i): continue
            path.append(choice)             # choose
            backtrack(path, i + 1)          # explore
            path.pop()                      # un-choose

WHICH LOOP?
  subsets / combinations -> loop from `start`, recurse with i + 1
  reuse the same element -> recurse with i
  permutations           -> loop from 0, skip used
  duplicates in input    -> sort, then skip i > start and nums[i] == nums[i-1]
Python shortcut: itertools.combinations / permutations / product solve many of these directly.
"""
from itertools import combinations, permutations, product


def check(label, actual, expected):
    assert actual == expected, f"{label}: expected {expected!r}, got {actual!r}"
    print(f"{label:<28} {actual!r}")


def subsets(nums):
    result, path = [], []

    def backtrack(start):
        result.append(path[:])              # every node is an answer
        for i in range(start, len(nums)):
            path.append(nums[i])
            backtrack(i + 1)
            path.pop()

    backtrack(0)
    return result


def subsets_itertools(nums):
    return [list(c) for r in range(len(nums) + 1) for c in combinations(nums, r)]


def subsets_with_duplicates(nums):
    nums, result, path = sorted(nums), [], []

    def backtrack(start):
        result.append(path[:])
        for i in range(start, len(nums)):
            if i > start and nums[i] == nums[i - 1]:
                continue                    # skip duplicates at the same level
            path.append(nums[i])
            backtrack(i + 1)
            path.pop()

    backtrack(0)
    return result


def permute(nums):
    result, path, used = [], [], [False] * len(nums)

    def backtrack():
        if len(path) == len(nums):
            result.append(path[:])
            return
        for i, n in enumerate(nums):
            if used[i]:
                continue
            used[i] = True
            path.append(n)
            backtrack()
            path.pop()
            used[i] = False

    backtrack()
    return result


def permute_string(s):
    return ["".join(p) for p in permutations(s)]


def combine(n, k):
    result, path = [], []

    def backtrack(start):
        if len(path) == k:
            result.append(path[:])
            return
        for i in range(start, n + 1):
            path.append(i)
            backtrack(i + 1)
            path.pop()

    backtrack(1)
    return result


def combination_sum(candidates, target):
    candidates, result, path = sorted(candidates), [], []

    def backtrack(start, remaining):
        if remaining == 0:
            result.append(path[:])
            return
        for i in range(start, len(candidates)):
            if candidates[i] > remaining:
                break                       # sorted -> everything after is too big
            path.append(candidates[i])
            backtrack(i, remaining - candidates[i])   # i (not i+1): the same number may be reused
            path.pop()

    backtrack(0, target)
    return result


KEYPAD = {"2": "abc", "3": "def", "4": "ghi", "5": "jkl", "6": "mno", "7": "pqrs", "8": "tuv", "9": "wxyz"}


def letter_combinations(digits):
    if not digits:
        return []
    return ["".join(p) for p in product(*(KEYPAD[d] for d in digits))]


def generate_parentheses(n):
    result = []

    def backtrack(current, open_count, close_count):
        if len(current) == 2 * n:
            result.append(current)
            return
        if open_count < n:
            backtrack(current + "(", open_count + 1, close_count)
        if close_count < open_count:
            backtrack(current + ")", open_count, close_count + 1)

    backtrack("", 0, 0)
    return result


def solve_n_queens(n):
    result, columns, diagonals, anti_diagonals, placement = [], set(), set(), set(), []

    def backtrack(row):
        if row == n:
            result.append(["." * c + "Q" + "." * (n - c - 1) for c in placement])
            return
        for col in range(n):
            if col in columns or row - col in diagonals or row + col in anti_diagonals:
                continue
            columns.add(col), diagonals.add(row - col), anti_diagonals.add(row + col)
            placement.append(col)
            backtrack(row + 1)
            placement.pop()
            columns.remove(col), diagonals.remove(row - col), anti_diagonals.remove(row + col)

    backtrack(0)
    return result


def word_exists(board, word):
    rows, cols = len(board), len(board[0])

    def dfs(r, c, i):
        if i == len(word):
            return True
        if not (0 <= r < rows and 0 <= c < cols) or board[r][c] != word[i]:
            return False
        board[r][c] = "#"                   # mark visited
        found = any(dfs(r + dr, c + dc, i + 1) for dr, dc in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        board[r][c] = word[i]               # restore
        return found

    return any(dfs(r, c, 0) for r in range(rows) for c in range(cols))


def palindrome_partition(s):
    result, path = [], []

    def backtrack(start):
        if start == len(s):
            result.append(path[:])
            return
        for end in range(start + 1, len(s) + 1):
            piece = s[start:end]
            if piece == piece[::-1]:
                path.append(piece)
                backtrack(end)
                path.pop()

    backtrack(0)
    return result


def solve_sudoku(board):
    """board: list of 9 lists of chars, '.' = empty. Solves in place, returns True on success."""
    def can_place(r, c, digit):
        box_r, box_c = 3 * (r // 3), 3 * (c // 3)
        return (digit not in board[r]
                and all(board[i][c] != digit for i in range(9))
                and all(digit != board[box_r + i][box_c + j] for i in range(3) for j in range(3)))

    for r in range(9):
        for c in range(9):
            if board[r][c] != ".":
                continue
            for digit in "123456789":
                if can_place(r, c, digit):
                    board[r][c] = digit
                    if solve_sudoku(board):
                        return True
                    board[r][c] = "."       # undo
            return False                    # nothing fits here -> backtrack
    return True                             # no empty cells left


if __name__ == "__main__":
    check("subsets", subsets([1, 2, 3]), [[], [1], [1, 2], [1, 2, 3], [1, 3], [2], [2, 3], [3]])
    check("subsets_itertools count", len(subsets_itertools([1, 2, 3])), 8)
    check("subsets_with_duplicates", subsets_with_duplicates([1, 2, 2]), [[], [1], [1, 2], [1, 2, 2], [2], [2, 2]])
    check("permute", permute([1, 2, 3]), [[1, 2, 3], [1, 3, 2], [2, 1, 3], [2, 3, 1], [3, 1, 2], [3, 2, 1]])
    check("permute_string", permute_string("abc"), ["abc", "acb", "bac", "bca", "cab", "cba"])
    check("combine(4, 2)", combine(4, 2), [[1, 2], [1, 3], [1, 4], [2, 3], [2, 4], [3, 4]])
    check("combination_sum", combination_sum([2, 3, 6, 7], 7), [[2, 2, 3], [7]])
    check("letter_combinations", letter_combinations("23"), ["ad", "ae", "af", "bd", "be", "bf", "cd", "ce", "cf"])
    check("generate_parentheses", generate_parentheses(3), ["((()))", "(()())", "(())()", "()(())", "()()()"])

    queens = solve_n_queens(4)
    check("solve_n_queens(4) count", len(queens), 2)
    check("solve_n_queens first", queens[0], [".Q..", "...Q", "Q...", "..Q."])

    board = [list("ABCE"), list("SFCS"), list("ADEE")]
    check("word_exists", [word_exists(board, w) for w in ("ABCCED", "SEE", "ABCB")], [True, True, False])
    check("palindrome_partition", palindrome_partition("aab"), [["a", "a", "b"], ["aa", "b"]])

    puzzle = [list(row) for row in ["53..7....", "6..195...", ".98....6.",
                                    "8...6...3", "4..8.3..1", "7...2...6",
                                    ".6....28.", "...419..5", "....8..79"]]
    check("solve_sudoku", solve_sudoku(puzzle), True)
    check("sudoku first row", "".join(puzzle[0]), "534678912")

    print("\nall checks passed")
