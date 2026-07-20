package com.kimanga.afyacheck.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
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
     * The SPA's oidc-client-ts talks directly to this origin (OIDC discovery + token endpoint)
     * from the browser, so it must be allowed in the CSP's connect-src below or every
     * signinRedirect()/silent-renew call gets blocked client-side before it even leaves the
     * page — same default as keycloak.admin.server-url since it's the same Keycloak instance.
     */
    @Value("${keycloak.admin.server-url:http://localhost:8180}")
    private String keycloakServerUrl;

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            KeycloakUserSyncFilter keycloakUserSyncFilter,
            AuditingAccessDeniedHandler auditingAccessDeniedHandler) throws Exception {
        http
                // Double-submit cookie CSRF (Spring's documented pattern for a same-origin SPA):
                // the token rides in a JS-readable cookie, api-client.ts echoes it back as the
                // X-XSRF-TOKEN header on every mutating request. Needed because the anonymous
                // questionnaire flow's /api/questionnaire/start relies on the ambient JSESSIONID
                // cookie alone (see KeycloakUserSyncFilter's JWT-bearer paths, which aren't
                // cookie-authenticated and so aren't CSRF-exploitable in the first place, but the
                // filter applies uniformly since that's simpler to reason about than a per-route
                // carve-out).
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )
                // CookieCsrfTokenRepository only writes the cookie once something actually reads
                // the deferred CsrfToken; without forcing that read here, a browser's first
                // (GET) page load never receives the cookie, so the first POST/DELETE 403s.
                .addFilterAfter(new CsrfCookieFilter(), BearerTokenAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions.accessDeniedHandler(auditingAccessDeniedHandler))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.changeSessionId())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/app/**",
                                "/assets/**",
                                "/favicon.svg",
                                "/index.html",
                                // vite-react-ssg's client runtime fetches these (no auth header
                                // attached — it's a plain browser fetch, not apiFetch) to look up
                                // prerendered loader data for the current route; unauthenticated
                                // visitors landing on "/" (or an /app/** deep link, which reuses
                                // that same prerendered shell) need to reach them unauthenticated
                                // too, or the fetch 401s with an empty body and vite-react-ssg's
                                // `.json()` call on it throws "Unexpected end of JSON input".
                                "/static-loader-data-manifest-*.json",
                                "/static-loader-data/**",
                                // PWA service worker + manifest (vite-plugin-pwa output). The
                                // browser fetches these with no auth header; without permitAll
                                // the SW registration 401s and the PWA install/precache never
                                // happens for anonymous users.
                                "/sw.js",
                                "/registerSW.js",
                                "/workbox-*.js",
                                "/manifest.webmanifest",
                                "/pwa-*.png",
                                // /app/** forwards here (see WebConfig) — needed alongside
                                // "/app/**" for the same reason "/index.html" is listed
                                // separately: Spring Security re-evaluates on the internal
                                // FORWARD dispatch too.
                                "/app-shell.html",
                                // Prerendered marketing/legal pages (see WebConfig) — public by nature,
                                // same as "/" above. Both the clean path and its forward:/*.html target
                                // are needed: Spring Security re-evaluates on the internal FORWARD
                                // dispatch too, so only permitting the clean path 401s on the forward
                                // (same reason "/index.html" is listed separately from "/app/**").
                                "/about",
                                "/about.html",
                                "/faq",
                                "/faq.html",
                                "/privacy",
                                "/privacy.html",
                                "/terms",
                                "/terms.html",
                                // Education pages: hub (learn.html) + topic files under /learn/.
                                "/learn",
                                "/learn.html",
                                "/learn/**",
                                // Anonymous risk-screening is a deliberate product requirement (this is a
                                // public health-screening tool) — the questionnaire, its results, the
                                // health-center finder, and the Maps key must not require login.
                                "/api/questionnaire/**",
                                "/api/questions/**",
                                "/api/results/**",
                                "/api/health-centers/**",
                                // Opt-in retest reminder: same anonymous access model as
                                // /api/results/notify (and the same strict rate limit).
                                "/api/reminders",
                                "/api/config/**"
                        ).permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // API docs describe admin endpoint shapes too, so gate the same as
                        // /api/admin/** rather than leaving the whole API contract public.
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .hasRole("ADMIN")
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
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                        // script-src/connect-src allow maps.googleapis.com for the health-centers
                        // Maps JS embed (see HealthCenterController, useGoogleMaps.ts); img-src
                        // covers the map tiles Maps JS pulls from Google's CDN domains.
                        //
                        // script-src needs 'unsafe-inline': vite-react-ssg emits two unhashed
                        // inline <script> tags in index.html (window.__staticRouterHydrationData
                        // and window.__VITE_REACT_SSG_HASH__) with no nonce/hash support of its
                        // own — without this the browser silently drops both, the hash stays
                        // undefined, the SPA's static-loader-data-manifest-undefined.json fetch
                        // 404s, and .json() throws "Unexpected end of JSON input" on every
                        // /app/** page load.
                        //
                        // connect-src needs the Keycloak origin: oidc-client-ts's signinRedirect()
                        // fetches the OIDC discovery document straight from the browser — without
                        // it that fetch is blocked by CSP and login always fails with "The
                        // authentication service is unavailable right now."
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; "
                                        + "script-src 'self' 'unsafe-inline' https://maps.googleapis.com; "
                                        + "style-src 'self' 'unsafe-inline'; "
                                        + "img-src 'self' data: https://*.googleapis.com https://*.gstatic.com https://*.ggpht.com; "
                                        + "font-src 'self' data:; "
                                        + "connect-src 'self' " + keycloakServerUrl + " https://maps.googleapis.com; "
                                        + "frame-ancestors 'self'; "
                                        + "base-uri 'self'; "
                                        + "form-action 'self'"
                        ))
                );

        return http.build();
    }

    /**
     * Forces the deferred {@link CsrfToken} to render into its cookie on every request (see the
     * comment above where this is registered) -- package-private and static so it can be unit
     * tested directly instead of only ever running inside a full Spring Security filter chain.
     */
    static class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
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
