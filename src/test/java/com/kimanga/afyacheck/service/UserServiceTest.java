package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.DTO.ServiceResult;
import com.kimanga.afyacheck.model.AdminAuditLog;
import com.kimanga.afyacheck.model.User;
import com.kimanga.afyacheck.model.UserRole;
import com.kimanga.afyacheck.repository.AdminAuditLogRepository;
import com.kimanga.afyacheck.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private KeycloakAdminService keycloakAdminService;
    private AdminAuditLogRepository adminAuditLogRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        keycloakAdminService = mock(KeycloakAdminService.class);
        adminAuditLogRepository = mock(AdminAuditLogRepository.class);
        userService = new UserService(userRepository, keycloakAdminService, adminAuditLogRepository);
    }

    @Test
    void logoutInvalidatesSessionAndClearsContext() {
        HttpSession session = mock(HttpSession.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        ServiceResult<String> result = userService.logout(session, response);

        assertThat(result.isSuccess()).isTrue();
        verify(session).invalidate();
        verify(response).setHeader(eq("Cache-Control"), anyString());
    }

    @Test
    void logoutHandlesNullSession() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        ServiceResult<String> result = userService.logout(null, response);
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void logoutReturnsFailureWhenExceptionThrown() {
        HttpSession session = mock(HttpSession.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        doThrow(new RuntimeException("boom")).when(session).invalidate();

        ServiceResult<String> result = userService.logout(session, response);

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void findByEmailDelegatesToRepository() {
        User user = new User();
        when(userRepository.findByEmail("x@example.com")).thenReturn(Optional.of(user));
        assertThat(userService.findByEmail("x@example.com")).contains(user);
    }

    @Test
    void changeUserRoleFailsWhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        ServiceResult<Void> result = userService.changeUserRole(1L, UserRole.ADMIN, "admin-kc-id");
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void changeUserRoleFailsWhenUserHasNoKeycloakIdentity() {
        User user = new User();
        user.setRole(UserRole.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ServiceResult<Void> result = userService.changeUserRole(1L, UserRole.ADMIN, "admin-kc-id");

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void changeUserRoleFailsWhenChangingOwnRole() {
        User user = new User();
        user.setKeycloakId("admin-kc-id");
        user.setRole(UserRole.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ServiceResult<Void> result = userService.changeUserRole(1L, UserRole.USER, "admin-kc-id");

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void changeUserRolePromotesUser() {
        User user = new User();
        user.setKeycloakId("user-kc-id");
        user.setRole(UserRole.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ServiceResult<Void> result = userService.changeUserRole(1L, UserRole.ADMIN, "admin-kc-id");

        assertThat(result.isSuccess()).isTrue();
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        verify(keycloakAdminService).setUserRole("user-kc-id", UserRole.ADMIN);
        verify(userRepository).save(user);
        verify(adminAuditLogRepository).save(any(AdminAuditLog.class));
    }

    @Test
    void changeUserRoleFailsWhenKeycloakCallThrows() {
        User user = new User();
        user.setKeycloakId("user-kc-id");
        user.setRole(UserRole.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doThrow(new RuntimeException("boom"))
                .when(keycloakAdminService)
                .setUserRole("user-kc-id", UserRole.ADMIN);

        ServiceResult<Void> result = userService.changeUserRole(1L, UserRole.ADMIN, "admin-kc-id");

        assertThat(result.isSuccess()).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    void isAdminReflectsUserRole() {
        User admin = new User();
        admin.setRole(UserRole.ADMIN);
        User regular = new User();
        regular.setRole(UserRole.USER);

        assertThat(userService.isAdmin(admin)).isTrue();
        assertThat(userService.isAdmin(regular)).isFalse();
    }
}
