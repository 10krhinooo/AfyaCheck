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
     * Promotes an existing registered user to ADMIN by email — there's no user-listing page to
     * pick an id from anymore (see AdminUsersPage removal), so email is the only identifier an
     * admin actually has on hand. Only works for users synced from a real Keycloak login
     * (keycloakId set by KeycloakUserSyncFilter on their first authenticated request); Keycloak
     * is the actual source of truth for roles (see KeycloakAdminService), so the local `users`
     * row is a best-effort mirror.
     */
    public ServiceResult<Void> promoteToAdmin(String email, String actingAdminKeycloakId) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            return ServiceResult.failure("No user found with that email");
        }

        User user = userOptional.get();
        if (user.getKeycloakId() == null) {
            return ServiceResult.failure("This user hasn't signed in yet — ask them to log in once first");
        }
        if (user.getKeycloakId().equals(actingAdminKeycloakId)) {
            return ServiceResult.failure("You're already an admin");
        }
        if (user.getRole() == UserRole.ADMIN) {
            return ServiceResult.failure(user.getEmail() + " is already an admin");
        }

        try {
            keycloakAdminService.setUserRole(user.getKeycloakId(), UserRole.ADMIN);
        } catch (Exception e) {
            logger.error("Failed to promote user {} to admin in Keycloak: {}", email, e.getMessage());
            return ServiceResult.failure("Could not update the user's role in Keycloak");
        }

        // Best-effort local reflection — KeycloakUserSyncFilter re-derives this from the
        // user's JWT on their next request regardless, since Keycloak is authoritative.
        user.setRole(UserRole.ADMIN);
        userRepository.save(user);

        AdminAuditLog log = new AdminAuditLog();
        log.setActorEmail(SecurityUtils.currentActorEmail());
        log.setAction("PROMOTE_TO_ADMIN");
        log.setTargetType("USER");
        log.setTargetId(email);
        log.setDetails("USER -> ADMIN");
        adminAuditLogRepository.save(log);

        return ServiceResult.success(user.getEmail() + " is now an admin", null);
    }

    /** Check if user is admin */
    public boolean isAdmin(User user) {
        return user.getRole() == UserRole.ADMIN;
    }
}