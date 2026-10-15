package client;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * PATTERN: DECLARATIVE REST CLIENT + CLIENT-SIDE LOAD BALANCING
 *
 * PROBLEM: calling another service by hand means URL building, serialization, timeouts,
 *          retries and picking one of N instances - repeated in every caller.
 * SOLUTION: declare the remote API as a Java interface; the framework writes the call,
 *           and the load balancer picks the instance.
 *
 * TRICK TO REMEMBER: "lb://NAME" (not a host) is the whole point - the name is resolved
 *                    through discovery at CALL time, so scaling up needs no redeploy.
 *
 * Dependencies: spring-cloud-starter-openfeign, spring-cloud-starter-loadbalancer
 */
@Configuration
@EnableFeignClients                 // scans for @FeignClient interfaces
class ClientConfig {

    /**
     * The non-Feign way. @LoadBalanced teaches RestClient to understand lb:// URIs.
     * RestClient is the Spring 6.1+ replacement for RestTemplate.
     */
    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClient() {
        return RestClient.builder();
    }
}

/** The remote API, written as an interface. No implementation - Feign generates it. */
@FeignClient(
        name = "INVENTORY-SERVICE",                     // the discovery id, NOT a hostname
        fallback = InventoryFallback.class,             // see 05: needs feign.circuitbreaker.enabled=true
        configuration = InventoryClient.Config.class)
interface InventoryClient {

    @GetMapping("/inventory/{sku}")
    Stock get(@PathVariable("sku") String sku);

    @PostMapping("/inventory/reserve")
    Reservation reserve(@RequestBody ReserveRequest request,
                        @RequestHeader("Idempotency-Key") String key);   // see 11

    @GetMapping("/inventory")
    List<Stock> search(@RequestParam("warehouse") String warehouse);

    record Stock(String sku, int available) { }
    record ReserveRequest(String sku, int qty) { }
    record Reservation(String id, boolean ok) { }

    /** Per-client config: timeouts belong here, never at the default. */
    class Config {
        @Bean
        feign.Request.Options options() {
            return new feign.Request.Options(
                    1_000, java.util.concurrent.TimeUnit.MILLISECONDS,   // connect timeout
                    3_000, java.util.concurrent.TimeUnit.MILLISECONDS,   // read timeout
                    true);                                              // follow redirects
        }

        /** Propagate the correlation id and the caller's token to the next hop. */
        @Bean
        feign.RequestInterceptor propagateHeaders() {
            return template -> template.header("X-Correlation-Id",
                    org.slf4j.MDC.get("correlationId"));
        }
    }
}

/** Fallback runs when the circuit is open or the call fails. Degrade, do not crash. */
@org.springframework.stereotype.Component
class InventoryFallback implements InventoryClient {
    public Stock get(String sku) { return new Stock(sku, 0); }                       // "assume out of stock"
    public Reservation reserve(ReserveRequest r, String k) { return new Reservation(null, false); }
    public List<Stock> search(String warehouse) { return List.of(); }
}

@Service
class CheckoutService {

    private final InventoryClient inventory;
    private final RestClient restClient;

    CheckoutService(InventoryClient inventory, RestClient.Builder builder) {
        this.inventory = inventory;
        this.restClient = builder.build();
    }

    boolean canFulfil(String sku, int qty) {
        return inventory.get(sku).available() >= qty;        // looks local, is a network call
    }

    /** Same call without Feign. Note lb://PRICING-SERVICE instead of a real host. */
    double price(String sku) {
        return restClient.get()
                .uri("lb://PRICING-SERVICE/prices/{sku}", sku)
                .retrieve()
                .body(Double.class);
    }
}
