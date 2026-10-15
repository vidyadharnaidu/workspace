package stacks;

import java.util.*;

/**
 * Stack problems.
 *
 * TRICKS:
 *  - Use Deque<Integer> stack = new ArrayDeque<>(); push / pop / peek.  (Don't use the old Stack class.)
 *  - MATCHING / NESTING (brackets, decode "3[a]", calculator with parens) -> push state on '(' / '[', pop on ')' / ']'
 *  - MONOTONIC STACK ("next greater / smaller", spans, histogram):
 *        for each i: while (stack not empty && a[top] < a[i]) -> a[i] is the answer for top -> pop
 *        then push i.  Store INDEXES, not values.
 *  - "Undo the last thing" (backspace, adjacent duplicates, path "..") -> stack (a StringBuilder works too)
 *  - Valid parentheses -> strings/ValidParentheses
 */
public class Stacks {

    // 1. MIN STACK: push, pop, top, getMin all O(1)
    // TRICK: each entry stores {value, min so far}
    static class MinStack {
        private final Deque<int[]> stack = new ArrayDeque<>();

        void push(int x) { stack.push(new int[]{x, stack.isEmpty() ? x : Math.min(x, getMin())}); }
        void pop()       { stack.pop(); }
        int top()        { return stack.peek()[0]; }
        int getMin()     { return stack.peek()[1]; }
    }

    // 2. EVALUATE REVERSE POLISH NOTATION: ["2","1","+","3","*"] -> 9
    // TRICK: number -> push; operator -> pop b, pop a, push (a op b)   (order matters for - and /)
    static int evalRPN(String[] tokens) {
        Deque<Integer> stack = new ArrayDeque<>();
        for (String t : tokens) {
            switch (t) {
                case "+" -> stack.push(stack.pop() + stack.pop());
                case "*" -> stack.push(stack.pop() * stack.pop());
                case "-" -> { int b = stack.pop(), a = stack.pop(); stack.push(a - b); }
                case "/" -> { int b = stack.pop(), a = stack.pop(); stack.push(a / b); }
                default -> stack.push(Integer.parseInt(t));
            }
        }
        return stack.pop();
    }

    // 3. BASIC CALCULATOR (+, -, parentheses): "(1+(4+5+2)-3)+(6+8)" -> 23
    // TRICK: on '(' push (result, sign) and start fresh; on ')' result = savedResult + savedSign * result
    static int calculate(String s) {
        Deque<Integer> stack = new ArrayDeque<>();
        int result = 0, sign = 1, num = 0;
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) {
                num = num * 10 + (c - '0');
            } else if (c == '+' || c == '-') {
                result += sign * num;
                num = 0;
                sign = c == '+' ? 1 : -1;
            } else if (c == '(') {
                stack.push(result);
                stack.push(sign);
                result = 0;
                sign = 1;
            } else if (c == ')') {
                result += sign * num;
                num = 0;
                result = result * stack.pop() + stack.pop(); // * saved sign + saved result
            }
        }
        return result + sign * num;
    }

    // 4. DECODE STRING: "3[a2[c]]" -> "accaccacc"
    // TRICK: two stacks (counts, previous strings). ']' -> previous + current.repeat(count)
    static String decodeString(String s) {
        Deque<Integer> counts = new ArrayDeque<>();
        Deque<StringBuilder> previous = new ArrayDeque<>();
        StringBuilder current = new StringBuilder();
        int k = 0;
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) {
                k = k * 10 + (c - '0');
            } else if (c == '[') {
                counts.push(k);
                previous.push(current);
                current = new StringBuilder();
                k = 0;
            } else if (c == ']') {
                current = previous.pop().append(current.toString().repeat(counts.pop()));
            } else {
                current.append(c);
            }
        }
        return current.toString();
    }

    // 5. SIMPLIFY UNIX PATH: "/a/./b/../../c/" -> "/c"
    static String simplifyPath(String path) {
        Deque<String> dirs = new ArrayDeque<>();
        for (String part : path.split("/")) {
            if (part.isEmpty() || part.equals(".")) continue;
            if (part.equals("..")) dirs.pollLast();
            else dirs.addLast(part);
        }
        return "/" + String.join("/", dirs);
    }

    // 6. NEXT GREATER ELEMENT: [2,1,2,4,3] -> [4,2,4,-1,-1]
    static int[] nextGreater(int[] a) {
        int[] res = new int[a.length];
        Arrays.fill(res, -1);
        Deque<Integer> stack = new ArrayDeque<>(); // indexes still waiting for a bigger value
        for (int i = 0; i < a.length; i++) {
            while (!stack.isEmpty() && a[stack.peek()] < a[i]) res[stack.pop()] = a[i];
            stack.push(i);
        }
        return res;
    }

    // 6b. NEXT GREATER (circular array): [1,2,1] -> [2,-1,2]
    // TRICK: loop twice (i % n), only push during the first pass
    static int[] nextGreaterCircular(int[] a) {
        int n = a.length;
        int[] res = new int[n];
        Arrays.fill(res, -1);
        Deque<Integer> stack = new ArrayDeque<>();
        for (int i = 0; i < 2 * n; i++) {
            int x = a[i % n];
            while (!stack.isEmpty() && a[stack.peek()] < x) res[stack.pop()] = x;
            if (i < n) stack.push(i);
        }
        return res;
    }

    // 7. DAILY TEMPERATURES (days until warmer): [73,74,75,71,69,72,76,73] -> [1,1,4,2,1,1,0,0]
    // Same as next greater, but store the DISTANCE i - j
    static int[] dailyTemperatures(int[] t) {
        int[] res = new int[t.length];
        Deque<Integer> stack = new ArrayDeque<>();
        for (int i = 0; i < t.length; i++) {
            while (!stack.isEmpty() && t[stack.peek()] < t[i]) {
                int j = stack.pop();
                res[j] = i - j;
            }
            stack.push(i);
        }
        return res;
    }

    // 8. STOCK SPAN (days in a row with price <= today): [100,80,60,70,60,75,85] -> [1,1,1,2,1,4,6]
    // TRICK: stack of {price, span}; absorb the spans of all smaller-or-equal prices
    static class StockSpanner {
        private final Deque<int[]> stack = new ArrayDeque<>();

        int next(int price) {
            int span = 1;
            while (!stack.isEmpty() && stack.peek()[0] <= price) span += stack.pop()[1];
            stack.push(new int[]{price, span});
            return span;
        }
    }

    // 9. LARGEST RECTANGLE IN HISTOGRAM: [2,1,5,6,2,3] -> 10
    // TRICK: increasing stack. When a bar is popped, its rectangle spans from (new top + 1) to (i - 1).
    //        Add a fake 0-height bar at the end to flush the stack.
    static int largestRectangle(int[] heights) {
        Deque<Integer> stack = new ArrayDeque<>();
        int best = 0;
        for (int i = 0; i <= heights.length; i++) {
            int h = i == heights.length ? 0 : heights[i];
            while (!stack.isEmpty() && heights[stack.peek()] >= h) {
                int height = heights[stack.pop()];
                int left = stack.isEmpty() ? -1 : stack.peek();
                best = Math.max(best, height * (i - left - 1));
            }
            stack.push(i);
        }
        return best;
    }

    // 10. TRAPPING RAIN WATER: [0,1,0,2,1,0,1,3,2,1,2,1] -> 6
    // TRICK (two pointers, easier than stack): move the LOWER side; water = maxSoFar on that side - height
    static int trap(int[] h) {
        int l = 0, r = h.length - 1, leftMax = 0, rightMax = 0, water = 0;
        while (l < r) {
            if (h[l] < h[r]) {
                leftMax = Math.max(leftMax, h[l]);
                water += leftMax - h[l++];
            } else {
                rightMax = Math.max(rightMax, h[r]);
                water += rightMax - h[r--];
            }
        }
        return water;
    }

    // 11. REMOVE ADJACENT DUPLICATES: "abbaca" -> "ca"
    // TRICK: StringBuilder as a stack
    static String removeAdjacentDuplicates(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            int last = sb.length() - 1;
            if (last >= 0 && sb.charAt(last) == c) sb.deleteCharAt(last);
            else sb.append(c);
        }
        return sb.toString();
    }

    // 12. REMOVE K DIGITS (smallest number): "1432219", k=3 -> "1219"
    // TRICK: monotonic increasing stack - pop bigger digits while k > 0
    static String removeKDigits(String num, int k) {
        StringBuilder st = new StringBuilder();
        for (char c : num.toCharArray()) {
            while (k > 0 && st.length() > 0 && st.charAt(st.length() - 1) > c) {
                st.deleteCharAt(st.length() - 1);
                k--;
            }
            st.append(c);
        }
        st.setLength(st.length() - k); // still have removals left -> cut from the end
        String res = st.toString().replaceFirst("^0+", "");
        return res.isEmpty() ? "0" : res;
    }

    // 13. ASTEROID COLLISION: [5,10,-5] -> [5,10]; [8,-8] -> []; [10,2,-5] -> [10]
    // TRICK: only "right-moving on stack" vs "incoming left-moving" collide
    static List<Integer> asteroidCollision(int[] asteroids) {
        Deque<Integer> stack = new ArrayDeque<>();
        for (int a : asteroids) {
            boolean alive = true;
            while (alive && a < 0 && !stack.isEmpty() && stack.peekLast() > 0) {
                if (stack.peekLast() < -a) stack.pollLast();          // top explodes, keep checking
                else {
                    if (stack.peekLast() == -a) stack.pollLast();     // both explode
                    alive = false;
                }
            }
            if (alive) stack.addLast(a);
        }
        return new ArrayList<>(stack);
    }

    // 14. BACKSPACE STRING COMPARE: "ab#c" vs "ad#c" -> true
    static boolean backspaceCompare(String a, String b) {
        return typed(a).equals(typed(b));
    }

    static String typed(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c != '#') sb.append(c);
            else if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        MinStack ms = new MinStack();
        ms.push(-2); ms.push(0); ms.push(-3);
        System.out.print("minStack          min=" + ms.getMin());                          // -3
        ms.pop();
        System.out.println(" after pop: top=" + ms.top() + " min=" + ms.getMin());          // top=0 min=-2

        System.out.println("evalRPN           " + evalRPN(new String[]{"2", "1", "+", "3", "*"})
                + " " + evalRPN(new String[]{"4", "13", "5", "/", "+"}));                    // 9 6
        System.out.println("calculate         " + calculate("(1+(4+5+2)-3)+(6+8)") + " " + calculate("2-(5-6)")); // 23 3
        System.out.println("decodeString      " + decodeString("3[a2[c]]") + " " + decodeString("2[abc]3[cd]ef")); // accaccacc abcabccdcdcdef
        System.out.println("simplifyPath      " + simplifyPath("/a/./b/../../c/") + " " + simplifyPath("/../"));   // /c /
        System.out.println("nextGreater       " + Arrays.toString(nextGreater(new int[]{2, 1, 2, 4, 3})));        // [4, 2, 4, -1, -1]
        System.out.println("nextGreaterCirc   " + Arrays.toString(nextGreaterCircular(new int[]{1, 2, 1})));      // [2, -1, 2]
        System.out.println("dailyTemperatures " + Arrays.toString(dailyTemperatures(new int[]{73, 74, 75, 71, 69, 72, 76, 73}))); // [1, 1, 4, 2, 1, 1, 0, 0]

        StockSpanner spanner = new StockSpanner();
        System.out.println("stockSpan         " + Arrays.stream(new int[]{100, 80, 60, 70, 60, 75, 85}).map(spanner::next).boxed().toList()); // [1, 1, 1, 2, 1, 4, 6]

        System.out.println("largestRectangle  " + largestRectangle(new int[]{2, 1, 5, 6, 2, 3}));             // 10
        System.out.println("trapRainWater     " + trap(new int[]{0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1}));        // 6
        System.out.println("removeAdjDups     " + removeAdjacentDuplicates("abbaca"));                         // ca
        System.out.println("removeKDigits     " + removeKDigits("1432219", 3) + " " + removeKDigits("10200", 1) + " " + removeKDigits("10", 2)); // 1219 200 0
        System.out.println("asteroids         " + asteroidCollision(new int[]{5, 10, -5}) + " "
                + asteroidCollision(new int[]{8, -8}) + " " + asteroidCollision(new int[]{10, 2, -5}));      // [5, 10] [] [10]
        System.out.println("backspaceCompare  " + backspaceCompare("ab#c", "ad#c") + " " + backspaceCompare("a#c", "b"));   // true false
    }
}
