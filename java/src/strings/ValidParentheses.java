package strings;

import java.util.*;

/**
 * Valid Parentheses: "()[]{}" -> true, "(]" -> false, "([)]" -> false
 *
 * TRICK: STACK. Open bracket -> push its CLOSING partner.
 *        Close bracket -> must equal what we pop. End -> stack must be empty.
 */
public class ValidParentheses {

    static boolean isValid(String s) {
        Deque<Character> stack = new ArrayDeque<>();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '(' -> stack.push(')');
                case '[' -> stack.push(']');
                case '{' -> stack.push('}');
                default -> {
                    if (stack.isEmpty() || stack.pop() != c) return false;
                }
            }
        }
        return stack.isEmpty();
    }

    public static void main(String[] args) {
        for (String s : List.of("()", "()[]{}", "(]", "([)]", "{[]}", "((", ""))
            System.out.printf("%-8s -> %s%n", "\"" + s + "\"", isValid(s));
    }
}
