import java.util.*;

/**
 * PATTERN: the grid IS the graph - neighbours are the 4 cells around you.
 * Every problem here is the same DFS with a different thing counted:
 *   bounds check -> valid cell check -> mark visited -> recurse 4 ways.
 * Marking the cell itself (flip '1' to '0') beats a separate visited[][].
 */
public class GridTraversal {

    static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    // HOOK: every unvisited '1' starts one island; sink the whole island, count 1.
    static int numIslands(char[][] g) {
        int count = 0;
        for (int r = 0; r < g.length; r++)
            for (int c = 0; c < g[0].length; c++)
                if (g[r][c] == '1') { sink(g, r, c); count++; }
        return count;
    }

    static void sink(char[][] g, int r, int c) {
        if (r < 0 || r >= g.length || c < 0 || c >= g[0].length || g[r][c] != '1') return;
        g[r][c] = '0';                                            // mark visited in place
        for (int[] d : DIRS) sink(g, r + d[0], c + d[1]);
    }

    // HOOK: same sink, but RETURN the size instead of counting islands.
    static int maxAreaOfIsland(int[][] g) {
        int best = 0;
        for (int r = 0; r < g.length; r++)
            for (int c = 0; c < g[0].length; c++) best = Math.max(best, area(g, r, c));
        return best;
    }

    static int area(int[][] g, int r, int c) {
        if (r < 0 || r >= g.length || c < 0 || c >= g[0].length || g[r][c] != 1) return 0;
        g[r][c] = 0;
        int sum = 1;
        for (int[] d : DIRS) sum += area(g, r + d[0], c + d[1]);
        return sum;
    }

    // HOOK: an island touching the border is NOT closed - return false up the recursion.
    static int closedIsland(int[][] g) {
        int count = 0;
        for (int r = 0; r < g.length; r++)
            for (int c = 0; c < g[0].length; c++)
                if (g[r][c] == 0 && closed(g, r, c)) count++;
        return count;
    }

    static boolean closed(int[][] g, int r, int c) {
        if (r < 0 || r >= g.length || c < 0 || c >= g[0].length) return false;  // hit the border
        if (g[r][c] == 1) return true;                            // water: this direction is fine
        g[r][c] = 1;
        boolean ok = true;
        for (int[] d : DIRS) ok &= closed(g, r + d[0], c + d[1]); // no short-circuit: sink it all
        return ok;
    }

    // HOOK: every land cell has 4 sides; subtract one for each land neighbour.
    static int islandPerimeter(int[][] g) {
        int per = 0;
        for (int r = 0; r < g.length; r++)
            for (int c = 0; c < g[0].length; c++) {
                if (g[r][c] != 1) continue;
                per += 4;
                if (r > 0 && g[r - 1][c] == 1) per -= 2;          // shared edge, both sides
                if (c > 0 && g[r][c - 1] == 1) per -= 2;
            }
        return per;
    }

    // HOOK: flood from the BORDER to mark the survivors, then flip everything else.
    static void surroundedRegions(char[][] b) {
        int rows = b.length, cols = b[0].length;
        for (int r = 0; r < rows; r++) { mark(b, r, 0); mark(b, r, cols - 1); }
        for (int c = 0; c < cols; c++) { mark(b, 0, c); mark(b, rows - 1, c); }
        for (char[] row : b)
            for (int c = 0; c < cols; c++) row[c] = row[c] == 'S' ? 'O' : 'X';
    }

    static void mark(char[][] b, int r, int c) {
        if (r < 0 || r >= b.length || c < 0 || c >= b[0].length || b[r][c] != 'O') return;
        b[r][c] = 'S';                                            // S = safe, reaches the border
        for (int[] d : DIRS) mark(b, r + d[0], c + d[1]);
    }

    // HOOK: flood fill - repaint while the colour matches the ORIGINAL colour.
    static int[][] floodFill(int[][] img, int sr, int sc, int colour) {
        if (img[sr][sc] != colour) fill(img, sr, sc, img[sr][sc], colour);
        return img;
    }

    static void fill(int[][] img, int r, int c, int from, int to) {
        if (r < 0 || r >= img.length || c < 0 || c >= img[0].length || img[r][c] != from) return;
        img[r][c] = to;
        for (int[] d : DIRS) fill(img, r + d[0], c + d[1], from, to);
    }

    // HOOK: a sub-island fails if ANY of its cells is water in grid1.
    static int countSubIslands(int[][] g1, int[][] g2) {
        int count = 0;
        for (int r = 0; r < g2.length; r++)
            for (int c = 0; c < g2[0].length; c++)
                if (g2[r][c] == 1 && sub(g1, g2, r, c)) count++;
        return count;
    }

    static boolean sub(int[][] g1, int[][] g2, int r, int c) {
        if (r < 0 || r >= g2.length || c < 0 || c >= g2[0].length || g2[r][c] == 0) return true;
        g2[r][c] = 0;
        boolean ok = g1[r][c] == 1;
        for (int[] d : DIRS) ok &= sub(g1, g2, r + d[0], c + d[1]);
        return ok;
    }

    // HOOK: BFS from ALL rotten oranges at once (multi-source); answer = levels - 1.
    static int orangesRotting(int[][] g) {
        Queue<int[]> q = new LinkedList<>();
        int fresh = 0;
        for (int r = 0; r < g.length; r++)
            for (int c = 0; c < g[0].length; c++) {
                if (g[r][c] == 2) q.add(new int[]{r, c});
                if (g[r][c] == 1) fresh++;
            }
        if (fresh == 0) return 0;
        int minutes = -1;
        while (!q.isEmpty()) {
            int size = q.size();
            for (int i = 0; i < size; i++) {
                int[] cell = q.poll();
                for (int[] d : DIRS) {
                    int nr = cell[0] + d[0], nc = cell[1] + d[1];
                    if (nr < 0 || nr >= g.length || nc < 0 || nc >= g[0].length) continue;
                    if (g[nr][nc] != 1) continue;
                    g[nr][nc] = 2;
                    fresh--;
                    q.add(new int[]{nr, nc});
                }
            }
            minutes++;
        }
        return fresh == 0 ? minutes : -1;
    }

    public static void main(String[] args) {
        System.out.println("numIslands        = " + numIslands(new char[][]{
                {'1', '1', '0', '0'}, {'1', '1', '0', '0'}, {'0', '0', '1', '0'}, {'0', '0', '0', '1'}}));
        System.out.println("maxAreaOfIsland   = " + maxAreaOfIsland(new int[][]{
                {1, 1, 0, 0}, {1, 1, 0, 0}, {0, 0, 1, 1}, {0, 0, 0, 1}}));
        System.out.println("closedIsland      = " + closedIsland(new int[][]{
                {1, 1, 1, 1, 1, 1, 1, 0}, {1, 0, 0, 0, 0, 1, 1, 0},
                {1, 0, 1, 0, 1, 1, 1, 0}, {1, 0, 0, 0, 0, 1, 0, 1}, {1, 1, 1, 1, 1, 1, 1, 0}}));
        System.out.println("islandPerimeter   = " + islandPerimeter(new int[][]{
                {0, 1, 0, 0}, {1, 1, 1, 0}, {0, 1, 0, 0}, {1, 1, 0, 0}}));

        char[][] board = {{'X', 'X', 'X', 'X'}, {'X', 'O', 'O', 'X'},
                          {'X', 'X', 'O', 'X'}, {'X', 'O', 'X', 'X'}};
        surroundedRegions(board);
        System.out.println("surroundedRegions = " + Arrays.deepToString(board));

        System.out.println("floodFill         = " + Arrays.deepToString(
                floodFill(new int[][]{{1, 1, 1}, {1, 1, 0}, {1, 0, 1}}, 1, 1, 2)));
        System.out.println("countSubIslands   = " + countSubIslands(
                new int[][]{{1, 1, 1, 0, 0}, {0, 1, 1, 1, 1}, {0, 0, 0, 0, 0}, {1, 0, 0, 0, 0}, {1, 1, 0, 1, 1}},
                new int[][]{{1, 1, 1, 0, 0}, {0, 0, 1, 1, 1}, {0, 1, 0, 0, 0}, {1, 0, 1, 1, 0}, {0, 1, 0, 1, 0}}));
        System.out.println("orangesRotting    = " + orangesRotting(new int[][]{
                {2, 1, 1}, {1, 1, 0}, {0, 1, 1}}));
    }
}
