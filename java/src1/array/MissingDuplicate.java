import java.util.*;

/** PATTERN: values are 1..n, so the array IS a hash map. Use index-as-key or XOR. */
public class MissingDuplicate {

    // HOOK: XOR indices against values; equal pairs cancel, the missing one survives.
    static int missingNumber(int[] a) {
        int x = a.length;
        for (int i = 0; i < a.length; i++) x ^= i ^ a[i];
        return x;
    }

    // HOOK: XOR everything -> a^b; split both groups by any set bit of a^b.
    static int[] twoMissingNumbers(int[] a, int n) {
        int x = 0;
        for (int i = 1; i <= n; i++) x ^= i;
        for (int v : a) x ^= v;
        int bit = x & -x;               // lowest set bit tells the two apart
        int p = 0, q = 0;
        for (int i = 1; i <= n; i++) { if ((i & bit) != 0) p ^= i; else q ^= i; }
        for (int v : a) { if ((v & bit) != 0) p ^= v; else q ^= v; }
        return new int[]{Math.min(p, q), Math.max(p, q)};
    }

    // HOOK: Floyd cycle - index chasing turns the array into a linked list.
    static int findDuplicate(int[] a) {
        int slow = a[0], fast = a[0];
        do { slow = a[slow]; fast = a[a[fast]]; } while (slow != fast);
        slow = a[0];
        while (slow != fast) { slow = a[slow]; fast = a[fast]; }
        return slow;
    }

    // HOOK: mark seen values by negating a[value-1]; positives are missing.
    static List<Integer> findDisappearedNumbers(int[] a) {
        for (int x : a) {
            int i = Math.abs(x) - 1;
            if (a[i] > 0) a[i] = -a[i];
        }
        List<Integer> res = new ArrayList<>();
        for (int i = 0; i < a.length; i++) if (a[i] > 0) res.add(i + 1);
        return res;
    }

    // HOOK: same negation trick - an already-negative slot names the duplicate.
    static int[] findErrorNums(int[] a) {
        int dup = -1;
        for (int x : a) {
            int i = Math.abs(x) - 1;
            if (a[i] < 0) dup = Math.abs(x); else a[i] = -a[i];
        }
        int missing = -1;
        for (int i = 0; i < a.length; i++) if (a[i] > 0) missing = i + 1;
        return new int[]{dup, missing};
    }

    // HOOK: cyclic sort - put value v at index v-1, then find the first mismatch.
    static int firstMissingPositive(int[] a) {
        int n = a.length;
        for (int i = 0; i < n; i++)
            while (a[i] > 0 && a[i] <= n && a[a[i] - 1] != a[i]) {
                int t = a[a[i] - 1];
                a[a[i] - 1] = a[i];
                a[i] = t;
            }
        for (int i = 0; i < n; i++) if (a[i] != i + 1) return i + 1;
        return n + 1;
    }

    public static void main(String[] args) {
        System.out.println("missingNumber     = " + missingNumber(new int[]{3, 0, 1}));
        System.out.println("twoMissing (n=6)  = " + Arrays.toString(twoMissingNumbers(new int[]{1, 3, 4, 6}, 6)));
        System.out.println("findDuplicate     = " + findDuplicate(new int[]{1, 3, 4, 2,2}));
        System.out.println("disappeared       = " + findDisappearedNumbers(new int[]{4, 3, 2, 7, 8, 2, 3, 1}));
        System.out.println("setMismatch       = " + Arrays.toString(findErrorNums(new int[]{1, 2, 2, 4})));
        System.out.println("firstMissingPos   = " + firstMissingPositive(new int[]{3, 4, -1, 1}));
    }
}
