package bff;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * PATTERNS: BACKEND FOR FRONTEND (BFF) + API COMPOSITION / AGGREGATOR
 *
 * PROBLEM 1 (BFF): the mobile app wants 4 fields on a slow connection; the web dashboard
 *                  wants 40. One shared API either overfetches for mobile or starves the web.
 * SOLUTION 1: one backend PER CLIENT TYPE, owned by that client's team. Each shapes the
 *             response for its own screen.
 *
 * PROBLEM 2 (Composition): the order page needs data from order + user + payment + shipping.
 *                          The client should not make 4 round trips and know 4 hosts.
 * SOLUTION 2: one call to the BFF, which fans out IN PARALLEL and stitches the result.
 *
 * TRICK TO REMEMBER: fan out in PARALLEL, never in a loop. Four sequential 100ms calls
 *                    are 400ms; four parallel ones are ~100ms. This is the single biggest
 *                    latency win in an aggregator.
 *
 * TRICK: the aggregator must DEGRADE, not fail. If reviews are down, return the order
 *        without reviews - one optional dependency must not take out the whole page.
 *
 * TRICK: GraphQL is the other answer to the same problem: the client picks the fields,
 *        so you do not need a BFF per client. The cost is query complexity and N+1s.
 *
 * WATCH OUT: an aggregator hides fan-out. One incoming request becoming 30 downstream
 *            calls is how you accidentally DDoS your own services.
 */
@RestController
public class MobileBffController {

    private final OrderClient orders;
    private final UserClient users;
    private final ShippingClient shipping;
    private final ReviewClient reviews;
    private final Executor executor;         // a bounded pool, NOT the common ForkJoinPool

    public MobileBffController(OrderClient orders, UserClient users, ShippingClient shipping,
                               ReviewClient reviews, Executor executor) {
        this.orders = orders;
        this.users = users;
        this.shipping = shipping;
        this.reviews = reviews;
        this.executor = executor;
    }

    /**
     * One client call, four parallel service calls, one small mobile-shaped response.
     */
    @GetMapping("/mobile/orders/{orderId}")
    public MobileOrderView orderPage(@PathVariable String orderId) {

        // Kick all of them off first - nothing blocks yet.
        CompletableFuture<Order> orderF = CompletableFuture
                .supplyAsync(() -> orders.get(orderId), executor);

        // This one depends on the order, so it chains rather than starting now.
        CompletableFuture<User> userF = orderF
                .thenApplyAsync(o -> users.get(o.customerId()), executor)
                .completeOnTimeout(User.unknown(), 500, java.util.concurrent.TimeUnit.MILLISECONDS);

        CompletableFuture<Shipment> shipmentF = CompletableFuture
                .supplyAsync(() -> shipping.forOrder(orderId), executor)
                .exceptionally(e -> Shipment.unknown());          // optional -> degrade

        CompletableFuture<List<Review>> reviewsF = CompletableFuture
                .supplyAsync(() -> reviews.forOrder(orderId), executor)
                .orTimeout(300, java.util.concurrent.TimeUnit.MILLISECONDS)
                .exceptionally(e -> List.of());                   // optional -> empty

        // Now join. allOf waits for every branch; join() after it never blocks long.
        CompletableFuture.allOf(orderF, userF, shipmentF, reviewsF)
                .orTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
                .join();

        Order order = orderF.join();
        return new MobileOrderView(
                order.id(),
                order.total(),
                order.status(),
                userF.join().displayName(),
                shipmentF.join().eta(),
                reviewsF.join().size());          // mobile wants the COUNT, not the reviews
    }

    /** The mobile-shaped DTO: 6 fields, not the union of four services' models. */
    record MobileOrderView(String orderId, double total, String status,
                           String customerName, String eta, int reviewCount) { }

    // ---------- the downstream contracts ----------
    record Order(String id, String customerId, double total, String status, List<String> lines) { }

    record User(String id, String displayName, String email) {
        static User unknown() { return new User("?", "Customer", null); }
    }

    record Shipment(String id, String eta, String carrier) {
        static Shipment unknown() { return new Shipment("?", "unknown", "?"); }
    }

    record Review(String id, int stars) { }

    interface OrderClient { Order get(String orderId); }
    interface UserClient { User get(String userId); }
    interface ShippingClient { Shipment forOrder(String orderId); }
    interface ReviewClient { List<Review> forOrder(String orderId); }

    /**
     * TRICK: give the fan-out its OWN bounded pool. Sharing the ForkJoinPool.commonPool()
     * means one slow dependency starves every parallel stream in the JVM.
     */
    static Executor bffPool() {
        return new java.util.concurrent.ThreadPoolExecutor(
                20, 50, 60, java.util.concurrent.TimeUnit.SECONDS,
                new java.util.concurrent.ArrayBlockingQueue<>(200),
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());  // backpressure
    }

    static Duration budget() { return Duration.ofSeconds(2); }
}
