import java.util.*;
import java.util.stream.Collectors;

/** PATTERN: trade memory for a second pass - count, seen-set, or complement lookup. */
public class HashCount {

    // HOOK: distinct count differs from length iff something repeats.
    static boolean containsDuplicate(int[] a) {
        return Arrays.stream(a).distinct().count() != a.length;
    }

    // HOOK: Boyer-Moore - count++ on match, count-- otherwise; swap candidate at 0.
    static int majorityElement(int[] a) {
        int cand = a[0], count = 0;
        for (int x : a) {
            if (count == 0) cand = x;
            count += (x == cand) ? 1 : -1;
        }
        return cand;
    }

    // HOOK: one array into a set, then filter the other and dedupe.
    static int[] intersection(int[] a, int[] b) {
        Set<Integer> s = Arrays.stream(a).boxed().collect(Collectors.toSet());
        return Arrays.stream(b).distinct().filter(s::contains).toArray();
    }

    // HOOK: k>0 -> look for x+k in a set; k==0 -> count values seen twice.
    static int findPairs(int[] a, int k) {
        Map<Integer, Integer> cnt = new HashMap<>();
        for (int x : a) cnt.merge(x, 1, Integer::sum);
        int res = 0;
        for (Map.Entry<Integer, Integer> e : cnt.entrySet())
            if (k == 0 ? e.getValue() > 1 : cnt.containsKey(e.getKey() + k)) res++;
        return res;
    }

    // HOOK: pairs contributed by a new x = how many x we already saw.
    static int numIdenticalPairs(int[] a) {
        Map<Integer, Integer> cnt = new HashMap<>();
        int res = 0;
        for (int x : a) res += cnt.merge(x, 1, Integer::sum) - 1;
        return res;
    }

    // HOOK: bucket by t%60, pair r with 60-r; r==0 and r==30 pair with themselves.
    static int numPairsDivisibleBy60(int[] time) {
        int[] rem = new int[60];
        int res = 0;
        for (int t : time) {
            int r = t % 60;
            res += rem[(60 - r) % 60];
            rem[r]++;
        }
        return res;
    }

    // HOOK: sort, then greedily consume x and its double from a count map.
    static int[] findOriginalArray(int[] changed) {
        if (changed.length % 2 != 0) return new int[0];
        Arrays.sort(changed);
        Map<Integer, Integer> cnt = new HashMap<>();
        for (int x : changed) cnt.merge(x, 1, Integer::sum);
        int[] res = new int[changed.length / 2];
        int k = 0;
        for (int x : changed) {
            if (cnt.getOrDefault(x, 0) == 0) continue;
            cnt.merge(x, -1, Integer::sum);
            if (cnt.getOrDefault(2 * x, 0) == 0) return new int[0];
            cnt.merge(2 * x, -1, Integer::sum);
            res[k++] = x;
        }
        return res;
    }

    // HOOK: sort, then each value must be at least previous+1.
    static int minIncrementForUnique(int[] a) {
        Arrays.sort(a);
        int moves = 0, need = 0;
        for (int x : a) {
            moves += Math.max(0, need - x);
            need = Math.max(x, need) + 1;
        }
        return moves;
    }

    // HOOK: the destination is the city that is never a source.
    static String destCity(List<List<String>> paths) {
        Set<String> sources = new HashSet<>();
        for (List<String> p : paths) sources.add(p.get(0));
        for (List<String> p : paths) if (!sources.contains(p.get(1))) return p.get(1);
        return "";
    }

    // HOOK: keep doubling while the value is still in the set.
    static int findFinalValue(int[] a, int original) {
        Set<Integer> s = new HashSet<>();
        for (int x : a) s.add(x);
        while (s.contains(original)) original *= 2;
        return original;
    }

    // HOOK: remember each char's FIRST index; answer is last - first - 1.
    static int maxLengthBetweenEqualCharacters(String s) {
        Map<Character, Integer> first = new HashMap<>();
        int best = -1;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (first.containsKey(c)) best = Math.max(best, i - first.get(c) - 1);
            else first.put(c, i);
        }
        return best;
    }

    public static void main(String[] args) {
        System.out.println("containsDuplicate = " + containsDuplicate(new int[]{1, 2, 3, 1}));
        System.out.println("majorityElement   = " + majorityElement(new int[]{2, 2, 1, 1, 1, 2, 2}));
        System.out.println("intersection      = " + Arrays.toString(intersection(new int[]{4, 9, 5}, new int[]{9, 4, 9, 8, 4})));
        System.out.println("findPairs k=2     = " + findPairs(new int[]{3, 1, 4, 1, 5}, 2));
        System.out.println("identicalPairs    = " + numIdenticalPairs(new int[]{1, 2, 3, 1, 1, 3}));
        System.out.println("divisibleBy60     = " + numPairsDivisibleBy60(new int[]{30, 20, 150, 100, 40}));
        System.out.println("findOriginalArray = " + Arrays.toString(findOriginalArray(new int[]{1, 3, 4, 2, 6, 8})));
        System.out.println("minIncrement      = " + minIncrementForUnique(new int[]{3, 2, 1, 2, 1, 7}));
        System.out.println("destCity          = " + destCity(List.of(
                List.of("London", "New York"), List.of("New York", "Lima"), List.of("Lima", "Sao Paulo"))));
        System.out.println("findFinalValue    = " + findFinalValue(new int[]{5, 3, 6, 1, 12}, 3));
        System.out.println("maxLenBetween     = " + maxLengthBetweenEqualCharacters("abca"));
    }
}
