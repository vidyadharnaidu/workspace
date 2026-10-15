package discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * PATTERN: SERVICE DISCOVERY (server side)
 *
 * PROBLEM: in the cloud, instances come and go and IPs change. Hard-coded hosts break,
 *          and a config file cannot keep up with autoscaling.
 * SOLUTION: a registry. Services REGISTER on startup and send heartbeats; callers LOOK UP
 *           by logical name ("ORDER-SERVICE") and get the current list of instances.
 *
 * TRICK TO REMEMBER: client-side discovery (Eureka) = the CALLER gets the list and picks one.
 *                    server-side discovery (k8s Service, ALB) = a proxy picks for you,
 *                    the caller just uses one stable DNS name.
 *                    On Kubernetes you usually do NOT need Eureka - DNS already does this.
 *
 * Two moving parts:
 *   1. the registry itself      -> @EnableEurekaServer (this file)
 *   2. every other service      -> spring-cloud-starter-netflix-eureka-client on the classpath
 *
 * Dependency: spring-cloud-starter-netflix-eureka-server
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}

/**
 * The CLIENT half, for reference: this is what any normal service does.
 * Just having the eureka-client starter + spring.application.name is enough to register.
 */
@RestController
@EnableDiscoveryClient
class WhoIsOutThereController {

    private final DiscoveryClient discoveryClient;

    WhoIsOutThereController(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    /** Manual lookup. In real code you use @LoadBalanced RestClient or Feign instead (see 04). */
    @GetMapping("/instances")
    List<String> instances() {
        return discoveryClient.getServices().stream()
                .flatMap(name -> discoveryClient.getInstances(name).stream())
                .map(this::describe)
                .toList();
    }

    private String describe(ServiceInstance i) {
        return i.getServiceId() + " -> " + i.getUri() + " (zone=" + i.getMetadata().get("zone") + ")";
    }
}
