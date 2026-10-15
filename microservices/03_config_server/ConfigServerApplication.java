package config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PATTERN: EXTERNALIZED CONFIGURATION / CONFIG SERVER
 *
 * PROBLEM: 20 services x 4 environments = 80 property files, and changing a timeout
 *          means rebuilding and redeploying.
 * SOLUTION: config lives in git, one server serves it, services fetch it at startup
 *           and can refresh without a restart.
 *
 * TRICK TO REMEMBER: the URL shape is  /{application}/{profile}/{label}
 *                    e.g. /order-service/prod/main  ->  order-service-prod.yml from branch main.
 *                    More specific wins: order-service-prod.yml overrides order-service.yml
 *                    overrides application.yml.
 *
 * Refresh without restart:
 *   @RefreshScope bean + POST /actuator/refresh        (one instance)
 *   Spring Cloud Bus + POST /actuator/busrefresh       (every instance, over Kafka/Rabbit)
 *
 * Secrets: do NOT put them in git. Use encryption ({cipher}...) or Vault/AWS Secrets Manager.
 *
 * Dependency: spring-cloud-config-server
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}

/**
 * The CLIENT half: how a service reads the served values.
 * Needs spring-cloud-starter-config and spring.config.import=configserver:http://localhost:8888
 */
@RestController
@RefreshScope                       // <- without this the bean keeps the value it got at startup
class FeatureFlagController {

    @Value("${features.new-checkout:false}")
    private boolean newCheckout;

    @GetMapping("/features")
    String features() {
        return "new-checkout = " + newCheckout;
    }
}

/**
 * Prefer @ConfigurationProperties over scattered @Value: it is typed, validated,
 * and picks up refreshes without needing @RefreshScope on every consumer.
 */
@ConfigurationProperties(prefix = "payment")
class PaymentProperties {

    private String apiUrl;
    private int timeoutMs = 2_000;
    private int retries = 2;

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public int getRetries() { return retries; }
    public void setRetries(int retries) { this.retries = retries; }
}
