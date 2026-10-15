package linkedlist;

import java.util.*;
import java.util.stream.*;

/**
 * Linked list problems.
 *
 * TRICKS:
 *  - DUMMY NODE before head -> no special case when the head changes (merge, remove, add).
 *  - REVERSE: "save next, point back, move prev, move curr"
 *  - SLOW/FAST pointers: middle (fast reaches end), cycle (they meet), cycle start (restart one from head)
 *  - GAP pointers: nth from end -> move fast n steps first, then both together
 *  - Many "hard" ones = find middle + reverse second half + merge (palindrome, reorder)
 *  - Draw 3 boxes and arrows on paper before coding!
 */
public class LinkedLists {

    static class ListNode {
        int val;
        ListNode next;
        ListNode(int val) { this.val = val; }

        // [1 -> 2 -> 3] using Java 9 Stream.iterate(seed, hasNext, next)
        @Override
        public String toString() {
            return Stream.iterate(this, Objects::nonNull, n -> n.next)
                    .map(n -> String.valueOf(n.val))
                    .collect(Collectors.joining(" -> ", "[", "]"));
        }
    }

    static ListNode of(int... values) {
        ListNode dummy = new ListNode(0), tail = dummy;
        for (int v : values) tail = tail.next = new ListNode(v);
        return dummy.next;
    }

    // 1. REVERSE (iterative): 1->2->3 -> 3->2->1
    static ListNode reverse(ListNode head) {
        ListNode prev = null, curr = head;
        while (curr != null) {
            ListNode next = curr.next;
            curr.next = prev;
            prev = curr;
            curr = next;
        }
        return prev;
    }

    // 1b. REVERSE (recursive): reverse the rest, then make my next point back at me
    static ListNode reverseRecursive(ListNode head) {
        if (head == null || head.next == null) return head;
        ListNode newHead = reverseRecursive(head.next);
        head.next.next = head;
        head.next = null;
        return newHead;
    }

    // 2. MIDDLE: 1->2->3->4->5 -> 3 (for even length returns the second middle)
    static ListNode middle(ListNode head) {
        ListNode slow = head, fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        return slow;
    }

    // 3. HAS CYCLE + WHERE IT STARTS (Floyd)
    // TRICK: after they meet, move one pointer to head; step both by 1; they meet at the cycle start
    static ListNode cycleStart(ListNode head) {
        ListNode slow = head, fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            if (slow == fast) {
                for (ListNode p = head; p != slow; p = p.next) slow = slow.next;
                return slow;
            }
        }
        return null; // no cycle
    }

    // 4. MERGE TWO SORTED: 1->2->4 + 1->3->4 -> 1->1->2->3->4->4
    static ListNode mergeTwo(ListNode a, ListNode b) {
        ListNode dummy = new ListNode(0), tail = dummy;
        while (a != null && b != null) {
            if (a.val <= b.val) { tail.next = a; a = a.next; }
            else { tail.next = b; b = b.next; }
            tail = tail.next;
        }
        tail.next = a != null ? a : b;
        return dummy.next;
    }

    // 5. MERGE K SORTED LISTS: min-heap of list heads
    static ListNode mergeK(List<ListNode> lists) {
        PriorityQueue<ListNode> heap = new PriorityQueue<>(Comparator.comparingInt(n -> n.val));
        lists.stream().filter(Objects::nonNull).forEach(heap::offer);
        ListNode dummy = new ListNode(0), tail = dummy;
        while (!heap.isEmpty()) {
            tail = tail.next = heap.poll();
            if (tail.next != null) heap.offer(tail.next);
        }
        return dummy.next;
    }

    // 6. REMOVE NTH FROM END: 1->2->3->4->5, n=2 -> 1->2->3->5
    static ListNode removeNthFromEnd(ListNode head, int n) {
        ListNode dummy = new ListNode(0);
        dummy.next = head;
        ListNode fast = dummy, slow = dummy;
        for (int i = 0; i <= n; i++) fast = fast.next; // gap of n+1
        while (fast != null) {
            fast = fast.next;
            slow = slow.next;
        }
        slow.next = slow.next.next;
        return dummy.next;
    }

    // 7. PALINDROME LIST: 1->2->2->1 -> true
    // TRICK: middle + reverse second half + compare
    static boolean isPalindrome(ListNode head) {
        ListNode slow = head, fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        for (ListNode a = head, b = reverse(slow); b != null; a = a.next, b = b.next)
            if (a.val != b.val) return false;
        return true;
    }

    // 8. INTERSECTION OF TWO LISTS
    // TRICK: when a pointer hits the end, jump to the OTHER head -> both walk lenA + lenB and meet
    static ListNode intersection(ListNode headA, ListNode headB) {
        ListNode a = headA, b = headB;
        while (a != b) {
            a = a == null ? headB : a.next;
            b = b == null ? headA : b.next;
        }
        return a;
    }

    // 9. REMOVE DUPLICATES FROM SORTED LIST: 1->1->2->3->3 -> 1->2->3
    static ListNode removeDuplicates(ListNode head) {
        for (ListNode cur = head; cur != null && cur.next != null; ) {
            if (cur.val == cur.next.val) cur.next = cur.next.next;
            else cur = cur.next;
        }
        return head;
    }

    // 10. ADD TWO NUMBERS (digits stored in reverse): 2->4->3 + 5->6->4 = 7->0->8  (342 + 465 = 807)
    static ListNode addTwoNumbers(ListNode a, ListNode b) {
        ListNode dummy = new ListNode(0), tail = dummy;
        int carry = 0;
        while (a != null || b != null || carry != 0) {
            int sum = carry + (a != null ? a.val : 0) + (b != null ? b.val : 0);
            carry = sum / 10;
            tail = tail.next = new ListNode(sum % 10);
            a = a != null ? a.next : null;
            b = b != null ? b.next : null;
        }
        return dummy.next;
    }

    // 11. REORDER LIST: 1->2->3->4->5 -> 1->5->2->4->3
    // TRICK: split at middle, reverse second half, weave together
    static void reorder(ListNode head) {
        if (head == null) return;
        ListNode slow = head, fast = head.next;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        ListNode second = reverse(slow.next);
        slow.next = null;
        for (ListNode first = head; second != null; ) {
            ListNode n1 = first.next, n2 = second.next;
            first.next = second;
            second.next = n1;
            first = n1;
            second = n2;
        }
    }

    // 12. ROTATE RIGHT BY K: 1->2->3->4->5, k=2 -> 4->5->1->2->3
    // TRICK: make it a circle, then cut at (length - k)
    static ListNode rotateRight(ListNode head, int k) {
        if (head == null) return null;
        int n = 1;
        ListNode tail = head;
        while (tail.next != null) { tail = tail.next; n++; }
        k %= n;
        if (k == 0) return head;
        tail.next = head; // circle
        ListNode newTail = head;
        for (int i = 0; i < n - k - 1; i++) newTail = newTail.next;
        ListNode newHead = newTail.next;
        newTail.next = null;
        return newHead;
    }

    // 13. REVERSE IN K-GROUPS: 1->2->3->4->5, k=2 -> 2->1->4->3->5
    // TRICK: check k nodes exist, reverse the REST recursively, then reverse k nodes onto it
    static ListNode reverseKGroup(ListNode head, int k) {
        ListNode check = head;
        for (int i = 0; i < k; i++) {
            if (check == null) return head; // fewer than k left -> keep as is
            check = check.next;
        }
        ListNode prev = reverseKGroup(check, k), curr = head;
        for (int i = 0; i < k; i++) {
            ListNode next = curr.next;
            curr.next = prev;
            prev = curr;
            curr = next;
        }
        return prev;
    }

    // 14. SORT LIST (merge sort, O(n log n)): 4->2->1->3 -> 1->2->3->4
    static ListNode sortList(ListNode head) {
        if (head == null || head.next == null) return head;
        ListNode slow = head, fast = head.next;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        ListNode right = slow.next;
        slow.next = null;
        return mergeTwo(sortList(head), sortList(right));
    }

    // 15. SWAP NODES IN PAIRS: 1->2->3->4 -> 2->1->4->3
    static ListNode swapPairs(ListNode head) {
        if (head == null || head.next == null) return head;
        ListNode second = head.next;
        head.next = swapPairs(second.next);
        second.next = head;
        return second;
    }

    public static void main(String[] args) {
        System.out.println("reverse          " + reverse(of(1, 2, 3, 4, 5)));             // [5 -> 4 -> 3 -> 2 -> 1]
        System.out.println("reverseRecursive " + reverseRecursive(of(1, 2, 3)));          // [3 -> 2 -> 1]
        System.out.println("middle           " + middle(of(1, 2, 3, 4, 5)).val);          // 3

        ListNode cyc = of(3, 2, 0, -4);
        cyc.next.next.next.next = cyc.next;                                                // -4 points back to 2
        System.out.println("cycleStart       " + cycleStart(cyc).val + ", no cycle: " + cycleStart(of(1, 2)));  // 2, null

        System.out.println("mergeTwo         " + mergeTwo(of(1, 2, 4), of(1, 3, 4)));     // [1 -> 1 -> 2 -> 3 -> 4 -> 4]
        System.out.println("mergeK           " + mergeK(List.of(of(1, 4, 5), of(1, 3, 4), of(2, 6)))); // [1 -> 1 -> 2 -> 3 -> 4 -> 4 -> 5 -> 6]
        System.out.println("removeNthFromEnd " + removeNthFromEnd(of(1, 2, 3, 4, 5), 2)); // [1 -> 2 -> 3 -> 5]
        System.out.println("isPalindrome     " + isPalindrome(of(1, 2, 2, 1)) + " " + isPalindrome(of(1, 2, 3))); // true false

        ListNode shared = of(8, 4, 5);
        ListNode a = of(4, 1);
        a.next.next = shared;
        ListNode b = of(5, 6, 1);
        b.next.next.next = shared;
        System.out.println("intersection     " + intersection(a, b).val);                 // 8

        System.out.println("removeDuplicates " + removeDuplicates(of(1, 1, 2, 3, 3)));     // [1 -> 2 -> 3]
        System.out.println("addTwoNumbers    " + addTwoNumbers(of(2, 4, 3), of(5, 6, 4))); // [7 -> 0 -> 8]

        ListNode r = of(1, 2, 3, 4, 5);
        reorder(r);
        System.out.println("reorder          " + r);                                       // [1 -> 5 -> 2 -> 4 -> 3]

        System.out.println("rotateRight      " + rotateRight(of(1, 2, 3, 4, 5), 2));      // [4 -> 5 -> 1 -> 2 -> 3]
        System.out.println("reverseKGroup    " + reverseKGroup(of(1, 2, 3, 4, 5), 2));    // [2 -> 1 -> 4 -> 3 -> 5]
        System.out.println("sortList         " + sortList(of(4, 2, 1, 3)));               // [1 -> 2 -> 3 -> 4]
        System.out.println("swapPairs        " + swapPairs(of(1, 2, 3, 4)));              // [2 -> 1 -> 4 -> 3]
    }
}
