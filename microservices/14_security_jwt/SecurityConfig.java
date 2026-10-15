package security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * PATTERN: ACCESS TOKEN / OAUTH2 RESOURCE SERVER  (+ token relay between services)
 *
 * PROBLEM: the gateway authenticated the user - but service C, three hops in, has no idea
 *          who the caller is. Passing a user id in a header is trivially forged.
 * SOLUTION: a signed JWT travels with the request. Every service VERIFIES the signature
 *           locally against the issuer's public keys (JWKS) - no call to the auth server
 *           per request, so it scales.
 *
 * TRICK TO REMEMBER:
 *   AUTHENTICATION = who are you   (the token's signature and issuer)
 *   AUTHORIZATION  = what may you do (the claims: scopes, roles)
 *
 * TRICK: the gateway does COARSE checks (is the token valid at all), each service does
 *        FINE checks (does this user own this order). Never trust "the gateway checked it" -
 *        that is the confused deputy problem, and anything inside the mesh could call you.
 *
 * TRICK: JWTs cannot be un-issued. Keep them SHORT lived (5-15 min) with a refresh token,
 *        or you need a revocation list and you have lost the statelessness you wanted.
 *
 * Dependencies: spring-boot-starter-oauth2-resource-server, spring-boot-starter-security
 */
@Configuration
@EnableMethodSecurity                 // turns on @PreAuthorize
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // No sessions, no CSRF: the token is the whole state.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").hasRole("OPS")
                        .requestMatchers("/orders/**").hasAuthority("SCOPE_orders.read")
                        .anyRequest().authenticated())

                // One line makes this a resource server: validate the JWT on every request.
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(authoritiesConverter())))

                .build();
    }

    /**
     * Keycloak, Auth0 and Cognito all nest roles differently. This maps their claim
     * into Spring's ROLE_ authorities, which is the bit everyone gets stuck on.
     */
    @Bean
    JwtAuthenticationConverter authoritiesConverter() {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        scopes.setAuthorityPrefix("SCOPE_");
        scopes.setAuthoritiesClaimName("scope");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> Stream.concat(
                scopes.convert(jwt).stream(),
                realmRoles(jwt).stream()).toList());
        return converter;
    }

    @SuppressWarnings("unchecked")
    private static Collection<GrantedAuthority> realmRoles(Jwt jwt) {
        var realmAccess = (java.util.Map<String, Object>) jwt.getClaims().get("realm_access");
        if (realmAccess == null) return List.of();
        var roles = (List<String>) realmAccess.getOrDefault("roles", List.of());
        return roles.stream()
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
    }
}

@RestController
class OrderSecurityController {

    /** Coarse: needs the role at all. */
    @GetMapping("/orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    List<String> myOrders(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();              // trustworthy: the signature covers it
        return List.of("order for " + userId);
    }

    /**
     * Fine: ownership. The token says WHO you are; only this service knows WHAT you own.
     * This check cannot live in the gateway.
     */
    @GetMapping("/orders/{id}")
    @PreAuthorize("@orderGuard.isOwner(#id, authentication.name) or hasRole('SUPPORT')")
    String getOrder(String id) {
        return "order " + id;
    }
}

/**
 * SERVICE TO SERVICE, where there is no user:
 *   CLIENT CREDENTIALS grant - the service itself has a client id/secret and gets its own
 *   token with its own scopes. Use this for background jobs and cron work.
 *
 *   TOKEN RELAY - for user-initiated calls, forward the user's token to the next hop so
 *   the ownership checks still work downstream:
 *
 *     spring:
 *       cloud:
 *         gateway:
 *           default-filters:
 *             - TokenRelay=
 *
 *   and in a Feign interceptor:
 *     template.header("Authorization", "Bearer " + currentToken());
 *
 * MTLS / service mesh (Istio, Linkerd) does the transport-level half: every pod gets a
 * certificate, so services prove their identity to each other without any app code.
 */
class ServiceToServiceNotes { }
