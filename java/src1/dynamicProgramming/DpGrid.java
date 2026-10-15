import java.util.*;

/** PATTERN: dp[r][c] from the cell above and the cell left. Roll to one row where you can. */
public class DpGrid {

    // HOOK: one row; row[c] += row[c-1] means "from above + from left".
    static int uniquePaths(int m, int n) {
        int[] row = new int[n];
        Arrays.fill(row, 1);
        for (int r = 1; r < m; r++)
            for (int c = 1; c < n; c++) row[c] += row[c - 1];
        return row[n - 1];
    }

    // HOOK: same roll, but take the cheaper of (above, left) and add the cell.
    static int minPathSum(int[][] g) {
        int n = g[0].length;
        int[] row = new int[n];
        for (int c = 1; c < n; c++) row[c] = Integer.MAX_VALUE;
        for (int[] line : g) {
            row[0] += line[0];
            for (int c = 1; c < n; c++) row[c] = Math.min(row[c], row[c - 1]) + line[c];
        }
        return row[n - 1];
    }

    // HOOK: each cell may come from up-left, up, up-right. Answer is the min of the last row.
    static int minFallingPathSum(int[][] g) {
        int n = g.length;
        int[] prev = g[0].clone();
        for (int r = 1; r < n; r++) {
            int[] cur = new int[n];
            for (int c = 0; c < n; c++) {
                int best = prev[c];
                if (c > 0) best = Math.min(best, prev[c - 1]);
                if (c < n - 1) best = Math.min(best, prev[c + 1]);
                cur[c] = g[r][c] + best;
            }
            prev = cur;
        }
        return Arrays.stream(prev).min().orElse(0);
    }

    // HOOK: walk BOTTOM-UP and the two children are always in range. No edge cases.
    static int minimumTotal(List<List<Integer>> tri) {
        int[] dp = new int[tri.size() + 1];
        for (int r = tri.size() - 1; r >= 0; r--)
            for (int c = 0; c <= r; c++)
                dp[c] = tri.get(r).get(c) + Math.min(dp[c], dp[c + 1]);
        return dp[0];
    }

    // HOOK: dp[r][c] = side of the square ENDING here = 1 + min(up, left, up-left).
    static int maximalSquare(char[][] m) {
        int rows = m.length, cols = m[0].length, best = 0;
        int[][] dp = new int[rows + 1][cols + 1];
        for (int r = 1; r <= rows; r++)
            for (int c = 1; c <= cols; c++)
                if (m[r - 1][c - 1] == '1') {
                    dp[r][c] = 1 + Math.min(dp[r - 1][c], Math.min(dp[r][c - 1], dp[r - 1][c - 1]));
                    best = Math.max(best, dp[r][c]);
                }
        return best * best;
    }

    private static final int[][] JUMPS = {{1, 2}, {2, 1}, {2, -1}, {1, -2},
            {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}};

    // HOOK: dp over MOVES - spread each cell's probability to its 8 targets, /8.
    static double knightProbability(int n, int k, int row, int col) {
        double[][] dp = new double[n][n];
        dp[row][col] = 1;
        for (int step = 0; step < k; step++) {
            double[][] next = new double[n][n];
            for (int r = 0; r < n; r++)
                for (int c = 0; c < n; c++) {
                    if (dp[r][c] == 0) continue;
                    for (int[] j : JUMPS) {
                        int nr = r + j[0], nc = c + j[1];
                        if (nr >= 0 && nr < n && nc >= 0 && nc < n) next[nr][nc] += dp[r][c] / 8;
                    }
                }
            dp = next;
        }
        return Arrays.stream(dp).flatMapToDouble(Arrays::stream).sum();
    }

    public static void main(String[] args) {
        System.out.println("uniquePaths 3x7   = " + uniquePaths(3, 7));
        System.out.println("minPathSum        = " + minPathSum(new int[][]{{1, 3, 1}, {1, 5, 1}, {4, 2, 1}}));
        System.out.println("minFallingPath    = " + minFallingPathSum(new int[][]{{2, 1, 3}, {6, 5, 4}, {7, 8, 9}}));
        System.out.println("triangle          = " + minimumTotal(List.of(
                List.of(2), List.of(3, 4), List.of(6, 5, 7), List.of(4, 1, 8, 3))));
        System.out.println("maximalSquare     = " + maximalSquare(new char[][]{
                {'1', '0', '1', '0', '0'},
                {'1', '0', '1', '1', '1'},
                {'1', '1', '1', '1', '1'},
                {'1', '0', '0', '1', '0'}}));
        System.out.println("knightProbability = " + knightProbability(3, 2, 0, 0));
    }
}
