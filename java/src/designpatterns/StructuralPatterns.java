package designpatterns;

import java.util.*;

/**
 * GoF STRUCTURAL patterns - all about "how objects are wired together".
 * Remember them as: A BCD FFP - Adapter, Bridge, Composite, Decorator, Facade, Flyweight, Proxy.
 *
 *  1. Adapter    make an incompatible API fit      -> wrap and translate
 *  2. Bridge     split abstraction from impl       -> two hierarchies, one field
 *  3. Composite  treat one and many the same       -> tree, leaf and node share an interface
 *  4. Decorator  add behaviour at runtime          -> wrapper that also implements the interface
 *  5. Facade     one simple door to a messy system -> single method hiding N calls
 *  6. Flyweight  share the heavy common part       -> cache of immutable state
 *  7. Proxy      stand-in that controls access     -> lazy / guard / cache / remote
 *
 * TRICK: Decorator vs Proxy - same shape, different intent.
 *        Decorator ADDS behaviour, Proxy CONTROLS access.
 */
public class StructuralPatterns {

    // ---------- 1. ADAPTER ----------
    interface JsonSource { String json(); }                 // what our code wants
    static class LegacyXml { String xml() { return "<user>ann</user>"; } }   // what we have

    record XmlToJsonAdapter(LegacyXml legacy) implements JsonSource {
        public String json() { return "{\"user\":\"" + legacy.xml().replaceAll("<.*?>", "") + "\"}"; }
    }

    // ---------- 2. BRIDGE ----------
    // TRICK: without it you get a class explosion (TvRemote, RadioRemote, TvBasicRemote...).
    interface Device { String on(); }                       // implementation side
    static class Tv    implements Device { public String on() { return "TV on"; } }
    static class Radio implements Device { public String on() { return "Radio on"; } }

    static class Remote {                                   // abstraction side
        protected final Device device;                      // <- the bridge is just this field
        Remote(Device device) { this.device = device; }
        String press() { return device.on(); }
    }
    static class AdvancedRemote extends Remote {            // grows independently of Device
        AdvancedRemote(Device d) { super(d); }
        @Override String press() { return device.on() + " + muted"; }
    }

    // ---------- 3. COMPOSITE ----------
    interface Node { int size(); String name(); }
    record File(String name, int size) implements Node { }
    record Folder(String name, List<Node> children) implements Node {
        public int size() { return children.stream().mapToInt(Node::size).sum(); }   // recursion is the whole trick
    }

    // ---------- 4. DECORATOR ----------
    interface Coffee { String desc(); double cost(); }
    record Espresso() implements Coffee { public String desc() { return "espresso"; } public double cost() { return 2.0; } }

    // A decorator IMPLEMENTS the interface and HOLDS one - so you can stack them forever.
    record WithMilk(Coffee inner) implements Coffee {
        public String desc() { return inner.desc() + " + milk"; }
        public double cost() { return inner.cost() + 0.5; }
    }
    record WithSugar(Coffee inner) implements Coffee {
        public String desc() { return inner.desc() + " + sugar"; }
        public double cost() { return inner.cost() + 0.2; }
    }

    // ---------- 5. FACADE ----------
    static class Cpu    { String boot()  { return "cpu"; } }
    static class Memory { String load()  { return "memory"; } }
    static class Disk   { String read()  { return "disk"; } }

    static class Computer {                                  // caller says start(), not 3 calls in the right order
        private final Cpu cpu = new Cpu();
        private final Memory mem = new Memory();
        private final Disk disk = new Disk();
        String start() { return String.join(" -> ", disk.read(), mem.load(), cpu.boot(), "ready"); }
    }

    // ---------- 6. FLYWEIGHT ----------
    // TRICK: split state into INTRINSIC (shared, immutable) and EXTRINSIC (passed in per call).
    record Glyph(char symbol, String font) {                 // intrinsic: heavy, shared
        String drawAt(int x, int y) { return symbol + "@" + x + "," + y; }   // extrinsic: x,y per use
    }
    static class GlyphPool {
        private static final Map<String, Glyph> CACHE = new HashMap<>();
        static Glyph get(char c, String font) { return CACHE.computeIfAbsent(c + font, k -> new Glyph(c, font)); }
        static int cached() { return CACHE.size(); }
    }

    // ---------- 7. PROXY ----------
    interface Image { String render(); }
    static class RealImage implements Image {                // expensive to create
        private final String file;
        RealImage(String file) { this.file = file; System.out.println("   (loading " + file + " from disk)"); }
        public String render() { return "image:" + file; }
    }
    static class LazyImageProxy implements Image {           // same interface, controls creation
        private final String file;
        private RealImage real;
        LazyImageProxy(String file) { this.file = file; }
        public String render() {
            if (real == null) real = new RealImage(file);    // created only on first real use
            return real.render();
        }
    }

    public static void main(String[] args) {
        System.out.println("1. adapter   : " + new XmlToJsonAdapter(new LegacyXml()).json());

        System.out.println("2. bridge    : " + new Remote(new Tv()).press()
                + " | " + new AdvancedRemote(new Radio()).press());

        Node tree = new Folder("root", List.of(
                new File("a.txt", 10),
                new Folder("sub", List.of(new File("b.txt", 20), new File("c.txt", 30)))));
        System.out.println("3. composite : " + tree.name() + " total size = " + tree.size());

        Coffee order = new WithSugar(new WithMilk(new Espresso()));   // wrap, wrap, wrap
        System.out.println("4. decorator : " + order.desc() + " = " + order.cost());

        System.out.println("5. facade    : " + new Computer().start());

        GlyphPool.get('a', "Arial"); GlyphPool.get('a', "Arial"); GlyphPool.get('b', "Arial");
        System.out.println("6. flyweight : 3 requests -> " + GlyphPool.cached() + " objects, "
                + GlyphPool.get('a', "Arial").drawAt(5, 7));

        Image img = new LazyImageProxy("cat.png");
        System.out.println("7. proxy     : created, nothing loaded yet");
        System.out.println("7. proxy     : " + img.render());
        System.out.println("7. proxy     : " + img.render() + " (cached, no second load)");
    }
}
