# GoF (Gamma) Design Patterns

Three files, one `main` each, no build step:

```
java java/src/designpatterns/CreationalPatterns.java
java java/src/designpatterns/StructuralPatterns.java
java java/src/designpatterns/BehavioralPatterns.java
```

## The 23 in one line each

### Creational — how the object gets made
| Pattern | One-line memory hook | Real Java example |
|---|---|---|
| Singleton | One instance, ever | `enum`, `Runtime.getRuntime()` |
| Factory Method | Caller says *what*, a method decides *which class* | `Calendar.getInstance()` |
| Abstract Factory | A factory of factories: a **matching family** of products | `DocumentBuilderFactory` |
| Builder | Many optional fields → `.a().b().build()` | `StringBuilder`, `Stream.Builder` |
| Prototype | Copy an existing object instead of constructing one | `Object.clone()` |

### Structural — how objects are wired together
| Pattern | One-line memory hook | Real Java example |
|---|---|---|
| Adapter | Wrap an incompatible API and translate | `Arrays.asList`, `InputStreamReader` |
| Bridge | Abstraction holds an implementation **field**, both grow separately | JDBC driver behind `Connection` |
| Composite | Leaf and container share one interface → recursion | Swing containers, file trees |
| Decorator | Wrapper that implements the interface and **adds** behaviour | `BufferedReader(new FileReader(..))` |
| Facade | One simple door to a messy subsystem | `javax.faces.context.FacesContext` |
| Flyweight | Share the heavy immutable part, pass the rest in | `Integer.valueOf` cache, `String` pool |
| Proxy | Same interface, but **controls** access (lazy, guard, cache, remote) | Hibernate lazy entities, RMI stubs |

### Behavioral — how objects talk and who decides
| Pattern | One-line memory hook | Real Java example |
|---|---|---|
| Chain of Responsibility | Handle it or pass it on | Servlet filters, log handlers |
| Command | Package a call as an object → queue, log, undo | `Runnable`, Swing `Action` |
| Interpreter | One class per grammar rule, each evaluates itself | `Pattern`, expression trees |
| Iterator | Walk without exposing the storage | `Iterator`, for-each |
| Mediator | N×N links become N links through one hub | `ExecutorService`, chat rooms |
| Memento | Opaque snapshot for undo | `Serializable` state, editor undo |
| Observer | Publish/subscribe, no coupling back | listeners, `Flow.Subscriber` |
| State | The object swaps its own behaviour; kills the big `switch` | order/connection lifecycles |
| Strategy | Caller picks the algorithm; one-method interface = lambda | `Comparator` |
| Template Method | `final` method fixes the step **order**, subclasses fill steps | `AbstractList`, `HttpServlet.service` |
| Visitor | Add a new **operation** without touching the classes; double dispatch | `FileVisitor`, AST walkers |

## Pairs that get confused in interviews

| These look alike | The difference |
|---|---|
| Decorator vs Proxy | Same shape. Decorator **adds** behaviour; Proxy **controls** access. |
| Strategy vs State | Strategy: **caller** picks the algorithm. State: the **object** picks and transitions itself. |
| Factory Method vs Abstract Factory | One product vs a **family** of products that must match. |
| Builder vs Abstract Factory | Builder builds **one** complex object step by step; Abstract Factory returns several at once. |
| Adapter vs Facade | Adapter changes an interface to fit; Facade simplifies many interfaces into one. |
| Composite vs Decorator | Both wrap. Composite = many children (a tree); Decorator = exactly one child. |
