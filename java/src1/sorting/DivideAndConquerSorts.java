import java.util.*;

/**
 * PATTERN: split the array, recurse, put it back together.
 * The difference is WHERE the work happens:
 *   mergeSort - split blindly down the middle, do the work while MERGING  (work after)
 *   quickSort - do the work while PARTITIONING, then recurse blindly     (work before)
 *
 *              avg         worst       space      stable?
 *   merge      O(n log n)  O(n log n)  O(n)       yes    (worst case guaranteed)
 *   quick      O(n log n)  O(n^2)      O(log n)   no     (random pivot avoids the worst)
 */
public class DivideAndConquerSorts {

    // HOOK: split, sort halves, MERGE. Allocate the temp buffer ONCE, not per call.
    static void mergeSort(int[] a) {
        if (a.length < 2) return;
        split(a, new int[a.length], 0, a.length - 1);
    }

    static void split(int[] a, int[] tmp, int lo, int hi) {
        if (lo >= hi) return;
        int mid = lo + (hi - lo) / 2;
        split(a, tmp, lo, mid);
        split(a, tmp, mid + 1, hi);
        merge(a, tmp, lo, mid, hi);
    }

    static void merge(int[] a, int[] tmp, int lo, int mid, int hi) {
        System.arraycopy(a, lo, tmp, lo, hi - lo + 1);
        int i = lo, j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            if (i > mid) a[k] = tmp[j++];
            else if (j > hi) a[k] = tmp[i++];
            else if (tmp[i] <= tmp[j]) a[k] = tmp[i++];            // <= keeps it STABLE
            else a[k] = tmp[j++];
        }
    }

    static final Random RND = new Random(7);

    // HOOK: partition around a pivot, then recurse on both sides. Nothing to merge.
    static void quickSort(int[] a) { quick(a, 0, a.length - 1); }

    static void quick(int[] a, int lo, int hi) {
        if (lo >= hi) return;
        int p = partition(a, lo, hi);
        quick(a, lo, p - 1);
        quick(a, p + 1, hi);
    }

    // HOOK: Lomuto - random pivot to the end, sweep, swap every smaller value forward.
    static int partition(int[] a, int lo, int hi) {
       // swap(a, lo + RND.nextInt(hi - lo + 1), hi);               // random pivot dodges O(n^2)
        int pivot = a[hi], i = lo;
        for (int j = lo; j < hi; j++) if (a[j] < pivot) swap(a, i++, j);
        swap(a, i, hi);                                           // pivot into its final slot
        return i;
    }

    // HOOK: quickSort but recurse into ONE side only. O(n) average - the kth largest trick.
    static int quickSelect(int[] a, int k) {
        int target = a.length - k;                                // kth largest = index n-k
        int lo = 0, hi = a.length - 1;
        while (true) {
            int p = partition(a, lo, hi);
            if (p == target) return a[p];
            if (p < target) lo = p + 1; else hi = p - 1;
        }
    }

    static void swap(int[] a, int i, int j) { int t = a[i]; a[i] = a[j]; a[j] = t; }

    static int[] sample() { return new int[]{5, 2, 9, 1, 5, 6, 0, -3}; }

    static String check(int[] a) {
        int[] expected = sample();
        Arrays.sort(expected);
        return Arrays.toString(a) + (Arrays.equals(a, expected) ? "  OK" : "  WRONG");
    }

    public static void main(String[] args) {
        int[] m = sample(); mergeSort(m);
        System.out.println("mergeSort     = " + check(m));

        int[] q = sample(); quickSort(q);
        System.out.println("quickSort     = " + check(q));

        System.out.println("quickSelect 1st largest = " + quickSelect(sample(), 1));
        System.out.println("quickSelect 3rd largest = " + quickSelect(sample(), 3));
    }
}
