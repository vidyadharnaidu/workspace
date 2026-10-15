package banking;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.*;

import static java.util.stream.Collectors.*;

/**
 * "Balance amount" interview questions.
 *
 *  1. Balance per account from a list of transactions (streams)
 *  2. Total, balance as of a date, running balance, overdrawn accounts, monthly totals
 *  3. Minimum starting balance so the balance never goes negative
 *  4. Thread-safe account: deposit / withdraw / transfer with MATCHING totals (no lost money)
 *  5. Split expenses (Splitwise): net balance per person + who pays whom
 *
 * TRICKS:
 *  - MONEY = BigDecimal (never double: 0.1 + 0.2 != 0.3). Create from String: new BigDecimal("10.25")
 *  - Compare BigDecimal with compareTo / signum, NOT equals ("2.0".equals("2.00") is false)
 *  - Sum BigDecimal in streams: reduce(BigDecimal.ZERO, BigDecimal::add)
 *    or inside groupingBy: reducing(BigDecimal.ZERO, mapper, BigDecimal::add)
 *  - DEBIT = negative, CREDIT = positive -> put a signed() method on the record
 *  - Transfer between 2 accounts: lock BOTH, always in the same order (by id) -> no deadlock
 */
public class BalanceAmount {

    enum Type { CREDIT, DEBIT }

    record Txn(String account, Type type, BigDecimal amount, LocalDate date) {
        BigDecimal signed() {
            return type == Type.CREDIT ? amount : amount.negate();
        }
    }

    static Txn txn(String account, Type type, String amount, String date) {
        return new Txn(account, type, new BigDecimal(amount), LocalDate.parse(date));
    }

    // ---------- 1 & 2. Balances from transactions ----------

    static Map<String, BigDecimal> balanceByAccount(List<Txn> txns) {
        return txns.stream().collect(groupingBy(Txn::account, TreeMap::new,
                reducing(BigDecimal.ZERO, Txn::signed, BigDecimal::add)));
    }

    static BigDecimal totalBalance(List<Txn> txns) {
        return txns.stream().map(Txn::signed).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static Map<String, BigDecimal> balanceAsOf(List<Txn> txns, LocalDate asOf) {
        return balanceByAccount(txns.stream().filter(t -> !t.date().isAfter(asOf)).toList());
    }

    // Running balance after each transaction (a plain loop - streams shouldn't carry mutable state)
    static List<BigDecimal> runningBalance(List<Txn> txns, String account) {
        List<BigDecimal> result = new ArrayList<>();
        BigDecimal balance = BigDecimal.ZERO;
        for (Txn t : txns.stream().filter(t -> t.account().equals(account)).sorted(Comparator.comparing(Txn::date)).toList()) {
            balance = balance.add(t.signed());
            result.add(balance);
        }
        return result;
    }

    static Optional<String> richestAccount(List<Txn> txns) {
        return balanceByAccount(txns).entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    static List<String> overdrawnAccounts(List<Txn> txns) {
        return balanceByAccount(txns).entrySet().stream()
                .filter(e -> e.getValue().signum() < 0)
                .map(Map.Entry::getKey)
                .toList();
    }

    // {2026-01={CREDIT=1800.00, DEBIT=250.50}, 2026-02={...}}
    static Map<YearMonth, Map<Type, BigDecimal>> monthlyTotals(List<Txn> txns) {
        return txns.stream().collect(groupingBy(t -> YearMonth.from(t.date()), TreeMap::new,
                groupingBy(Txn::type, () -> new EnumMap<>(Type.class),
                        reducing(BigDecimal.ZERO, Txn::amount, BigDecimal::add))));
    }

    // ---------- 3. Minimum starting balance ----------

    // changes [-3, 2, -3, 4, 2] -> balances -3, -1, -4, 0, 2 -> lowest is -4 -> need 4 to start
    // TRICK: prefix sum, remember the LOWEST point
    static long minStartingBalance(long[] changes) {
        long balance = 0, lowest = 0;
        for (long c : changes) {
            balance += c;
            lowest = Math.min(lowest, balance);
        }
        return -lowest;
    }

    // ---------- 4. Thread-safe bank account ----------

    static class Account {
        final int id;
        private BigDecimal balance;
        private final Lock lock = new ReentrantLock();

        Account(int id, String initial) {
            this.id = id;
            this.balance = new BigDecimal(initial);
        }

        void deposit(BigDecimal amount) {
            lock.lock();
            try {
                balance = balance.add(amount);
            } finally {
                lock.unlock();
            }
        }

        boolean withdraw(BigDecimal amount) {
            lock.lock();
            try {
                if (balance.compareTo(amount) < 0) return false; // insufficient funds
                balance = balance.subtract(amount);
                return true;
            } finally {
                lock.unlock();
            }
        }

        BigDecimal balance() {
            lock.lock();
            try {
                return balance;
            } finally {
                lock.unlock();
            }
        }
    }

    // TRICK: lock the lower id first. Two threads doing A->B and B->A both lock A first -> no deadlock.
    static boolean transfer(Account from, Account to, BigDecimal amount) {
        Account first = from.id < to.id ? from : to;
        Account second = first == from ? to : from;
        first.lock.lock();
        try {
            second.lock.lock();
            try {
                if (from.balance.compareTo(amount) < 0) return false;
                from.balance = from.balance.subtract(amount);
                to.balance = to.balance.add(amount);
                return true;
            } finally {
                second.lock.unlock();
            }
        } finally {
            first.lock.unlock();
        }
    }

    static void concurrentDepositWithdrawDemo() throws InterruptedException {
        Account account = new Account(1, "0.00");
        BigDecimal ten = new BigDecimal("10.00");
        AtomicInteger deposits = new AtomicInteger(), withdrawals = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(8);
        for (int i = 0; i < 10_000; i++) {
            pool.submit(() -> { account.deposit(ten); deposits.incrementAndGet(); });
            pool.submit(() -> { if (account.withdraw(ten)) withdrawals.incrementAndGet(); });
        }
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);

        BigDecimal expected = ten.multiply(BigDecimal.valueOf(deposits.get() - withdrawals.get()));
        System.out.println("4a. deposits=" + deposits + " withdrawals=" + withdrawals
                + " balance=" + account.balance() + " expected=" + expected
                + " match=" + (account.balance().compareTo(expected) == 0));
    }

    static void concurrentTransferDemo() throws InterruptedException {
        List<Account> accounts = IntStream.range(0, 5).mapToObj(i -> new Account(i, "1000.00")).toList();
        BigDecimal totalBefore = accounts.stream().map(Account::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        AtomicInteger ok = new AtomicInteger(), rejected = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(8);
        for (int i = 0; i < 20_000; i++) {
            pool.submit(() -> {
                ThreadLocalRandom rnd = ThreadLocalRandom.current();
                int a = rnd.nextInt(accounts.size()), b = rnd.nextInt(accounts.size());
                if (a == b) return;
                BigDecimal amount = BigDecimal.valueOf(rnd.nextInt(1, 50_000), 2); // 0.01 .. 499.99
                if (transfer(accounts.get(a), accounts.get(b), amount)) ok.incrementAndGet();
                else rejected.incrementAndGet();
            });
        }
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);

        BigDecimal totalAfter = accounts.stream().map(Account::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean noneNegative = accounts.stream().allMatch(acc -> acc.balance().signum() >= 0);
        System.out.println("4b. transfers ok=" + ok + " rejected=" + rejected
                + " totalBefore=" + totalBefore + " totalAfter=" + totalAfter
                + " match=" + (totalBefore.compareTo(totalAfter) == 0) + " noneNegative=" + noneNegative);
    }

    // ---------- 5. Split expenses (Splitwise) ----------

    record Expense(String paidBy, BigDecimal amount, List<String> splitAmong) {}

    // net > 0 -> others owe you; net < 0 -> you owe
    static Map<String, BigDecimal> netBalances(List<Expense> expenses) {
        Map<String, BigDecimal> net = new TreeMap<>();
        for (Expense e : expenses) {
            BigDecimal share = e.amount().divide(BigDecimal.valueOf(e.splitAmong().size()), 2, RoundingMode.HALF_UP);
            net.merge(e.paidBy(), e.amount(), BigDecimal::add);
            e.splitAmong().forEach(person -> net.merge(person, share.negate(), BigDecimal::add));
        }
        return net;
    }

    // TRICK: two max-heaps (biggest creditor, biggest debtor); pay the smaller of the two, push back the rest
    static List<String> settleUp(Map<String, BigDecimal> net) {
        Comparator<Map.Entry<String, BigDecimal>> biggestFirst = Map.Entry.<String, BigDecimal>comparingByValue().reversed();
        PriorityQueue<Map.Entry<String, BigDecimal>> creditors = new PriorityQueue<>(biggestFirst);
        PriorityQueue<Map.Entry<String, BigDecimal>> debtors = new PriorityQueue<>(biggestFirst);
        net.forEach((person, amount) -> {
            if (amount.signum() > 0) creditors.add(Map.entry(person, amount));
            else if (amount.signum() < 0) debtors.add(Map.entry(person, amount.negate()));
        });

        List<String> payments = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Map.Entry<String, BigDecimal> creditor = creditors.poll(), debtor = debtors.poll();
            BigDecimal pay = creditor.getValue().min(debtor.getValue());
            payments.add(debtor.getKey() + " pays " + creditor.getKey() + " " + pay);
            if (creditor.getValue().compareTo(pay) > 0) creditors.add(Map.entry(creditor.getKey(), creditor.getValue().subtract(pay)));
            if (debtor.getValue().compareTo(pay) > 0) debtors.add(Map.entry(debtor.getKey(), debtor.getValue().subtract(pay)));
        }
        return payments;
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("0. double: 0.1 + 0.2 = " + (0.1 + 0.2)
                + " | BigDecimal: " + new BigDecimal("0.1").add(new BigDecimal("0.2")));

        List<Txn> txns = List.of(
                txn("ACC1", Type.CREDIT, "1000.00", "2026-01-05"),
                txn("ACC2", Type.CREDIT, "500.00", "2026-01-07"),
                txn("ACC1", Type.DEBIT, "250.50", "2026-01-10"),
                txn("ACC3", Type.CREDIT, "300.00", "2026-01-20"),
                txn("ACC1", Type.DEBIT, "100.00", "2026-02-01"),
                txn("ACC2", Type.DEBIT, "700.00", "2026-02-03"),
                txn("ACC1", Type.CREDIT, "50.25", "2026-02-15"),
                txn("ACC3", Type.DEBIT, "20.00", "2026-02-20"));

        System.out.println("1. balanceByAccount  " + balanceByAccount(txns));                           // {ACC1=699.75, ACC2=-200.00, ACC3=280.00}
        System.out.println("2. totalBalance      " + totalBalance(txns));                               // 779.75
        System.out.println("2. asOf 2026-01-31   " + balanceAsOf(txns, LocalDate.parse("2026-01-31"))); // {ACC1=749.50, ACC2=500.00, ACC3=300.00}
        System.out.println("2. running ACC1      " + runningBalance(txns, "ACC1"));                     // [1000.00, 749.50, 649.50, 699.75]
        System.out.println("2. richestAccount    " + richestAccount(txns).orElse("-"));                 // ACC1
        System.out.println("2. overdrawn         " + overdrawnAccounts(txns));                          // [ACC2]
        System.out.println("2. monthlyTotals     " + monthlyTotals(txns));

        System.out.println("3. minStartBalance   " + minStartingBalance(new long[]{-3, 2, -3, 4, 2}));  // 4

        concurrentDepositWithdrawDemo();
        concurrentTransferDemo();

        List<Expense> trip = List.of(
                new Expense("Alice", new BigDecimal("90.00"), List.of("Alice", "Bob", "Carol")),
                new Expense("Bob", new BigDecimal("60.00"), List.of("Bob", "Carol")),
                new Expense("Carol", new BigDecimal("30.00"), List.of("Alice", "Bob", "Carol")));
        Map<String, BigDecimal> net = netBalances(trip);
        System.out.println("5. netBalances       " + net);                                              // {Alice=50.00, Bob=-10.00, Carol=-40.00}
        System.out.println("5. settleUp          " + settleUp(net));                                    // [Carol pays Alice 40.00, Bob pays Alice 10.00]
    }
}
