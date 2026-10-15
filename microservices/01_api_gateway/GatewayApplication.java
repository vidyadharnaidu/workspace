package gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * PATTERN: API GATEWAY
 *
 * PROBLEM: the mobile app would otherwise need the host of all 12 services, and each
 *          service would repeat auth, CORS, rate limiting and logging.
 * SOLUTION: one entry point that routes, and owns the cross-cutting concerns.
 *
 * TRICK TO REMEMBER: the gateway does ROUTING + CROSS-CUTTING. The moment it starts
 *                    doing business logic, it has become a distributed monolith.
 *
 * Dependencies: spring-cloud-starter-gateway, spring-cloud-starter-netflix-eureka-client
 * Spring Cloud Gateway is REACTIVE (Netty) - do not add spring-boot-starter-web to it.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    /**
     * Routes in Java. The same thing can be done in application.yml (see this folder) -
     * yml for static routes, Java when you need conditions or loops.
     *
     * lb://ORDER-SERVICE means "ask the discovery server where ORDER-SERVICE lives and
     * load balance across its instances".
     */
    @Bean
    RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("orders", r -> r
                        .path("/api/orders/**")
                        .filters(f -> f
                                .stripPrefix(1)                              // /api/orders/7 -> /orders/7
                                .addRequestHeader("X-Source", "gateway")
                                .circuitBreaker(cb -> cb.setName("ordersCb")
                                        .setFallbackUri("forward:/fallback/orders")))
                        .uri("lb://ORDER-SERVICE"))
                .route("payments", r -> r
                        .path("/api/payments/**")
                        .filters(f -> f.stripPrefix(1)
                                .retry(retry -> retry.setRetries(2)))        // retry only GETs by default
                        .uri("lb://PAYMENT-SERVICE"))
                .route("legacy", r -> r
                        .path("/api/reports/**")
                        .uri("http://legacy-monolith:8080"))                 // see 15_strangler_fig
                .build();
    }

    /**
     * A global filter runs on EVERY route. This is the correlation id that makes
     * distributed tracing readable - generate it once, pass it everywhere.
     */
    @Bean
    GlobalFilter correlationIdFilter() {
        return (exchange, chain) -> {
            String id = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");
            String correlationId = (id != null) ? id : java.util.UUID.randomUUID().toString();

            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header("X-Correlation-Id", correlationId)
                    .build();

            ServerWebExchange next = exchange.mutate().request(mutated).build();
            next.getResponse().getHeaders().add("X-Correlation-Id", correlationId);

            return chain.filter(next);
        };
    }

    /** Ordering: lower value runs earlier. Auth filters belong near the front. */
    @Bean
    OrderedAuthFilter authFilter() {
        return new OrderedAuthFilter();
    }

    static class OrderedAuthFilter implements GlobalFilter, Ordered {
        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (auth == null && exchange.getRequest().getPath().value().startsWith("/api/orders")) {
                exchange.getResponse().setRawStatusCode(401);
                return exchange.getResponse().setComplete();          // short circuit, never reaches the service
            }
            return chain.filter(exchange);
        }

        @Override public int getOrder() { return -100; }
    }
}
