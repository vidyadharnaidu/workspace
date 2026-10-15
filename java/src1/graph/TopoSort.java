import java.util.*;

/**
 * PATTERN: Kahn's BFS. Build an adjacency list plus an indegree count,
 * queue every node with indegree 0, and pop-then-decrement.
 * If you emit fewer than n nodes, the graph had a CYCLE. That single fact
 * answers "can finish?", "give me an order", and "is it a valid DAG?".
 */
public class TopoSort {

    // HOOK: Kahn's algorithm. Returns the order, or an EMPTY array if a cycle exists.
    static int[] topologicalOrder(int n, int[][] edges) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        int[] indegree = new int[n];
        for (int[] e : edges) {                                   // e = {from, to}
            adj.get(e[0]).add(e[1]);
            indegree[e[1]]++;
        }
        Queue<Integer> q = new LinkedList<>();
        for (int i = 0; i < n; i++) if (indegree[i] == 0) q.add(i);

        int[] order = new int[n];
        int k = 0;
        while (!q.isEmpty()) {
            int cur = q.poll();
            order[k++] = cur;
            for (int nb : adj.get(cur)) if (--indegree[nb] == 0) q.add(nb);
        }
        return k == n ? order : new int[0];                       // short == cycle
    }

    // HOOK: "can finish" is just "the topological order exists".
    static boolean canFinish(int numCourses, int[][] prerequisites) {
        return findOrder(numCourses, prerequisites).length == numCourses;
    }

    // HOOK: prerequisites are given as {course, needs} -> the edge runs needs -> course.
    static int[] findOrder(int numCourses, int[][] prerequisites) {
        int[][] edges = new int[prerequisites.length][2];
        for (int i = 0; i < prerequisites.length; i++)
            edges[i] = new int[]{prerequisites[i][1], prerequisites[i][0]};
        return topologicalOrder(numCourses, edges);
    }

    // HOOK: compare adjacent words for the FIRST differing char -> one edge. Then topo sort.
    static String alienOrder(String[] words) {
        Map<Character, Set<Character>> adj = new HashMap<>();
        Map<Character, Integer> indegree = new HashMap<>();
        for (String w : words)
            for (char c : w.toCharArray()) {
                adj.putIfAbsent(c, new HashSet<>());
                indegree.putIfAbsent(c, 0);
            }
        for (int i = 0; i + 1 < words.length; i++) {
            String a = words[i], b = words[i + 1];
            if (a.length() > b.length() && a.startsWith(b)) return "";   // "abc" before "ab"
            for (int j = 0; j < Math.min(a.length(), b.length()); j++) {
                char x = a.charAt(j), y = b.charAt(j);
                if (x == y) continue;
                if (adj.get(x).add(y)) indegree.merge(y, 1, Integer::sum);
                break;                                            // only the FIRST difference
            }
        }
        Queue<Character> q = new LinkedList<>();
        indegree.forEach((c, d) -> { if (d == 0) q.add(c); });
        StringBuilder sb = new StringBuilder();
        while (!q.isEmpty()) {
            char c = q.poll();
            sb.append(c);
            for (char nb : adj.get(c)) if (indegree.merge(nb, -1, Integer::sum) == 0) q.add(nb);
        }
        return sb.length() == indegree.size() ? sb.toString() : "";
    }

    // HOOK: DFS cycle check with 3 colours - 0 unseen, 1 ON THE STACK, 2 done.
    static boolean hasCycleDirected(int n, int[][] edges) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) adj.get(e[0]).add(e[1]);
        int[] state = new int[n];
        for (int i = 0; i < n; i++) if (state[i] == 0 && cyclic(i, adj, state)) return true;
        return false;
    }

    static boolean cyclic(int cur, List<List<Integer>> adj, int[] state) {
        state[cur] = 1;                                           // grey: on the current path
        for (int nb : adj.get(cur)) {
            if (state[nb] == 1) return true;                      // back edge -> cycle
            if (state[nb] == 0 && cyclic(nb, adj, state)) return true;
        }
        state[cur] = 2;                                           // black: fully explored
        return false;
    }

    public static void main(String[] args) {
        System.out.println("topoOrder         = " + Arrays.toString(
                topologicalOrder(4, new int[][]{{0, 1}, {0, 2}, {1, 3}, {2, 3}})));
        System.out.println("topo with cycle   = " + Arrays.toString(
                topologicalOrder(2, new int[][]{{0, 1}, {1, 0}})));
        System.out.println("canFinish         = " + canFinish(2, new int[][]{{1, 0}}));
        System.out.println("canFinish cycle   = " + canFinish(2, new int[][]{{1, 0}, {0, 1}}));
        System.out.println("courseOrder       = " + Arrays.toString(
                findOrder(4, new int[][]{{1, 0}, {2, 0}, {3, 1}, {3, 2}})));
        System.out.println("alienOrder        = " + alienOrder(new String[]{"wrt", "wrf", "er", "ett", "rftt"}));
        System.out.println("alienOrder invalid= '" + alienOrder(new String[]{"abc", "ab"}) + "'");
        System.out.println("hasCycleDirected  = " + hasCycleDirected(3, new int[][]{{0, 1}, {1, 2}, {2, 0}}));
        System.out.println("hasCycle none     = " + hasCycleDirected(3, new int[][]{{0, 1}, {1, 2}}));
    }
}
