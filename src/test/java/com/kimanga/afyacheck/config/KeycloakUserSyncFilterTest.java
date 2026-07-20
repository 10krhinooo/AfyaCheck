package com.kimanga.afyacheck.config;

import com.kimanga.afyacheck.model.AuthProvider;
import com.kimanga.afyacheck.model.User;
import com.kimanga.afyacheck.model.UserRole;
import com.kimanga.afyacheck.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KeycloakUserSyncFilterTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final KeycloakUserSyncFilter filter = new KeycloakUserSyncFilter(userRepository);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private Jwt jwt(String sub, String email, String name) {
        return new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of("sub", sub, "email", email, "name", name));
    }

    @Test
    void createsNewUserRowOnFirstSeen() throws Exception {
        Jwt jwt = jwt("kc-123", "amina@example.com", "Amina Mwangi");
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(userRepository.findByKeycloakId("kc-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("amina@example.com")).thenReturn(Optional.empty());

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getKeycloakId()).isEqualTo("kc-123");
        assertThat(saved.getEmail()).isEqualTo("amina@example.com");
        assertThat(saved.getName()).isEqualTo("Amina Mwangi");
        assertThat(saved.getProvider()).isEqualTo(AuthProvider.KEYCLOAK);
        assertThat(saved.getRole()).isEqualTo(UserRole.USER);
        verify(chain).doFilter(request, response);
    }

    @Test
    void relinksExistingRowByEmailWhenKeycloakIdChanged() throws Exception {
        // Simulates a Keycloak dev-realm re-import minting a new subject UUID for an email
        // that already has a local row under the old UUID (see the fix's comment in
        // KeycloakUserSyncFilter) — must re-link, not attempt a second INSERT that would
        // violate the email unique constraint.
        Jwt jwt = jwt("kc-new-789", "amina@example.com", "Amina Mwangi");
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        User existing = new User();
        existing.setKeycloakId("kc-old-123");
        existing.setEmail("amina@example.com");
        existing.setRole(UserRole.USER);
        when(userRepository.findByKeycloakId("kc-new-789")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("amina@example.com")).thenReturn(Optional.of(existing));

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(userRepository).save(existing);
        assertThat(existing.getKeycloakId()).isEqualTo("kc-new-789");
        verify(userRepository, never()).save(argThat(u -> u != existing));
    }

    @Test
    void updatesExistingUserRowAndPromotesToAdmin() throws Exception {
        Jwt jwt = jwt("kc-456", "admin@example.com", "Admin User");
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        User existing = new User();
        existing.setKeycloakId("kc-456");
        existing.setRole(UserRole.USER);
        when(userRepository.findByKeycloakId("kc-456")).thenReturn(Optional.of(existing));

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(userRepository).save(existing);
        assertThat(existing.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void skipsSyncWhenNotJwtAuthenticated() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(userRepository, never()).save(any());
        verify(chain).doFilter(request, response);
    }
}
