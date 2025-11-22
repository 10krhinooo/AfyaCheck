package com.kimanga.afyacheck.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

        String redirectUrl = isAdmin ? "/admin/dashboard" : "/dashboard";

        System.out.println("=== AUTH SUCCESS HANDLER ===");
        System.out.println("User: " + authentication.getName());
        System.out.println("Is Admin: " + isAdmin);
        System.out.println("Redirecting to: " + redirectUrl);

        response.sendRedirect(redirectUrl);
    }
}