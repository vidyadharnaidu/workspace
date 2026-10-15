import java.util.*;

/** PATTERN: preorder + an explicit null marker is enough to rebuild any tree. */
public class TreeSerialize {

    static class TreeNode {
        int val;
        TreeNode left, right;
        TreeNode(int v) { val = v; }
    }

    // HOOK: preorder, writing "#" for null. Without the markers it is ambiguous.
    static String serialize(TreeNode root) {
        StringBuilder sb = new StringBuilder();
        write(root, sb);
        return sb.toString();
    }

    static void write(TreeNode n, StringBuilder sb) {
        if (n == null) { sb.append("#,"); return; }
        sb.append(n.val).append(',');
        write(n.left, sb);
        write(n.right, sb);
    }

    // HOOK: read the same order with a queue cursor - value, then left, then right.
    static TreeNode deserialize(String data) {
        return read(new LinkedList<>(Arrays.asList(data.split(","))));
    }

    static TreeNode read(Queue<String> q) {
        String s = q.poll();
        if (s == null || s.equals("#")) return null;
        TreeNode n = new TreeNode(Integer.parseInt(s));
        n.left = read(q);
        n.right = read(q);
        return n;
    }

    // HOOK: preorder gives the ROOT; inorder tells you how many nodes go left.
    static TreeNode buildTree(int[] preorder, int[] inorder) {
        Map<Integer, Integer> pos = new HashMap<>();
        for (int i = 0; i < inorder.length; i++) pos.put(inorder[i], i);
        return make(preorder, 0, 0, inorder.length - 1, pos);
    }

    static TreeNode make(int[] pre, int root, int lo, int hi, Map<Integer, Integer> pos) {
        if (lo > hi) return null;
        TreeNode n = new TreeNode(pre[root]);
        int mid = pos.get(pre[root]);
        int leftCount = mid - lo;
        n.left = make(pre, root + 1, lo, mid - 1, pos);
        n.right = make(pre, root + 1 + leftCount, mid + 1, hi, pos);
        return n;
    }

    static List<Integer> levelOrder(TreeNode root) {
        List<Integer> out = new ArrayList<>();
        if (root == null) return out;
        Queue<TreeNode> q = new LinkedList<>();
        q.add(root);
        while (!q.isEmpty()) {
            TreeNode n = q.poll();
            out.add(n.val);
            if (n.left != null) q.add(n.left);
            if (n.right != null) q.add(n.right);
        }
        return out;
    }

    public static void main(String[] args) {
        TreeNode built = buildTree(new int[]{3, 9, 20, 15, 7}, new int[]{9, 3, 15, 20, 7});
        System.out.println("buildTree levels  = " + levelOrder(built));

        String s = serialize(built);
        System.out.println("serialize         = " + s);
        System.out.println("round trip levels = " + levelOrder(deserialize(s)));
        System.out.println("round trip equal  = " + s.equals(serialize(deserialize(s))));
    }
}
