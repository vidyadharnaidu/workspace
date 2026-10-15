package trees;

import java.util.*;
import java.util.stream.*;

/**
 * Binary tree & BST problems.
 *
 * TRICKS:
 *  - Almost every tree problem = "solve for left, solve for right, combine at root" (recursion).
 *  - Level-by-level (level order, right view, zigzag) = BFS QUEUE, loop q.size() times per level.
 *  - Traversal order names say WHERE the root goes: PRE = root first, IN = root middle, POST = root last.
 *  - BST inorder = SORTED. (kth smallest, validate, etc.)
 *  - Validate BST = pass (min, max) bounds down, NOT just compare with children.
 *  - "Answer is not what recursion returns" (diameter, max path) = return height, update a global best.
 */
public class BinaryTrees {

    static class TreeNode {
        int val;
        TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    // Build tree from LeetCode-style level order: [3, 9, 20, null, null, 15, 7]
    static TreeNode build(Integer... values) {
        if (values.length == 0 || values[0] == null) return null;
        TreeNode root = new TreeNode(values[0]);
        Deque<TreeNode> queue = new ArrayDeque<>(List.of(root));
        for (int i = 1; i < values.length; i += 2) {
            TreeNode node = queue.poll();
            if (values[i] != null) queue.add(node.left = new TreeNode(values[i]));
            if (i + 1 < values.length && values[i + 1] != null) queue.add(node.right = new TreeNode(values[i + 1]));
        }
        return root;
    }

    // ---------- Traversals ----------
    static void preorder(TreeNode n, List<Integer> out)  { if (n == null) return; out.add(n.val); preorder(n.left, out); preorder(n.right, out); }
    static void inorder(TreeNode n, List<Integer> out)   { if (n == null) return; inorder(n.left, out); out.add(n.val); inorder(n.right, out); }
    static void postorder(TreeNode n, List<Integer> out) { if (n == null) return; postorder(n.left, out); postorder(n.right, out); out.add(n.val); }

    // Iterative inorder. TRICK: go LEFT as far as possible, pop & visit, then go RIGHT once.
    static List<Integer> inorderIterative(TreeNode root) {
        List<Integer> out = new ArrayList<>();
        Deque<TreeNode> stack = new ArrayDeque<>();
        TreeNode cur = root;
        while (cur != null || !stack.isEmpty()) {
            while (cur != null) {
                stack.push(cur);
                cur = cur.left;
            }
            cur = stack.pop();
            out.add(cur.val);
            cur = cur.right;
        }
        return out;
    }

    // Level order (BFS)
    static List<List<Integer>> levelOrder(TreeNode root) {
        List<List<Integer>> res = new ArrayList<>();
        if (root == null) return res;
        Deque<TreeNode> queue = new ArrayDeque<>(List.of(root));
        while (!queue.isEmpty()) {
            List<Integer> level = new ArrayList<>();
            for (int i = queue.size(); i > 0; i--) {
                TreeNode n = queue.poll();
                level.add(n.val);
                if (n.left != null) queue.add(n.left);
                if (n.right != null) queue.add(n.right);
            }
            res.add(level);
        }
        return res;
    }

    // Zigzag = level order, reverse the odd levels
    static List<List<Integer>> zigzag(TreeNode root) {
        List<List<Integer>> levels = levelOrder(root);
        return IntStream.range(0, levels.size())
                .mapToObj(i -> {
                    List<Integer> level = new ArrayList<>(levels.get(i));
                    if (i % 2 == 1) Collections.reverse(level);
                    return level;
                })
                .toList();
    }

    // Right side view = last node of each level
    static List<Integer> rightSideView(TreeNode root) {
        return levelOrder(root).stream().map(level -> level.get(level.size() - 1)).toList();
    }

    // ---------- Recursive "left + right + combine" ----------
    static int maxDepth(TreeNode n) {
        return n == null ? 0 : 1 + Math.max(maxDepth(n.left), maxDepth(n.right));
    }

    // Careful: a node with ONE child is not a leaf
    static int minDepth(TreeNode n) {
        if (n == null) return 0;
        if (n.left == null) return 1 + minDepth(n.right);
        if (n.right == null) return 1 + minDepth(n.left);
        return 1 + Math.min(minDepth(n.left), minDepth(n.right));
    }

    static int countNodes(TreeNode n) {
        return n == null ? 0 : 1 + countNodes(n.left) + countNodes(n.right);
    }

    static TreeNode invert(TreeNode n) {
        if (n == null) return null;
        TreeNode tmp = n.left;
        n.left = invert(n.right);
        n.right = invert(tmp);
        return n;
    }

    static boolean isSameTree(TreeNode a, TreeNode b) {
        if (a == null || b == null) return a == b;
        return a.val == b.val && isSameTree(a.left, b.left) && isSameTree(a.right, b.right);
    }

    // Symmetric = left subtree is a MIRROR of right subtree (outer pair, inner pair)
    static boolean isSymmetric(TreeNode root) {
        return root == null || isMirror(root.left, root.right);
    }

    static boolean isMirror(TreeNode a, TreeNode b) {
        if (a == null || b == null) return a == b;
        return a.val == b.val && isMirror(a.left, b.right) && isMirror(a.right, b.left);
    }

    // Balanced: TRICK return -1 as "unbalanced" signal while computing height
    static boolean isBalanced(TreeNode root) {
        return balancedHeight(root) != -1;
    }

    static int balancedHeight(TreeNode n) {
        if (n == null) return 0;
        int l = balancedHeight(n.left), r = balancedHeight(n.right);
        if (l == -1 || r == -1 || Math.abs(l - r) > 1) return -1;
        return 1 + Math.max(l, r);
    }

    // Diameter: longest path (in edges) between any two nodes = max(leftHeight + rightHeight)
    static int diameter(TreeNode root) {
        int[] best = {0};
        heightForDiameter(root, best);
        return best[0];
    }

    static int heightForDiameter(TreeNode n, int[] best) {
        if (n == null) return 0;
        int l = heightForDiameter(n.left, best), r = heightForDiameter(n.right, best);
        best[0] = Math.max(best[0], l + r);
        return 1 + Math.max(l, r);
    }

    // Root-to-leaf path sum exists?
    static boolean hasPathSum(TreeNode n, int target) {
        if (n == null) return false;
        if (n.left == null && n.right == null) return target == n.val;
        return hasPathSum(n.left, target - n.val) || hasPathSum(n.right, target - n.val);
    }

    // All root-to-leaf paths: "3->9", "3->20->15", ...
    static List<String> allPaths(TreeNode n) {
        if (n == null) return List.of();
        if (n.left == null && n.right == null) return List.of(String.valueOf(n.val));
        return Stream.concat(allPaths(n.left).stream(), allPaths(n.right).stream())
                .map(p -> n.val + "->" + p)
                .toList();
    }

    // Lowest common ancestor (any binary tree). TRICK: if p and q found on different sides -> I'm the LCA.
    static TreeNode lca(TreeNode n, int p, int q) {
        if (n == null || n.val == p || n.val == q) return n;
        TreeNode left = lca(n.left, p, q), right = lca(n.right, p, q);
        if (left != null && right != null) return n;
        return left != null ? left : right;
    }

    // ---------- BST ----------
    static boolean isValidBST(TreeNode n) {
        return isValidBST(n, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    static boolean isValidBST(TreeNode n, long min, long max) {
        if (n == null) return true;
        if (n.val <= min || n.val >= max) return false;
        return isValidBST(n.left, min, n.val) && isValidBST(n.right, n.val, max);
    }

    static boolean searchBST(TreeNode n, int target) {
        while (n != null && n.val != target) n = target < n.val ? n.left : n.right;
        return n != null;
    }

    static TreeNode insertBST(TreeNode n, int val) {
        if (n == null) return new TreeNode(val);
        if (val < n.val) n.left = insertBST(n.left, val);
        else n.right = insertBST(n.right, val);
        return n;
    }

    // LCA in BST: both smaller -> go left, both bigger -> go right, otherwise split point = answer
    static TreeNode lcaBST(TreeNode n, int p, int q) {
        while (n != null) {
            if (p < n.val && q < n.val) n = n.left;
            else if (p > n.val && q > n.val) n = n.right;
            else return n;
        }
        return null;
    }

    // Kth smallest in BST = kth element of inorder (stop early)
    static int kthSmallest(TreeNode root, int k) {
        Deque<TreeNode> stack = new ArrayDeque<>();
        TreeNode cur = root;
        while (true) {
            while (cur != null) {
                stack.push(cur);
                cur = cur.left;
            }
            cur = stack.pop();
            if (--k == 0) return cur.val;
            cur = cur.right;
        }
    }

    // Sorted array -> height-balanced BST: middle element is the root
    static TreeNode sortedArrayToBST(int[] a, int lo, int hi) {
        if (lo > hi) return null;
        int mid = (lo + hi) / 2;
        TreeNode n = new TreeNode(a[mid]);
        n.left = sortedArrayToBST(a, lo, mid - 1);
        n.right = sortedArrayToBST(a, mid + 1, hi);
        return n;
    }

    static List<Integer> traverse(TreeNode root, String order) {
        List<Integer> out = new ArrayList<>();
        switch (order) {
            case "pre" -> preorder(root, out);
            case "in" -> inorder(root, out);
            case "post" -> postorder(root, out);
        }
        return out;
    }

    public static void main(String[] args) {
        //       3
        //      / \
        //     9   20
        //        /  \
        //       15   7
        TreeNode tree = build(3, 9, 20, null, null, 15, 7);
        System.out.println("preorder     " + traverse(tree, "pre"));      // [3, 9, 20, 15, 7]
        System.out.println("inorder      " + traverse(tree, "in"));       // [9, 3, 15, 20, 7]
        System.out.println("postorder    " + traverse(tree, "post"));     // [9, 15, 7, 20, 3]
        System.out.println("inorder iter " + inorderIterative(tree));     // [9, 3, 15, 20, 7]
        System.out.println("levelOrder   " + levelOrder(tree));           // [[3], [9, 20], [15, 7]]
        System.out.println("zigzag       " + zigzag(tree));               // [[3], [20, 9], [15, 7]]
        System.out.println("rightView    " + rightSideView(tree));        // [3, 20, 7]
        System.out.println("maxDepth     " + maxDepth(tree));             // 3
        System.out.println("minDepth     " + minDepth(tree));             // 2
        System.out.println("countNodes   " + countNodes(tree));           // 5
        System.out.println("diameter     " + diameter(tree));             // 3
        System.out.println("balanced     " + isBalanced(tree));           // true
        System.out.println("hasPathSum30 " + hasPathSum(tree, 30));       // true (3+20+7)
        System.out.println("allPaths     " + allPaths(tree));             // [3->9, 3->20->15, 3->20->7]
        System.out.println("lca(15,7)    " + lca(tree, 15, 7).val);       // 20
        System.out.println("lca(9,7)     " + lca(tree, 9, 7).val);        // 3
        System.out.println("sameTree     " + isSameTree(tree, build(3, 9, 20, null, null, 15, 7))); // true
        System.out.println("symmetric    " + isSymmetric(build(1, 2, 2, 3, 4, 4, 3)));             // true
        System.out.println("inverted     " + levelOrder(invert(build(3, 9, 20, null, null, 15, 7)))); // [[3], [20, 9], [7, 15]]

        //        5
        //      /   \
        //     3     8
        //    / \   / \
        //   2   4 7   9
        TreeNode bst = build(5, 3, 8, 2, 4, 7, 9);
        System.out.println("validBST     " + isValidBST(bst));                              // true
        System.out.println("invalidBST   " + isValidBST(build(5, 1, 4, null, null, 3, 6))); // false
        System.out.println("searchBST 7  " + searchBST(bst, 7));                            // true
        System.out.println("kthSmallest3 " + kthSmallest(bst, 3));                          // 4
        System.out.println("lcaBST(2,4)  " + lcaBST(bst, 2, 4).val);                        // 3
        System.out.println("lcaBST(2,9)  " + lcaBST(bst, 2, 9).val);                        // 5
        insertBST(bst, 6);
        System.out.println("after insert " + traverse(bst, "in"));                          // [2, 3, 4, 5, 6, 7, 8, 9]

        int[] sorted = {-10, -3, 0, 5, 9};
        System.out.println("sorted->BST  " + levelOrder(sortedArrayToBST(sorted, 0, sorted.length - 1))); // [[0], [-10, 5], [-3, 9]]
    }
}
