import java.util.*;

/**
 * PATTERN: disjoint sets. Learn these 12 lines once and a whole class of
 * "are these connected / how many groups" problems becomes trivial.
 *   find  = follow parents to the root, flattening as you go
 *   union = point one root at the other; returns false if ALREADY joined
 * That false return is the cycle detector.
 */
public class UnionFindOps {

    static class DSU {
        final int[] parent, rank;
        int components;

        DSU(int n) {
            parent = new int[n];
            rank = new int[n];
            components = n;
            for (int i = 0; i < n; i++) parent[i] = i;
        }

        int find(int x) {
            if (parent[x] != x) parent[x] = find(parent[x]);       // path compression
            return parent[x];
        }

        boolean union(int a, int b) {
            int ra = find(a), rb = find(b);
            if (ra == rb) return false;                            // already together -> cycle
            if (rank[ra] < rank[rb]) { int t = ra; ra = rb; rb = t; }
            parent[rb] = ra;
            if (rank[ra] == rank[rb]) rank[ra]++;
            components--;
            return true;
        }

        boolean connected(int a, int b) { return find(a) == find(b); }
    }

    // HOOK: union every edge; the leftover component count is the answer.
    static int countComponents(int n, int[][] edges) {
        DSU dsu = new DSU(n);
        for (int[] e : edges) dsu.union(e[0], e[1]);
        return dsu.components;
    }

    // HOOK: same thing on an adjacency MATRIX instead of an edge list.
    static int findCircleNum(int[][] isConnected) {
        int n = isConnected.length;
        DSU dsu = new DSU(n);
        for (int i = 0; i < n; i++)
            for (int j = i + 1; j < n; j++)
                if (isConnected[i][j] == 1) dsu.union(i, j);
        return dsu.components;
    }

    // HOOK: a tree has exactly n-1 edges AND no cycle. union() returning false = cycle.
    static boolean validTree(int n, int[][] edges) {
        if (edges.length != n - 1) return false;
        DSU dsu = new DSU(n);
        for (int[] e : edges) if (!dsu.union(e[0], e[1])) return false;
        return true;
    }

    // HOOK: sort by time, union, and report the moment components drops to 1.
    static int earliestAcq(int[][] logs, int n) {
        Arrays.sort(logs, Comparator.comparingInt(l -> l[0]));
        DSU dsu = new DSU(n);
        for (int[] log : logs) {
            dsu.union(log[1], log[2]);
            if (dsu.components == 1) return log[0];
        }
        return -1;
    }

    // HOOK: flatten the grid - cell (r,c) becomes index r*cols + c.
    static List<Integer> numIslands2(int rows, int cols, int[][] positions) {
        DSU dsu = new DSU(rows * cols);
        boolean[] land = new boolean[rows * cols];
        int islands = 0;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        List<Integer> res = new ArrayList<>();
        for (int[] p : positions) {
            int id = p[0] * cols + p[1];
            if (!land[id]) {
                land[id] = true;
                islands++;                                         // new island, maybe merged below
                for (int[] d : dirs) {
                    int nr = p[0] + d[0], nc = p[1] + d[1];
                    if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue;
                    int nid = nr * cols + nc;
                    if (land[nid] && dsu.union(id, nid)) islands--;
                }
            }
            res.add(islands);
        }
        return res;
    }

    public static void main(String[] args) {
        System.out.println("countComponents   = " + countComponents(5, new int[][]{{0, 1}, {1, 2}, {3, 4}}));
        System.out.println("numberOfProvinces = " + findCircleNum(new int[][]{{1, 1, 0}, {1, 1, 0}, {0, 0, 1}}));
        System.out.println("validTree         = " + validTree(5, new int[][]{{0, 1}, {0, 2}, {0, 3}, {1, 4}}));
        System.out.println("validTree cycle   = " + validTree(4, new int[][]{{0, 1}, {1, 2}, {2, 3}, {1, 3}}));
        System.out.println("earliestAcq       = " + earliestAcq(new int[][]{
                {20190101, 0, 1}, {20190104, 3, 4}, {20190107, 2, 3},
                {20190211, 1, 5}, {20190224, 2, 4}, {20190301, 0, 3},
                {20190312, 1, 2}, {20190322, 4, 5}}, 6));
        System.out.println("numIslands2       = " + numIslands2(3, 3,
                new int[][]{{0, 0}, {0, 1}, {1, 2}, {2, 1}}));
    }
}
