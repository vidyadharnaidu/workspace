package math;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.*;

/**
 * Math & number problems (+ a few bit tricks).
 *
 * TRICKS:
 *  - DIGITS: last digit = n % 10, drop it = n / 10, append = rev * 10 + d
 *  - DIVISORS / PRIME CHECK: only loop i * i <= n (divisors come in pairs i and n / i)
 *  - MANY PRIMES: Sieve of Eratosthenes, cross out multiples starting at i * i
 *  - GCD: gcd(a, b) = gcd(b, a % b);  LCM = a / gcd * b
 *  - POWER: square the base, halve the exponent -> O(log n)
 *  - OVERFLOW: compute in long, or Math.addExact / multiplyExact, or BigInteger
 *  - "Add two numbers as strings/binary": walk from the END with a carry
 *  - Cycle in a number sequence (happy number) -> Set of seen values (or slow/fast)
 *  - BITS: x & (x - 1) removes the lowest set bit; a ^ a = 0; Integer.bitCount(x)
 *  - Already elsewhere: fizzbuzz, fibonacci, factorial, isPrime, armstrong, gcd, reverseInt
 *    (algorithms/ClassicAlgorithms), missing number (arrays), sqrt (binarysearch)
 */
public class MathProblems {

    // 1. SIEVE OF ERATOSTHENES: primes up to 30 -> [2, 3, 5, 7, 11, 13, 17, 19, 23, 29]
    static List<Integer> sieve(int n) {
        boolean[] composite = new boolean[n + 1];
        for (int i = 2; (long) i * i <= n; i++)
            if (!composite[i])
                for (int j = i * i; j <= n; j += i) composite[j] = true;
        return IntStream.rangeClosed(2, n).filter(i -> !composite[i]).boxed().toList();
    }

    // 2. PRIME FACTORS: 360 -> [2, 2, 2, 3, 3, 5]
    static List<Integer> primeFactors(int n) {
        List<Integer> res = new ArrayList<>();
        for (int p = 2; (long) p * p <= n; p++)
            while (n % p == 0) {
                res.add(p);
                n /= p;
            }
        if (n > 1) res.add(n); // leftover is a prime bigger than sqrt
        return res;
    }

    // 3. GCD / LCM (also of an array with streams)
    static int gcd(int a, int b) { return b == 0 ? a : gcd(b, a % b); }
    static long lcm(long a, long b) { return a / gcd((int) a, (int) b) * b; }

    static int gcdOfArray(int[] a) { return Arrays.stream(a).reduce(0, MathProblems::gcd); }
    static long lcmOfArray(int[] a) { return Arrays.stream(a).asLongStream().reduce(1, MathProblems::lcm); }

    // 4. FAST POWER: pow(2.0, 10) = 1024, pow(2.0, -2) = 0.25
    static double pow(double x, long n) {
        if (n < 0) { x = 1 / x; n = -n; }
        double result = 1;
        while (n > 0) {
            if ((n & 1) == 1) result *= x; // odd exponent -> take one x
            x *= x;
            n >>= 1;
        }
        return result;
    }

    // 4b. MODULAR POWER: (base^exp) % mod, used for huge answers "mod 1_000_000_007"
    static long modPow(long base, long exp, long mod) {
        long result = 1;
        base %= mod;
        while (exp > 0) {
            if ((exp & 1) == 1) result = result * base % mod;
            base = base * base % mod;
            exp >>= 1;
        }
        return result;
    }

    // 5. PALINDROME NUMBER without String: 121 -> true, -121 -> false, 10 -> false
    // TRICK: reverse only HALF the digits
    static boolean isPalindrome(int x) {
        if (x < 0 || (x % 10 == 0 && x != 0)) return false;
        int rev = 0;
        while (x > rev) {
            rev = rev * 10 + x % 10;
            x /= 10;
        }
        return x == rev || x == rev / 10; // odd length: drop middle digit
    }

    // 6. REVERSE INTEGER with overflow check: -123 -> -321, 1534236469 -> 0
    static int reverse(int x) {
        long rev = 0;
        while (x != 0) {
            rev = rev * 10 + x % 10;
            x /= 10;
        }
        return rev > Integer.MAX_VALUE || rev < Integer.MIN_VALUE ? 0 : (int) rev;
    }

    // 7. PERFECT NUMBER (sum of proper divisors == n): 28 -> true (1+2+4+7+14)
    static boolean isPerfect(int n) {
        if (n <= 1) return false;
        int sum = 1;
        for (int i = 2; i * i <= n; i++)
            if (n % i == 0) sum += i + (i != n / i ? n / i : 0);
        return sum == n;
    }

    // 8. TRAILING ZEROS IN n!: 25 -> 6
    // TRICK: count factors of 5 (there are always more 2s): n/5 + n/25 + n/125 ...
    static int trailingZeros(int n) {
        int count = 0;
        while (n > 0) {
            n /= 5;
            count += n;
        }
        return count;
    }

    // 9. EXCEL COLUMN: "AB" -> 28 ; 28 -> "AB" ; 701 -> "ZY"
    static int titleToNumber(String s) {
        return s.chars().reduce(0, (acc, c) -> acc * 26 + (c - 'A' + 1));
    }

    static String numberToTitle(int n) {
        StringBuilder sb = new StringBuilder();
        while (n > 0) {
            n--;                               // 1-based -> 0-based (no zero digit in Excel)
            sb.append((char) ('A' + n % 26));
            n /= 26;
        }
        return sb.reverse().toString();
    }

    // 10. ROMAN NUMERALS: "MCMXCIV" -> 1994 and back
    static final Map<Character, Integer> ROMAN = Map.of('I', 1, 'V', 5, 'X', 10, 'L', 50, 'C', 100, 'D', 500, 'M', 1000);

    // TRICK: if a symbol is smaller than the next one, SUBTRACT it (IV = -1 + 5)
    static int romanToInt(String s) {
        int total = 0;
        for (int i = 0; i < s.length(); i++) {
            int v = ROMAN.get(s.charAt(i));
            if (i + 1 < s.length() && v < ROMAN.get(s.charAt(i + 1))) total -= v;
            else total += v;
        }
        return total;
    }

    // TRICK: greedy with the 13 values INCLUDING the pairs (900 CM, 400 CD, 90 XC, ...)
    static String intToRoman(int num) {
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] symbols = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++)
            while (num >= values[i]) {
                sb.append(symbols[i]);
                num -= values[i];
            }
        return sb.toString();
    }

    // 11. ADD BINARY: "1010" + "1011" -> "10101"   (change 2 -> 10 for "add strings")
    static String addBinary(String a, String b) {
        StringBuilder sb = new StringBuilder();
        int i = a.length() - 1, j = b.length() - 1, carry = 0;
        while (i >= 0 || j >= 0 || carry > 0) {
            int sum = carry + (i >= 0 ? a.charAt(i--) - '0' : 0) + (j >= 0 ? b.charAt(j--) - '0' : 0);
            sb.append(sum % 2);
            carry = sum / 2;
        }
        return sb.reverse().toString();
    }

    // 12. MULTIPLY STRINGS: "123" * "456" -> "56088"
    // TRICK: digit i * digit j lands in positions [i + j, i + j + 1]
    static String multiply(String a, String b) {
        int[] res = new int[a.length() + b.length()];
        for (int i = a.length() - 1; i >= 0; i--)
            for (int j = b.length() - 1; j >= 0; j--) {
                int sum = (a.charAt(i) - '0') * (b.charAt(j) - '0') + res[i + j + 1];
                res[i + j + 1] = sum % 10;
                res[i + j] += sum / 10;
            }
        String s = Arrays.stream(res).mapToObj(String::valueOf).collect(Collectors.joining()).replaceFirst("^0+", "");
        return s.isEmpty() ? "0" : s;
    }

    // 13. PLUS ONE: [1,2,9] -> [1,3,0], [9,9] -> [1,0,0]
    static int[] plusOne(int[] digits) {
        int[] d = digits.clone();
        for (int i = d.length - 1; i >= 0; i--) {
            if (d[i] < 9) { d[i]++; return d; }
            d[i] = 0;
        }
        int[] res = new int[d.length + 1]; // all were 9
        res[0] = 1;
        return res;
    }

    // 14. HAPPY NUMBER: 19 -> 1^2+9^2=82 -> 68 -> 100 -> 1 -> true; 2 -> loops -> false
    static boolean isHappy(int n) {
        Set<Integer> seen = new HashSet<>();
        while (n != 1 && seen.add(n))
            n = String.valueOf(n).chars().map(c -> (c - '0') * (c - '0')).sum();
        return n == 1;
    }

    // 15. DIGITAL ROOT (add digits until one digit): 38 -> 3+8=11 -> 2
    static int addDigits(int n) {
        return n == 0 ? 0 : 1 + (n - 1) % 9;
    }

    // 16. FRACTION TO RECURRING DECIMAL: 1/2 -> "0.5", 4/333 -> "0.(012)", -50/8 -> "-6.25"
    // TRICK: long division; when a REMAINDER repeats, the digits since then repeat
    static String fractionToDecimal(int numerator, int denominator) {
        if (numerator == 0) return "0";
        StringBuilder sb = new StringBuilder();
        if ((numerator < 0) ^ (denominator < 0)) sb.append('-');
        long n = Math.abs((long) numerator), d = Math.abs((long) denominator);
        sb.append(n / d);
        long rem = n % d;
        if (rem == 0) return sb.toString();
        sb.append('.');
        Map<Long, Integer> seenAt = new HashMap<>();
        while (rem != 0) {
            if (seenAt.containsKey(rem)) {
                sb.insert((int) seenAt.get(rem), "(").append(')');
                break;
            }
            seenAt.put(rem, sb.length());
            rem *= 10;
            sb.append(rem / d);
            rem %= d;
        }
        return sb.toString();
    }

    // 17. nCr (combinations) without overflow from factorials: C(5,2) = 10
    static long nCr(int n, int k) {
        k = Math.min(k, n - k);
        long res = 1;
        for (int i = 1; i <= k; i++) res = res * (n - k + i) / i; // always divides exactly
        return res;
    }

    // 18. NTH UGLY NUMBER (factors only 2, 3, 5): 10 -> 12
    // TRICK: 3 pointers, next = min(u[i2]*2, u[i3]*3, u[i5]*5), advance ALL pointers that match
    static int nthUgly(int n) {
        int[] u = new int[n];
        u[0] = 1;
        int i2 = 0, i3 = 0, i5 = 0;
        for (int k = 1; k < n; k++) {
            u[k] = Math.min(u[i2] * 2, Math.min(u[i3] * 3, u[i5] * 5));
            if (u[k] == u[i2] * 2) i2++;
            if (u[k] == u[i3] * 3) i3++;
            if (u[k] == u[i5] * 5) i5++;
        }
        return u[n - 1];
    }

    // 19. BASE CONVERSION: 255 -> "11111111" (base 2), "ff" (base 16)
    static String toBase(int n, int base) {
        if (n == 0) return "0";
        StringBuilder sb = new StringBuilder();
        for (; n > 0; n /= base) sb.append(Character.forDigit(n % base, base));
        return sb.reverse().toString();
    }

    // 20. GCD OF STRINGS: "ABCABC", "ABC" -> "ABC";  "LEET", "CODE" -> ""
    // TRICK: a common divisor exists only if a+b == b+a; its length is gcd(lengths)
    static String gcdOfStrings(String a, String b) {
        return (a + b).equals(b + a) ? a.substring(0, gcd(a.length(), b.length())) : "";
    }

    // 21. LEAP YEAR: divisible by 4, except centuries, unless divisible by 400
    static boolean isLeap(int year) {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
    }

    // 22. CLOCK ANGLE: 3:30 -> 75.0
    // hour hand moves 30 deg/hour + 0.5 deg/minute, minute hand 6 deg/minute
    static double clockAngle(int hour, int minutes) {
        double diff = Math.abs((30 * (hour % 12) + 0.5 * minutes) - 6 * minutes);
        return Math.min(diff, 360 - diff);
    }

    // 23. RECTANGLE OVERLAP [x1,y1,x2,y2]: overlap on BOTH axes
    static boolean rectanglesOverlap(int[] a, int[] b) {
        return a[0] < b[2] && b[0] < a[2] && a[1] < b[3] && b[1] < a[3];
    }

    // 24. PYTHAGOREAN TRIPLETS up to n (streams flatMap)
    static List<List<Integer>> pythagoreanTriplets(int n) {
        return IntStream.rangeClosed(1, n).boxed()
                .flatMap(a -> IntStream.rangeClosed(a, n)
                        .filter(b -> {
                            double c = Math.sqrt(a * a + b * b);
                            return c == Math.floor(c) && c <= n;
                        })
                        .mapToObj(b -> List.of(a, b, (int) Math.sqrt(a * a + b * b))))
                .toList();
    }

    // 25. FISHER-YATES SHUFFLE: swap each position i with a random index in [0, i]
    static int[] shuffle(int[] a, Random rnd) {
        int[] s = a.clone();
        for (int i = s.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = s[i]; s[i] = s[j]; s[j] = t;
        }
        return s;
    }

    // 26. BIG NUMBERS: 30! with BigInteger
    static BigInteger bigFactorial(int n) {
        return IntStream.rangeClosed(1, n).mapToObj(BigInteger::valueOf).reduce(BigInteger.ONE, BigInteger::multiply);
    }

    // ---------- Bit tricks ----------

    // SINGLE NUMBER (others appear twice): [4,1,2,1,2] -> 4.  a ^ a = 0, a ^ 0 = a
    static int singleNumber(int[] a) { return Arrays.stream(a).reduce(0, (x, y) -> x ^ y); }

    // POWER OF TWO: exactly one bit set
    static boolean isPowerOfTwo(int n) { return n > 0 && (n & (n - 1)) == 0; }

    // COUNT SET BITS (manual): drop lowest set bit until zero. 11 (1011) -> 3
    static int countBits(int n) {
        int count = 0;
        for (; n != 0; n &= n - 1) count++;
        return count;
    }

    // COUNTING BITS 0..n: bits[i] = bits[i >> 1] + (i & 1).  5 -> [0,1,1,2,1,2]
    static int[] countingBits(int n) {
        int[] bits = new int[n + 1];
        for (int i = 1; i <= n; i++) bits[i] = bits[i >> 1] + (i & 1);
        return bits;
    }

    // HAMMING DISTANCE: differing bits = bitCount(x ^ y). 1 (001) vs 4 (100) -> 2
    static int hammingDistance(int x, int y) { return Integer.bitCount(x ^ y); }

    public static void main(String[] args) {
        System.out.println("sieve(30)          " + sieve(30));
        System.out.println("primeFactors(360)  " + primeFactors(360));                                    // [2, 2, 2, 3, 3, 5]
        System.out.println("factor counts      " + primeFactors(360).stream()
                .collect(Collectors.groupingBy(p -> p, TreeMap::new, Collectors.counting())));             // {2=3, 3=2, 5=1}
        System.out.println("gcd/lcm            " + gcd(12, 18) + " " + lcm(4, 6) + " | array " + gcdOfArray(new int[]{12, 18, 24}) + " " + lcmOfArray(new int[]{2, 3, 4})); // 6 12 | array 6 12
        System.out.println("pow                " + pow(2.0, 10) + " " + pow(2.0, -2));                         // 1024.0 0.25
        System.out.println("modPow 2^100       " + modPow(2, 100, 1_000_000_007) + " (BigInteger: "
                + BigInteger.TWO.modPow(BigInteger.valueOf(100), BigInteger.valueOf(1_000_000_007)) + ")");
        System.out.println("isPalindrome       " + isPalindrome(121) + " " + isPalindrome(-121) + " " + isPalindrome(10) + " " + isPalindrome(12321)); // true false false true
        System.out.println("reverse            " + reverse(-123) + " " + reverse(1534236469));                  // -321 0
        System.out.println("isPerfect          " + isPerfect(28) + " " + isPerfect(12));                        // true false
        System.out.println("trailingZeros(25!) " + trailingZeros(25));                                          // 6
        System.out.println("excel              " + titleToNumber("AB") + " " + numberToTitle(28) + " " + numberToTitle(701)); // 28 AB ZY
        System.out.println("roman              " + romanToInt("MCMXCIV") + " " + intToRoman(1994) + " " + intToRoman(3749)); // 1994 MCMXCIV MMMDCCXLIX
        System.out.println("addBinary          " + addBinary("1010", "1011"));                                  // 10101
        System.out.println("multiplyStrings    " + multiply("123", "456") + " " + multiply("0", "52"));          // 56088 0
        System.out.println("plusOne            " + Arrays.toString(plusOne(new int[]{1, 2, 9})) + " " + Arrays.toString(plusOne(new int[]{9, 9}))); // [1, 3, 0] [1, 0, 0]
        System.out.println("isHappy            " + isHappy(19) + " " + isHappy(2));                              // true false
        System.out.println("digitalRoot(38)    " + addDigits(38));                                               // 2
        System.out.println("fraction           " + fractionToDecimal(1, 2) + " " + fractionToDecimal(4, 333) + " " + fractionToDecimal(-50, 8) + " " + fractionToDecimal(1, 6)); // 0.5 0.(012) -6.25 0.1(6)
        System.out.println("nCr                " + nCr(5, 2) + " " + nCr(52, 5));                                // 10 2598960
        System.out.println("nthUgly(10)        " + nthUgly(10));                                                 // 12
        System.out.println("toBase             " + toBase(255, 2) + " " + toBase(255, 16) + " " + Integer.toString(255, 16)); // 11111111 ff ff
        System.out.println("gcdOfStrings       '" + gcdOfStrings("ABCABC", "ABC") + "' '" + gcdOfStrings("LEET", "CODE") + "'"); // 'ABC' ''
        System.out.println("isLeap             " + isLeap(2024) + " " + isLeap(1900) + " " + isLeap(2000));       // true false true
        System.out.println("clockAngle         " + clockAngle(3, 30) + " " + clockAngle(12, 0));                  // 75.0 0.0
        System.out.println("rectanglesOverlap  " + rectanglesOverlap(new int[]{0, 0, 2, 2}, new int[]{1, 1, 3, 3})
                + " " + rectanglesOverlap(new int[]{0, 0, 1, 1}, new int[]{1, 0, 2, 1}));                        // true false
        System.out.println("pythagorean(20)    " + pythagoreanTriplets(20));

        int[] shuffled = shuffle(new int[]{1, 2, 3, 4, 5}, new Random(42));
        System.out.println("shuffle            " + Arrays.toString(shuffled) + " sameElements="
                + Arrays.equals(Arrays.stream(shuffled).sorted().toArray(), new int[]{1, 2, 3, 4, 5}));
        System.out.println("30!                " + bigFactorial(30));                                            // 265252859812191058636308480000000

        System.out.println("singleNumber       " + singleNumber(new int[]{4, 1, 2, 1, 2}));                      // 4
        System.out.println("isPowerOfTwo       " + isPowerOfTwo(16) + " " + isPowerOfTwo(18));                   // true false
        System.out.println("countBits(11)      " + countBits(11) + " " + Integer.bitCount(11));                  // 3 3
        System.out.println("countingBits(5)    " + Arrays.toString(countingBits(5)));                            // [0, 1, 1, 2, 1, 2]
        System.out.println("hammingDistance    " + hammingDistance(1, 4));                                       // 2
    }
}
