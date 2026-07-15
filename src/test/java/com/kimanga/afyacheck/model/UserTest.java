package com.kimanga.afyacheck.model;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void onCreateSetsCreatedAndUpdatedTimestamps() {
        User user = new User();
        user.onCreate();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
    }

    @Test
    void onUpdateRefreshesUpdatedAt() {
        User user = new User();
        user.onUpdate();
        assertThat(user.getUpdatedAt()).isNotNull();
    }

    @Test
    void getAuthoritiesPrefixesRoleName() {
        User user = new User();
        user.setRole(UserRole.ADMIN);
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        assertThat(authorities).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
    }

    @Test
    void accountFlagsAreAlwaysNonExpiredAndNonLocked() {
        User user = new User();
        assertThat(user.isAccountNonExpired()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void isEnabledReflectsEnabledFlag() {
        User user = new User();
        user.setEnabled(null);
        assertThat(user.isEnabled()).isFalse();
        user.setEnabled(true);
        assertThat(user.isEnabled()).isTrue();
        user.setEnabled(false);
        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    void isAdminReflectsRole() {
        User user = new User();
        user.setRole(UserRole.ADMIN);
        assertThat(user.isAdmin()).isTrue();

        user.setRole(UserRole.USER);
        assertThat(user.isAdmin()).isFalse();
    }
}
