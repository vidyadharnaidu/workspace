"""
Binary tree and BST problems.

Run: python python/07_trees_tries/binary_trees.py

REMEMBER:
  * Most tree problems: solve left, solve right, combine at the root (recursion).
  * Level by level = BFS with a deque, processing len(queue) nodes per level.
  * BST in-order traversal is SORTED (kth smallest, validation).
  * Validate BST with (low, high) bounds, not by comparing with the children only.
  * "Answer isn't what recursion returns" (diameter) -> return height, update a nonlocal best.
"""
from collections import deque


def check(label, actual, expected):
    assert actual == expected, f"{label}: expected {expected!r}, got {actual!r}"
    print(f"{label:<28} {actual!r}")


class TreeNode:
    __slots__ = ("val", "left", "right")

    def __init__(self, val=0, left=None, right=None):
        self.val, self.left, self.right = val, left, right

    def __repr__(self):
        return f"TreeNode({self.val})"


def build(values):
    """LeetCode-style level order list with None: [3, 9, 20, None, None, 15, 7]"""
    if not values:
        return None
    root = TreeNode(values[0])
    queue, i = deque([root]), 1
    while queue and i < len(values):
        node = queue.popleft()
        if i < len(values) and values[i] is not None:
            node.left = TreeNode(values[i])
            queue.append(node.left)
        i += 1
        if i < len(values) and values[i] is not None:
            node.right = TreeNode(values[i])
            queue.append(node.right)
        i += 1
    return root


def inorder(node):                       # generator: lazy and composable
    if node:
        yield from inorder(node.left)
        yield node.val
        yield from inorder(node.right)


def preorder(node):
    if node:
        yield node.val
        yield from preorder(node.left)
        yield from preorder(node.right)


def postorder(node):
    if node:
        yield from postorder(node.left)
        yield from postorder(node.right)
        yield node.val


def inorder_iterative(root):
    result, stack, node = [], [], root
    while node or stack:
        while node:                      # go left as far as possible
            stack.append(node)
            node = node.left
        node = stack.pop()
        result.append(node.val)
        node = node.right                # then one step right
    return result


def level_order(root):
    if not root:
        return []
    result, queue = [], deque([root])
    while queue:
        level = []
        for _ in range(len(queue)):      # exactly one level per outer loop
            node = queue.popleft()
            level.append(node.val)
            queue.extend(child for child in (node.left, node.right) if child)
        result.append(level)
    return result


def zigzag(root):
    return [level if i % 2 == 0 else level[::-1] for i, level in enumerate(level_order(root))]


def right_side_view(root):
    return [level[-1] for level in level_order(root)]


def max_depth(node):
    return 0 if not node else 1 + max(max_depth(node.left), max_depth(node.right))


def min_depth(node):
    if not node:
        return 0
    if not node.left:
        return 1 + min_depth(node.right)     # a node with one child is not a leaf
    if not node.right:
        return 1 + min_depth(node.left)
    return 1 + min(min_depth(node.left), min_depth(node.right))


def count_nodes(node):
    return 0 if not node else 1 + count_nodes(node.left) + count_nodes(node.right)


def invert(node):
    if node:
        node.left, node.right = invert(node.right), invert(node.left)
    return node


def is_same_tree(a, b):
    if not a or not b:
        return a is b
    return a.val == b.val and is_same_tree(a.left, b.left) and is_same_tree(a.right, b.right)


def is_symmetric(root):
    def mirror(a, b):
        if not a or not b:
            return a is b
        return a.val == b.val and mirror(a.left, b.right) and mirror(a.right, b.left)
    return not root or mirror(root.left, root.right)


def is_balanced(root):
    def height(node):                    # returns -1 to signal "unbalanced"
        if not node:
            return 0
        left, right = height(node.left), height(node.right)
        if left < 0 or right < 0 or abs(left - right) > 1:
            return -1
        return 1 + max(left, right)
    return height(root) != -1


def diameter(root):
    best = 0

    def height(node):
        nonlocal best
        if not node:
            return 0
        left, right = height(node.left), height(node.right)
        best = max(best, left + right)   # longest path THROUGH this node, in edges
        return 1 + max(left, right)

    height(root)
    return best


def has_path_sum(node, target):
    if not node:
        return False
    if not node.left and not node.right:
        return target == node.val
    return has_path_sum(node.left, target - node.val) or has_path_sum(node.right, target - node.val)


def all_paths(node):
    if not node:
        return []
    if not node.left and not node.right:
        return [str(node.val)]
    return [f"{node.val}->{path}" for path in all_paths(node.left) + all_paths(node.right)]


def lowest_common_ancestor(node, p, q):
    if not node or node.val in (p, q):
        return node
    left = lowest_common_ancestor(node.left, p, q)
    right = lowest_common_ancestor(node.right, p, q)
    return node if left and right else left or right      # found on both sides -> this is the LCA


def is_valid_bst(node, low=float("-inf"), high=float("inf")):
    if not node:
        return True
    if not low < node.val < high:
        return False
    return is_valid_bst(node.left, low, node.val) and is_valid_bst(node.right, node.val, high)


def search_bst(node, target):
    while node and node.val != target:
        node = node.left if target < node.val else node.right
    return node


def insert_bst(node, value):
    if not node:
        return TreeNode(value)
    if value < node.val:
        node.left = insert_bst(node.left, value)
    else:
        node.right = insert_bst(node.right, value)
    return node


def lca_bst(node, p, q):
    while node:
        if p < node.val and q < node.val:
            node = node.left
        elif p > node.val and q > node.val:
            node = node.right
        else:
            return node                     # the split point
    return None


def kth_smallest(root, k):
    for i, value in enumerate(inorder(root), start=1):   # generator stops early
        if i == k:
            return value
    return None


def sorted_array_to_bst(values):
    if not values:
        return None
    mid = len(values) // 2
    return TreeNode(values[mid], sorted_array_to_bst(values[:mid]), sorted_array_to_bst(values[mid + 1:]))


def serialize(node):
    """Pre-order with '#' for None."""
    parts = []

    def walk(n):
        if not n:
            parts.append("#")
            return
        parts.append(str(n.val))
        walk(n.left)
        walk(n.right)

    walk(node)
    return ",".join(parts)


def deserialize(data):
    tokens = iter(data.split(","))

    def build_node():
        token = next(tokens)
        if token == "#":
            return None
        node = TreeNode(int(token))
        node.left = build_node()
        node.right = build_node()
        return node

    return build_node()


if __name__ == "__main__":
    #       3
    #     9   20
    #        15  7
    tree = build([3, 9, 20, None, None, 15, 7])
    check("inorder", list(inorder(tree)), [9, 3, 15, 20, 7])
    check("preorder", list(preorder(tree)), [3, 9, 20, 15, 7])
    check("postorder", list(postorder(tree)), [9, 15, 7, 20, 3])
    check("inorder_iterative", inorder_iterative(tree), [9, 3, 15, 20, 7])
    check("level_order", level_order(tree), [[3], [9, 20], [15, 7]])
    check("zigzag", zigzag(tree), [[3], [20, 9], [15, 7]])
    check("right_side_view", right_side_view(tree), [3, 20, 7])
    check("max_depth / min_depth", (max_depth(tree), min_depth(tree)), (3, 2))
    check("count_nodes", count_nodes(tree), 5)
    check("diameter", diameter(tree), 3)
    check("is_balanced", is_balanced(tree), True)
    check("has_path_sum(30)", has_path_sum(tree, 30), True)
    check("all_paths", all_paths(tree), ["3->9", "3->20->15", "3->20->7"])
    check("lowest_common_ancestor", (lowest_common_ancestor(tree, 15, 7).val, lowest_common_ancestor(tree, 9, 7).val), (20, 3))
    check("is_same_tree", is_same_tree(tree, build([3, 9, 20, None, None, 15, 7])), True)
    check("is_symmetric", is_symmetric(build([1, 2, 2, 3, 4, 4, 3])), True)
    check("invert", level_order(invert(build([3, 9, 20, None, None, 15, 7]))), [[3], [20, 9], [7, 15]])

    bst = build([5, 3, 8, 2, 4, 7, 9])
    check("is_valid_bst", (is_valid_bst(bst), is_valid_bst(build([5, 1, 4, None, None, 3, 6]))), (True, False))
    check("search_bst", search_bst(bst, 7).val, 7)
    check("kth_smallest(3)", kth_smallest(bst, 3), 4)
    check("lca_bst", (lca_bst(bst, 2, 4).val, lca_bst(bst, 2, 9).val), (3, 5))
    insert_bst(bst, 6)
    check("after insert_bst(6)", list(inorder(bst)), [2, 3, 4, 5, 6, 7, 8, 9])
    check("sorted_array_to_bst", level_order(sorted_array_to_bst([-10, -3, 0, 5, 9])), [[0], [-3, 9], [-10, 5]])

    data = serialize(tree)
    check("serialize", data, "3,9,#,#,20,15,#,#,7,#,#")
    check("deserialize round trip", level_order(deserialize(data)), level_order(tree))

    print("\nall checks passed")
