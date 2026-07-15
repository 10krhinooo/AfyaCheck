package com.kimanga.afyacheck.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Custom OAuth2User implementation that wraps both OAuth2User and UserDetails
 * This allows us to use the same principal for both OAuth2 and form-based authentication
 */
public class CustomOAuth2User implements OAuth2User, UserDetails {

    private final OAuth2User oauth2User;
    private final User user;

    public CustomOAuth2User(OAuth2User oauth2User, User user) {
        this.oauth2User = oauth2User;
        this.user = user;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Use the role from the User entity
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getName() {
        return user.getUsername() != null ? user.getUsername() : oauth2User.getName();
    }

    // UserDetails methods
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail(); // Using email as username
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(user.getEnabled());
    }

    // Custom getters to access user information
    public Long getId() {
        return user.getId();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getImageUrl() {
        return user.getImageUrl();
    }

    public User getUser() {
        return user;
    }

    public boolean isEmailVerified() {
        return Boolean.TRUE.equals(user.getEmailVerified());
    }

    // Helper method to check if user is admin
    public boolean isAdmin() {
        return user.getRole() == UserRole.ADMIN;
    }

    // Get the user's actual name
    public String getFullName() {
        return user.getName();
    }

    // Get the user's role
    public UserRole getRole() {
        return user.getRole();
    }

    // Get the username (not email)
    public String getActualUsername() {
        return user.getUsername();
    }
}