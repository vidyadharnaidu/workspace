package queues;

import java.util.*;
import java.util.stream.*;

/**
 * Queue problems.
 *
 * TRICKS:
 *  - Queue<Integer> q = new ArrayDeque<>();  offer (add to back) / poll (remove front) / peek
 *  - Deque = both ends: offerFirst/offerLast, pollFirst/pollLast, peekFirst/peekLast
 *  - "Last N items / last N seconds" (moving average, recent counter) -> offer new, poll old from the front
 *  - "First X that is still valid" (first unique, first negative in window) -> queue of candidates,
 *    drop invalid ones from the front
 *  - Circular queue = array + head + size; index = (head + i) % capacity
 *  - Related: BFS (graphs/Graphs), sliding window max with Deque (slidingwindow), PriorityQueue (heaps),
 *    BlockingQueue (concurrency/ProducerConsumer)
 */
public class Queues {

    // 1. QUEUE USING TWO STACKS
    // TRICK: push into "in"; when "out" is empty, pour everything from in -> out (reverses order). Amortized O(1).
    static class MyQueue {
        private final Deque<Integer> in = new ArrayDeque<>(), out = new ArrayDeque<>();

        void push(int x) { in.push(x); }
        int pop()        { peek(); return out.pop(); }
        boolean empty()  { return in.isEmpty() && out.isEmpty(); }

        int peek() {
            if (out.isEmpty()) while (!in.isEmpty()) out.push(in.pop());
            return out.peek();
        }
    }

    // 2. STACK USING ONE QUEUE
    // TRICK: after offering x, rotate the older (size - 1) elements behind it -> x is at the front
    static class MyStack {
        private final Queue<Integer> q = new ArrayDeque<>();

        void push(int x) {
            q.offer(x);
            for (int i = 0; i < q.size() - 1; i++) q.offer(q.poll());
        }

        int pop()  { return q.poll(); }
        int top()  { return q.peek(); }
    }

    // 3. CIRCULAR QUEUE (ring buffer)
    static class CircularQueue {
        private final int[] data;
        private int head = 0, size = 0;

        CircularQueue(int capacity) { data = new int[capacity]; }

        boolean enqueue(int value) {
            if (isFull()) return false;
            data[(head + size) % data.length] = value;
            size++;
            return true;
        }

        boolean dequeue() {
            if (isEmpty()) return false;
            head = (head + 1) % data.length;
            size--;
            return true;
        }

        int front()       { return isEmpty() ? -1 : data[head]; }
        int rear()        { return isEmpty() ? -1 : data[(head + size - 1) % data.length]; }
        boolean isEmpty() { return size == 0; }
        boolean isFull()  { return size == data.length; }
    }

    // 4. MOVING AVERAGE OF LAST K: k=3, stream 1,10,3,5 -> 1.0, 5.5, 4.67, 6.0
    static class MovingAverage {
        private final Queue<Integer> window = new ArrayDeque<>();
        private final int k;
        private double sum;

        MovingAverage(int k) { this.k = k; }

        double next(int x) {
            window.offer(x);
            sum += x;
            if (window.size() > k) sum -= window.poll();
            return sum / window.size();
        }
    }

    // 5. RECENT COUNTER (requests in last 3000 ms): ping 1, 100, 3001, 3002 -> 1, 2, 3, 3
    static class RecentCounter {
        private final Queue<Integer> times = new ArrayDeque<>();

        int ping(int t) {
            times.offer(t);
            while (times.peek() < t - 3000) times.poll();
            return times.size();
        }
    }

    // 6. FIRST UNIQUE CHARACTER IN A STREAM: "aabc" -> a, #, b, b
    static class FirstUnique {
        private final Map<Character, Integer> count = new HashMap<>();
        private final Queue<Character> candidates = new ArrayDeque<>();

        char add(char c) {
            count.merge(c, 1, Integer::sum);
            candidates.offer(c);
            while (!candidates.isEmpty() && count.get(candidates.peek()) > 1) candidates.poll();
            return candidates.isEmpty() ? '#' : candidates.peek();
        }
    }

    // 7. FIRST NEGATIVE IN EVERY WINDOW OF K: [12,-1,-7,8,-15,30,16,28], k=3 -> [-1,-1,-7,-15,-15,0]
    // TRICK: queue of indexes of negatives; drop the front when it leaves the window
    static int[] firstNegativeInWindow(int[] a, int k) {
        Deque<Integer> negatives = new ArrayDeque<>();
        int[] res = new int[a.length - k + 1];
        for (int i = 0; i < a.length; i++) {
            if (a[i] < 0) negatives.offer(i);
            if (!negatives.isEmpty() && negatives.peek() <= i - k) negatives.poll();
            if (i >= k - 1) res[i - k + 1] = negatives.isEmpty() ? 0 : a[negatives.peek()];
        }
        return res;
    }

    // 8. GENERATE BINARY NUMBERS 1..n: 5 -> [1, 10, 11, 100, 101]
    // TRICK: queue starting with "1"; each poll adds s+"0" and s+"1"
    static List<String> binaryNumbers(int n) {
        Queue<String> q = new ArrayDeque<>(List.of("1"));
        List<String> res = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String s = q.poll();
            res.add(s);
            q.offer(s + "0");
            q.offer(s + "1");
        }
        return res;
    }

    // (the streams one-liner an interviewer will also accept)
    static List<String> binaryNumbersStream(int n) {
        return IntStream.rangeClosed(1, n).mapToObj(Integer::toBinaryString).toList();
    }

    // 9. REVERSE FIRST K ELEMENTS OF A QUEUE: [1,2,3,4,5], k=3 -> [3,2,1,4,5]
    // TRICK: poll k into a stack, pop them back into the queue, then rotate the remaining (size - k) to the back
    static Queue<Integer> reverseFirstK(Queue<Integer> q, int k) {
        Deque<Integer> stack = new ArrayDeque<>();
        for (int i = 0; i < k; i++) stack.push(q.poll());
        while (!stack.isEmpty()) q.offer(stack.pop());
        for (int i = 0; i < q.size() - k; i++) q.offer(q.poll());
        return q;
    }

    // 10. JOSEPHUS / HOT POTATO: n=5 people, every 2nd is out -> survivor 3
    // TRICK: rotate k-1 people to the back, remove the kth
    static int josephus(int n, int k) {
        Queue<Integer> q = IntStream.rangeClosed(1, n).boxed().collect(Collectors.toCollection(ArrayDeque::new));
        while (q.size() > 1) {
            for (int i = 0; i < k - 1; i++) q.offer(q.poll());
            q.poll();
        }
        return q.peek();
    }

    // 11. DEQUE: PALINDROME CHECK from both ends
    static boolean isPalindrome(String s) {
        Deque<Character> dq = s.chars().mapToObj(c -> (char) c).collect(Collectors.toCollection(ArrayDeque::new));
        while (dq.size() > 1)
            if (!dq.pollFirst().equals(dq.pollLast())) return false;
        return true;
    }

    public static void main(String[] args) {
        MyQueue mq = new MyQueue();
        mq.push(1); mq.push(2);
        System.out.print("queueWith2Stacks   peek=" + mq.peek() + " pop=" + mq.pop());      // peek=1 pop=1
        mq.push(3);
        System.out.println(" pop=" + mq.pop() + " pop=" + mq.pop() + " empty=" + mq.empty()); // pop=2 pop=3 empty=true

        MyStack st = new MyStack();
        st.push(1); st.push(2); st.push(3);
        System.out.println("stackWithQueue     top=" + st.top() + " pop=" + st.pop() + " pop=" + st.pop()); // top=3 pop=3 pop=2

        CircularQueue cq = new CircularQueue(3);
        System.out.println("circularQueue      " + cq.enqueue(1) + " " + cq.enqueue(2) + " " + cq.enqueue(3) + " " + cq.enqueue(4)
                + " rear=" + cq.rear() + " full=" + cq.isFull() + " deq=" + cq.dequeue() + " enq4=" + cq.enqueue(4)
                + " front=" + cq.front() + " rear=" + cq.rear());   // true true true false rear=3 full=true deq=true enq4=true front=2 rear=4

        MovingAverage ma = new MovingAverage(3);
        System.out.println("movingAverage      " + IntStream.of(1, 10, 3, 5).mapToObj(x -> String.format("%.2f", ma.next(x))).toList()); // [1.00, 5.50, 4.67, 6.00]

        RecentCounter rc = new RecentCounter();
        System.out.println("recentCounter      " + IntStream.of(1, 100, 3001, 3002).map(rc::ping).boxed().toList()); // [1, 2, 3, 3]

        FirstUnique fu = new FirstUnique();
        System.out.println("firstUniqueStream  " + "aabc".chars().mapToObj(c -> fu.add((char) c)).toList());        // [a, #, b, b]

        System.out.println("firstNegWindow     " + Arrays.toString(firstNegativeInWindow(new int[]{12, -1, -7, 8, -15, 30, 16, 28}, 3))); // [-1, -1, -7, -15, -15, 0]
        System.out.println("binaryNumbers      " + binaryNumbers(5) + " " + binaryNumbersStream(5));                  // [1, 10, 11, 100, 101] x2
        System.out.println("reverseFirstK      " + reverseFirstK(new ArrayDeque<>(List.of(1, 2, 3, 4, 5)), 3));        // [3, 2, 1, 4, 5]
        System.out.println("josephus(5,2)      " + josephus(5, 2));                                                    // 3
        System.out.println("dequePalindrome    " + isPalindrome("racecar") + " " + isPalindrome("queue"));              // true false
    }
}
