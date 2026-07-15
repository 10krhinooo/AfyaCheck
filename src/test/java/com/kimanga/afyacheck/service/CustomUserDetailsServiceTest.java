package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.model.User;
import com.kimanga.afyacheck.model.UserRole;
import com.kimanga.afyacheck.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

    @Test
    void loadUserByUsernameThrowsWhenUserNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadUserByUsernameReturnsDisabledUserDetailsWhenNotEnabled() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setPassword("hashed");
        user.setRole(UserRole.USER);
        user.setEnabled(false);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("user@example.com");

        assertThat(details.getUsername()).isEqualTo("user@example.com");
        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    void loadUserByUsernameReturnsEnabledUserDetailsWhenEnabled() {
        User user = new User();
        user.setEmail("user2@example.com");
        user.setPassword("hashed");
        user.setRole(UserRole.ADMIN);
        user.setEnabled(true);
        when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("user2@example.com");

        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities()).extracting(a -> a.getAuthority()).containsExactly("ROLE_ADMIN");
    }
}
