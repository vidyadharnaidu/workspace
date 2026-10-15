package algorithms;

import java.util.*;
import java.util.stream.*;

/**
 * Classic short programs - mostly one-liners with streams.
 */
public class ClassicAlgorithms {

    // FIZZBUZZ
    static List<String> fizzBuzz(int n) {
        return IntStream.rangeClosed(1, n)
                .mapToObj(i -> i % 15 == 0 ? "FizzBuzz" : i % 3 == 0 ? "Fizz" : i % 5 == 0 ? "Buzz" : String.valueOf(i))
                .toList();
    }

    // FIBONACCI: TRICK Stream.iterate over pairs [a, b] -> [b, a+b]
    static List<Long> fibonacci(int n) {
        return Stream.iterate(new long[]{0, 1}, f -> new long[]{f[1], f[0] + f[1]})
                .limit(n).map(f -> f[0]).toList();
    }

    // FACTORIAL
    static long factorial(int n) {
        return LongStream.rangeClosed(1, n).reduce(1, (a, b) -> a * b);
    }

    // PRIME: check divisors only up to sqrt(n)
    static boolean isPrime(int n) {
        return n > 1 && IntStream.rangeClosed(2, (int) Math.sqrt(n)).noneMatch(i -> n % i == 0);
    }

    static List<Integer> primesUpTo(int n) {
        return IntStream.rangeClosed(2, n).filter(ClassicAlgorithms::isPrime).boxed().toList();
    }

    // ARMSTRONG: 153 = 1^3 + 5^3 + 3^3
    static boolean isArmstrong(int n) {
        String s = String.valueOf(n);
        return s.chars().map(c -> (int) Math.pow(c - '0', s.length())).sum() == n;
    }

    // SUM OF DIGITS: 1234 -> 10
    static int sumDigits(int n) {
        return String.valueOf(Math.abs(n)).chars().map(c -> c - '0').sum();
    }

    // REVERSE INTEGER: 1234 -> 4321   TRICK: rev = rev*10 + n%10
    static int reverseInt(int n) {
        int rev = 0;
        while (n != 0) { rev = rev * 10 + n % 10; n /= 10; }
        return rev;
    }

    // GCD (Euclid): gcd(a, b) = gcd(b, a % b)
    static int gcd(int a, int b) { return b == 0 ? a : gcd(b, a % b); }

    // SWAP two numbers without temp
    static int[] swap(int a, int b) { a = a + b; b = a - b; a = a - b; return new int[]{a, b}; }

    // REVERSE LINKED LIST: TRICK prev/curr/next - "save next, point back, move both"
    static class Node {
        int val; Node next;
        Node(int val, Node next) { this.val = val; this.next = next; }
    }

    static Node reverseList(Node head) {
        Node prev = null, curr = head;
        while (curr != null) {
            Node next = curr.next;
            curr.next = prev;
            prev = curr;
            curr = next;
        }
        return prev;
    }

    // DETECT CYCLE / MIDDLE: TRICK slow moves 1, fast moves 2
    static Node middle(Node head) {
        Node slow = head, fast = head;
        while (fast != null && fast.next != null) { slow = slow.next; fast = fast.next.next; }
        return slow;
    }

    static String listToString(Node n) {
        StringBuilder sb = new StringBuilder();
        for (; n != null; n = n.next) sb.append(n.val).append(n.next != null ? "->" : "");
        return sb.toString();
    }

    // CLIMBING STAIRS (1 or 2 steps): it's just Fibonacci
    static int climbStairs(int n) {
        int a = 1, b = 1;
        for (int i = 0; i < n; i++) { int t = a + b; a = b; b = t; }
        return a;
    }

    // BUBBLE SORT (they still ask it)
    static int[] bubbleSort(int[] a) {
        for (int i = 0; i < a.length - 1; i++)
            for (int j = 0; j < a.length - 1 - i; j++)
                if (a[j] > a[j + 1]) { int t = a[j]; a[j] = a[j + 1]; a[j + 1] = t; }
        return a;
    }

    public static void main(String[] args) {
        System.out.println("fizzBuzz    " + fizzBuzz(15));
        System.out.println("fibonacci   " + fibonacci(10));
        System.out.println("factorial   " + factorial(10));
        System.out.println("primes      " + primesUpTo(30));
        System.out.println("armstrong   " + isArmstrong(153) + " " + isArmstrong(154));
        System.out.println("sumDigits   " + sumDigits(1234));
        System.out.println("reverseInt  " + reverseInt(1234));
        System.out.println("gcd         " + gcd(48, 18));
        System.out.println("swap        " + Arrays.toString(swap(5, 9)));

        Node head = new Node(1, new Node(2, new Node(3, new Node(4, new Node(5, null)))));
        System.out.println("middle      " + middle(head).val);
        System.out.println("reverseList " + listToString(reverseList(head)));

        System.out.println("climbStairs " + climbStairs(5));
        System.out.println("bubbleSort  " + Arrays.toString(bubbleSort(new int[]{5, 2, 9, 1, 5, 6})));
    }
}
