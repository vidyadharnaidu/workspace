import java.util.*;

/**
 * PATTERN: queue + "snapshot the size at the top of each loop".
 *   int size = q.size();  for (int i = 0; i < size; i++) { ... }
 * That one line is what separates one level from the next. Everything here reuses it.
 */
public class TreeBfs {

    static class TreeNode {
        int val;
        TreeNode left, right;
        TreeNode(int v) { val = v; }
    }

    // HOOK: the template. Snapshot size, drain exactly that many, enqueue children.
    static List<List<Integer>> levelOrder(TreeNode root) {
        List<List<Integer>> res = new ArrayList<>();
        if (root == null) return res;
        Queue<TreeNode> q = new LinkedList<>();
        q.add(root);
        while (!q.isEmpty()) {
            int size = q.size();
            List<Integer> level = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                TreeNode n = q.poll();
                level.add(n.val);
                if (n.left != null) q.add(n.left);
                if (n.right != null) q.add(n.right);
            }
            res.add(level);
        }
        return res;
    }

    // HOOK: same template; just reverse every odd level on the way out.
    static List<List<Integer>> zigzagLevelOrder(TreeNode root) {
        List<List<Integer>> res = levelOrder(root);
        for (int i = 1; i < res.size(); i += 2) Collections.reverse(res.get(i));
        return res;
    }

    // HOOK: same template, but the QUEUE carries a column index; collect into a TreeMap.
    static List<List<Integer>> verticalOrder(TreeNode root) {
        List<List<Integer>> res = new ArrayList<>();
        if (root == null) return res;
        Map<Integer, List<Integer>> cols = new TreeMap<>();
        Queue<TreeNode> q = new LinkedList<>();
        Queue<Integer> qc = new LinkedList<>();
        q.add(root);
        qc.add(0);
        while (!q.isEmpty()) {
            TreeNode n = q.poll();
            int c = qc.poll();
            cols.computeIfAbsent(c, k -> new ArrayList<>()).add(n.val);
            if (n.left != null) { q.add(n.left); qc.add(c - 1); }
            if (n.right != null) { q.add(n.right); qc.add(c + 1); }
        }
        return new ArrayList<>(cols.values());
    }

    // HOOK: same template; reverse the VALUES of odd levels in place.
    static TreeNode reverseOddLevels(TreeNode root) {
        List<TreeNode> level = new ArrayList<>(List.of(root));
        int depth = 0;
        while (!level.isEmpty()) {
            if (depth % 2 == 1) {
                for (int i = 0, j = level.size() - 1; i < j; i++, j--) {
                    int t = level.get(i).val;
                    level.get(i).val = level.get(j).val;
                    level.get(j).val = t;
                }
            }
            List<TreeNode> next = new ArrayList<>();
            for (TreeNode n : level) {
                if (n.left != null) next.add(n.left);
                if (n.right != null) next.add(n.right);
            }
            level = next;
            depth++;
        }
        return root;
    }

    // HOOK: cousins = same depth, different parent. Track both while walking levels.
    static boolean isCousins(TreeNode root, int x, int y) {
        Queue<TreeNode> q = new LinkedList<>();
        q.add(root);
        while (!q.isEmpty()) {
            int size = q.size();
            boolean foundX = false, foundY = false;
            for (int i = 0; i < size; i++) {
                TreeNode n = q.poll();
                if (n.val == x) foundX = true;
                if (n.val == y) foundY = true;
                if (n.left != null && n.right != null) {          // siblings are NOT cousins
                    boolean pair = (n.left.val == x && n.right.val == y)
                                || (n.left.val == y && n.right.val == x);
                    if (pair) return false;
                }
                if (n.left != null) q.add(n.left);
                if (n.right != null) q.add(n.right);
            }
            if (foundX && foundY) return true;
            if (foundX || foundY) return false;                   // only one at this depth
        }
        return false;
    }

    // HOOK: infection spreads to PARENTS too -> build a parent map, then plain BFS.
    static int amountOfTime(TreeNode root, int start) {
        Map<Integer, List<Integer>> adj = new HashMap<>();
        link(root, null, adj);
        Set<Integer> seen = new HashSet<>(Set.of(start));
        Queue<Integer> q = new LinkedList<>(List.of(start));
        int minutes = -1;
        while (!q.isEmpty()) {
            int size = q.size();
            for (int i = 0; i < size; i++)
                for (int nb : adj.getOrDefault(q.poll(), List.of()))
                    if (seen.add(nb)) q.add(nb);
            minutes++;
        }
        return minutes;
    }

    // Turn the tree into an undirected graph: every edge goes both ways.
    static void link(TreeNode n, TreeNode parent, Map<Integer, List<Integer>> adj) {
        if (n == null) return;
        adj.computeIfAbsent(n.val, k -> new ArrayList<>());
        if (parent != null) {
            adj.get(n.val).add(parent.val);
            adj.get(parent.val).add(n.val);
        }
        link(n.left, n, adj);
        link(n.right, n, adj);
    }

    static TreeNode build(Integer... v) {
        if (v.length == 0 || v[0] == null) return null;
        TreeNode root = new TreeNode(v[0]);
        Queue<TreeNode> q = new LinkedList<>();
        q.add(root);
        int i = 1;
        while (i < v.length) {
            TreeNode n = q.poll();
            if (i < v.length && v[i] != null) { n.left = new TreeNode(v[i]); q.add(n.left); }
            i++;
            if (i < v.length && v[i] != null) { n.right = new TreeNode(v[i]); q.add(n.right); }
            i++;
        }
        return root;
    }

    static List<Integer> flatten(TreeNode root) {
        List<Integer> out = new ArrayList<>();
        for (List<Integer> lvl : levelOrder(root)) out.addAll(lvl);
        return out;
    }

    public static void main(String[] args) {
        TreeNode t = build(3, 9, 20, null, null, 15, 7);
        System.out.println("levelOrder        = " + levelOrder(t));
        System.out.println("zigzag            = " + zigzagLevelOrder(t));
        System.out.println("verticalOrder     = " + verticalOrder(build(3, 9, 20, null, null, 15, 7)));
        System.out.println("reverseOddLevels  = " + flatten(reverseOddLevels(build(2, 3, 5, 8, 13, 21, 34))));
        System.out.println("cousins(4,5) true = " + isCousins(build(1, 2, 3, null, 4, null, 5), 4, 5));
        System.out.println("siblings(2,3) no  = " + isCousins(build(1, 2, 3, null, 4, null, 5), 2, 3));
        System.out.println("diffDepth(4,3) no = " + isCousins(build(1, 2, 3, 4), 4, 3));
        System.out.println("amountOfTime      = " + amountOfTime(build(1, 5, 3, null, 4, 10, 6, 9, 2), 3));
    }
}
