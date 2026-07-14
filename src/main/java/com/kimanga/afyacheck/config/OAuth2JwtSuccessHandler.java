package com.kimanga.afyacheck.config;

import com.kimanga.afyacheck.model.CustomOAuth2User;
import com.kimanga.afyacheck.service.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OAuth2 login is an inherently redirect-driven browser flow (through
 * Google/GitHub and back), so it can't be replaced with a JSON fetch call
 * the way form login is on the React side. Instead, on success this issues
 * a refresh token cookie exactly like /api/auth/login does, then redirects
 * to a frontend route that immediately calls /api/auth/refresh to mint an
 * access token, so the SPA ends up in the same authenticated state either
 * way.
 */
@Component
public class OAuth2JwtSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;

    @Value("${app.base-url}")
    private String baseUrl;

    public OAuth2JwtSuccessHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        if (authentication.getPrincipal() instanceof CustomOAuth2User oauth2User) {
            String refreshToken = jwtService.generateRefreshToken(oauth2User.getUser());
            response.addCookie(buildRefreshTokenCookie(refreshToken));
        }

        response.sendRedirect(baseUrl + "/auth/callback");
    }

    private Cookie buildRefreshTokenCookie(String refreshToken) {
        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setAttribute("SameSite", "Strict");
        cookie.setMaxAge((int) jwtService.getRefreshTokenTtlSeconds());
        return cookie;
    }
}
