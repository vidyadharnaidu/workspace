package backtracking;

import java.util.*;
import java.util.stream.*;

/**
 * Backtracking problems.
 *
 * TEMPLATE - "CHOOSE, EXPLORE, UN-CHOOSE":
 *
 *     void backtrack(path, start) {
 *         if (goal reached) { result.add(new ArrayList<>(path)); return; }   // COPY the path!
 *         for (choice from start) {
 *             if (!valid) continue;
 *             path.add(choice);          // choose
 *             backtrack(path, next);     // explore
 *             path.remove(last);         // un-choose
 *         }
 *     }
 *
 * WHICH LOOP START?
 *   Subsets / combinations -> loop from "start", recurse with i + 1   (order doesn't matter)
 *   Combination sum (reuse) -> recurse with i                        (same element again)
 *   Permutations            -> loop from 0, skip used[i]              (order matters)
 *   Duplicates in input     -> SORT, skip if i > start && a[i] == a[i-1]
 */
public class Backtracking {

    // 1. SUBSETS: [1,2,3] -> [[], [1], [1,2], [1,2,3], [1,3], [2], [2,3], [3]]
    static List<List<Integer>> subsets(int[] nums) {
        List<List<Integer>> res = new ArrayList<>();
        subsets(nums, 0, new ArrayList<>(), res);
        return res;
    }

    static void subsets(int[] nums, int start, List<Integer> path, List<List<Integer>> res) {
        res.add(new ArrayList<>(path)); // every node is an answer
        for (int i = start; i < nums.length; i++) {
            path.add(nums[i]);
            subsets(nums, i + 1, path, res);
            path.remove(path.size() - 1);
        }
    }

    // 1b. SUBSETS with streams + BITMASK: number 0..2^n-1, bit i set -> take nums[i]
    static List<List<Integer>> subsetsBitmask(int[] nums) {
        return IntStream.range(0, 1 << nums.length)
                .mapToObj(mask -> IntStream.range(0, nums.length)
                        .filter(i -> (mask >> i & 1) == 1)
                        .mapToObj(i -> nums[i])
                        .toList())
                .toList();
    }

    // 2. SUBSETS WITH DUPLICATES: [1,2,2] -> [[], [1], [1,2], [1,2,2], [2], [2,2]]
    static List<List<Integer>> subsetsWithDup(int[] nums) {
        int[] sorted = nums.clone();
        Arrays.sort(sorted);
        List<List<Integer>> res = new ArrayList<>();
        subsetsWithDup(sorted, 0, new ArrayList<>(), res);
        return res;
    }

    static void subsetsWithDup(int[] nums, int start, List<Integer> path, List<List<Integer>> res) {
        res.add(new ArrayList<>(path));
        for (int i = start; i < nums.length; i++) {
            if (i > start && nums[i] == nums[i - 1]) continue; // skip duplicate at same level
            path.add(nums[i]);
            subsetsWithDup(nums, i + 1, path, res);
            path.remove(path.size() - 1);
        }
    }

    // 3. PERMUTATIONS: [1,2,3] -> 6 lists
    static List<List<Integer>> permutations(int[] nums) {
        List<List<Integer>> res = new ArrayList<>();
        permute(nums, new boolean[nums.length], new ArrayList<>(), res);
        return res;
    }

    static void permute(int[] nums, boolean[] used, List<Integer> path, List<List<Integer>> res) {
        if (path.size() == nums.length) {
            res.add(new ArrayList<>(path));
            return;
        }
        for (int i = 0; i < nums.length; i++) {
            if (used[i]) continue;
            used[i] = true;
            path.add(nums[i]);
            permute(nums, used, path, res);
            path.remove(path.size() - 1);
            used[i] = false;
        }
    }

    // 3b. STRING PERMUTATIONS with streams: pick each char as first + permutations of the rest
    static List<String> permutations(String s) {
        if (s.isEmpty()) return List.of("");
        return IntStream.range(0, s.length()).boxed()
                .flatMap(i -> permutations(s.substring(0, i) + s.substring(i + 1)).stream()
                        .map(rest -> s.charAt(i) + rest))
                .toList();
    }

    // 4. COMBINATIONS: n=4, k=2 -> [[1,2],[1,3],[1,4],[2,3],[2,4],[3,4]]
    static List<List<Integer>> combine(int n, int k) {
        List<List<Integer>> res = new ArrayList<>();
        combine(n, k, 1, new ArrayList<>(), res);
        return res;
    }

    static void combine(int n, int k, int start, List<Integer> path, List<List<Integer>> res) {
        if (path.size() == k) {
            res.add(new ArrayList<>(path));
            return;
        }
        for (int i = start; i <= n; i++) {
            path.add(i);
            combine(n, k, i + 1, path, res);
            path.remove(path.size() - 1);
        }
    }

    // 5. COMBINATION SUM (reuse allowed): [2,3,6,7], 7 -> [[2,2,3],[7]]
    // TRICK: recurse with i (not i+1) to allow reuse; sort so we can BREAK early
    static List<List<Integer>> combinationSum(int[] candidates, int target) {
        int[] sorted = candidates.clone();
        Arrays.sort(sorted);
        List<List<Integer>> res = new ArrayList<>();
        combinationSum(sorted, target, 0, new ArrayList<>(), res);
        return res;
    }

    static void combinationSum(int[] c, int remain, int start, List<Integer> path, List<List<Integer>> res) {
        if (remain == 0) {
            res.add(new ArrayList<>(path));
            return;
        }
        for (int i = start; i < c.length && c[i] <= remain; i++) {
            path.add(c[i]);
            combinationSum(c, remain - c[i], i, path, res);
            path.remove(path.size() - 1);
        }
    }

    // 6. LETTER COMBINATIONS OF A PHONE NUMBER: "23" -> [ad, ae, af, bd, be, bf, cd, ce, cf]
    static final String[] KEYS = {"", "", "abc", "def", "ghi", "jkl", "mno", "pqrs", "tuv", "wxyz"};

    static List<String> letterCombinations(String digits) {
        List<String> res = new ArrayList<>();
        if (!digits.isEmpty()) letters(digits, 0, new StringBuilder(), res);
        return res;
    }

    static void letters(String digits, int i, StringBuilder path, List<String> res) {
        if (i == digits.length()) {
            res.add(path.toString());
            return;
        }
        for (char c : KEYS[digits.charAt(i) - '0'].toCharArray()) {
            path.append(c);
            letters(digits, i + 1, path, res);
            path.deleteCharAt(path.length() - 1);
        }
    }

    // 7. GENERATE PARENTHESES: n=3 -> [((())), (()()), (())(), ()(()), ()()()]
    // TRICK: add "(" if open < n; add ")" if close < open
    static List<String> generateParentheses(int n) {
        List<String> res = new ArrayList<>();
        parens(n, 0, 0, "", res);
        return res;
    }

    static void parens(int n, int open, int close, String path, List<String> res) {
        if (path.length() == 2 * n) {
            res.add(path);
            return;
        }
        if (open < n) parens(n, open + 1, close, path + "(", res);   // Strings are immutable -> no un-choose needed
        if (close < open) parens(n, open, close + 1, path + ")", res);
    }

    // 8. N-QUEENS: n=4 -> 2 solutions
    // TRICK: one queen per ROW; track used columns, diagonals (r - c) and anti-diagonals (r + c)
    static List<List<String>> solveNQueens(int n) {
        List<List<String>> res = new ArrayList<>();
        placeQueen(n, 0, new int[n], new HashSet<>(), new HashSet<>(), new HashSet<>(), res);
        return res;
    }

    static void placeQueen(int n, int row, int[] queenCol, Set<Integer> cols, Set<Integer> diag, Set<Integer> anti,
                           List<List<String>> res) {
        if (row == n) {
            res.add(Arrays.stream(queenCol)
                    .mapToObj(c -> ".".repeat(c) + "Q" + ".".repeat(n - c - 1))
                    .toList());
            return;
        }
        for (int c = 0; c < n; c++) {
            if (cols.contains(c) || diag.contains(row - c) || anti.contains(row + c)) continue;
            queenCol[row] = c;
            cols.add(c); diag.add(row - c); anti.add(row + c);
            placeQueen(n, row + 1, queenCol, cols, diag, anti, res);
            cols.remove(c); diag.remove(row - c); anti.remove(row + c);
        }
    }

    // 9. WORD SEARCH in grid: TRICK mark cell as '#' while visiting, restore after
    static boolean exist(char[][] board, String word) {
        for (int r = 0; r < board.length; r++)
            for (int c = 0; c < board[0].length; c++)
                if (search(board, word, 0, r, c)) return true;
        return false;
    }

    static boolean search(char[][] b, String word, int i, int r, int c) {
        if (i == word.length()) return true;
        if (r < 0 || c < 0 || r >= b.length || c >= b[0].length || b[r][c] != word.charAt(i)) return false;
        char saved = b[r][c];
        b[r][c] = '#';
        boolean found = search(b, word, i + 1, r + 1, c) || search(b, word, i + 1, r - 1, c)
                || search(b, word, i + 1, r, c + 1) || search(b, word, i + 1, r, c - 1);
        b[r][c] = saved;
        return found;
    }

    // 10. PALINDROME PARTITIONING: "aab" -> [[a, a, b], [aa, b]]
    // TRICK: try every prefix; if it's a palindrome, cut it and recurse on the rest
    static List<List<String>> partition(String s) {
        List<List<String>> res = new ArrayList<>();
        partition(s, 0, new ArrayList<>(), res);
        return res;
    }

    static void partition(String s, int start, List<String> path, List<List<String>> res) {
        if (start == s.length()) {
            res.add(new ArrayList<>(path));
            return;
        }
        for (int end = start + 1; end <= s.length(); end++) {
            String piece = s.substring(start, end);
            if (!new StringBuilder(piece).reverse().toString().equals(piece)) continue;
            path.add(piece);
            partition(s, end, path, res);
            path.remove(path.size() - 1);
        }
    }

    // 11. SUDOKU SOLVER: find empty cell, try 1-9, recurse, undo on failure
    static boolean solveSudoku(char[][] b) {
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++) {
                if (b[r][c] != '.') continue;
                for (char d = '1'; d <= '9'; d++) {
                    if (canPlace(b, r, c, d)) {
                        b[r][c] = d;
                        if (solveSudoku(b)) return true;
                        b[r][c] = '.';
                    }
                }
                return false; // nothing fits here -> backtrack
            }
        return true; // no empty cells left
    }

    static boolean canPlace(char[][] b, int r, int c, char d) {
        for (int i = 0; i < 9; i++) {
            if (b[r][i] == d || b[i][c] == d) return false;
            if (b[3 * (r / 3) + i / 3][3 * (c / 3) + i % 3] == d) return false; // 3x3 box
        }
        return true;
    }

    public static void main(String[] args) {
        System.out.println("subsets          " + subsets(new int[]{1, 2, 3}));
        System.out.println("subsets bitmask  " + subsetsBitmask(new int[]{1, 2, 3}));
        System.out.println("subsetsWithDup   " + subsetsWithDup(new int[]{1, 2, 2}));
        System.out.println("permutations     " + permutations(new int[]{1, 2, 3}));
        System.out.println("perm string      " + permutations("abc"));
        System.out.println("combine(4,2)     " + combine(4, 2));
        System.out.println("combinationSum   " + combinationSum(new int[]{2, 3, 6, 7}, 7));
        System.out.println("letterCombos     " + letterCombinations("23"));
        System.out.println("parentheses(3)   " + generateParentheses(3));

        List<List<String>> queens = solveNQueens(4);
        System.out.println("nQueens(4)       " + queens.size() + " solutions, first:");
        queens.get(0).forEach(row -> System.out.println("                 " + row));

        char[][] board = {"ABCE".toCharArray(), "SFCS".toCharArray(), "ADEE".toCharArray()};
        System.out.println("wordSearch       ABCCED=" + exist(board, "ABCCED") + " SEE=" + exist(board, "SEE") + " ABCB=" + exist(board, "ABCB"));
        System.out.println("palindromeParts  " + partition("aab"));

        char[][] sudoku = Stream.of(
                "53..7....", "6..195...", ".98....6.",
                "8...6...3", "4..8.3..1", "7...2...6",
                ".6....28.", "...419..5", "....8..79").map(String::toCharArray).toArray(char[][]::new);
        System.out.println("sudoku solved    " + solveSudoku(sudoku));
        Arrays.stream(sudoku).forEach(row -> System.out.println("                 " + new String(row)));
    }
}
