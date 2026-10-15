import java.util.*;

/**
 * PATTERN: the three O(n^2) sorts. Tell them apart by WHAT the inner loop does:
 *   bubble    - swaps ADJACENT pairs, biggest bubbles to the end
 *   selection - SCANS for the minimum, one swap per pass
 *   insertion - SHIFTS bigger elements right, drops the key into the hole
 *
 *              best     avg      worst    stable?  in-place?
 *   bubble     O(n)*    O(n^2)   O(n^2)   yes      yes     (*with the early exit)
 *   selection  O(n^2)   O(n^2)   O(n^2)   no       yes     (always n^2 - no early exit)
 *   insertion  O(n)     O(n^2)   O(n^2)   yes      yes     (best on nearly-sorted data)
 */
public class SimpleSorts {

    // HOOK: adjacent swaps; the largest "bubbles" to position i each pass.
    static void bubbleSort(int[] a) {
        for (int i = a.length - 1; i > 0; i--) {
            boolean swapped = false;
            for (int j = 0; j < i; j++)
                if (a[j] > a[j + 1]) { swap(a, j, j + 1); swapped = true; }
            if (!swapped) return;                                 // already sorted -> O(n)
        }
    }

    // HOOK: find the min in the unsorted tail, swap it into place. One swap per pass.
    static void selectionSort(int[] a) {
        for (int i = 0; i < a.length - 1; i++) {
            int min = i;
            for (int j = i + 1; j < a.length; j++) if (a[j] < a[min]) min = j;
            swap(a, i, min);
        }
    }

    // HOOK: hold a[i] as the key, shift everything bigger right, drop the key in.
    static void insertionSort(int[] a) {
        for (int i = 1; i < a.length; i++) {
            int key = a[i], j = i - 1;
            while (j >= 0 && a[j] > key) a[j + 1] = a[j--];        // shift, do not swap
            a[j + 1] = key;
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
        int[] b = sample(); bubbleSort(b);
        System.out.println("bubbleSort    = " + check(b));

        int[] s = sample(); selectionSort(s);
        System.out.println("selectionSort = " + check(s));

        int[] i = sample(); insertionSort(i);
        System.out.println("insertionSort = " + check(i));

        int[] sorted = {1, 2, 3, 4, 5};
        bubbleSort(sorted);
        System.out.println("bubble on sorted (early exit) = " + Arrays.toString(sorted));
    }
}
