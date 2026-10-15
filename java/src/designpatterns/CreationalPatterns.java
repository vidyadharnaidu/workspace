package designpatterns;

import java.util.*;

/**
 * GoF CREATIONAL patterns - all about "how the object gets made".
 * Remember them as: SFABP - Singleton, Factory, Abstract factory, Builder, Prototype.
 *
 *  1. Singleton        one instance, ever              -> enum
 *  2. Factory Method   one product, subclass decides   -> switch on a type
 *  3. Abstract Factory a FAMILY of matching products   -> factory of factories
 *  4. Builder          many optional fields            -> fluent .x().y().build()
 *  5. Prototype        copy instead of construct       -> clone/copy constructor
 */
public class CreationalPatterns {

    // ---------- 1. SINGLETON ----------
    // TRICK: enum is the shortest thread-safe, serialization-safe, reflection-safe singleton.
    enum Config {
        INSTANCE;
        private final Map<String, String> values = new HashMap<>(Map.of("env", "prod"));
        String get(String k) { return values.get(k); }
    }

    // The classic lazy version interviewers ask you to write by hand.
    static class LazyConfig {
        private static volatile LazyConfig instance;          // volatile: stops half-built publication
        private LazyConfig() { }
        static LazyConfig getInstance() {
            if (instance == null) {                            // 1st check: no lock in the common case
                synchronized (LazyConfig.class) {
                    if (instance == null) instance = new LazyConfig();   // 2nd check: only one wins
                }
            }
            return instance;
        }
    }

    // ---------- 2. FACTORY METHOD ----------
    interface Shape { String draw(); }
    record Circle(int r) implements Shape { public String draw() { return "circle r=" + r; } }
    record Square(int a) implements Shape { public String draw() { return "square a=" + a; } }

    static class ShapeFactory {                                 // caller says WHAT, not HOW
        static Shape of(String type) {
            return switch (type) {
                case "circle" -> new Circle(2);
                case "square" -> new Square(3);
                default -> throw new IllegalArgumentException("unknown: " + type);
            };
        }
    }

    // ---------- 3. ABSTRACT FACTORY ----------
    // TRICK: factory method makes ONE thing; abstract factory makes a MATCHING SET.
    interface Button { String render(); }
    interface Checkbox { String render(); }
    interface UiFactory { Button button(); Checkbox checkbox(); }

    static class DarkFactory implements UiFactory {
        public Button button() { return () -> "dark button"; }
        public Checkbox checkbox() { return () -> "dark checkbox"; }
    }
    static class LightFactory implements UiFactory {
        public Button button() { return () -> "light button"; }
        public Checkbox checkbox() { return () -> "light checkbox"; }
    }

    // ---------- 4. BUILDER ----------
    // TRICK: use it when a constructor would need 4+ args, half of them optional.
    static class Pizza {
        private final String size;           // required
        private final boolean cheese, bacon; // optional
        private Pizza(Builder b) { this.size = b.size; this.cheese = b.cheese; this.bacon = b.bacon; }

        static Builder size(String size) { return new Builder(size); }

        static class Builder {
            private final String size;
            private boolean cheese, bacon;
            Builder(String size) { this.size = size; }
            Builder cheese() { this.cheese = true; return this; }   // each setter returns this
            Builder bacon()  { this.bacon  = true; return this; }
            Pizza build() { return new Pizza(this); }               // object is immutable after this
        }
        @Override public String toString() { return size + " pizza" + (cheese ? " +cheese" : "") + (bacon ? " +bacon" : ""); }
    }

    // ---------- 5. PROTOTYPE ----------
    // TRICK: copying an existing object beats building a new one when construction is expensive.
    static class Document implements Cloneable {
        String title;
        List<String> pages;                                   // mutable -> must deep copy
        Document(String title, List<String> pages) { this.title = title; this.pages = pages; }

        @Override public Document clone() {
            return new Document(title, new ArrayList<>(pages)); // DEEP copy; super.clone() would share the list
        }
        @Override public String toString() { return title + pages; }
    }

    public static void main(String[] args) {
        System.out.println("1. singleton      : " + Config.INSTANCE.get("env")
                + ", same instance = " + (LazyConfig.getInstance() == LazyConfig.getInstance()));

        System.out.println("2. factory method : " + ShapeFactory.of("circle").draw() + " / " + ShapeFactory.of("square").draw());

        UiFactory ui = new DarkFactory();                       // swap to LightFactory -> whole UI changes
        System.out.println("3. abstract factory: " + ui.button().render() + " + " + ui.checkbox().render()
                + " | " + new LightFactory().button().render());

        System.out.println("4. builder        : " + Pizza.size("large").cheese().bacon().build());

        Document original = new Document("spec", new ArrayList<>(List.of("p1", "p2")));
        Document copy = original.clone();
        copy.title = "spec-v2";
        copy.pages.add("p3");                                   // original untouched -> deep copy worked
        System.out.println("5. prototype      : " + original + " vs " + copy);
    }
}
