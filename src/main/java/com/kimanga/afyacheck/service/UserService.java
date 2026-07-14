package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.mail.EmailService;
import com.kimanga.afyacheck.model.AuthProvider;
import com.kimanga.afyacheck.model.CustomOAuth2User;
import com.kimanga.afyacheck.model.User;
import com.kimanga.afyacheck.model.UserRole;
import com.kimanga.afyacheck.model.VerificationToken;
import com.kimanga.afyacheck.model.PasswordResetToken;
import com.kimanga.afyacheck.repository.UserRepository;
import com.kimanga.afyacheck.repository.VerificationTokenRepository;
import com.kimanga.afyacheck.repository.PasswordResetTokenRepository;
import com.kimanga.afyacheck.util.AlertMessage;
import com.kimanga.afyacheck.DTO.ServiceResult;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
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

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.base-url}")
    private String baseUrl;

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

        if (user.getPassword() == null || user.getPassword().length() < 8) {
            return ServiceResult.failure("Password must be at least 8 characters long");
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
        String verificationUrl = baseUrl + "/verify?token=" + token;
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
            // Return the same success response regardless of whether the email
            // exists, to avoid leaking account existence to an attacker.
            return ServiceResult.success(AlertMessage.PASSWORD_RESET_LINK_SENT, null);
        }

        User user = optionalUser.get();
        passwordResetTokenRepository.deleteByUser(user);

        // Generate reset token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(1))
                .build();
        passwordResetTokenRepository.save(resetToken);

        // Prepare reset email
        String resetUrl = baseUrl + "/reset-password?token=" + token;
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
     * Resolve the currently authenticated User entity.
     *
     * authentication.getName() means different things depending on how the
     * user logged in: for form login it's the email (CustomUserDetailsService
     * builds the principal with the email as username), but for OAuth2 login
     * CustomOAuth2User.getName() returns the username, not the email. Looking
     * an OAuth2 principal's getName() up via findByEmail would silently fail,
     * so OAuth2 principals are resolved directly instead.
     */
    public Optional<User> resolveCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof CustomOAuth2User oauth2User) {
            return Optional.of(oauth2User.getUser());
        }
        return findByEmail(authentication.getName());
    }

    /** Make a user admin by email */
    public ServiceResult<Void> makeAdmin(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            return ServiceResult.failure("User not found");
        }

        User user = userOptional.get();
        user.setRole(UserRole.ADMIN);
        userRepository.save(user);

        return ServiceResult.success("User promoted to admin successfully", null);
    }

    /** Check if user is admin */
    public boolean isAdmin(User user) {
        return user.getRole() == UserRole.ADMIN;
    }
}