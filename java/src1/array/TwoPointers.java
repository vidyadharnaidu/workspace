import java.util.*;

/** PATTERN: sort (or already sorted) -> shrink from both ends. */
public class TwoPointers {

    // HOOK: sorted -> sum too big? pull right in. too small? push left out.
    static int[] twoSumSorted(int[] a, int target) {
        int l = 0, r = a.length - 1;
        while (l < r) {
            int s = a[l] + a[r];
            if (s == target) return new int[]{l + 1, r + 1};
            if (s < target) l++; else r--;
        }
        return new int[]{-1, -1};
    }

    // HOOK: sort, fix i, two-pointer the rest; skip duplicates at every level.
    static List<List<Integer>> threeSum(int[] a) {
        Arrays.sort(a);
        List<List<Integer>> res = new ArrayList<>();
        for (int i = 0; i < a.length - 2; i++) {
            if (i > 0 && a[i] == a[i - 1]) continue;
            int l = i + 1, r = a.length - 1;
            while (l < r) {
                int s = a[i] + a[l] + a[r];
                if (s == 0) {
                    res.add(Arrays.asList(a[i], a[l], a[r]));
                    while (l < r && a[l] == a[l + 1]) l++;
                    while (l < r && a[r] == a[r - 1]) r--;
                    l++; r--;
                } else if (s < 0) l++; else r--;
            }
        }
        return res;
    }

    // HOOK: same walk as threeSum, but track best |sum - target| instead of == 0.
    static int threeSumClosest(int[] a, int target) {
        Arrays.sort(a);
        int best = a[0] + a[1] + a[2];
        for (int i = 0; i < a.length - 2; i++) {
            int l = i + 1, r = a.length - 1;
            while (l < r) {
                int s = a[i] + a[l] + a[r];
                if (Math.abs(s - target) < Math.abs(best - target)) best = s;
                if (s < target) l++; else if (s > target) r--; else return s;
            }
        }
        return best;
    }

    // HOOK: threeSum with one more outer loop; long sum to dodge overflow.
    static List<List<Integer>> fourSum(int[] a, int target) {
        Arrays.sort(a);
        List<List<Integer>> res = new ArrayList<>();
        int n = a.length;
        for (int i = 0; i < n - 3; i++) {
            if (i > 0 && a[i] == a[i - 1]) continue;
            for (int j = i + 1; j < n - 2; j++) {
                if (j > i + 1 && a[j] == a[j - 1]) continue;
                int l = j + 1, r = n - 1;
                while (l < r) {
                    long s = (long) a[i] + a[j] + a[l] + a[r];
                    if (s == target) {
                        res.add(Arrays.asList(a[i], a[j], a[l], a[r]));
                        while (l < r && a[l] == a[l + 1]) l++;
                        while (l < r && a[r] == a[r - 1]) r--;
                        l++; r--;
                    } else if (s < target) l++; else r--;
                }
            }
        }
        return res;
    }

    // HOOK: area = width * min(wall); always move the SHORTER wall inward.
    static int maxArea(int[] h) {
        int l = 0, r = h.length - 1, best = 0;
        while (l < r) {
            best = Math.max(best, (r - l) * Math.min(h[l], h[r]));
            if (h[l] < h[r]) l++; else r--;
        }
        return best;
    }

    // HOOK: water(i) = min(maxLeft, maxRight) - h[i]; always advance the lower side.
    static int trap(int[] h) {
        int l = 0, r = h.length - 1, lm = 0, rm = 0, water = 0;
        while (l < r) {
            if (h[l] < h[r]) {
                lm = Math.max(lm, h[l]);
                water += lm - h[l++];
            } else {
                rm = Math.max(rm, h[r]);
                water += rm - h[r--];
            }
        }
        return water;
    }

    // HOOK: Dutch flag - 0s land left of lo, 2s land right of hi, mid walks.
    static void sortColors(int[] a) {
        int lo = 0, mid = 0, hi = a.length - 1;
        while (mid <= hi) {
            if (a[mid] == 0) swap(a, lo++, mid++);
            else if (a[mid] == 2) swap(a, mid, hi--);
            else mid++;
        }
    }

    // HOOK: slow marks the next non-zero slot; swap every non-zero forward.
    static void moveZeroes(int[] a) {
        int slow = 0;
        for (int fast = 0; fast < a.length; fast++)
            if (a[fast] != 0) swap(a, slow++, fast);
    }

    // HOOK: write pointer keeps only values differing from the last kept one.
    static int removeDuplicates(int[] a) {
        if (a.length == 0) return 0;
        int k = 1;
        for (int i = 1; i < a.length; i++)
            if (a[i] != a[k - 1]) a[k++] = a[i];
        return k;
    }

    // HOOK: write pointer simply skips val.
    static int removeElement(int[] a, int val) {
        int k = 0;
        for (int x : a) if (x != val) a[k++] = x;
        return k;
    }

    // HOOK: merge from the BACK so you never overwrite an unread nums1 slot.
    static void merge(int[] a, int m, int[] b, int n) {
        int i = m - 1, j = n - 1, w = m + n - 1;
        while (j >= 0) a[w--] = (i >= 0 && a[i] > b[j]) ? a[i--] : b[j--];
    }

    // HOOK: run start, extend while consecutive, emit "s" or "s->e".
    static List<String> summaryRanges(int[] a) {
        List<String> res = new ArrayList<>();
        for (int i = 0; i < a.length; ) {
            int s = i;
            while (i + 1 < a.length && a[i + 1] == a[i] + 1) i++;
            res.add(s == i ? "" + a[s] : a[s] + "->" + a[i]);
            i++;
        }
        return res;
    }

    // HOOK: sort, pair smallest with largest; every pair must hit the same sum.
    static boolean dividePlayers(int[] skill) {
        Arrays.sort(skill);
        int l = 0, r = skill.length - 1, target = skill[l] + skill[r];
        while (l < r) {
            if (skill[l] + skill[r] != target) return false;
            l++; r--;
        }
        return true;
    }

    static void swap(int[] a, int i, int j) { int t = a[i]; a[i] = a[j]; a[j] = t; }

    public static void main(String[] args) {
        System.out.println("twoSumSorted  = " + Arrays.toString(twoSumSorted(new int[]{2, 7, 11, 15}, 9)));
        System.out.println("threeSum      = " + threeSum(new int[]{-1, 0, 1, 2, -1, -4}));
        System.out.println("threeSumClose = " + threeSumClosest(new int[]{-1, 2, 1, -4}, 1));
        System.out.println("fourSum       = " + fourSum(new int[]{1, 0, -1, 0, -2, 2}, 0));
        System.out.println("maxArea       = " + maxArea(new int[]{1, 8, 6, 2, 5, 4, 8, 3, 7}));
        System.out.println("trap          = " + trap(new int[]{0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1}));

        int[] colors = {2, 0, 2, 1, 1, 0};
        sortColors(colors);
        System.out.println("sortColors    = " + Arrays.toString(colors));

        int[] z = {0, 1, 0, 3, 12};
        moveZeroes(z);
        System.out.println("moveZeroes    = " + Arrays.toString(z));

        int[] dup = {1, 1, 2, 2, 3};
        System.out.println("removeDups    = " + removeDuplicates(dup) + " " + Arrays.toString(dup));

        int[] rm = {3, 2, 2, 3};
        System.out.println("removeElement = " + removeElement(rm, 3) + " " + Arrays.toString(rm));

        int[] m1 = {1, 2, 3, 0, 0, 0};
        merge(m1, 3, new int[]{2, 5, 6}, 3);
        System.out.println("merge         = " + Arrays.toString(m1));

        System.out.println("summaryRanges = " + summaryRanges(new int[]{0, 1, 2, 4, 5, 7}));
        System.out.println("dividePlayers = " + dividePlayers(new int[]{3, 2, 5, 1, 3, 4}));
    }
}
