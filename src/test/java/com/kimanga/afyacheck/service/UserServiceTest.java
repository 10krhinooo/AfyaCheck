package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.DTO.ServiceResult;
import com.kimanga.afyacheck.mail.EmailService;
import com.kimanga.afyacheck.model.AdminAuditLog;
import com.kimanga.afyacheck.model.PasswordResetToken;
import com.kimanga.afyacheck.model.User;
import com.kimanga.afyacheck.model.UserRole;
import com.kimanga.afyacheck.model.VerificationToken;
import com.kimanga.afyacheck.repository.AdminAuditLogRepository;
import com.kimanga.afyacheck.repository.PasswordResetTokenRepository;
import com.kimanga.afyacheck.repository.UserRepository;
import com.kimanga.afyacheck.repository.VerificationTokenRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private VerificationTokenRepository verificationTokenRepository;
    private PasswordResetTokenRepository passwordResetTokenRepository;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private KeycloakAdminService keycloakAdminService;
    private AdminAuditLogRepository adminAuditLogRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        verificationTokenRepository = mock(VerificationTokenRepository.class);
        passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        emailService = mock(EmailService.class);
        keycloakAdminService = mock(KeycloakAdminService.class);
        adminAuditLogRepository = mock(AdminAuditLogRepository.class);
        userService = new UserService(userRepository, verificationTokenRepository,
                passwordResetTokenRepository, passwordEncoder, emailService,
                keycloakAdminService, adminAuditLogRepository);
    }

    private User validNewUser() {
        User user = new User();
        user.setUsername("newuser");
        user.setName("New User");
        user.setEmail("new@example.com");
        user.setPassword("plain");
        return user;
    }

    @Test
    void registerFailsWhenUsernameMissing() {
        User user = validNewUser();
        user.setUsername(" ");
        ServiceResult<User> result = userService.register(user);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Username is required");
    }

    @Test
    void registerFailsWhenNameMissing() {
        User user = validNewUser();
        user.setName(null);
        ServiceResult<User> result = userService.register(user);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Full name is required");
    }

    @Test
    void registerFailsWhenUsernameTaken() {
        User user = validNewUser();
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(new User()));
        ServiceResult<User> result = userService.register(user);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Username already exists");
    }

    @Test
    void registerFailsWhenEmailTaken() {
        User user = validNewUser();
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(new User()));
        ServiceResult<User> result = userService.register(user);
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void registerSucceedsAndSendsVerificationEmail() {
        User user = validNewUser();
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plain")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ServiceResult<User> result = userService.register(user);

        assertThat(result.isSuccess()).isTrue();
        assertThat(user.getPassword()).isEqualTo("encoded");
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getEnabled()).isFalse();
        assertThat(user.getEmailVerified()).isFalse();
        verify(verificationTokenRepository).save(any(VerificationToken.class));
        verify(emailService).sendHtmlMail(eq("new@example.com"), anyString(), eq("email/verify-email.html"), any());
    }

    @Test
    void verifyUserFailsWhenTokenNotFound() {
        when(verificationTokenRepository.findByToken("bad")).thenReturn(Optional.empty());
        ServiceResult<Void> result = userService.verifyUser("bad");
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void verifyUserFailsWhenTokenExpired() {
        VerificationToken token = VerificationToken.builder()
                .token("t1").user(new User()).expiryDate(LocalDateTime.now().minusHours(1)).build();
        when(verificationTokenRepository.findByToken("t1")).thenReturn(Optional.of(token));
        ServiceResult<Void> result = userService.verifyUser("t1");
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void verifyUserSucceedsAndEnablesUser() {
        User user = new User();
        VerificationToken token = VerificationToken.builder()
                .token("t1").user(user).expiryDate(LocalDateTime.now().plusHours(1)).build();
        when(verificationTokenRepository.findByToken("t1")).thenReturn(Optional.of(token));

        ServiceResult<Void> result = userService.verifyUser("t1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(user.getEnabled()).isTrue();
        verify(userRepository).save(user);
        verify(verificationTokenRepository).delete(token);
    }

    @Test
    void initiatePasswordResetFailsWhenUserNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        ServiceResult<Void> result = userService.initiatePasswordReset("missing@example.com");
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void initiatePasswordResetSendsEmailWhenUserFound() {
        User user = validNewUser();
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(user));

        ServiceResult<Void> result = userService.initiatePasswordReset("new@example.com");

        assertThat(result.isSuccess()).isTrue();
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendHtmlMail(eq("new@example.com"), anyString(), eq("email/reset-password.html"), any());
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
    void resetPasswordFailsWhenTokenNotFound() {
        when(passwordResetTokenRepository.findByToken("bad")).thenReturn(Optional.empty());
        ServiceResult<Void> result = userService.resetPassword("bad", "newpw");
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void resetPasswordFailsWhenTokenExpired() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("t1").user(new User()).expiryDate(LocalDateTime.now().minusMinutes(1)).build();
        when(passwordResetTokenRepository.findByToken("t1")).thenReturn(Optional.of(token));
        ServiceResult<Void> result = userService.resetPassword("t1", "newpw");
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void resetPasswordSucceedsAndUpdatesPassword() {
        User user = new User();
        PasswordResetToken token = PasswordResetToken.builder()
                .token("t1").user(user).expiryDate(LocalDateTime.now().plusMinutes(30)).build();
        when(passwordResetTokenRepository.findByToken("t1")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newpw")).thenReturn("encoded-new");

        ServiceResult<Void> result = userService.resetPassword("t1", "newpw");

        assertThat(result.isSuccess()).isTrue();
        assertThat(user.getPassword()).isEqualTo("encoded-new");
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).delete(token);
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
