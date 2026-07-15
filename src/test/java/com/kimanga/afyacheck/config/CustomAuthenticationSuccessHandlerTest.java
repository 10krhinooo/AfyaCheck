package com.kimanga.afyacheck.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CustomAuthenticationSuccessHandlerTest {

    private final CustomAuthenticationSuccessHandler handler = new CustomAuthenticationSuccessHandler();

    @Test
    void redirectsAdminToAdminDashboard() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin@example.com", "pw", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        handler.onAuthenticationSuccess(request, response, auth);

        verify(response).sendRedirect(eq("/admin/dashboard"));
    }

    @Test
    void redirectsRegularUserToDashboard() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user@example.com", "pw", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        handler.onAuthenticationSuccess(request, response, auth);

        verify(response).sendRedirect(eq("/dashboard"));
    }
}
