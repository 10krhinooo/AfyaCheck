package com.kimanga.afyacheck.model;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CustomOAuth2UserTest {

    private User baseUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setUsername("theuser");
        user.setName("The User");
        user.setPassword("secret");
        user.setImageUrl("http://img");
        user.setRole(UserRole.ADMIN);
        user.setEnabled(true);
        user.setEmailVerified(true);
        return user;
    }

    private OAuth2User oauth2User() {
        return new DefaultOAuth2User(
                Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")),
                Map.of("sub", "12345", "name", "OAuth Name"),
                "sub"
        );
    }

    @Test
    void delegatesAttributesToWrappedOAuth2User() {
        CustomOAuth2User customUser = new CustomOAuth2User(oauth2User(), baseUser());
        assertThat(customUser.getAttributes()).containsEntry("sub", "12345");
    }

    @Test
    void authoritiesComeFromUserRole() {
        CustomOAuth2User customUser = new CustomOAuth2User(oauth2User(), baseUser());
        assertThat(customUser.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
    }

    @Test
    void getNamePrefersUsernameOverOAuth2Name() {
        CustomOAuth2User customUser = new CustomOAuth2User(oauth2User(), baseUser());
        assertThat(customUser.getName()).isEqualTo("theuser");
    }

    @Test
    void getNameFallsBackToOAuth2NameWhenUsernameMissing() {
        User user = baseUser();
        user.setUsername(null);
        CustomOAuth2User customUser = new CustomOAuth2User(oauth2User(), user);
        // DefaultOAuth2User.getName() returns the value of the configured
        // name-attribute-key, which is "sub" here, not the "name" attribute.
        assertThat(customUser.getName()).isEqualTo("12345");
    }

    @Test
    void userDetailsMethodsDelegateToUser() {
        CustomOAuth2User customUser = new CustomOAuth2User(oauth2User(), baseUser());
        assertThat(customUser.getPassword()).isEqualTo("secret");
        assertThat(customUser.getUsername()).isEqualTo("user@example.com");
        assertThat(customUser.isAccountNonExpired()).isTrue();
        assertThat(customUser.isAccountNonLocked()).isTrue();
        assertThat(customUser.isCredentialsNonExpired()).isTrue();
        assertThat(customUser.isEnabled()).isTrue();
    }

    @Test
    void isEnabledFalseWhenUserFlagNull() {
        User user = baseUser();
        user.setEnabled(null);
        CustomOAuth2User customUser = new CustomOAuth2User(oauth2User(), user);
        assertThat(customUser.isEnabled()).isFalse();
    }

    @Test
    void customAccessorsExposeUserFields() {
        CustomOAuth2User customUser = new CustomOAuth2User(oauth2User(), baseUser());
        assertThat(customUser.getId()).isEqualTo(1L);
        assertThat(customUser.getEmail()).isEqualTo("user@example.com");
        assertThat(customUser.getImageUrl()).isEqualTo("http://img");
        assertThat(customUser.getUser()).isNotNull();
        assertThat(customUser.isEmailVerified()).isTrue();
        assertThat(customUser.isAdmin()).isTrue();
        assertThat(customUser.getFullName()).isEqualTo("The User");
        assertThat(customUser.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(customUser.getActualUsername()).isEqualTo("theuser");
    }

    @Test
    void isEmailVerifiedFalseWhenNull() {
        User user = baseUser();
        user.setEmailVerified(null);
        CustomOAuth2User customUser = new CustomOAuth2User(oauth2User(), user);
        assertThat(customUser.isEmailVerified()).isFalse();
    }

    @Test
    void isAdminFalseForNonAdminRole() {
        User user = baseUser();
        user.setRole(UserRole.USER);
        CustomOAuth2User customUser = new CustomOAuth2User(oauth2User(), user);
        assertThat(customUser.isAdmin()).isFalse();
    }
}
