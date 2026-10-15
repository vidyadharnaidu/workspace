import java.util.*;

/**
 * PATTERN: a BST's INORDER walk is sorted. Nearly every BST problem is one of:
 *   - inorder and use the sortedness (kth smallest, validate, recover, to-list)
 *   - compare val to the target and go ONE way (search, insert, LCA, range sum)
 */
public class BstOps {

    static class TreeNode {
        int val;
        TreeNode left, right;
        TreeNode(int v) { val = v; }
    }

    // HOOK: pass down an open interval (low, high) and tighten it on each side.
    static boolean isValidBST(TreeNode root) {
        return valid(root, null, null);
    }

    static boolean valid(TreeNode n, Integer low, Integer high) {
        if (n == null) return true;
        if (low != null && n.val <= low) return false;
        if (high != null && n.val >= high) return false;
        return valid(n.left, low, n.val) && valid(n.right, n.val, high);
    }

    // HOOK: iterative inorder with a stack; stop after popping k nodes.
    static int kthSmallest(TreeNode root, int k) {
        Deque<TreeNode> st = new ArrayDeque<>();
        TreeNode cur = root;
        while (cur != null || !st.isEmpty()) {
            while (cur != null) { st.push(cur); cur = cur.left; }
            cur = st.pop();
            if (--k == 0) return cur.val;
            cur = cur.right;
        }
        return -1;
    }

    // HOOK: prune - below low go right only, above high go left only.
    static int rangeSumBST(TreeNode n, int low, int high) {
        if (n == null) return 0;
        if (n.val < low) return rangeSumBST(n.right, low, high);
        if (n.val > high) return rangeSumBST(n.left, low, high);
        return n.val + rangeSumBST(n.left, low, high) + rangeSumBST(n.right, low, high);
    }

    static int running;

    // HOOK: REVERSE inorder (right, node, left) visits values largest-first.
    static TreeNode convertBST(TreeNode root) {
        running = 0;
        addGreater(root);
        return root;
    }

    static void addGreater(TreeNode n) {
        if (n == null) return;
        addGreater(n.right);
        running += n.val;
        n.val = running;
        addGreater(n.left);
    }

    // HOOK: the middle element is the root; recurse on the two halves.
    static TreeNode sortedArrayToBST(int[] a) {
        return toBst(a, 0, a.length - 1);
    }

    static TreeNode toBst(int[] a, int lo, int hi) {
        if (lo > hi) return null;
        int mid = (lo + hi) / 2;
        TreeNode n = new TreeNode(a[mid]);
        n.left = toBst(a, lo, mid - 1);
        n.right = toBst(a, mid + 1, hi);
        return n;
    }

    static TreeNode first, second, prev;

    // HOOK: inorder must be ascending; the two dips mark the swapped pair.
    static void recoverTree(TreeNode root) {
        first = second = prev = null;
        findSwapped(root);
        int t = first.val;
        first.val = second.val;
        second.val = t;
    }

    static void findSwapped(TreeNode n) {
        if (n == null) return;
        findSwapped(n.left);
        if (prev != null && prev.val > n.val) {
            if (first == null) first = prev;                      // first dip -> the larger one
            second = n;                                           // last dip -> the smaller one
        }
        prev = n;
        findSwapped(n.right);
    }

    // HOOK: plain two-sum with a seen-set; the BST shape is a red herring.
    static boolean findTarget(TreeNode root, int k) {
        return findWith(root, k, new HashSet<>());
    }

    static boolean findWith(TreeNode n, int k, Set<Integer> seen) {
        if (n == null) return false;
        if (seen.contains(k - n.val)) return true;
        seen.add(n.val);
        return findWith(n.left, k, seen) || findWith(n.right, k, seen);
    }

    // HOOK: both smaller -> go left; both bigger -> go right; else you are AT the split.
    static TreeNode lcaBST(TreeNode n, int p, int q) {
        while (n != null) {
            if (p < n.val && q < n.val) n = n.left;
            else if (p > n.val && q > n.val) n = n.right;
            else return n;
        }
        return null;
    }

    static TreeNode head, tail;

    // HOOK: inorder, stitching prev.right = cur and cur.left = prev as you go.
    static TreeNode treeToDoublyList(TreeNode root) {
        if (root == null) return null;
        head = tail = null;
        stitch(root);
        head.left = tail;                                         // close the circle
        tail.right = head;
        return head;
    }

    static void stitch(TreeNode n) {
        if (n == null) return;
        stitch(n.left);
        if (tail == null) head = n; else { tail.right = n; n.left = tail; }
        tail = n;
        stitch(n.right);
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

    static List<Integer> inorder(TreeNode n) {
        List<Integer> out = new ArrayList<>();
        Deque<TreeNode> st = new ArrayDeque<>();
        while (n != null || !st.isEmpty()) {
            while (n != null) { st.push(n); n = n.left; }
            n = st.pop();
            out.add(n.val);
            n = n.right;
        }
        return out;
    }

    public static void main(String[] args) {
        System.out.println("isValidBST        = " + isValidBST(build(2, 1, 3)));
        System.out.println("isValidBST false  = " + isValidBST(build(5, 1, 4, null, null, 3, 6)));
        System.out.println("kthSmallest k=1   = " + kthSmallest(build(3, 1, 4, null, 2), 1));
        System.out.println("rangeSumBST 7..15 = " + rangeSumBST(build(10, 5, 15, 3, 7, null, 18), 7, 15));
        System.out.println("convertBST        = " + inorder(convertBST(build(4, 1, 6, 0, 2, 5, 7))));
        System.out.println("sortedArrayToBST  = " + inorder(sortedArrayToBST(new int[]{-10, -3, 0, 5, 9})));
        System.out.println("findTarget 9      = " + findTarget(build(5, 3, 6, 2, 4, null, 7), 9));
        System.out.println("lcaBST(2,4)       = " + lcaBST(build(6, 2, 8, 0, 4, 7, 9), 2, 4).val);

        TreeNode bad = build(3, 1, 4, null, null, 2);
        recoverTree(bad);
        System.out.println("recoverBST        = " + inorder(bad));

        TreeNode dll = treeToDoublyList(build(4, 2, 5, 1, 3));
        StringBuilder sb = new StringBuilder();
        TreeNode cur = dll;
        do { sb.append(cur.val).append(' '); cur = cur.right; } while (cur != dll);
        System.out.println("bstToDLL          = " + sb.toString().trim());
    }
}
