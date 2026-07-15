package com.kimanga.afyacheck.config;

import com.kimanga.afyacheck.model.User;
import com.kimanga.afyacheck.model.UserRole;
import com.kimanga.afyacheck.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AdminInitializerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AdminInitializer initializer = new AdminInitializer(userRepository);

    @Test
    void promotesExistingNonAdminUserToAdmin() throws Exception {
        User user = new User();
        user.setRole(UserRole.USER);
        when(userRepository.findByEmail(eq("victor.moruri@strathmore.edu"))).thenReturn(Optional.of(user));

        initializer.run();

        verify(userRepository).save(user);
        assert user.getRole() == UserRole.ADMIN;
    }

    @Test
    void leavesAlreadyAdminUserUnchanged() throws Exception {
        User user = new User();
        user.setRole(UserRole.ADMIN);
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));

        initializer.run();

        verify(userRepository, never()).save(any());
    }

    @Test
    void doesNothingWhenUserNotFound() throws Exception {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        initializer.run();

        verify(userRepository, never()).save(any());
    }
}
