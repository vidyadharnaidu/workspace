import java.util.*;

/** PATTERN: scan with an index cursor, build digits/terms, clamp overflow. */
public class StringParsing {

    // HOOK: trim, sign, digits; clamp with "res > MAX/10" BEFORE multiplying.
    static int myAtoi(String s) {
        int i = 0, n = s.length(), sign = 1;
        while (i < n && s.charAt(i) == ' ') i++;
        if (i < n && (s.charAt(i) == '+' || s.charAt(i) == '-')) sign = s.charAt(i++) == '-' ? -1 : 1;
        int res = 0;
        while (i < n && Character.isDigit(s.charAt(i))) {
            int d = s.charAt(i++) - '0';
            if (res > Integer.MAX_VALUE / 10 || (res == Integer.MAX_VALUE / 10 && d > 7))
                return sign == 1 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            res = res * 10 + d;
        }
        return sign * res;
    }

    // HOOK: grade-school multiply - digits i and j land in slots i+j and i+j+1.
    static String multiply(String a, String b) {
        if (a.equals("0") || b.equals("0")) return "0";
        int m = a.length(), n = b.length();
        int[] pos = new int[m + n];
        for (int i = m - 1; i >= 0; i--)
            for (int j = n - 1; j >= 0; j--) {
                int mul = (a.charAt(i) - '0') * (b.charAt(j) - '0') + pos[i + j + 1];
                pos[i + j + 1] = mul % 10;
                pos[i + j] += mul / 10;
            }
        StringBuilder sb = new StringBuilder();
        for (int p : pos) if (!(sb.length() == 0 && p == 0)) sb.append(p);
        return sb.toString();
    }

    // HOOK: long division; remember each remainder's position to bracket the cycle.
    static String fractionToDecimal(int numerator, int denominator) {
        if (numerator == 0) return "0";
        StringBuilder sb = new StringBuilder();
        if ((numerator < 0) ^ (denominator < 0)) sb.append('-');
        long num = Math.abs((long) numerator), den = Math.abs((long) denominator);
        sb.append(num / den);
        long rem = num % den;
        if (rem == 0) return sb.toString();
        sb.append('.');
        Map<Long, Integer> seen = new HashMap<>();
        while (rem != 0) {
            if (seen.containsKey(rem)) {
                sb.insert((int) seen.get(rem), '(').append(')');
                break;
            }
            seen.put(rem, sb.length());
            rem *= 10;
            sb.append(rem / den);
            rem %= den;
        }
        return sb.toString();
    }

    // HOOK: split keeping signs, add via a/b + c/d = (ad+cb)/bd, then reduce by gcd.
    static String fractionAddition(String expr) {
        long num = 0, den = 1;
        // insert a '+' before each leading term so split always yields signed terms
        for (String term : expr.replace("-", "+-").split("\\+")) {
            if (term.isEmpty()) continue;
            String[] p = term.split("/");
            long n = Long.parseLong(p[0]), d = Long.parseLong(p[1]);
            num = num * d + n * den;
            den *= d;
            long g = gcd(Math.abs(num), den);
            if (g != 0) { num /= g; den /= g; }
        }
        return num + "/" + den;
    }

    static long gcd(long a, long b) { return b == 0 ? a : gcd(b, a % b); }

    private static final String[] BELOW_20 = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven",
            "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen"};
    private static final String[] TENS = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty",
            "Seventy", "Eighty", "Ninety"};
    private static final String[] UNITS = {"", "Thousand", "Million", "Billion"};

    // HOOK: chunk into groups of 3 digits, name each chunk, append the unit.
    static String numberToWords(int num) {
        if (num == 0) return "Zero";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; num > 0; i++) {
            if (num % 1000 != 0) sb.insert(0, three(num % 1000) + UNITS[i] + " ");
            num /= 1000;
        }
        return sb.toString().trim().replaceAll(" +", " ");
    }

    static String three(int n) {
        if (n == 0) return "";
        if (n < 20) return BELOW_20[n] + " ";
        if (n < 100) return TENS[n / 10] + " " + three(n % 10);
        return BELOW_20[n / 100] + " Hundred " + three(n % 100);
    }

    public static void main(String[] args) {
        System.out.println("myAtoi            = " + myAtoi("   -42abc"));
        System.out.println("myAtoi overflow   = " + myAtoi("91283472332"));
        System.out.println("multiply          = " + multiply("123", "456"));
        System.out.println("fractionToDecimal = " + fractionToDecimal(4, 333));
        System.out.println("fractionToDec 1/2 = " + fractionToDecimal(1, 2));
        System.out.println("fractionAddition  = " + fractionAddition("-1/2+1/2+1/3"));
        System.out.println("numberToWords     = " + numberToWords(1234567));
        System.out.println("numberToWords 20  = " + numberToWords(20));
    }
}
