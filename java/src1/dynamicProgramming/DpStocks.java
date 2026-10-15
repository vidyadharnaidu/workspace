import java.util.*;

/**
 * PATTERN: state machine over (transactions used, holding or not).
 * buy[j]  = best balance after the j-th BUY   = max(buy[j],  sell[j-1] - price)
 * sell[j] = best balance after the j-th SELL  = max(sell[j], buy[j]    + price)
 * Learn this pair and every stock variant falls out of it.
 */
public class DpStocks {

    // HOOK: the general solver. k >= n/2 degenerates to unlimited trading.
    static int maxProfitK(int k, int[] prices) {
        int n = prices.length;
        if (n < 2 || k == 0) return 0;
        if (k >= n / 2) return unlimited(prices);

        int[] buy = new int[k + 1], sell = new int[k + 1];
        Arrays.fill(buy, Integer.MIN_VALUE / 2);                  // /2 so +price cannot overflow
        for (int price : prices)
            for (int j = 1; j <= k; j++) {
                buy[j] = Math.max(buy[j], sell[j - 1] - price);
                sell[j] = Math.max(sell[j], buy[j] + price);
            }
        return sell[k];
    }

    // HOOK: unlimited trades -> just bank every upward step.
    static int unlimited(int[] p) {
        int total = 0;
        for (int i = 1; i < p.length; i++) total += Math.max(0, p[i] - p[i - 1]);
        return total;
    }

    // HOOK: one transaction = cheapest-so-far.
    static int maxProfit1(int[] p) {
        int min = Integer.MAX_VALUE, best = 0;
        for (int x : p) {
            min = Math.min(min, x);
            best = Math.max(best, x - min);
        }
        return best;
    }

    // HOOK: at most two transactions - the k=2 case, worth memorising unrolled.
    static int maxProfit3(int[] p) {
        int buy1 = Integer.MIN_VALUE, sell1 = 0, buy2 = Integer.MIN_VALUE, sell2 = 0;
        for (int price : p) {
            buy1 = Math.max(buy1, -price);
            sell1 = Math.max(sell1, buy1 + price);
            buy2 = Math.max(buy2, sell1 - price);
            sell2 = Math.max(sell2, buy2 + price);
        }
        return sell2;
    }

    // HOOK: at most k transactions.
    static int maxProfit4(int k, int[] p) { return maxProfitK(k, p); }

    public static void main(String[] args) {
        System.out.println("maxProfit1        = " + maxProfit1(new int[]{7, 1, 5, 3, 6, 4}));
        System.out.println("maxProfit3        = " + maxProfit3(new int[]{3, 3, 5, 0, 0, 3, 1, 4}));
        System.out.println("maxProfit3 via k  = " + maxProfitK(2, new int[]{3, 3, 5, 0, 0, 3, 1, 4}));
        System.out.println("maxProfit4 k=2    = " + maxProfit4(2, new int[]{3, 2, 6, 5, 0, 3}));
        System.out.println("unlimited         = " + unlimited(new int[]{7, 1, 5, 3, 6, 4}));
    }
}
