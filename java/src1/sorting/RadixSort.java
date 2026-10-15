import java.util.*;

/**
 * PATTERN: NON-COMPARISON sorts. They beat the O(n log n) comparison lower bound
 * by using the values as ARRAY INDICES instead of comparing them.
 *
 *   counting - one bucket per value.   O(n + range).  Needs a small value range.
 *   radix    - counting sort per DIGIT, least significant first. O(d * (n + 10)).
 *
 * Radix only works because counting sort is STABLE: sorting by the tens digit
 * must preserve the order the units digit already established.
 */
public class RadixSort {

    // HOOK: count each value, prefix-sum the counts, then place from the BACK (stable).
    static void countingSort(int[] a) {
        if (a.length == 0) return;
        int min = Arrays.stream(a).min().getAsInt();
        int max = Arrays.stream(a).max().getAsInt();
        int[] count = new int[max - min + 1];
        for (int x : a) count[x - min]++;
        for (int i = 1; i < count.length; i++) count[i] += count[i - 1];
        int[] out = new int[a.length];
        for (int i = a.length - 1; i >= 0; i--) out[--count[a[i] - min]] = a[i];
        System.arraycopy(out, 0, a, 0, a.length);
    }

    // HOOK: counting sort by the 1s, then the 10s, then the 100s... until exp passes max.
    static void radixSort(int[] a) {
        if (a.length == 0) return;
        int min = Arrays.stream(a).min().getAsInt();
        int shift = min < 0 ? -min : 0;                           // make everything non-negative
        if (shift > 0) for (int i = 0; i < a.length; i++) a[i] += shift;

        int max = Arrays.stream(a).max().getAsInt();
        for (int exp = 1; max / exp > 0; exp *= 10) byDigit(a, exp);

        if (shift > 0) for (int i = 0; i < a.length; i++) a[i] -= shift;
    }

    static void byDigit(int[] a, int exp) {
        int[] out = new int[a.length], count = new int[10];
        for (int x : a) count[(x / exp) % 10]++;
        for (int i = 1; i < 10; i++) count[i] += count[i - 1];
        for (int i = a.length - 1; i >= 0; i--) {                 // BACK to front = stable
            int d = (a[i] / exp) % 10;
            out[--count[d]] = a[i];
        }
        System.arraycopy(out, 0, a, 0, a.length);
    }

    static int[] sample() { return new int[]{5, 2, 9, 1, 5, 6, 0, -3}; }

    static String check(int[] a) {
        int[] expected = sample();
        Arrays.sort(expected);
        return Arrays.toString(a) + (Arrays.equals(a, expected) ? "  OK" : "  WRONG");
    }

    public static void main(String[] args) {
        int[] c = sample(); countingSort(c);
        System.out.println("countingSort  = " + check(c));

        int[] r = sample(); radixSort(r);
        System.out.println("radixSort     = " + check(r));

        int[] big = {170, 45, 75, 90, 802, 24, 2, 66};
        radixSort(big);
        System.out.println("radix 3-digit = " + Arrays.toString(big));
    }
}
