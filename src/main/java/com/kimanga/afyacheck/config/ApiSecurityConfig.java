package com.kimanga.afyacheck.config;

import com.kimanga.afyacheck.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

/**
 * A second, stateless SecurityFilterChain for the /api/** REST surface,
 * ordered ahead of the existing session-based chain (see SecurityConfig)
 * so it claims /api/** requests first. The two chains coexist for as long
 * as any Thymeleaf pages haven't been migrated to the React frontend.
 *
 * CSRF is disabled for this chain: every /api/** endpoint other than
 * /api/auth/refresh requires an explicit Authorization header, which a
 * browser never attaches automatically, so CSRF doesn't apply. The one
 * endpoint driven by an ambient cookie (the refresh token) defends against
 * CSRF via that cookie's SameSite=Strict attribute instead of a token
 * exchange, which would be redundant given SameSite=Strict already blocks
 * the cookie from being sent on a cross-site request at all.
 */
@Configuration
public class ApiSecurityConfig {

    @Value("${app.frontend.dev-origin:http://localhost:5173}")
    private String frontendDevOrigin;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
        return new JwtAuthenticationFilter(jwtService);
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // Without this, Spring Security's default behavior for a
                // rejected /api/** request falls back to the session chain's
                // login-redirect handling (since no entry point is set on
                // this chain) instead of returning a plain JSON error, which
                // is never correct for a REST API.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Not authenticated"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, "Access denied"))
                );

        return http.build();
    }

    private void writeJsonError(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of("error", message));
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // In production the SPA is served from the same origin as the API
        // (see the Gradle/Vite build integration), so this is only needed
        // for the Vite dev server during local development.
        configuration.setAllowedOrigins(List.of(frontendDevOrigin));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
