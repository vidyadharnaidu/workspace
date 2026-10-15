"""
Linked list problems.

Run: python python/05_linked_lists/linked_lists.py

REMEMBER:
  * DUMMY node before head -> no special case when the head changes.
  * Reverse: prev/curr; save next, point back, move both.
  * Slow/fast pointers: middle, cycle detection, nth from end (gap), palindrome.
  * In Python a list is usually better than a linked list; interviews still ask for the pointer work.
"""
import heapq


def check(label, actual, expected):
    assert actual == expected, f"{label}: expected {expected!r}, got {actual!r}"
    print(f"{label:<28} {actual!r}")


class ListNode:
    __slots__ = ("val", "next")

    def __init__(self, val=0, next=None):
        self.val = val
        self.next = next

    def __repr__(self):
        return f"ListNode({self.val})"


def build(values):
    dummy = tail = ListNode()
    for v in values:
        tail.next = ListNode(v)
        tail = tail.next
    return dummy.next


def to_list(head, limit=1000):
    out = []
    while head and len(out) < limit:
        out.append(head.val)
        head = head.next
    return out


def reverse(head):
    prev = None
    while head:
        head.next, prev, head = prev, head, head.next   # save next, point back, move both
    return prev


def reverse_recursive(head):
    if not head or not head.next:
        return head
    new_head = reverse_recursive(head.next)
    head.next.next = head
    head.next = None
    return new_head


def middle(head):
    slow = fast = head
    while fast and fast.next:
        slow, fast = slow.next, fast.next.next
    return slow


def cycle_start(head):
    slow = fast = head
    while fast and fast.next:
        slow, fast = slow.next, fast.next.next
        if slow is fast:                       # they met -> there is a cycle
            walker = head
            while walker is not slow:          # restart one pointer from the head
                walker, slow = walker.next, slow.next
            return walker
    return None


def merge_two_sorted(a, b):
    dummy = tail = ListNode()
    while a and b:
        if a.val <= b.val:
            tail.next, a = a, a.next
        else:
            tail.next, b = b, b.next
        tail = tail.next
    tail.next = a or b
    return dummy.next


def merge_k_sorted(heads):
    heap = [(node.val, i, node) for i, node in enumerate(heads) if node]   # i breaks ties (nodes aren't comparable)
    heapq.heapify(heap)
    dummy = tail = ListNode()
    while heap:
        _, i, node = heapq.heappop(heap)
        tail.next = tail = node
        if node.next:
            heapq.heappush(heap, (node.next.val, i, node.next))
    tail.next = None
    return dummy.next


def remove_nth_from_end(head, n):
    dummy = ListNode(0, head)
    fast = slow = dummy
    for _ in range(n + 1):                     # gap of n+1
        fast = fast.next
    while fast:
        fast, slow = fast.next, slow.next
    slow.next = slow.next.next
    return dummy.next


def is_palindrome(head):
    slow = fast = head
    while fast and fast.next:
        slow, fast = slow.next, fast.next.next
    second = reverse(slow)
    first = head
    while second:
        if first.val != second.val:
            return False
        first, second = first.next, second.next
    return True


def remove_duplicates_sorted(head):
    node = head
    while node and node.next:
        if node.val == node.next.val:
            node.next = node.next.next
        else:
            node = node.next
    return head


def add_two_numbers(a, b):                     # digits stored in reverse order
    dummy = tail = ListNode()
    carry = 0
    while a or b or carry:
        total = carry + (a.val if a else 0) + (b.val if b else 0)
        carry, digit = divmod(total, 10)
        tail.next = tail = ListNode(digit)
        a, b = a.next if a else None, b.next if b else None
    return dummy.next


def reorder(head):                             # L0 -> Ln -> L1 -> Ln-1 ...
    if not head:
        return None
    slow, fast = head, head.next
    while fast and fast.next:
        slow, fast = slow.next, fast.next.next
    second = reverse(slow.next)
    slow.next = None
    first = head
    while second:
        first.next, second.next, first, second = second, first.next, first.next, second.next
    return head


def rotate_right(head, k):
    if not head:
        return None
    length, tail = 1, head
    while tail.next:
        tail, length = tail.next, length + 1
    k %= length
    if not k:
        return head
    tail.next = head                           # close the ring
    new_tail = head
    for _ in range(length - k - 1):
        new_tail = new_tail.next
    head, new_tail.next = new_tail.next, None   # cut it open
    return head


def reverse_k_group(head, k):
    node = head
    for _ in range(k):
        if not node:
            return head                        # fewer than k left -> leave as is
        node = node.next
    prev = reverse_k_group(node, k)            # reverse the rest first
    curr = head
    for _ in range(k):
        curr.next, prev, curr = prev, curr, curr.next
    return prev


def sort_list(head):                           # merge sort, O(n log n)
    if not head or not head.next:
        return head
    slow, fast = head, head.next
    while fast and fast.next:
        slow, fast = slow.next, fast.next.next
    right, slow.next = slow.next, None
    return merge_two_sorted(sort_list(head), sort_list(right))


def swap_pairs(head):
    if not head or not head.next:
        return head
    second = head.next
    head.next = swap_pairs(second.next)
    second.next = head
    return second


if __name__ == "__main__":
    check("reverse", to_list(reverse(build([1, 2, 3, 4, 5]))), [5, 4, 3, 2, 1])
    check("reverse_recursive", to_list(reverse_recursive(build([1, 2, 3]))), [3, 2, 1])
    check("middle", middle(build([1, 2, 3, 4, 5])).val, 3)

    cyclic = build([3, 2, 0, -4])
    cyclic.next.next.next.next = cyclic.next            # tail points back to the second node
    check("cycle_start", cycle_start(cyclic).val, 2)
    check("cycle_start (no cycle)", cycle_start(build([1, 2])), None)

    check("merge_two_sorted", to_list(merge_two_sorted(build([1, 2, 4]), build([1, 3, 4]))), [1, 1, 2, 3, 4, 4])
    check("merge_k_sorted", to_list(merge_k_sorted([build([1, 4, 5]), build([1, 3, 4]), build([2, 6])])),
          [1, 1, 2, 3, 4, 4, 5, 6])
    check("remove_nth_from_end", to_list(remove_nth_from_end(build([1, 2, 3, 4, 5]), 2)), [1, 2, 3, 5])
    check("is_palindrome", (is_palindrome(build([1, 2, 2, 1])), is_palindrome(build([1, 2, 3]))), (True, False))
    check("remove_duplicates_sorted", to_list(remove_duplicates_sorted(build([1, 1, 2, 3, 3]))), [1, 2, 3])
    check("add_two_numbers", to_list(add_two_numbers(build([2, 4, 3]), build([5, 6, 4]))), [7, 0, 8])

    reordered = build([1, 2, 3, 4, 5])
    reorder(reordered)
    check("reorder", to_list(reordered), [1, 5, 2, 4, 3])

    check("rotate_right", to_list(rotate_right(build([1, 2, 3, 4, 5]), 2)), [4, 5, 1, 2, 3])
    check("reverse_k_group", to_list(reverse_k_group(build([1, 2, 3, 4, 5]), 2)), [2, 1, 4, 3, 5])
    check("sort_list", to_list(sort_list(build([4, 2, 1, 3]))), [1, 2, 3, 4])
    check("swap_pairs", to_list(swap_pairs(build([1, 2, 3, 4]))), [2, 1, 4, 3])

    print("\nall checks passed")
