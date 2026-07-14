package com.kimanga.afyacheck.controllers;

import com.kimanga.afyacheck.model.User;
import com.kimanga.afyacheck.service.UserService;
import com.kimanga.afyacheck.service.SessionService;
import com.kimanga.afyacheck.util.AlertMessage;
import com.kimanga.afyacheck.DTO.ServiceResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpSession;

import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService;
    private final SessionService sessionService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "success", required = false) String success,
                            @RequestParam(value = "resetLinkSent", required = false) String resetLinkSent,
                            @RequestParam(value = "resetSuccess", required = false) String resetSuccess,
                            @RequestParam(value = "verified", required = false) String verified,
                            @RequestParam(value = "invalidToken", required = false) String invalidToken,
                            @RequestParam(value = "oauth", required = false) String oauth,
                            HttpSession httpSession,
                            Model model) {

        // Create session even on login page to maintain consistency
        try {
            String sessionId = httpSession.getId();
            String createdSessionId = sessionService.createOrGetSession(sessionId);
            logger.info("Created session on login page: {}", createdSessionId);
        } catch (Exception e) {
            logger.error("Error creating session on login page", e);
        }

        if (error != null) model.addAttribute("error", "❌ Invalid email or password.");
        if (logout != null) model.addAttribute("message", "✅ You have been logged out successfully.");
        if (success != null) model.addAttribute("message", AlertMessage.REGISTRATION_SUCCESS);
        if (resetLinkSent != null) model.addAttribute("message", AlertMessage.PASSWORD_RESET_LINK_SENT);
        if (resetSuccess != null) model.addAttribute("message", AlertMessage.PASSWORD_RESET_SUCCESS);
        if (verified != null) model.addAttribute("message", AlertMessage.VERIFICATION_SUCCESS);
        if (invalidToken != null) model.addAttribute("error", AlertMessage.VERIFICATION_FAILED);
        if (oauth != null) model.addAttribute("oauth", AlertMessage.OAUTH_SUCCESS);

        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model, HttpSession httpSession) {
        // Create session on register page
        try {
            String sessionId = httpSession.getId();
            String createdSessionId = sessionService.createOrGetSession(sessionId);
            logger.info("Created session on register page: {}", createdSessionId);
        } catch (Exception e) {
            logger.error("Error creating session on register page", e);
        }

        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user,
                               BindingResult result,
                               Model model,
                               HttpSession httpSession) {

        if (result.hasErrors()) {
            String errorMessage = result.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining(" "));
            model.addAttribute("errorMessage", errorMessage);
            return "register";
        }

        try {
            // Delegate all logic to the service
            ServiceResult<User> serviceResult = userService.register(user);

            if (serviceResult.isSuccess()) {
                model.addAttribute("successMessage", serviceResult.getMessage());
                model.addAttribute("user", new User()); // Clear form

                // Create session after successful registration
                String sessionId = httpSession.getId();
                String createdSessionId = sessionService.createOrGetSession(sessionId);
                logger.info("Created session after registration: {}", createdSessionId);
            } else {
                model.addAttribute("errorMessage", serviceResult.getMessage());
            }

            return "register";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Registration failed: " + e.getMessage());
            return "register";
        }
    }


    @GetMapping("/dashboard")
    public String showDashboard(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            model.addAttribute("username", username);

            // Check if user has ADMIN role
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

            if (isAdmin) {
                return "redirect:/admin/dashboard";
            }

            return "dashboard";
        }
        return "redirect:/login";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage(HttpSession httpSession) {
        // Create session on forgot password page
        try {
            String sessionId = httpSession.getId();
            String createdSessionId = sessionService.createOrGetSession(sessionId);
            logger.info("Created session on forgot password page: {}", createdSessionId);
        } catch (Exception e) {
            logger.error("Error creating session on forgot password page", e);
        }

        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, Model model, HttpSession httpSession) {
        ServiceResult<Void> result = userService.initiatePasswordReset(email);

        if (!result.isSuccess()) {
            model.addAttribute("errorMessage", result.getMessage());
            return "forgot-password";
        }

        return "redirect:/login?resetLinkSent";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token, Model model, HttpSession httpSession) {
        // Create session on reset password page
        try {
            String sessionId = httpSession.getId();
            String createdSessionId = sessionService.createOrGetSession(sessionId);
            logger.info("Created session on reset password page: {}", createdSessionId);
        } catch (Exception e) {
            logger.error("Error creating session on reset password page", e);
        }

        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token, @RequestParam String password, Model model, HttpSession httpSession) {
        ServiceResult<Void> result = userService.resetPassword(token, password);

        if (!result.isSuccess()) {
            model.addAttribute("errorMessage", result.getMessage());
            model.addAttribute("token", token);
            return "reset-password";
        }

        return "redirect:/login?resetSuccess";
    }

    @GetMapping("/verify")
    public String verifyUser(@RequestParam String token, HttpSession httpSession) {
        // Create session on verification page
        try {
            String sessionId = httpSession.getId();
            String createdSessionId = sessionService.createOrGetSession(sessionId);
            logger.info("Created session on verification page: {}", createdSessionId);
        } catch (Exception e) {
            logger.error("Error creating session on verification page", e);
        }

        ServiceResult<Void> result = userService.verifyUser(token);

        if (result.isSuccess()) {
            return "redirect:/login?verified";
        } else {
            return "redirect:/login?invalidToken";
        }
    }
}