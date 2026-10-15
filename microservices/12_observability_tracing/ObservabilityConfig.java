package observability;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * PATTERN: OBSERVABILITY - distributed tracing, metrics, health checks, log correlation
 *
 * PROBLEM: one user request touches 6 services. When it is slow, which one is slow?
 *          When it fails, whose log do you read?
 * SOLUTION: the THREE PILLARS.
 *   LOGS    what happened          -> structured JSON + a trace id in every line
 *   METRICS how much / how often   -> Micrometer -> Prometheus -> Grafana
 *   TRACES  where the time went    -> Micrometer Tracing -> Zipkin / Jaeger / Tempo
 *
 * TRICK TO REMEMBER: a TRACE is the whole request across services; a SPAN is one unit of
 *                    work inside it. The traceId is propagated over HTTP headers
 *                    (W3C `traceparent`) so the pieces can be stitched back together.
 *
 * TRICK: instrument the FOUR GOLDEN SIGNALS - latency, traffic, errors, saturation.
 *        And alert on SYMPTOMS the user feels (p99 latency, error rate), not on causes
 *        (CPU 80%), or you drown in noise.
 *
 * TRICK: sample traces in production (1-10%). 100% sampling costs more than the service.
 *
 * Dependencies: spring-boot-starter-actuator, micrometer-registry-prometheus,
 *               micrometer-tracing-bridge-brave, zipkin-reporter-brave
 */
public class ObservabilityConfig {

    // ---------- custom business metrics ----------
    @Service
    static class OrderMetrics {

        private final Counter placed;
        private final Counter failed;
        private final MeterRegistry registry;

        OrderMetrics(MeterRegistry registry) {
            this.registry = registry;
            this.placed = Counter.builder("orders.placed")
                    .description("orders successfully placed")
                    .tag("channel", "web")             // tags = dimensions you can slice by
                    .register(registry);
                    // TRICK: never tag with something unbounded (orderId, email).
                    // Each tag value is a new time series - that is how you kill Prometheus.
            this.failed = registry.counter("orders.failed");
        }

        void recordPlaced(double amount) {
            placed.increment();
            registry.summary("orders.amount").record(amount);    // distribution, for p50/p95/p99
        }

        /** A gauge tracks a value that goes up AND down. */
        void trackQueueDepth(java.util.Queue<?> queue) {
            registry.gauge("orders.queue.depth", queue, java.util.Queue::size);
        }
    }

    // ---------- tracing: spans and log correlation ----------
    @Service
    static class CheckoutService {

        private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

        private final Tracer tracer;

        CheckoutService(Tracer tracer) { this.tracer = tracer; }

        /** @Observed creates a span AND a timer AND (with Brave) the log context, in one annotation. */
        @Observed(name = "checkout", contextualName = "checkout-flow")
        public void checkout(String orderId) {
            // The traceId is already in the MDC, so every log line below carries it.
            log.info("checkout started for {}", orderId);
            reserveStock(orderId);
        }

        /** Manual span when you want to time a specific block and tag it. */
        void reserveStock(String orderId) {
            var span = tracer.nextSpan().name("reserve-stock").start();
            try (var ignored = tracer.withSpan(span)) {
                span.tag("order.id", orderId);
                // ... the work ...
            } catch (Exception e) {
                span.error(e);                       // marks the span red in Jaeger
                throw e;
            } finally {
                span.end();                          // ALWAYS in finally or the span leaks
            }
        }

        /** Propagating the correlation id by hand, e.g. onto a Kafka message. */
        String currentTraceId() {
            var span = tracer.currentSpan();
            return span == null ? "none" : span.context().traceId();
        }

        /** Pushing business context into every log line of this thread. */
        void withCustomer(String customerId, Runnable work) {
            MDC.put("customerId", customerId);
            try {
                work.run();
            } finally {
                MDC.remove("customerId");            // pooled threads: always remove
            }
        }
    }

    // ---------- health checks: liveness vs readiness ----------

    /**
     * TRICK - the distinction Kubernetes cares about:
     *   LIVENESS  "am I broken beyond repair?"  failing -> the pod is RESTARTED
     *   READINESS "can I serve traffic now?"    failing -> taken OUT of the load balancer
     *
     * Never put a downstream dependency in LIVENESS. If the database blips, a liveness
     * check on it restarts every pod at once and turns an outage into a catastrophe.
     * Dependencies belong in READINESS.
     */
    @Component
    static class PaymentGatewayHealth implements HealthIndicator {

        @Override
        public Health health() {
            try {
                long latency = ping();
                if (latency > 1_000) {
                    return Health.status("DEGRADED")
                            .withDetail("latencyMs", latency)
                            .build();
                }
                return Health.up().withDetail("latencyMs", latency).build();
            } catch (Exception e) {
                return Health.down(e).build();
            }
        }

        private long ping() { return 42; }
    }

    @Timed(value = "legacy.report", percentiles = {0.5, 0.95, 0.99})
    static class ReportJob implements Runnable {
        public void run() { }
    }
}
