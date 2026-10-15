package graphs;

import java.util.*;
import java.util.stream.*;

/**
 * Graph problems.
 *
 * TRICKS:
 *  - Build:     Map<Integer, List<Integer>> + computeIfAbsent
 *  - BFS:       QUEUE + visited, mark visited when ADDING -> shortest path in unweighted graph
 *  - DFS:       RECURSION (or stack) + visited -> explore / count components / flood fill
 *  - Grid:      cells are nodes, 4 directions are edges, "sink" visited cells
 *  - Multi-source BFS: put ALL starting cells in the queue first (rotting oranges)
 *  - Topo sort: KAHN = count in-degrees, start with 0 in-degree nodes. Leftover nodes = CYCLE.
 *  - Dijkstra:  BFS with a PRIORITY QUEUE ordered by distance (no negative weights)
 *  - Union-Find: parent[] + find with path compression; union returns false -> cycle
 */
public class Graphs {

    // BUILD adjacency list from edge list
    static Map<Integer, List<Integer>> buildGraph(int[][] edges, boolean directed) {
        Map<Integer, List<Integer>> g = new TreeMap<>();
        for (int[] e : edges) {
            g.computeIfAbsent(e[0], k -> new ArrayList<>()).add(e[1]);
            g.computeIfAbsent(e[1], k -> new ArrayList<>());
            if (!directed) g.get(e[1]).add(e[0]);
        }
        return g;
    }

    // BFS
    static List<Integer> bfs(Map<Integer, List<Integer>> g, int start) {
        List<Integer> order = new ArrayList<>();
        Set<Integer> visited = new HashSet<>(Set.of(start));
        Deque<Integer> queue = new ArrayDeque<>(List.of(start));
        while (!queue.isEmpty()) {
            int node = queue.poll();
            order.add(node);
            for (int next : g.get(node))
                if (visited.add(next)) queue.offer(next);
        }
        return order;
    }

    // DFS (recursive)
    static void dfs(Map<Integer, List<Integer>> g, int node, Set<Integer> visited, List<Integer> order) {
        if (!visited.add(node)) return;
        order.add(node);
        for (int next : g.get(node)) dfs(g, next, visited, order);
    }

    // SHORTEST PATH (unweighted): BFS + parent map, then walk back from destination
    static List<Integer> shortestPath(Map<Integer, List<Integer>> g, int src, int dst) {
        Map<Integer, Integer> parent = new HashMap<>();
        parent.put(src, null);
        Deque<Integer> queue = new ArrayDeque<>(List.of(src));
        while (!queue.isEmpty()) {
            int node = queue.poll();
            if (node == dst) break;
            for (int next : g.get(node)) {
                if (!parent.containsKey(next)) {
                    parent.put(next, node);
                    queue.offer(next);
                }
            }
        }
        if (!parent.containsKey(dst)) return List.of();
        LinkedList<Integer> path = new LinkedList<>();
        for (Integer at = dst; at != null; at = parent.get(at)) path.addFirst(at);
        return path;
    }

    // NUMBER OF ISLANDS: count '1' cells that start a new flood fill
    static int numIslands(char[][] grid) {
        int count = 0;
        for (int r = 0; r < grid.length; r++)
            for (int c = 0; c < grid[0].length; c++)
                if (grid[r][c] == '1') {
                    count++;
                    sink(grid, r, c);
                }
        return count;
    }

    static void sink(char[][] g, int r, int c) {
        if (r < 0 || c < 0 || r >= g.length || c >= g[0].length || g[r][c] != '1') return;
        g[r][c] = '0';
        sink(g, r + 1, c);
        sink(g, r - 1, c);
        sink(g, r, c + 1);
        sink(g, r, c - 1);
    }

    // ROTTING ORANGES: multi-source BFS, one "level" per minute. -1 if some stay fresh.
    static int orangesRotting(int[][] grid) {
        Deque<int[]> queue = new ArrayDeque<>();
        int fresh = 0;
        for (int r = 0; r < grid.length; r++)
            for (int c = 0; c < grid[0].length; c++) {
                if (grid[r][c] == 2) queue.add(new int[]{r, c});
                else if (grid[r][c] == 1) fresh++;
            }

        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int minutes = 0;
        while (!queue.isEmpty() && fresh > 0) {
            for (int i = queue.size(); i > 0; i--) {
                int[] cell = queue.poll();
                for (int[] d : dirs) {
                    int r = cell[0] + d[0], c = cell[1] + d[1];
                    if (r >= 0 && c >= 0 && r < grid.length && c < grid[0].length && grid[r][c] == 1) {
                        grid[r][c] = 2;
                        fresh--;
                        queue.add(new int[]{r, c});
                    }
                }
            }
            minutes++;
        }
        return fresh == 0 ? minutes : -1;
    }

    // TOPOLOGICAL SORT (Kahn) / COURSE SCHEDULE. Edge {a, b} means a must come before b.
    // Returns empty list if there is a cycle.
    static List<Integer> topoSort(int n, int[][] edges) {
        List<List<Integer>> g = IntStream.range(0, n).<List<Integer>>mapToObj(i -> new ArrayList<>()).toList();
        int[] indegree = new int[n];
        for (int[] e : edges) {
            g.get(e[0]).add(e[1]);
            indegree[e[1]]++;
        }

        Deque<Integer> queue = IntStream.range(0, n).filter(i -> indegree[i] == 0).boxed()
                .collect(Collectors.toCollection(ArrayDeque::new));
        List<Integer> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            int node = queue.poll();
            order.add(node);
            for (int next : g.get(node))
                if (--indegree[next] == 0) queue.add(next);
        }
        return order.size() == n ? order : List.of();
    }

    static boolean canFinishCourses(int n, int[][] prerequisites) {
        return topoSort(n, prerequisites).size() == n;
    }

    // DIJKSTRA. Edge {from, to, weight}. Returns shortest distance from src to every node.
    static int[] dijkstra(int n, int[][] edges, int src) {
        List<List<int[]>> g = IntStream.range(0, n).<List<int[]>>mapToObj(i -> new ArrayList<>()).toList();
        for (int[] e : edges) g.get(e[0]).add(new int[]{e[1], e[2]});

        int[] dist = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[src] = 0;
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1])); // {node, dist}
        pq.add(new int[]{src, 0});

        while (!pq.isEmpty()) {
            int[] cur = pq.poll();
            int node = cur[0], d = cur[1];
            if (d > dist[node]) continue; // stale entry
            for (int[] edge : g.get(node)) {
                int next = edge[0], nd = d + edge[1];
                if (nd < dist[next]) {
                    dist[next] = nd;
                    pq.add(new int[]{next, nd});
                }
            }
        }
        return dist;
    }

    // UNION-FIND: connected components + cycle detection in undirected graph
    static class UnionFind {
        final int[] parent;
        int components;

        UnionFind(int n) {
            parent = IntStream.range(0, n).toArray();
            components = n;
        }

        int find(int x) {
            return parent[x] == x ? x : (parent[x] = find(parent[x]));
        }

        boolean union(int a, int b) {
            int ra = find(a), rb = find(b);
            if (ra == rb) return false; // already connected -> this edge makes a CYCLE
            parent[ra] = rb;
            components--;
            return true;
        }
    }

    // BIPARTITE (2-coloring): BFS, give neighbours the opposite color; same color -> not bipartite
    static boolean isBipartite(int[][] adj) {
        int[] color = new int[adj.length]; // 0 = none, 1 / -1 = colors
        for (int start = 0; start < adj.length; start++) {
            if (color[start] != 0) continue;
            color[start] = 1;
            Deque<Integer> queue = new ArrayDeque<>(List.of(start));
            while (!queue.isEmpty()) {
                int node = queue.poll();
                for (int next : adj[node]) {
                    if (color[next] == color[node]) return false;
                    if (color[next] == 0) {
                        color[next] = -color[node];
                        queue.add(next);
                    }
                }
            }
        }
        return true;
    }

    public static void main(String[] args) {
        //  0 - 1
        //  |   |
        //  2 - 3 - 4
        Map<Integer, List<Integer>> g = buildGraph(new int[][]{{0, 1}, {0, 2}, {1, 3}, {2, 3}, {3, 4}}, false);
        System.out.println("graph          " + g);
        System.out.println("bfs            " + bfs(g, 0));                        // [0, 1, 2, 3, 4]
        List<Integer> dfsOrder = new ArrayList<>();
        dfs(g, 0, new HashSet<>(), dfsOrder);
        System.out.println("dfs            " + dfsOrder);                         // [0, 1, 3, 2, 4]
        System.out.println("shortestPath   " + shortestPath(g, 0, 4));            // [0, 1, 3, 4]

        char[][] grid = {
                "11000".toCharArray(),
                "11000".toCharArray(),
                "00100".toCharArray(),
                "00011".toCharArray()};
        System.out.println("numIslands     " + numIslands(grid));                 // 3

        System.out.println("orangesRotting " + orangesRotting(new int[][]{{2, 1, 1}, {1, 1, 0}, {0, 1, 1}})); // 4

        System.out.println("topoSort       " + topoSort(6, new int[][]{{5, 2}, {5, 0}, {4, 0}, {4, 1}, {2, 3}, {3, 1}})); // [4, 5, 2, 0, 3, 1]
        System.out.println("canFinish      " + canFinishCourses(2, new int[][]{{0, 1}}));          // true
        System.out.println("canFinish cyc  " + canFinishCourses(2, new int[][]{{0, 1}, {1, 0}}));  // false

        int[][] weighted = {{0, 1, 4}, {0, 2, 1}, {2, 1, 2}, {1, 3, 1}, {2, 3, 5}, {3, 4, 3}};
        System.out.println("dijkstra       " + Arrays.toString(dijkstra(5, weighted, 0)));        // [0, 3, 1, 4, 7]

        UnionFind uf = new UnionFind(5);
        uf.union(0, 1);
        uf.union(1, 2);
        uf.union(3, 4);
        System.out.println("components     " + uf.components);                    // 2
        System.out.println("edge 2-0 cycle " + !uf.union(2, 0));                  // true

        System.out.println("bipartite      " + isBipartite(new int[][]{{1, 3}, {0, 2}, {1, 3}, {0, 2}}));        // true
        System.out.println("bipartite tri  " + isBipartite(new int[][]{{1, 2}, {0, 2}, {0, 1}}));                // false
    }
}
