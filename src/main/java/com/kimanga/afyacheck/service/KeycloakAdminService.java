package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.model.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Calls Keycloak's own Admin REST API to assign/remove realm roles on a real Keycloak user.
 * This exists because Keycloak — not the local `users` table — is the source of truth for
 * roles: the JWT resource-server config in SecurityConfig only validates tokens it receives,
 * it can't grant or revoke a role. Authenticates as the confidential "afyacheck-backend"
 * client's service account (realm-management manage-users/view-users/query-users roles, see
 * keycloak/realm-export.json), not as any real user.
 */
@Service
public class KeycloakAdminService {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakAdminService.class);
    private static final List<String> MANAGED_ROLES = List.of("USER", "ADMIN");

    private final RestTemplate restTemplate;

    @Value("${keycloak.admin.server-url}")
    private String serverUrl;

    @Value("${keycloak.admin.realm}")
    private String realm;

    @Value("${keycloak.admin.client-id}")
    private String clientId;

    @Value("${keycloak.admin.client-secret}")
    private String clientSecret;

    public KeycloakAdminService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Sets a Keycloak user's realm role to exactly the given role — removing whichever of
     * USER/ADMIN they currently hold (if different) and assigning the new one. Leaves the
     * "default-roles-{realm}" composite (granted at registration) untouched.
     */
    public void setUserRole(String keycloakUserId, UserRole role) {
        String token = fetchAdminToken();

        List<Map<String, Object>> currentRoles = getUserRealmRoles(token, keycloakUserId);
        List<Map<String, Object>> toRemove = currentRoles.stream()
                .filter(r -> MANAGED_ROLES.contains((String) r.get("name")) && !role.name().equals(r.get("name")))
                .toList();
        if (!toRemove.isEmpty()) {
            deleteRealmRoles(token, keycloakUserId, toRemove);
        }

        boolean alreadyHasTarget = currentRoles.stream().anyMatch(r -> role.name().equals(r.get("name")));
        if (!alreadyHasTarget) {
            Map<String, Object> targetRole = getRealmRole(token, role.name());
            addRealmRoles(token, keycloakUserId, List.of(targetRole));
        }

        logger.info("Set Keycloak user {} realm role to {}", keycloakUserId, role);
    }

    private String fetchAdminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String url = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";
        Map<?, ?> response = restTemplate
                .postForEntity(url, new HttpEntity<>(form, headers), Map.class)
                .getBody();
        return (String) response.get("access_token");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getUserRealmRoles(String token, String userId) {
        String url = serverUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm";
        Object[] body = restTemplate
                .exchange(url, HttpMethod.GET, new HttpEntity<>(authHeaders(token)), Object[].class)
                .getBody();
        return body == null ? List.of() : List.of(body).stream().map(o -> (Map<String, Object>) o).toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getRealmRole(String token, String roleName) {
        String url = serverUrl + "/admin/realms/" + realm + "/roles/" + roleName;
        return restTemplate
                .exchange(url, HttpMethod.GET, new HttpEntity<>(authHeaders(token)), Map.class)
                .getBody();
    }

    private void addRealmRoles(String token, String userId, List<Map<String, Object>> roles) {
        String url = serverUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm";
        restTemplate.postForEntity(url, new HttpEntity<>(roles, authHeaders(token)), Void.class);
    }

    private void deleteRealmRoles(String token, String userId, List<Map<String, Object>> roles) {
        String url = serverUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm";
        restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(roles, authHeaders(token)), Void.class);
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
