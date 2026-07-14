package com.kimanga.afyacheck.config;

import com.kimanga.afyacheck.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Reads the Authorization: Bearer header on /api/** requests, validates the
 * access token, and populates the SecurityContext. Requests with no token,
 * or an invalid/expired one, simply proceed unauthenticated; downstream
 * authorization rules (see ApiSecurityConfig) reject them if the endpoint
 * requires authentication.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length());
            Optional<Claims> claims = jwtService.parseAccessToken(token);

            if (claims.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticate(claims.get());
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(Claims claims) {
        String email = jwtService.getEmail(claims);
        String role = jwtService.getRole(claims);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(email, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
