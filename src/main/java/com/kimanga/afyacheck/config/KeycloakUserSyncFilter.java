package com.kimanga.afyacheck.config;

import com.kimanga.afyacheck.model.AuthProvider;
import com.kimanga.afyacheck.model.User;
import com.kimanga.afyacheck.model.UserRole;
import com.kimanga.afyacheck.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Keeps a thin local "users" row per Keycloak identity, synced on every authenticated
 * request (simpler than standing up a Keycloak event listener SPI or webhook receiver for
 * this app's scale). This is what AdminController's user list/CRUD reads from — it's a read
 * cache of Keycloak identity, not the source of truth for it, and only ROLE_ADMIN status
 * matters for authorization (already resolved from the JWT itself by
 * SecurityConfig#keycloakJwtAuthenticationConverter, independent of this row).
 *
 * Registered explicitly in SecurityConfig via addFilterAfter(...) so it runs inside Spring
 * Security's chain, after the JWT auth filter has populated the SecurityContext — NOT as a
 * generic Spring Boot auto-registered servlet filter (see the disabling FilterRegistrationBean
 * in SecurityConfig, which prevents it running a second time outside the security chain).
 */
@Component
public class KeycloakUserSyncFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public KeycloakUserSyncFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            syncUser(jwtAuth);
        }
        filterChain.doFilter(request, response);
    }

    private void syncUser(JwtAuthenticationToken jwtAuth) {
        Jwt jwt = jwtAuth.getToken();
        String keycloakId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        boolean isAdmin = jwtAuth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        UserRole role = isAdmin ? UserRole.ADMIN : UserRole.USER;

        // Falls back to matching by email (and re-linking the keycloakId) before creating a new
        // row: Keycloak's dev storage is ephemeral (see README, "nothing is persisted across
        // docker compose down/--force-recreate"), so a realm re-import mints a fresh subject
        // UUID for the same email. Without this fallback, findByKeycloakId misses and the save
        // below tries to INSERT a second row with that email, violating the unique constraint.
        User user = userRepository.findByKeycloakId(keycloakId)
                .or(() -> userRepository.findByEmail(email).map(existing -> {
                    existing.setKeycloakId(keycloakId);
                    return existing;
                }))
                .orElseGet(() -> {
                    User created = new User();
                    created.setKeycloakId(keycloakId);
                    created.setProvider(AuthProvider.KEYCLOAK);
                    created.setEnabled(true);
                    created.setEmailVerified(true);
                    return created;
                });

        user.setEmail(email);
        user.setName(name != null ? name : email);
        user.setRole(role);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
}
