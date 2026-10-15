package heaps;

import java.util.*;
import java.util.function.Function;
import java.util.stream.*;

/**
 * Heap / PriorityQueue problems.
 *
 * TRICKS:
 *  - PriorityQueue is a MIN-heap by default. MAX-heap: new PriorityQueue<>(Comparator.reverseOrder())
 *  - "K LARGEST"  -> MIN-heap of size k (poll when size > k; the top is the kth largest)
 *  - "K SMALLEST" / "K CLOSEST" -> MAX-heap of size k
 *  - "MERGE K SORTED" -> heap holds ONE item per source {value, sourceIdx, positionIdx}
 *  - "INTERVALS / ROOMS" -> sort by start, min-heap of END times
 *  - "ALWAYS TAKE THE 2 SMALLEST/LARGEST" (ropes, stones) -> poll twice, push result
 *  - Running median -> two heaps (see arrays/Median)
 *  - add/poll O(log n), peek O(1), build from collection O(n)
 */
public class Heaps {

    // 0. BASICS: heap sort with streams
    static List<Integer> heapSortDesc(List<Integer> nums) {
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());
        maxHeap.addAll(nums);
        return Stream.generate(maxHeap::poll).limit(nums.size()).toList();
    }

    // 1. KTH LARGEST IN A STREAM: k=3, start [4,5,8,2]; add 3,5,10,9,4 -> 4,5,5,8,8
    static class KthLargest {
        private final PriorityQueue<Integer> minHeap = new PriorityQueue<>();
        private final int k;

        KthLargest(int k, int[] nums) {
            this.k = k;
            for (int x : nums) add(x);
        }

        int add(int x) {
            minHeap.offer(x);
            if (minHeap.size() > k) minHeap.poll();
            return minHeap.peek();
        }
    }

    // 2. TOP K FREQUENT ELEMENTS: [1,1,1,2,2,3], k=2 -> [1, 2]
    // TRICK: count with streams, min-heap by count of size k
    static List<Integer> topKFrequent(int[] nums, int k) {
        Map<Integer, Long> count = Arrays.stream(nums).boxed()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        PriorityQueue<Integer> heap = new PriorityQueue<>(Comparator.comparing(count::get));
        for (int num : count.keySet()) {
            heap.offer(num);
            if (heap.size() > k) heap.poll();
        }
        List<Integer> res = new ArrayList<>(heap);
        res.sort(Comparator.comparing(count::get, Comparator.reverseOrder()));
        return res;
    }

    // 3. K CLOSEST POINTS TO ORIGIN: [[3,3],[5,-1],[-2,4]], k=2 -> [[3,3],[-2,4]]
    // TRICK: MAX-heap by distance of size k -> farthest gets kicked out
    static List<int[]> kClosest(int[][] points, int k) {
        Comparator<int[]> byDist = Comparator.comparingInt(p -> p[0] * p[0] + p[1] * p[1]);
        PriorityQueue<int[]> maxHeap = new PriorityQueue<>(byDist.reversed());
        for (int[] p : points) {
            maxHeap.offer(p);
            if (maxHeap.size() > k) maxHeap.poll();
        }
        return maxHeap.stream().sorted(byDist).toList();
    }

    // 4. MERGE K SORTED ARRAYS: [[1,4,5],[1,3,4],[2,6]] -> [1,1,2,3,4,4,5,6]
    static List<Integer> mergeKSorted(int[][] arrays) {
        PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(e -> e[0])); // {value, array, index}
        for (int i = 0; i < arrays.length; i++)
            if (arrays[i].length > 0) heap.offer(new int[]{arrays[i][0], i, 0});

        List<Integer> res = new ArrayList<>();
        while (!heap.isEmpty()) {
            int[] e = heap.poll();
            res.add(e[0]);
            int arr = e[1], next = e[2] + 1;
            if (next < arrays[arr].length) heap.offer(new int[]{arrays[arr][next], arr, next});
        }
        return res;
    }

    // 5. KTH SMALLEST IN SORTED MATRIX: [[1,5,9],[10,11,13],[12,13,15]], k=8 -> 13
    // TRICK: same as merge K sorted (each row is a sorted list), stop at the kth poll
    static int kthSmallestInMatrix(int[][] m, int k) {
        PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(e -> e[0])); // {value, row, col}
        for (int r = 0; r < m.length; r++) heap.offer(new int[]{m[r][0], r, 0});
        for (int i = 1; i < k; i++) {
            int[] e = heap.poll();
            if (e[2] + 1 < m[0].length) heap.offer(new int[]{m[e[1]][e[2] + 1], e[1], e[2] + 1});
        }
        return heap.peek()[0];
    }

    // 6. MEETING ROOMS II (min rooms): [[0,30],[5,10],[15,20]] -> 2
    // TRICK: sort by start; heap of end times; if earliest end <= start, reuse that room
    static int minMeetingRooms(int[][] meetings) {
        int[][] sorted = meetings.clone();
        Arrays.sort(sorted, Comparator.comparingInt(m -> m[0]));
        PriorityQueue<Integer> ends = new PriorityQueue<>();
        for (int[] m : sorted) {
            if (!ends.isEmpty() && ends.peek() <= m[0]) ends.poll();
            ends.offer(m[1]);
        }
        return ends.size();
    }

    // 7. LAST STONE WEIGHT: [2,7,4,1,8,1] -> 1
    // smash the two heaviest, push back the difference
    static int lastStoneWeight(int[] stones) {
        PriorityQueue<Integer> maxHeap = Arrays.stream(stones).boxed()
                .collect(Collectors.toCollection(() -> new PriorityQueue<Integer>(Comparator.reverseOrder())));
        while (maxHeap.size() > 1) {
            int y = maxHeap.poll(), x = maxHeap.poll();
            if (y != x) maxHeap.offer(y - x);
        }
        return maxHeap.isEmpty() ? 0 : maxHeap.peek();
    }

    // 8. MIN COST TO CONNECT ROPES: [4,3,2,6] -> 29
    // always join the two shortest (Huffman idea)
    static int connectRopes(int[] ropes) {
        PriorityQueue<Integer> minHeap = Arrays.stream(ropes).boxed()
                .collect(Collectors.toCollection(PriorityQueue::new));
        int cost = 0;
        while (minHeap.size() > 1) {
            int joined = minHeap.poll() + minHeap.poll();
            cost += joined;
            minHeap.offer(joined);
        }
        return cost;
    }

    // 9. SORT A K-SORTED ARRAY (each element at most k away): [6,5,3,2,8,10,9], k=3 -> sorted
    // TRICK: heap of size k+1 -> its min is always the next element
    static int[] sortKSorted(int[] a, int k) {
        PriorityQueue<Integer> heap = new PriorityQueue<>();
        int[] res = new int[a.length];
        int w = 0;
        for (int x : a) {
            heap.offer(x);
            if (heap.size() > k) res[w++] = heap.poll();
        }
        while (!heap.isEmpty()) res[w++] = heap.poll();
        return res;
    }

    // 10. REORGANIZE STRING (no two same chars adjacent): "aab" -> "aba", "aaab" -> ""
    // TRICK: max-heap by count; hold back the char just used for one round
    static String reorganizeString(String s) {
        Map<Character, Integer> count = new HashMap<>();
        for (char c : s.toCharArray()) count.merge(c, 1, Integer::sum);
        PriorityQueue<Character> heap = new PriorityQueue<>((a, b) -> count.get(b) - count.get(a));
        heap.addAll(count.keySet());

        StringBuilder sb = new StringBuilder();
        Character held = null;
        while (!heap.isEmpty()) {
            char c = heap.poll();
            sb.append(c);
            count.merge(c, -1, Integer::sum);
            if (held != null && count.get(held) > 0) heap.offer(held);
            held = c;
        }
        return sb.length() == s.length() ? sb.toString() : "";
    }

    // 11. TASK SCHEDULER: tasks [A,A,A,B,B,B], cooldown n=2 -> 8 (A B idle A B idle A B)
    // TRICK (formula): (maxCount - 1) * (n + 1) + numberOfTasksWithMaxCount, at least tasks.length
    static int leastInterval(char[] tasks, int n) {
        int[] count = new int[26];
        for (char t : tasks) count[t - 'A']++;
        int max = Arrays.stream(count).max().getAsInt();
        long withMax = Arrays.stream(count).filter(c -> c == max).count();
        return Math.max(tasks.length, (int) ((max - 1) * (n + 1) + withMax));
    }

    public static void main(String[] args) {
        System.out.println("heapSortDesc     " + heapSortDesc(List.of(3, 1, 4, 1, 5, 9, 2)));         // [9, 5, 4, 3, 2, 1, 1]

        KthLargest kth = new KthLargest(3, new int[]{4, 5, 8, 2});
        System.out.println("kthLargest       " + IntStream.of(3, 5, 10, 9, 4).map(kth::add).boxed().toList()); // [4, 5, 5, 8, 8]

        System.out.println("topKFrequent     " + topKFrequent(new int[]{1, 1, 1, 2, 2, 3}, 2));           // [1, 2]
        System.out.println("kClosest         " + kClosest(new int[][]{{3, 3}, {5, -1}, {-2, 4}}, 2)
                .stream().map(Arrays::toString).toList());                                           // [[3, 3], [-2, 4]]
        System.out.println("mergeKSorted     " + mergeKSorted(new int[][]{{1, 4, 5}, {1, 3, 4}, {2, 6}})); // [1, 1, 2, 3, 4, 4, 5, 6]
        System.out.println("kthSmallestMatrx " + kthSmallestInMatrix(new int[][]{{1, 5, 9}, {10, 11, 13}, {12, 13, 15}}, 8)); // 13
        System.out.println("meetingRooms     " + minMeetingRooms(new int[][]{{0, 30}, {5, 10}, {15, 20}}));  // 2
        System.out.println("lastStoneWeight  " + lastStoneWeight(new int[]{2, 7, 4, 1, 8, 1}));           // 1
        System.out.println("connectRopes     " + connectRopes(new int[]{4, 3, 2, 6}));                    // 29
        System.out.println("sortKSorted      " + Arrays.toString(sortKSorted(new int[]{6, 5, 3, 2, 8, 10, 9}, 3))); // [2, 3, 5, 6, 8, 9, 10]
        System.out.println("reorganize       " + reorganizeString("aab") + " '" + reorganizeString("aaab") + "' " + reorganizeString("aabb")); // aba '' abab/baba
        System.out.println("taskScheduler    " + leastInterval("AAABBB".toCharArray(), 2));               // 8
    }
}
