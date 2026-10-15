import java.util.*;

/**
 * PATTERN: recurse left, recurse right, combine. Two shapes cover almost everything:
 *   1. RETURN a value up the tree      (depth, diameter, LCA)
 *   2. PASS state down the tree        (path sum, good nodes)
 * When a node's answer and its parent's answer differ, return one and keep the other
 * in a field - that is the diameter / maxPathSum trick.
 */
public class TreeDfs {

    static class TreeNode {
        int val;
        TreeNode left, right;
        TreeNode(int v) { val = v; }
    }

    // HOOK: depth = 1 + max(left, right). The template every other method copies.
    static int maxDepth(TreeNode root) {
        return root == null ? 0 : 1 + Math.max(maxDepth(root.left), maxDepth(root.right));
    }

    // HOOK: swap the children, then recurse.
    static TreeNode invertTree(TreeNode root) {
        if (root == null) return null;
        TreeNode t = root.left;
        root.left = invertTree(root.right);
        root.right = invertTree(t);
        return root;
    }

    // HOOK: both null -> true; one null -> false; else values match and children match.
    static boolean isSameTree(TreeNode p, TreeNode q) {
        if (p == null || q == null) return p == q;
        return p.val == q.val && isSameTree(p.left, q.left) && isSameTree(p.right, q.right);
    }

    // HOOK: isSameTree at THIS node, or the subtree sits somewhere below.
    static boolean isSubtree(TreeNode root, TreeNode sub) {
        if (root == null) return sub == null;
        return isSameTree(root, sub) || isSubtree(root.left, sub) || isSubtree(root.right, sub);
    }

    // HOOK: mirror check - compare left.left with right.right, left.right with right.left.
    static boolean isSymmetric(TreeNode root) {
        return root == null || mirror(root.left, root.right);
    }

    static boolean mirror(TreeNode a, TreeNode b) {
        if (a == null || b == null) return a == b;
        return a.val == b.val && mirror(a.left, b.right) && mirror(a.right, b.left);
    }

    static int diameter;

    // HOOK: RETURN depth upward, but RECORD left+right at each node in a field.
    static int diameterOfBinaryTree(TreeNode root) {
        diameter = 0;
        depthFor(root);
        return diameter;
    }

    static int depthFor(TreeNode n) {
        if (n == null) return 0;
        int l = depthFor(n.left), r = depthFor(n.right);
        diameter = Math.max(diameter, l + r);                     // path THROUGH this node
        return 1 + Math.max(l, r);                                // path ENDING at this node
    }

    static int best;

    // HOOK: same split - return one-sided gain upward, record the two-sided sum.
    static int maxPathSum(TreeNode root) {
        best = Integer.MIN_VALUE;
        gain(root);
        return best;
    }

    static int gain(TreeNode n) {
        if (n == null) return 0;
        int l = Math.max(0, gain(n.left));                        // negative branches are dropped
        int r = Math.max(0, gain(n.right));
        best = Math.max(best, n.val + l + r);
        return n.val + Math.max(l, r);
    }

    // HOOK: PASS the remaining sum down; at a leaf check it hit zero.
    static boolean hasPathSum(TreeNode root, int target) {
        if (root == null) return false;
        if (root.left == null && root.right == null) return target == root.val;
        return hasPathSum(root.left, target - root.val) || hasPathSum(root.right, target - root.val);
    }

    // HOOK: same walk + a path list; add before recursing, remove after. Backtracking.
    static List<List<Integer>> pathSum(TreeNode root, int target) {
        List<List<Integer>> res = new ArrayList<>();
        walk(root, target, new ArrayList<>(), res);
        return res;
    }

    static void walk(TreeNode n, int target, List<Integer> path, List<List<Integer>> res) {
        if (n == null) return;
        path.add(n.val);
        if (n.left == null && n.right == null && target == n.val) res.add(new ArrayList<>(path));
        walk(n.left, target - n.val, path, res);
        walk(n.right, target - n.val, path, res);
        path.remove(path.size() - 1);                             // undo
    }

    // HOOK: PASS the max seen on the path down; a node is good if it is >= that max.
    static int goodNodes(TreeNode root) {
        return countGood(root, Integer.MIN_VALUE);
    }

    static int countGood(TreeNode n, int max) {
        if (n == null) return 0;
        int self = n.val >= max ? 1 : 0;
        max = Math.max(max, n.val);
        return self + countGood(n.left, max) + countGood(n.right, max);
    }

    // HOOK: return the node if it IS p or q; the first node seeing both sides is the LCA.
    static TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
        if (root == null || root == p || root == q) return root;
        TreeNode l = lowestCommonAncestor(root.left, p, q);
        TreeNode r = lowestCommonAncestor(root.right, p, q);
        if (l != null && r != null) return root;                  // p and q split here
        return l != null ? l : r;
    }

    static int moves;

    // HOOK: each subtree returns its coin SURPLUS; |surplus| coins cross that edge.
    static int distributeCoins(TreeNode root) {
        moves = 0;
        surplus(root);
        return moves;
    }

    static int surplus(TreeNode n) {
        if (n == null) return 0;
        int l = surplus(n.left), r = surplus(n.right);
        moves += Math.abs(l) + Math.abs(r);
        return n.val + l + r - 1;                                 // this node keeps exactly 1
    }

    // Level-order builder: nulls mark absent children.
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

    public static void main(String[] args) {
        TreeNode t = build(3, 9, 20, null, null, 15, 7);
        System.out.println("maxDepth          = " + maxDepth(t));
        System.out.println("isSameTree        = " + isSameTree(build(1, 2, 3), build(1, 2, 3)));
        System.out.println("isSubtree         = " + isSubtree(build(3, 4, 5, 1, 2), build(4, 1, 2)));
        System.out.println("isSymmetric       = " + isSymmetric(build(1, 2, 2, 3, 4, 4, 3)));
        System.out.println("diameter          = " + diameterOfBinaryTree(build(1, 2, 3, 4, 5)));
        System.out.println("maxPathSum        = " + maxPathSum(build(-10, 9, 20, null, null, 15, 7)));
        System.out.println("hasPathSum 22     = " + hasPathSum(build(5, 4, 8, 11, null, 13, 4, 7, 2), 22));
        System.out.println("pathSum 22        = " + pathSum(build(5, 4, 8, 11, null, 13, 4, 7, 2, null, null, 5, 1), 22));
        System.out.println("goodNodes         = " + goodNodes(build(3, 1, 4, 3, null, 1, 5)));
        System.out.println("distributeCoins   = " + distributeCoins(build(3, 0, 0)));

        TreeNode lcaTree = build(3, 5, 1, 6, 2, 0, 8);
        System.out.println("LCA(5,1)          = " + lowestCommonAncestor(lcaTree, lcaTree.left, lcaTree.right).val);

        TreeNode inv = invertTree(build(4, 2, 7, 1, 3, 6, 9));
        System.out.println("invertTree root.l = " + inv.left.val + " root.r = " + inv.right.val);
    }
}
