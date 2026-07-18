package com.kimanga.afyacheck.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Reads identity off the current request's JWT — used by audit logging call sites. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static String currentActorEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            String email = jwtAuth.getToken().getClaimAsString("email");
            if (email != null) {
                return email;
            }
        }
        return "unknown";
    }
}
