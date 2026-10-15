package strangler;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

/**
 * PATTERN: STRANGLER FIG  (+ anti-corruption layer, + parallel run)
 *
 * PROBLEM: you have a monolith and you want microservices. A big-bang rewrite is the
 *          classic way to burn two years and ship nothing.
 * SOLUTION: put a facade (the gateway) in front of the monolith. Move ONE capability at a
 *           time behind it. Route that path to the new service; everything else still goes
 *           to the monolith. Repeat until the monolith has nothing left, then delete it.
 *
 * TRICK TO REMEMBER: named after the strangler fig, which grows around a tree until the
 *                    tree is gone and the fig stands on its own. The FACADE comes first -
 *                    without the routing layer you cannot move anything incrementally.
 *
 * Order of extraction: start with what is LEAST coupled and HIGHEST value
 * (notifications, search, reporting), not with the core domain.
 *
 * The hard part is never the code. It is the SHARED DATABASE. Usual sequence:
 *   1. new service reads and writes the monolith's tables   (ugly, but it moves)
 *   2. new service owns its own tables, syncs via events / CDC
 *   3. monolith stops touching those tables
 *   4. delete the old code path
 */
@Configuration
public class StranglerFigRouting {

    /**
     * The routing table IS the migration plan. Each line that moves off `monolith`
     * is one capability extracted.
     */
    @Bean
    RouteLocator stranglerRoutes(RouteLocatorBuilder builder) {
        return builder.routes()

                // ---- already extracted: these are real services now ----
                .route("notifications", r -> r.path("/api/notifications/**")
                        .uri("lb://NOTIFICATION-SERVICE"))

                .route("search", r -> r.path("/api/search/**")
                        .uri("lb://SEARCH-SERVICE"))

                // ---- being extracted: canary by header or weight ----
                // 10% of order traffic hits the new service; 90% still the monolith.
                .route("orders-new", r -> r.path("/api/orders/**")
                        .and().weight("orders", 10)
                        .uri("lb://ORDER-SERVICE"))
                .route("orders-legacy", r -> r.path("/api/orders/**")
                        .and().weight("orders", 90)
                        .uri("http://monolith:8080"))

                // Internal testers get the new path regardless of the weight.
                .route("orders-beta", r -> r.path("/api/orders/**")
                        .and().header("X-Beta-User", "true")
                        .uri("lb://ORDER-SERVICE"))

                // ---- not yet extracted: everything else still goes to the monolith ----
                .route("monolith", r -> r.path("/**")
                        .uri("http://monolith:8080"))

                .build();
    }
}

/**
 * ANTI-CORRUPTION LAYER (ACL)
 *
 * The monolith's model is 15 years of accumulated compromise. If the new service speaks
 * that model, the mess spreads into the new code and you have rewritten nothing.
 *
 * TRICK: the ACL is a TRANSLATION layer at the boundary. Legacy vocabulary stops there;
 *        inside the new service, only the clean domain model exists.
 */
@Service
class LegacyOrderAcl {

    private final LegacyMonolithClient legacy;

    LegacyOrderAcl(LegacyMonolithClient legacy) { this.legacy = legacy; }

    /** Everything ugly - flags, codes, date strings - is confined to this one method. */
    Order fetch(String orderId) {
        LegacyOrderDto dto = legacy.getOrderRecord(orderId);

        return new Order(
                dto.ORD_ID,
                mapStatus(dto.STAT_CD),                    // "03" -> SHIPPED
                dto.AMT_CENTS / 100.0,                     // cents -> currency
                dto.CUST_REF.trim());
    }

    private OrderStatus mapStatus(String legacyCode) {
        return switch (legacyCode) {
            case "01" -> OrderStatus.NEW;
            case "02" -> OrderStatus.PAID;
            case "03" -> OrderStatus.SHIPPED;
            case "09", "99" -> OrderStatus.CANCELLED;      // two codes, one meaning, of course
            default -> OrderStatus.UNKNOWN;
        };
    }

    /** The clean model the rest of the service uses. No legacy words anywhere. */
    record Order(String id, OrderStatus status, double amount, String customerId) { }

    enum OrderStatus { NEW, PAID, SHIPPED, CANCELLED, UNKNOWN }

    static class LegacyOrderDto {
        String ORD_ID;
        String STAT_CD;
        long AMT_CENTS;
        String CUST_REF;
    }

    interface LegacyMonolithClient {
        LegacyOrderDto getOrderRecord(String orderId);
    }
}

/**
 * PARALLEL RUN (a.k.a. dark launch / shadow traffic)
 *
 * Before flipping the weight to 100%, send the request to BOTH and compare. You get real
 * production traffic against the new service with zero user risk.
 *
 * TRICK: the shadow call must be ASYNC and its result DISCARDED - it must never affect
 *        the response or the latency the user sees. And never shadow a WRITE unless the
 *        new service writes somewhere throwaway.
 */
@Service
class ParallelRun {

    private final LegacyOrderAcl legacy;
    private final NewOrderService modern;
    private final java.util.concurrent.Executor shadowPool;

    ParallelRun(LegacyOrderAcl legacy, NewOrderService modern, java.util.concurrent.Executor shadowPool) {
        this.legacy = legacy;
        this.modern = modern;
        this.shadowPool = shadowPool;
    }

    LegacyOrderAcl.Order get(String orderId) {
        LegacyOrderAcl.Order authoritative = legacy.fetch(orderId);   // the answer the user gets

        shadowPool.execute(() -> {                                     // fire and forget
            try {
                LegacyOrderAcl.Order candidate = modern.fetch(orderId);
                if (!candidate.equals(authoritative)) {
                    // metrics.counter("shadow.mismatch").increment();
                    // log.warn("mismatch for {}: {} vs {}", orderId, authoritative, candidate);
                }
            } catch (Exception ignored) {
                // A shadow failure is a metric, never an error the user sees.
            }
        });

        return authoritative;
    }

    interface NewOrderService { LegacyOrderAcl.Order fetch(String orderId); }
}
