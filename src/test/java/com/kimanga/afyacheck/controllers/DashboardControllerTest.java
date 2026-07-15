package com.kimanga.afyacheck.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardControllerTest {

    private final DashboardController controller = new DashboardController();

    @Test
    void returnsAdminFlagForAdminUser() {
        Authentication auth = new UsernamePasswordAuthenticationToken("admin", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        ResponseEntity<?> response = controller.dashboard(auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new DashboardController.DashboardResponse("admin", true));
    }

    @Test
    void returnsNonAdminForRegularUser() {
        Authentication auth = new UsernamePasswordAuthenticationToken("user", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        ResponseEntity<?> response = controller.dashboard(auth);

        assertThat(response.getBody()).isEqualTo(new DashboardController.DashboardResponse("user", false));
    }

    @Test
    void returns401WhenUnauthenticated() {
        ResponseEntity<?> response = controller.dashboard(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
