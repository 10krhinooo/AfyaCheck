package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.mail.EmailService;
import com.kimanga.afyacheck.model.AdminAuditLog;
import com.kimanga.afyacheck.model.AuthProvider;
import com.kimanga.afyacheck.model.User;
import com.kimanga.afyacheck.model.UserRole;
import com.kimanga.afyacheck.model.VerificationToken;
import com.kimanga.afyacheck.model.PasswordResetToken;
import com.kimanga.afyacheck.repository.AdminAuditLogRepository;
import com.kimanga.afyacheck.repository.UserRepository;
import com.kimanga.afyacheck.repository.VerificationTokenRepository;
import com.kimanga.afyacheck.repository.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.kimanga.afyacheck.util.AlertMessage;
import com.kimanga.afyacheck.util.SecurityUtils;
import com.kimanga.afyacheck.DTO.ServiceResult;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final KeycloakAdminService keycloakAdminService;
    private final AdminAuditLogRepository adminAuditLogRepository;

    /** Register user and send verification email */
    public ServiceResult<User> register(User user) {
        // Validate required fields
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return ServiceResult.failure("Username is required");
        }

        if (user.getName() == null || user.getName().trim().isEmpty()) {
            return ServiceResult.failure("Full name is required");
        }

        // Check if username already exists
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ServiceResult.failure("Username already exists");
        }

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ServiceResult.failure(AlertMessage.EMAIL_ALREADY_EXISTS);
        }

        // Set user properties
        user.setProvider(AuthProvider.LOCAL);
        user.setEmailVerified(false);
        user.setEnabled(false);
        user.setRole(UserRole.USER); // Set default role
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User savedUser = userRepository.save(user);

        // Generate verification token
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .user(savedUser)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .build();
        verificationTokenRepository.save(verificationToken);

        // Prepare verification email
        String verificationUrl = "http://localhost:8080/verify?token=" + token;
        Context context = new Context();
        context.setVariable("userName", savedUser.getUsername());
        context.setVariable("userEmail", savedUser.getEmail());
        context.setVariable("verificationUrl", verificationUrl);

        emailService.sendHtmlMail(
                savedUser.getEmail(),
                "Verify Your AfyaCheck Account",
                "email/verify-email.html",
                context
        );

        return ServiceResult.success(AlertMessage.REGISTRATION_SUCCESS, savedUser);
    }

    /** Verify user account */
    public ServiceResult<Void> verifyUser(String token) {
        Optional<VerificationToken> optionalToken = verificationTokenRepository.findByToken(token);

        if (optionalToken.isEmpty()) {
            return ServiceResult.failure(AlertMessage.VERIFICATION_FAILED);
        }

        VerificationToken verificationToken = optionalToken.get();
        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return ServiceResult.failure(AlertMessage.VERIFICATION_FAILED);
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        verificationTokenRepository.delete(verificationToken);
        return ServiceResult.success(AlertMessage.VERIFICATION_SUCCESS, null);
    }

    /** Initiate password reset */
    public ServiceResult<Void> initiatePasswordReset(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            return ServiceResult.failure(AlertMessage.USER_NOT_FOUND);
        }

        User user = optionalUser.get();

        // Generate reset token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(1))
                .build();
        passwordResetTokenRepository.save(resetToken);

        // Prepare reset email
        String resetUrl = "http://localhost:8080/reset-password?token=" + token;
        Context context = new Context();
        context.setVariable("userName", user.getUsername());
        context.setVariable("userEmail", user.getEmail());
        context.setVariable("resetUrl", resetUrl);

        emailService.sendHtmlMail(
                user.getEmail(),
                "AfyaCheck Password Reset Request",
                "email/reset-password.html",
                context
        );

        return ServiceResult.success(AlertMessage.PASSWORD_RESET_LINK_SENT, null);
    }

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

    /** Reset user password using token */
    public ServiceResult<Void> resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> optionalToken = passwordResetTokenRepository.findByToken(token);

        if (optionalToken.isEmpty()) {
            return ServiceResult.failure(AlertMessage.PASSWORD_RESET_FAILED);
        }

        PasswordResetToken resetToken = optionalToken.get();
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return ServiceResult.failure(AlertMessage.PASSWORD_RESET_FAILED);
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);
        return ServiceResult.success(AlertMessage.PASSWORD_RESET_SUCCESS, null);
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