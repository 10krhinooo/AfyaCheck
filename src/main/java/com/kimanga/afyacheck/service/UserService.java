package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.model.AdminAuditLog;
import com.kimanga.afyacheck.model.User;
import com.kimanga.afyacheck.model.UserRole;
import com.kimanga.afyacheck.repository.AdminAuditLogRepository;
import com.kimanga.afyacheck.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.kimanga.afyacheck.util.SecurityUtils;
import com.kimanga.afyacheck.DTO.ServiceResult;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final AdminAuditLogRepository adminAuditLogRepository;

    /** Handle logout */
    public ServiceResult<String> logout(HttpSession session, HttpServletResponse response) {
        try {
            // Invalidate session
            if (session != null) {
                session.invalidate();
            }

            // Clear authentication
            SecurityContextHolder.clearContext();

            // Prevent caching - Add these headers to prevent back button access
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
            response.setHeader("X-Content-Type-Options", "nosniff");

            return ServiceResult.success("✅ You have been logged out successfully.", null);
        } catch (Exception e) {
            return ServiceResult.failure("❌ Logout failed. Please try again.");
        }
    }

    /** Find user by email */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Changes a user's realm role in Keycloak — the actual source of truth for authorization
     * (see KeycloakAdminService). Only works for users synced from a real Keycloak login
     * (keycloakId set by KeycloakUserSyncFilter); the legacy pre-Keycloak local-account rows
     * have no Keycloak identity to update.
     */
    public ServiceResult<Void> changeUserRole(Long userId, UserRole role, String actingAdminKeycloakId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ServiceResult.failure("User not found");
        }

        User user = userOptional.get();
        if (user.getKeycloakId() == null) {
            return ServiceResult.failure("This user has no Keycloak identity to update");
        }
        if (user.getKeycloakId().equals(actingAdminKeycloakId)) {
            return ServiceResult.failure("You cannot change your own role");
        }

        UserRole previousRole = user.getRole();
        try {
            keycloakAdminService.setUserRole(user.getKeycloakId(), role);
        } catch (Exception e) {
            logger.error("Failed to change Keycloak role for user {}: {}", userId, e.getMessage());
            return ServiceResult.failure("Could not update the user's role in Keycloak");
        }

        // Best-effort local reflection — KeycloakUserSyncFilter re-derives this from the
        // user's JWT on their next request regardless, since Keycloak is authoritative.
        user.setRole(role);
        userRepository.save(user);

        AdminAuditLog log = new AdminAuditLog();
        log.setActorEmail(SecurityUtils.currentActorEmail());
        log.setAction("CHANGE_USER_ROLE");
        log.setTargetType("USER");
        log.setTargetId(String.valueOf(userId));
        log.setDetails(previousRole + " -> " + role);
        adminAuditLogRepository.save(log);

        return ServiceResult.success("User role updated to " + role, null);
    }

    /** Check if user is admin */
    public boolean isAdmin(User user) {
        return user.getRole() == UserRole.ADMIN;
    }
}