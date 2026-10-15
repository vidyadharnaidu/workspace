package eventsourcing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * PATTERN: EVENT SOURCING
 *
 * PROBLEM: a normal UPDATE destroys history. "Why is this balance 42?" is unanswerable,
 *          and auditors, debugging and analytics all want the story, not the snapshot.
 * SOLUTION: store the EVENTS as the source of truth. Current state = replay the events.
 *
 * TRICK TO REMEMBER: the event log is append-only and immutable. You never UPDATE or
 *                    DELETE an event - you append a correcting one (like an accounting
 *                    ledger; a mistake is fixed by a reversing entry, not an eraser).
 *
 * TRICK: replaying 100k events per read is too slow -> SNAPSHOT every N events and
 *        replay only from the snapshot forward.
 *
 * TRICK: event schema evolution is the real cost. Old events must stay readable FOREVER.
 *        Add fields with defaults; never rename or repurpose a field.
 *
 * Almost always paired with CQRS (see 08): the event log is the write side, projections
 * feed the read side.
 *
 * Production: Axon, EventStoreDB, or Kafka with compaction. This is the hand-rolled shape.
 */
public class AccountEventSourcing {

    // ---------- the events ARE the database ----------
    public sealed interface AccountEvent {
        String accountId();
        Instant at();

        record Opened(String accountId, String owner, Instant at) implements AccountEvent { }
        record Deposited(String accountId, double amount, Instant at) implements AccountEvent { }
        record Withdrawn(String accountId, double amount, Instant at) implements AccountEvent { }
        record Frozen(String accountId, String reason, Instant at) implements AccountEvent { }
    }

    /**
     * The AGGREGATE: rebuilt from events, decides whether a new event is allowed.
     *
     * Two distinct jobs, and interviewers love the distinction:
     *   apply()  - replays a PAST event, never validates (history cannot be wrong)
     *   handle() - validates a NEW command, then emits an event
     */
    public static class Account {

        private String id;
        private String owner;
        private double balance;
        private boolean frozen;
        private long version;               // how many events built this state

        /** Replay: fold the events into state. */
        public static Account rehydrate(String id, List<AccountEvent> history) {
            Account account = new Account();
            account.id = id;
            history.forEach(account::apply);
            return account;
        }

        private void apply(AccountEvent event) {
            if (event instanceof AccountEvent.Opened e) {
                this.owner = e.owner();
            } else if (event instanceof AccountEvent.Deposited e) {
                this.balance += e.amount();
            } else if (event instanceof AccountEvent.Withdrawn e) {
                this.balance -= e.amount();
            } else if (event instanceof AccountEvent.Frozen e) {
                this.frozen = true;
            }
            this.version++;
        }

        /** Command handling: the ONLY place business rules live. */
        public AccountEvent handle(double withdrawAmount) {
            if (frozen) throw new IllegalStateException("account frozen");
            if (withdrawAmount <= 0) throw new IllegalArgumentException("amount must be positive");
            if (balance < withdrawAmount) throw new IllegalStateException("insufficient funds");
            return new AccountEvent.Withdrawn(id, withdrawAmount, Instant.now());
        }

        public double balance() { return balance; }
        public long version() { return version; }
    }

    // ---------- the store ----------
    static class StoredEvent {
        String eventId;
        String aggregateId;
        String type;            // "Deposited"
        String payload;         // JSON
        long sequence;          // per-aggregate, 1,2,3... - the concurrency guard
        Instant at;
    }

    interface EventStore extends JpaRepository<StoredEvent, String> {
        List<StoredEvent> findByAggregateIdOrderBySequenceAsc(String aggregateId);
        List<StoredEvent> findByAggregateIdAndSequenceGreaterThanOrderBySequenceAsc(String id, long afterSequence);
    }

    @Service
    static class AccountService {

        private final EventStore store;
        private final SnapshotStore snapshots;

        AccountService(EventStore store, SnapshotStore snapshots) {
            this.store = store;
            this.snapshots = snapshots;
        }

        /** Load: newest snapshot + only the events after it. */
        Account load(String accountId) {
            var snapshot = snapshots.latest(accountId);
            List<StoredEvent> tail = (snapshot == null)
                    ? store.findByAggregateIdOrderBySequenceAsc(accountId)
                    : store.findByAggregateIdAndSequenceGreaterThanOrderBySequenceAsc(accountId, snapshot.version);

            Account account = (snapshot == null) ? new Account() : snapshot.toAccount();
            tail.stream().map(this::deserialize).forEach(account::apply);
            return account;
        }

        @Transactional
        public void withdraw(String accountId, double amount) {
            Account account = load(accountId);
            AccountEvent event = account.handle(amount);          // may reject

            // OPTIMISTIC CONCURRENCY: unique index on (aggregateId, sequence) means a
            // concurrent writer at the same version fails, instead of silently overwriting.
            append(accountId, event, account.version() + 1);

            if ((account.version() + 1) % 100 == 0) snapshots.save(accountId, load(accountId));
        }

        private void append(String aggregateId, AccountEvent event, long sequence) { /* insert StoredEvent */ }
        private AccountEvent deserialize(StoredEvent stored) { return null; }
    }

    interface SnapshotStore {
        Snapshot latest(String aggregateId);
        void save(String aggregateId, Account account);
    }

    static class Snapshot {
        String aggregateId;
        double balance;
        long version;
        Account toAccount() { return new Account(); }
    }

    /**
     * The superpower: TIME TRAVEL. Any past state is a replay with a filter -
     * impossible with a table that only holds the current row.
     */
    static Account stateAsOf(List<AccountEvent> history, Instant pointInTime, String id) {
        return Account.rehydrate(id, history.stream()
                .filter(e -> e.at().isBefore(pointInTime))
                .toList());
    }
}
