package matrix;

import java.util.*;
import java.util.stream.*;

/**
 * Matrix (2D array) problems.
 *
 * TRICKS:
 *  - rows = m.length, cols = m[0].length. Print with Arrays.deepToString(m).
 *  - NEIGHBOURS: int[][] DIRS = {{1,0},{-1,0},{0,1},{0,-1}} + bounds check (8 dirs: loop dr, dc in -1..1)
 *  - ROTATE 90 clockwise = TRANSPOSE + REVERSE each row   (counter-clockwise = transpose + reverse columns)
 *  - SPIRAL = 4 boundaries top/bottom/left/right; shrink after each side
 *  - DIAGONALS: same r - c -> same "\" diagonal; same r + c -> same "/" diagonal
 *  - "Distance to nearest X" -> multi-source BFS from all X cells
 *  - In-place updates that depend on old values (game of life, set zeroes) -> use a copy or mark first, change later
 *  - Related: islands (graphs), word search / sudoku (backtracking), search 2D matrix (binarysearch),
 *    unique paths / min path sum (dp), kth smallest in matrix (heaps)
 */
public class Matrix {

    static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    // 1. ROTATE 90 CLOCKWISE in place: [[1,2,3],[4,5,6],[7,8,9]] -> [[7,4,1],[8,5,2],[9,6,3]]
    static void rotate(int[][] m) {
        int n = m.length;
        for (int i = 0; i < n; i++)
            for (int j = i + 1; j < n; j++) {
                int t = m[i][j]; m[i][j] = m[j][i]; m[j][i] = t;          // transpose
            }
        for (int[] row : m)
            for (int l = 0, r = n - 1; l < r; l++, r--) {
                int t = row[l]; row[l] = row[r]; row[r] = t;              // reverse row
            }
    }

    // 2. TRANSPOSE (any shape) with streams: [[1,2,3],[4,5,6]] -> [[1,4],[2,5],[3,6]]
    static int[][] transpose(int[][] m) {
        return IntStream.range(0, m[0].length)
                .mapToObj(c -> IntStream.range(0, m.length).map(r -> m[r][c]).toArray())
                .toArray(int[][]::new);
    }

    // 3. SPIRAL ORDER: [[1,2,3],[4,5,6],[7,8,9]] -> [1,2,3,6,9,8,7,4,5]
    static List<Integer> spiralOrder(int[][] m) {
        List<Integer> res = new ArrayList<>();
        int top = 0, bottom = m.length - 1, left = 0, right = m[0].length - 1;
        while (top <= bottom && left <= right) {
            for (int c = left; c <= right; c++) res.add(m[top][c]);
            top++;
            for (int r = top; r <= bottom; r++) res.add(m[r][right]);
            right--;
            if (top <= bottom) {
                for (int c = right; c >= left; c--) res.add(m[bottom][c]);
                bottom--;
            }
            if (left <= right) {
                for (int r = bottom; r >= top; r--) res.add(m[r][left]);
                left++;
            }
        }
        return res;
    }

    // 4. GENERATE SPIRAL MATRIX: n=3 -> [[1,2,3],[8,9,4],[7,6,5]]
    static int[][] generateSpiral(int n) {
        int[][] m = new int[n][n];
        int top = 0, bottom = n - 1, left = 0, right = n - 1, k = 1;
        while (top <= bottom && left <= right) {
            for (int c = left; c <= right; c++) m[top][c] = k++;
            top++;
            for (int r = top; r <= bottom; r++) m[r][right] = k++;
            right--;
            for (int c = right; c >= left && top <= bottom; c--) m[bottom][c] = k++;
            bottom--;
            for (int r = bottom; r >= top && left <= right; r--) m[r][left] = k++;
            left++;
        }
        return m;
    }

    // 5. SET MATRIX ZEROES: [[1,1,1],[1,0,1],[1,1,1]] -> [[1,0,1],[0,0,0],[1,0,1]]
    // TRICK: first REMEMBER which rows/cols have a zero, THEN write zeros (else zeros spread)
    static void setZeroes(int[][] m) {
        Set<Integer> rows = new HashSet<>(), cols = new HashSet<>();
        for (int r = 0; r < m.length; r++)
            for (int c = 0; c < m[0].length; c++)
                if (m[r][c] == 0) { rows.add(r); cols.add(c); }
        for (int r = 0; r < m.length; r++)
            for (int c = 0; c < m[0].length; c++)
                if (rows.contains(r) || cols.contains(c)) m[r][c] = 0;
    }

    // 6. DIAGONAL SUM (both diagonals, center counted once): [[1,2,3],[4,5,6],[7,8,9]] -> 25
    static int diagonalSum(int[][] m) {
        int n = m.length;
        return IntStream.range(0, n).map(i -> m[i][i] + (i != n - 1 - i ? m[i][n - 1 - i] : 0)).sum();
    }

    // 7. DIAGONAL (ZIGZAG) TRAVERSE: [[1,2,3],[4,5,6],[7,8,9]] -> [1,2,4,7,5,3,6,8,9]
    // TRICK: cells with the same r + c are on one diagonal; reverse every even diagonal
    static List<Integer> diagonalTraverse(int[][] m) {
        int rows = m.length, cols = m[0].length;
        return IntStream.range(0, rows + cols - 1)
                .mapToObj(d -> {
                    List<Integer> diag = IntStream.rangeClosed(Math.max(0, d - cols + 1), Math.min(d, rows - 1))
                            .mapToObj(r -> m[r][d - r])
                            .collect(Collectors.toList());
                    if (d % 2 == 0) Collections.reverse(diag);
                    return diag;
                })
                .flatMap(List::stream)
                .toList();
    }

    // 8. VALID SUDOKU (partially filled)
    // TRICK: one Set<String>; add "5 in row 0", "5 in col 3", "5 in box 0-1" -> add returns false = duplicate
    static boolean isValidSudoku(char[][] board) {
        Set<String> seen = new HashSet<>();
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++) {
                char d = board[r][c];
                if (d == '.') continue;
                if (!seen.add(d + " in row " + r) || !seen.add(d + " in col " + c)
                        || !seen.add(d + " in box " + r / 3 + "-" + c / 3)) return false;
            }
        return true;
    }

    // 9. GAME OF LIFE (next state): live cell survives with 2-3 neighbours; dead cell born with exactly 3
    static int[][] gameOfLife(int[][] board) {
        int rows = board.length, cols = board[0].length;
        int[][] next = new int[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                int live = 0;
                for (int dr = -1; dr <= 1; dr++)
                    for (int dc = -1; dc <= 1; dc++) {
                        int nr = r + dr, nc = c + dc;
                        if ((dr != 0 || dc != 0) && nr >= 0 && nc >= 0 && nr < rows && nc < cols) live += board[nr][nc];
                    }
                next[r][c] = live == 3 || (live == 2 && board[r][c] == 1) ? 1 : 0;
            }
        return next;
    }

    // 10. FLOOD FILL (paint bucket): DFS to same-colored neighbours
    static int[][] floodFill(int[][] img, int sr, int sc, int color) {
        if (img[sr][sc] != color) fill(img, sr, sc, img[sr][sc], color);
        return img;
    }

    static void fill(int[][] img, int r, int c, int from, int to) {
        if (r < 0 || c < 0 || r >= img.length || c >= img[0].length || img[r][c] != from) return;
        img[r][c] = to;
        for (int[] d : DIRS) fill(img, r + d[0], c + d[1], from, to);
    }

    // 11. DISTANCE TO NEAREST 0 (01 matrix): [[0,0,0],[0,1,0],[1,1,1]] -> [[0,0,0],[0,1,0],[1,2,1]]
    // TRICK: multi-source BFS starting from ALL zeros at once
    static int[][] nearestZero(int[][] m) {
        int rows = m.length, cols = m[0].length;
        int[][] dist = new int[rows][cols];
        Deque<int[]> queue = new ArrayDeque<>();
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                if (m[r][c] == 0) queue.add(new int[]{r, c});
                else dist[r][c] = -1; // not visited yet
            }
        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            for (int[] d : DIRS) {
                int r = cell[0] + d[0], c = cell[1] + d[1];
                if (r >= 0 && c >= 0 && r < rows && c < cols && dist[r][c] == -1) {
                    dist[r][c] = dist[cell[0]][cell[1]] + 1;
                    queue.add(new int[]{r, c});
                }
            }
        }
        return dist;
    }

    // 12. MAXIMAL SQUARE of 1s (area): DP -> side[r][c] = 1 + min(top, left, top-left)
    static int maximalSquare(char[][] m) {
        int rows = m.length, cols = m[0].length, best = 0;
        int[][] side = new int[rows + 1][cols + 1];
        for (int r = 1; r <= rows; r++)
            for (int c = 1; c <= cols; c++)
                if (m[r - 1][c - 1] == '1') {
                    side[r][c] = 1 + Math.min(side[r - 1][c - 1], Math.min(side[r - 1][c], side[r][c - 1]));
                    best = Math.max(best, side[r][c]);
                }
        return best * best;
    }

    // 13. MATRIX MULTIPLICATION: [[1,2],[3,4]] x [[5,6],[7,8]] -> [[19,22],[43,50]]
    // TRICK: result[i][j] = row i of A  dot  column j of B
    static int[][] multiply(int[][] a, int[][] b) {
        int[][] res = new int[a.length][b[0].length];
        for (int i = 0; i < a.length; i++)
            for (int j = 0; j < b[0].length; j++)
                for (int k = 0; k < b.length; k++)
                    res[i][j] += a[i][k] * b[k][j];
        return res;
    }

    // 14. PASCAL'S TRIANGLE: 5 -> [[1],[1,1],[1,2,1],[1,3,3,1],[1,4,6,4,1]]
    // TRICK: each inner value = sum of the two values above it
    static List<List<Integer>> pascal(int numRows) {
        List<List<Integer>> rows = new ArrayList<>();
        List<Integer> row = List.of(1);
        for (int i = 0; i < numRows; i++) {
            rows.add(row);
            List<Integer> prev = row;
            row = IntStream.rangeClosed(0, prev.size())
                    .mapToObj(j -> j == 0 || j == prev.size() ? 1 : prev.get(j - 1) + prev.get(j))
                    .toList();
        }
        return rows;
    }

    // 15. ROW WITH MAXIMUM ONES (streams): [[0,1,1],[1,1,1],[0,0,1]] -> row 1
    static int rowWithMaxOnes(int[][] m) {
        return IntStream.range(0, m.length).boxed()
                .max(Comparator.comparingInt(r -> Arrays.stream(m[r]).sum()))
                .orElse(-1);
    }

    public static void main(String[] args) {
        int[][] rot = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        rotate(rot);
        System.out.println("rotate           " + Arrays.deepToString(rot));                                    // [[7, 4, 1], [8, 5, 2], [9, 6, 3]]
        System.out.println("transpose        " + Arrays.deepToString(transpose(new int[][]{{1, 2, 3}, {4, 5, 6}}))); // [[1, 4], [2, 5], [3, 6]]
        System.out.println("spiralOrder      " + spiralOrder(new int[][]{{1, 2, 3}, {4, 5, 6}, {7, 8, 9}}));      // [1, 2, 3, 6, 9, 8, 7, 4, 5]
        System.out.println("spiral 3x4       " + spiralOrder(new int[][]{{1, 2, 3, 4}, {5, 6, 7, 8}, {9, 10, 11, 12}})); // [1, 2, 3, 4, 8, 12, 11, 10, 9, 5, 6, 7]
        System.out.println("generateSpiral   " + Arrays.deepToString(generateSpiral(3)));                          // [[1, 2, 3], [8, 9, 4], [7, 6, 5]]

        int[][] z = {{1, 1, 1}, {1, 0, 1}, {1, 1, 1}};
        setZeroes(z);
        System.out.println("setZeroes        " + Arrays.deepToString(z));                                      // [[1, 0, 1], [0, 0, 0], [1, 0, 1]]

        System.out.println("diagonalSum      " + diagonalSum(new int[][]{{1, 2, 3}, {4, 5, 6}, {7, 8, 9}}));      // 25
        System.out.println("diagonalTraverse " + diagonalTraverse(new int[][]{{1, 2, 3}, {4, 5, 6}, {7, 8, 9}})); // [1, 2, 4, 7, 5, 3, 6, 8, 9]

        String[] sudoku = {"53..7....", "6..195...", ".98....6.", "8...6...3", "4..8.3..1", "7...2...6", ".6....28.", "...419..5", "....8..79"};
        char[][] board = Arrays.stream(sudoku).map(String::toCharArray).toArray(char[][]::new);
        System.out.print("validSudoku      " + isValidSudoku(board));                                          // true
        board[0][0] = '8'; // column 0 already has an 8
        System.out.println(" " + isValidSudoku(board));                                                        // false

        System.out.println("gameOfLife       " + Arrays.deepToString(gameOfLife(new int[][]{{0, 1, 0}, {0, 0, 1}, {1, 1, 1}, {0, 0, 0}}))); // [[0, 0, 0], [1, 0, 1], [0, 1, 1], [0, 1, 0]]
        System.out.println("floodFill        " + Arrays.deepToString(floodFill(new int[][]{{1, 1, 1}, {1, 1, 0}, {1, 0, 1}}, 1, 1, 2))); // [[2, 2, 2], [2, 2, 0], [2, 0, 1]]
        System.out.println("nearestZero      " + Arrays.deepToString(nearestZero(new int[][]{{0, 0, 0}, {0, 1, 0}, {1, 1, 1}}))); // [[0, 0, 0], [0, 1, 0], [1, 2, 1]]

        char[][] squares = Stream.of("10100", "10111", "11111", "10010").map(String::toCharArray).toArray(char[][]::new);
        System.out.println("maximalSquare    " + maximalSquare(squares));                                      // 4
        System.out.println("multiply         " + Arrays.deepToString(multiply(new int[][]{{1, 2}, {3, 4}}, new int[][]{{5, 6}, {7, 8}}))); // [[19, 22], [43, 50]]
        System.out.println("pascal           " + pascal(5));                                                   // [[1], [1, 1], [1, 2, 1], [1, 3, 3, 1], [1, 4, 6, 4, 1]]
        System.out.println("rowWithMaxOnes   " + rowWithMaxOnes(new int[][]{{0, 1, 1}, {1, 1, 1}, {0, 0, 1}}));   // 1
    }
}
