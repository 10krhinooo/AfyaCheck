package com.kimanga.afyacheck.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

        return ResponseEntity.ok(new DashboardResponse(authentication.getName(), isAdmin));
    }
}
