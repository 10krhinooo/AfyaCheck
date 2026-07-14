package com.kimanga.afyacheck.controllers.api;

import com.kimanga.afyacheck.DTO.ServiceResult;
import com.kimanga.afyacheck.DTO.auth.AuthResponse;
import com.kimanga.afyacheck.DTO.auth.CurrentUserDTO;
import com.kimanga.afyacheck.DTO.auth.ForgotPasswordRequest;
import com.kimanga.afyacheck.DTO.auth.LoginRequest;
import com.kimanga.afyacheck.DTO.auth.RegisterRequest;
import com.kimanga.afyacheck.DTO.auth.ResetPasswordRequest;
import com.kimanga.afyacheck.model.AuthProvider;
import com.kimanga.afyacheck.model.User;
import com.kimanga.afyacheck.service.JwtService;
import com.kimanga.afyacheck.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private static final Logger logger = LoggerFactory.getLogger(AuthApiController.class);
    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;

    public AuthApiController(AuthenticationManager authenticationManager, UserService userService, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException | org.springframework.security.authentication.DisabledException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid email or password"));
        }

        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + request.getEmail()));

        return ResponseEntity.ok(issueTokens(user, response));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setProvider(AuthProvider.LOCAL);

        ServiceResult<User> result = userService.register(user);
        if (!result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", result.getMessage()));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", result.getMessage()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        ServiceResult<Void> result = userService.initiatePasswordReset(request.getEmail());
        // Always the same response regardless of outcome, to avoid leaking
        // account existence (see UserService.initiatePasswordReset).
        return ResponseEntity.ok(Map.of("message", result.getMessage()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        ServiceResult<Void> result = userService.resetPassword(request.getToken(), request.getNewPassword());
        if (!result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", result.getMessage()));
        }
        return ResponseEntity.ok(Map.of("message", result.getMessage()));
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam String token) {
        ServiceResult<Void> result = userService.verifyUser(token);
        if (!result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", result.getMessage()));
        }
        return ResponseEntity.ok(Map.of("message", result.getMessage()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        Optional<String> refreshToken = readRefreshTokenCookie(request);
        if (refreshToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No refresh token"));
        }

        Optional<Claims> claims = jwtService.parseRefreshToken(refreshToken.get());
        if (claims.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid or expired refresh token"));
        }

        Optional<User> user = userService.findByEmail(jwtService.getEmail(claims.get()));
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User no longer exists"));
        }

        return ResponseEntity.ok(issueTokens(user.get(), response));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        Cookie expired = new Cookie(REFRESH_COOKIE_NAME, "");
        expired.setHttpOnly(true);
        expired.setSecure(true);
        expired.setPath("/api/auth");
        expired.setMaxAge(0);
        response.addCookie(expired);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        Optional<User> user = userService.findByEmail(authentication.getName());
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User no longer exists"));
        }
        return ResponseEntity.ok(toCurrentUserDTO(user.get()));
    }

    private AuthResponse issueTokens(User user, HttpServletResponse response) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        response.addCookie(buildRefreshTokenCookie(refreshToken));

        return new AuthResponse(accessToken, "Bearer", jwtService.getAccessTokenTtlSeconds(), toCurrentUserDTO(user));
    }

    private Cookie buildRefreshTokenCookie(String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setAttribute("SameSite", "Strict");
        cookie.setMaxAge((int) jwtService.getRefreshTokenTtlSeconds());
        return cookie;
    }

    private Optional<String> readRefreshTokenCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        for (Cookie cookie : request.getCookies()) {
            if (REFRESH_COOKIE_NAME.equals(cookie.getName())) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    private CurrentUserDTO toCurrentUserDTO(User user) {
        return new CurrentUserDTO(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getName(),
                List.of("ROLE_" + user.getRole().name())
        );
    }
}
