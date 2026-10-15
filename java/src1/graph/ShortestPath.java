import java.util.*;

/**
 * PATTERN: pick by edge weight.
 *   all edges weight 1  -> plain BFS, first arrival IS the shortest
 *   weighted edges      -> Dijkstra: the same BFS with a PriorityQueue
 * Both keep a dist[] (or a seen set) so a node is finalised only once.
 */
public class ShortestPath {

    // HOOK: BFS level count. Works ONLY because every edge costs the same.
    static int shortestPathUnweighted(int n, int[][] edges, int start, int target) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) { adj.get(e[0]).add(e[1]); adj.get(e[1]).add(e[0]); }

        boolean[] seen = new boolean[n];
        Queue<Integer> q = new LinkedList<>(List.of(start));
        seen[start] = true;
        int steps = 0;
        while (!q.isEmpty()) {
            int size = q.size();
            for (int i = 0; i < size; i++) {
                int cur = q.poll();
                if (cur == target) return steps;
                for (int nb : adj.get(cur)) if (!seen[nb]) { seen[nb] = true; q.add(nb); }
            }
            steps++;
        }
        return -1;
    }

    // HOOK: Dijkstra - min-heap of {dist, node}; skip any entry worse than dist[node].
    static int[] dijkstra(int n, int[][] edges, int src) {
        List<List<int[]>> adj = new ArrayList<>();                // {neighbour, weight}
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) adj.get(e[0]).add(new int[]{e[1], e[2]});

        int[] dist = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[src] = 0;
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
        pq.add(new int[]{0, src});
        while (!pq.isEmpty()) {
            int[] top = pq.poll();
            if (top[0] > dist[top[1]]) continue;                  // stale entry
            for (int[] e : adj.get(top[1])) {
                int nd = top[0] + e[1];
                if (nd < dist[e[0]]) { dist[e[0]] = nd; pq.add(new int[]{nd, e[0]}); }
            }
        }
        return dist;
    }

    // HOOK: Dijkstra from the source; the answer is the LARGEST finite distance.
    static int networkDelayTime(int[][] times, int n, int k) {
        int[][] edges = new int[times.length][3];
        for (int i = 0; i < times.length; i++)
            edges[i] = new int[]{times[i][0] - 1, times[i][1] - 1, times[i][2]};
        int[] dist = dijkstra(n, edges, k - 1);
        int max = 0;
        for (int d : dist) {
            if (d == Integer.MAX_VALUE) return -1;                // unreachable node
            max = Math.max(max, d);
        }
        return max;
    }

    // HOOK: BFS over STRINGS - the neighbours are the 8 single-digit turns.
    static int openLock(String[] deadends, String target) {
        Set<String> dead = new HashSet<>(Arrays.asList(deadends));
        if (dead.contains("0000")) return -1;
        Set<String> seen = new HashSet<>(Set.of("0000"));
        Queue<String> q = new LinkedList<>(List.of("0000"));
        int turns = 0;
        while (!q.isEmpty()) {
            int size = q.size();
            for (int i = 0; i < size; i++) {
                String cur = q.poll();
                if (cur.equals(target)) return turns;
                for (int j = 0; j < 4; j++)
                    for (int delta : new int[]{1, 9}) {           // +1 and -1 (mod 10)
                        char[] c = cur.toCharArray();
                        c[j] = (char) ('0' + (c[j] - '0' + delta) % 10);
                        String next = new String(c);
                        if (!dead.contains(next) && seen.add(next)) q.add(next);
                    }
            }
            turns++;
        }
        return -1;
    }

    // HOOK: BFS where neighbours are "words differing by one letter".
    static int ladderLength(String begin, String end, List<String> wordList) {
        Set<String> dict = new HashSet<>(wordList);
        if (!dict.contains(end)) return 0;
        Queue<String> q = new LinkedList<>(List.of(begin));
        Set<String> seen = new HashSet<>(Set.of(begin));
        int steps = 1;
        while (!q.isEmpty()) {
            int size = q.size();
            for (int i = 0; i < size; i++) {
                String cur = q.poll();
                if (cur.equals(end)) return steps;
                char[] c = cur.toCharArray();
                for (int j = 0; j < c.length; j++) {
                    char old = c[j];
                    for (char ch = 'a'; ch <= 'z'; ch++) {
                        c[j] = ch;
                        String next = new String(c);
                        if (dict.contains(next) && seen.add(next)) q.add(next);
                    }
                    c[j] = old;
                }
            }
            steps++;
        }
        return 0;
    }

    // HOOK: 8-directional BFS on a grid; count CELLS visited, not edges.
    static int shortestPathBinaryMatrix(int[][] g) {
        int n = g.length;
        if (g[0][0] == 1 || g[n - 1][n - 1] == 1) return -1;
        Queue<int[]> q = new LinkedList<>(List.of(new int[]{0, 0}));
        g[0][0] = 1;                                              // mark visited
        int len = 1;
        while (!q.isEmpty()) {
            int size = q.size();
            for (int i = 0; i < size; i++) {
                int[] cell = q.poll();
                if (cell[0] == n - 1 && cell[1] == n - 1) return len;
                for (int dr = -1; dr <= 1; dr++)
                    for (int dc = -1; dc <= 1; dc++) {
                        int nr = cell[0] + dr, nc = cell[1] + dc;
                        if (nr < 0 || nr >= n || nc < 0 || nc >= n || g[nr][nc] != 0) continue;
                        g[nr][nc] = 1;
                        q.add(new int[]{nr, nc});
                    }
            }
            len++;
        }
        return -1;
    }

    public static void main(String[] args) {
        System.out.println("BFS shortest      = " + shortestPathUnweighted(
                6, new int[][]{{0, 1}, {1, 2}, {2, 5}, {0, 3}, {3, 4}, {4, 5}}, 0, 5));
        System.out.println("dijkstra dists    = " + Arrays.toString(dijkstra(
                4, new int[][]{{0, 1, 1}, {1, 2, 2}, {0, 2, 5}, {2, 3, 1}}, 0)));
        System.out.println("networkDelayTime  = " + networkDelayTime(
                new int[][]{{2, 1, 1}, {2, 3, 1}, {3, 4, 1}}, 4, 2));
        System.out.println("openLock          = " + openLock(
                new String[]{"0201", "0101", "0102", "1212", "2002"}, "0202"));
        System.out.println("wordLadder        = " + ladderLength("hit", "cog",
                List.of("hot", "dot", "dog", "lot", "log", "cog")));
        System.out.println("binaryMatrixPath  = " + shortestPathBinaryMatrix(
                new int[][]{{0, 0, 0}, {1, 1, 0}, {1, 1, 0}}));
    }
}
