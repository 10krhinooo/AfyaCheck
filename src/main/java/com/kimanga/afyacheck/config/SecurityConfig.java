package com.kimanga.afyacheck.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Identity is owned by Keycloak (Authorization Code + PKCE from the React SPA against the
 * "afyacheck" realm) — this app is a stateless OAuth2 resource server that only validates
 * the resulting JWTs (see application.properties' issuer-uri and keycloak/realm-export.json).
 *
 * The app-level questionnaire session (SessionService, keyed off HttpSession) is unrelated to
 * identity and unaffected by this: SessionCreationPolicy stays IF_REQUIRED (not STATELESS) so
 * anonymous users can still start/progress a questionnaire and get a JSESSIONID-backed session,
 * exactly as before. Auth just no longer rides on that session.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Still needed by UserService's register()/resetPassword() methods, which remain in the
     * codebase (called only internally/from tests now that AuthController and its Thymeleaf
     * forms are gone — Keycloak's hosted screens own registration and password reset).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, KeycloakUserSyncFilter keycloakUserSyncFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.changeSessionId())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**",
                                "/app/**",
                                "/assets/**",
                                "/favicon.svg",
                                // Anonymous risk-screening is a deliberate product requirement (this is a
                                // public health-screening tool) — the questionnaire, its results, the
                                // health-center finder, and the Maps key must not require login.
                                "/api/questionnaire/**",
                                "/api/questions/**",
                                "/api/results/**",
                                "/api/config/**"
                        ).permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter()))
                )
                // Runs after BearerTokenAuthenticationFilter so the JWT's Authentication is
                // already on the SecurityContext when it syncs the local users-table cache row.
                .addFilterAfter(keycloakUserSyncFilter, BearerTokenAuthenticationFilter.class)
                .headers(headers -> headers
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                        )
                        .frameOptions(frame -> frame.sameOrigin())
                );

        return http.build();
    }

    /**
     * KeycloakUserSyncFilter is registered explicitly above (inside Spring Security's chain).
     * Without this, Spring Boot would also auto-register it as a generic servlet filter
     * (since it's a @Component implementing Filter), running it a second time outside the
     * security chain — before the JWT has been validated, so SecurityContext would be empty.
     */
    @Bean
    public FilterRegistrationBean<KeycloakUserSyncFilter> disableAutoRegistration(KeycloakUserSyncFilter filter) {
        FilterRegistrationBean<KeycloakUserSyncFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * Maps Keycloak's realm roles (JWT claim realm_access.roles, e.g. ["ADMIN"]) into
     * ROLE_-prefixed GrantedAuthoritys, so @PreAuthorize("hasRole('ADMIN')") on AdminController
     * keeps working exactly as it did when roles came from User.getAuthorities().
     */
    @Bean
    public JwtAuthenticationConverter keycloakJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
            Collection<org.springframework.security.core.GrantedAuthority> scopeAuthorities = scopeConverter.convert(jwt);

            Object realmAccess = jwt.getClaim("realm_access");
            Stream<String> realmRoles = Stream.empty();
            if (realmAccess instanceof Map<?, ?> realmAccessMap && realmAccessMap.get("roles") instanceof List<?> roles) {
                realmRoles = roles.stream().map(Object::toString);
            }

            List<org.springframework.security.core.GrantedAuthority> roleAuthorities = realmRoles
                    .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toList());

            return Stream.concat(scopeAuthorities.stream(), roleAuthorities.stream()).toList();
        });
        return converter;
    }
}
