package designpatterns;

import java.util.*;
import java.util.function.*;

/**
 * GoF BEHAVIORAL patterns - all about "how objects talk and who decides".
 * 11 of them: Chain, Command, Interpreter, Iterator, Mediator, Memento,
 *             Observer, State, Strategy, Template method, Visitor.
 *
 * TRICK - the three that get confused:
 *   Strategy : I pick the algorithm               (caller chooses)
 *   State    : the object picks, and swaps itself (transitions live in the states)
 *   Command  : I package the call as an object    (so it can be queued, logged, undone)
 */
public class BehavioralPatterns {

    // ---------- 1. CHAIN OF RESPONSIBILITY ----------
    // TRICK: each handler either handles it or passes it on. Think servlet filters.
    static abstract class Approver {
        private Approver next;
        Approver then(Approver next) { this.next = next; return next; }   // returns next, so the chain reads left to right
        final String handle(int amount) {
            if (canApprove(amount)) return getClass().getSimpleName() + " approved " + amount;
            return next != null ? next.handle(amount) : "nobody can approve " + amount;
        }
        abstract boolean canApprove(int amount);
    }
    static class TeamLead extends Approver { boolean canApprove(int a) { return a <= 100; } }
    static class Manager  extends Approver { boolean canApprove(int a) { return a <= 1_000; } }
    static class Director extends Approver { boolean canApprove(int a) { return a <= 10_000; } }

    // ---------- 2. COMMAND ----------
    // TRICK: turn a method call into an object, and you get undo, queueing and logging for free.
    interface Command { void execute(); void undo(); }
    static class Light {
        boolean on;
        @Override public String toString() { return on ? "light ON" : "light OFF"; }
    }
    record TurnOn(Light light) implements Command {
        public void execute() { light.on = true; }
        public void undo()    { light.on = false; }
    }

    // ---------- 3. INTERPRETER ----------
    // TRICK: one class per grammar rule; each one knows how to evaluate itself.
    interface Expr { int eval(); }
    record Num(int v) implements Expr { public int eval() { return v; } }
    record Add(Expr l, Expr r) implements Expr { public int eval() { return l.eval() + r.eval(); } }
    record Mul(Expr l, Expr r) implements Expr { public int eval() { return l.eval() * r.eval(); } }

    // ---------- 4. ITERATOR ----------
    // TRICK: walk a collection without exposing how it stores things. Java's for-each IS this pattern.
    static class Playlist implements Iterable<String> {
        private final String[] songs = {"one", "two", "three"};
        public Iterator<String> iterator() {
            return new Iterator<>() {
                int i = 0;
                public boolean hasNext() { return i < songs.length; }
                public String next() { return songs[i++]; }
            };
        }
    }

    // ---------- 5. MEDIATOR ----------
    // TRICK: N objects talking to each other = N^2 links. Route everything through one hub instead.
    static class ChatRoom {
        private final List<User> users = new ArrayList<>();
        void join(User u) { users.add(u); u.room = this; }
        void send(User from, String msg) {
            users.stream().filter(u -> u != from).forEach(u -> u.inbox.add(from.name + ": " + msg));
        }
    }
    static class User {
        final String name;
        ChatRoom room;
        final List<String> inbox = new ArrayList<>();
        User(String name) { this.name = name; }
        void say(String msg) { room.send(this, msg); }       // users never reference each other
    }

    // ---------- 6. MEMENTO ----------
    // TRICK: undo. The snapshot is opaque - only the originator knows how to read it back.
    static class Editor {
        private String text = "";
        void type(String s) { text += s; }
        String save() { return text; }                       // the memento
        void restore(String memento) { text = memento; }
        @Override public String toString() { return "[" + text + "]"; }
    }

    // ---------- 7. OBSERVER ----------
    // TRICK: publish/subscribe. One subject, many listeners, no coupling back.
    static class Stock {
        private final List<Consumer<Double>> listeners = new ArrayList<>();
        void subscribe(Consumer<Double> l) { listeners.add(l); }
        void setPrice(double p) { listeners.forEach(l -> l.accept(p)); }
    }

    // ---------- 8. STATE ----------
    // TRICK: replaces a big switch on a status field. Each state returns the NEXT state.
    interface OrderState { OrderState next(); String name(); }
    enum Order implements OrderState {
        NEW       { public OrderState next() { return PAID; } },
        PAID      { public OrderState next() { return SHIPPED; } },
        SHIPPED   { public OrderState next() { return DELIVERED; } },
        DELIVERED { public OrderState next() { return this; } }
    }

    // ---------- 9. STRATEGY ----------
    // TRICK: an interface with one method = a lambda. Swap the algorithm at runtime.
    interface Discount extends DoubleUnaryOperator { }
    static final Discount NONE   = p -> p;
    static final Discount TEN_PC = p -> p * 0.9;
    static final Discount FLAT_5 = p -> Math.max(0, p - 5);

    // ---------- 10. TEMPLATE METHOD ----------
    // TRICK: the base class owns the ORDER of the steps (final), subclasses fill the steps in.
    static abstract class Report {
        final String render() { return header() + " | " + body() + " | footer"; }   // final = the template
        String header() { return "header"; }                 // hook with a default
        abstract String body();                              // subclass must supply
    }
    static class SalesReport extends Report { String body() { return "sales = 42"; } }
    static class AuditReport extends Report {
        @Override String header() { return "AUDIT"; }
        String body() { return "no findings"; }
    }

    // ---------- 11. VISITOR ----------
    // TRICK: add a new OPERATION without touching the classes. Double dispatch: accept() -> visit().
    interface Visitor<R> { R visitCircle(Circle c); R visitRect(Rect r); }
    interface Shape { <R> R accept(Visitor<R> v); }
    record Circle(double r) implements Shape { public <R> R accept(Visitor<R> v) { return v.visitCircle(this); } }
    record Rect(double w, double h) implements Shape { public <R> R accept(Visitor<R> v) { return v.visitRect(this); } }

    static class AreaVisitor implements Visitor<Double> {
        public Double visitCircle(Circle c) { return Math.PI * c.r() * c.r(); }
        public Double visitRect(Rect r) { return r.w() * r.h(); }
    }

    public static void main(String[] args) {
        // 1. Chain
        Approver lead = new TeamLead();
        lead.then(new Manager()).then(new Director());
        System.out.println("1.  chain      : " + lead.handle(50) + " / " + lead.handle(5_000) + " / " + lead.handle(99_999));

        // 2. Command, with an undo stack
        Light light = new Light();
        Deque<Command> history = new ArrayDeque<>();
        Command on = new TurnOn(light);
        on.execute();
        history.push(on);
        System.out.print("2.  command    : " + light);
        history.pop().undo();
        System.out.println(" -> undo -> " + light);

        // 3. Interpreter: (2 + 3) * 4
        System.out.println("3.  interpreter: (2+3)*4 = " + new Mul(new Add(new Num(2), new Num(3)), new Num(4)).eval());

        // 4. Iterator
        StringJoiner songs = new StringJoiner(",");
        for (String song : new Playlist()) songs.add(song);
        System.out.println("4.  iterator   : " + songs);

        // 5. Mediator
        ChatRoom room = new ChatRoom();
        User ann = new User("ann"), bob = new User("bob"), cat = new User("cat");
        room.join(ann); room.join(bob); room.join(cat);
        ann.say("hi all");
        System.out.println("5.  mediator   : bob " + bob.inbox + ", cat " + cat.inbox);

        // 6. Memento
        Editor editor = new Editor();
        editor.type("hello");
        String saved = editor.save();
        editor.type(" world");
        System.out.print("6.  memento    : " + editor);
        editor.restore(saved);
        System.out.println(" -> undo -> " + editor);

        // 7. Observer
        Stock stock = new Stock();
        stock.subscribe(p -> System.out.println("7.  observer   : chart redraws at " + p));
        stock.subscribe(p -> { if (p > 100) System.out.println("7.  observer   : alert, price is " + p); });
        stock.setPrice(120.5);

        // 8. State
        OrderState state = Order.NEW;
        StringJoiner flow = new StringJoiner(" -> ").add(state.name());
        for (int i = 0; i < 3; i++) { state = state.next(); flow.add(state.name()); }
        System.out.println("8.  state      : " + flow);

        // 9. Strategy
        double price = 50;
        System.out.printf("9.  strategy   : none=%.1f ten-percent=%.1f flat-5=%.1f%n",
                NONE.applyAsDouble(price), TEN_PC.applyAsDouble(price), FLAT_5.applyAsDouble(price));

        // 10. Template method
        System.out.println("10. template   : " + new SalesReport().render() + " || " + new AuditReport().render());

        // 11. Visitor
        List<Shape> shapes = List.of(new Circle(1), new Rect(2, 3));
        AreaVisitor area = new AreaVisitor();
        System.out.printf("11. visitor    : total area = %.2f%n", shapes.stream().mapToDouble(s -> s.accept(area)).sum());
    }
}
