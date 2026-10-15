import java.util.*;
import java.util.stream.IntStream;

/** PATTERN: one-pass scans, greedy running state, and in-place rearrangement. */
public class ArrayMisc {

    // HOOK: reverse all, reverse first k, reverse the rest. O(1) space.
    static void rotate(int[] a, int k) {
        int n = a.length;
        k %= n;
        reverse(a, 0, n - 1);
        reverse(a, 0, k - 1);
        reverse(a, k, n - 1);
    }

    static void reverse(int[] a, int i, int j) {
        while (i < j) { int t = a[i]; a[i++] = a[j]; a[j--] = t; }
    }

    // HOOK: walk from the back; a digit < 9 just increments and returns.
    static int[] plusOne(int[] d) {
        for (int i = d.length - 1; i >= 0; i--) {
            if (d[i] < 9) { d[i]++; return d; }
            d[i] = 0;
        }
        int[] res = new int[d.length + 1];
        res[0] = 1;
        return res;                                              // all nines -> 1000..
    }

    // HOOK: row[j] = prev[j-1] + prev[j], ends pinned to 1.
    static List<List<Integer>> pascalsTriangle(int n) {
        List<List<Integer>> res = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            List<Integer> row = new ArrayList<>();
            for (int j = 0; j <= i; j++)
                row.add(j == 0 || j == i ? 1 : res.get(i - 1).get(j - 1) + res.get(i - 1).get(j));
            res.add(row);
        }
        return res;
    }

    // HOOK: single row in place - update RIGHT to LEFT so you read old values.
    static List<Integer> pascalRow(int k) {
        Integer[] row = new Integer[k + 1];
        Arrays.fill(row, 0);
        row[0] = 1;
        for (int i = 1; i <= k; i++)
            for (int j = i; j > 0; j--) row[j] += row[j - 1];
        return Arrays.asList(row);
    }

    // HOOK: track the cheapest price so far; profit = today - cheapest.
    static int maxProfit(int[] p) {
        int min = Integer.MAX_VALUE, best = 0;
        for (int x : p) {
            min = Math.min(min, x);
            best = Math.max(best, x - min);
        }
        return best;
    }

    // HOOK: unlimited trades -> bank every upward step.
    static int maxProfit2(int[] p) {
        return IntStream.range(1, p.length).map(i -> Math.max(0, p[i] - p[i - 1])).sum();
    }

    // HOOK: if total gas >= total cost a solution exists; restart at every deficit.
    static int canCompleteCircuit(int[] gas, int[] cost) {
        int total = 0, tank = 0, start = 0;
        for (int i = 0; i < gas.length; i++) {
            int d = gas[i] - cost[i];
            total += d;
            tank += d;
            if (tank < 0) { start = i + 1; tank = 0; }
        }
        return total < 0 ? -1 : start;
    }

    // HOOK: plant greedily when left and right neighbours are both empty.
    static boolean canPlaceFlowers(int[] bed, int n) {
        for (int i = 0; i < bed.length && n > 0; i++) {
            boolean left = i == 0 || bed[i - 1] == 0;
            boolean right = i == bed.length - 1 || bed[i + 1] == 0;
            if (bed[i] == 0 && left && right) { bed[i] = 1; n--; }
        }
        return n == 0;
    }

    // HOOK: person i ahead of k buys min(t[i], t[k]); behind k buys min(t[i], t[k]-1).
    static int timeRequiredToBuy(int[] t, int k) {
        return IntStream.range(0, t.length)
                .map(i -> i <= k ? Math.min(t[i], t[k]) : Math.min(t[i], t[k] - 1))
                .sum();
    }

    // HOOK: track the minimum running prefix; start high enough to keep it >= 1.
    static int minStartValue(int[] a) {
        int run = 0, min = 0;
        for (int x : a) { run += x; min = Math.min(min, run); }
        return 1 - min;
    }

    // HOOK: cars that escape are leading Ls and trailing Rs; everything else collides.
    static int countCollisions(String s) {
        int l = 0, r = s.length() - 1;
        while (l < s.length() && s.charAt(l) == 'L') l++;
        while (r >= 0 && s.charAt(r) == 'R') r--;
        int res = 0;
        for (int i = l; i <= r; i++) if (s.charAt(i) != 'S') res++;
        return res;
    }

    // HOOK: sum = 2*specialSum + outlier -> (sum - x) must be even and its half present.
    static int largestOutlier(int[] a) {
        int sum = 0;
        Map<Integer, Integer> cnt = new HashMap<>();
        for (int x : a) { sum += x; cnt.merge(x, 1, Integer::sum); }
        int best = Integer.MIN_VALUE;
        for (int x : a) {
            int rest = sum - x;
            if (rest % 2 != 0) continue;
            int half = rest / 2;
            int avail = cnt.getOrDefault(half, 0) - (half == x ? 1 : 0);
            if (avail > 0) best = Math.max(best, x);
        }
        return best;
    }

    // HOOK: Fisher-Yates - swap i with a random index in [0, i].
    static int[] shuffle(int[] a, Random rnd) {
        int[] b = a.clone();
        for (int i = b.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = b[i]; b[i] = b[j]; b[j] = t;
        }
        return b;
    }

    public static void main(String[] args) {
        int[] r = {1, 2, 3, 4, 5, 6, 7};
        rotate(r, 3);
        System.out.println("rotate k=3        = " + Arrays.toString(r));
        System.out.println("plusOne           = " + Arrays.toString(plusOne(new int[]{9, 9})));
        System.out.println("pascalsTriangle   = " + pascalsTriangle(5));
        System.out.println("pascalRow k=3     = " + pascalRow(3));
        System.out.println("maxProfit         = " + maxProfit(new int[]{7, 1, 5, 3, 6, 4}));
        System.out.println("maxProfit2        = " + maxProfit2(new int[]{7, 1, 5, 3, 6, 4}));
        System.out.println("gasStation        = " + canCompleteCircuit(new int[]{1, 2, 3, 4, 5}, new int[]{3, 4, 5, 1, 2}));
        System.out.println("canPlaceFlowers   = " + canPlaceFlowers(new int[]{1, 0, 0, 0, 1}, 1));
        System.out.println("timeToBuyTickets  = " + timeRequiredToBuy(new int[]{2, 3, 2}, 2));
        System.out.println("minStartValue     = " + minStartValue(new int[]{-3, 2, -3, 4, 2}));
        System.out.println("countCollisions   = " + countCollisions("RLRSLL"));
        System.out.println("largestOutlier    = " + largestOutlier(new int[]{2, 3, 5, 10}));
        System.out.println("shuffle           = " + Arrays.toString(shuffle(new int[]{1, 2, 3, 4, 5}, new Random(42))));
    }
}
