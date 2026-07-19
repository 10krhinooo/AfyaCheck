package com.kimanga.afyacheck.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * JSON API backing the React authenticated dashboard (see /app/dashboard). Replaces
 * AuthController.showDashboard's MVC redirect-to-admin-or-render-dashboard logic — the
 * client now does that branching itself based on this response (Phase 4 of the migration).
 */
@RestController
public class DashboardController {

    public record DashboardResponse(String username, boolean isAdmin) {}

    @GetMapping("/api/dashboard")
    public ResponseEntity<?> dashboard(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        String displayName = authentication.getName();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String givenName = jwt.getClaimAsString("given_name");
            String preferredUsername = jwt.getClaimAsString("preferred_username");
            String name = jwt.getClaimAsString("name");
            displayName = givenName != null ? givenName : preferredUsername != null ? preferredUsername : name != null ? name : displayName;
        }

        return ResponseEntity.ok(new DashboardResponse(displayName, isAdmin));
    }
}
