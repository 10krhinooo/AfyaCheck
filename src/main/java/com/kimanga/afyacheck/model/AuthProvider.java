package com.kimanga.afyacheck.model;

public enum AuthProvider {
    LOCAL,
    GOOGLE,
    GITHUB,
    APPLE,
    // Users synced from a Keycloak JWT (see KeycloakUserSyncFilter). Google/GitHub sign-in
    // now happens via Keycloak Identity Brokering rather than this app's own OAuth2 client,
    // so new social-login users land here too, not under GOOGLE/GITHUB.
    KEYCLOAK
}