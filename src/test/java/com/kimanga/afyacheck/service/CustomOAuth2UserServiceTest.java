package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.model.AuthProvider;
import com.kimanga.afyacheck.model.User;
import com.kimanga.afyacheck.model.UserRole;
import com.kimanga.afyacheck.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomOAuth2UserServiceTest {

    private UserRepository userRepository;
    private CustomOAuth2UserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new CustomOAuth2UserService();
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
    }

    private OAuth2UserRequest googleUserRequest() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .clientId("client-id")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("http://auth")
                .tokenUri("http://token")
                .userInfoUri("http://userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build();
        OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "token",
                Instant.now(), Instant.now().plusSeconds(3600));
        return new OAuth2UserRequest(registration, token);
    }

    private OAuth2User googleOAuth2User(String email) {
        Map<String, Object> attrs = Map.of(
                "sub", "google-sub-1",
                "name", "Google User",
                "email", email,
                "picture", "http://img"
        );
        return new DefaultOAuth2User(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")), attrs, "sub");
    }

    @Test
    void processOAuth2UserRegistersNewUser() {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Object result = ReflectionTestUtils.invokeMethod(service, "processOAuth2User",
                googleUserRequest(), googleOAuth2User("new@example.com"));

        assertThat(result).isNotNull();
    }

    @Test
    void registerNewUserGeneratesUniqueUsernameOnCollision() {
        when(userRepository.findByUsername("new")).thenReturn(Optional.of(new User()));
        when(userRepository.findByUsername("new1")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        com.kimanga.afyacheck.config.OAuth2UserInfo info = mock(com.kimanga.afyacheck.config.OAuth2UserInfo.class);
        when(info.getEmail()).thenReturn("new@example.com");
        when(info.getName()).thenReturn("New User");
        when(info.getImageUrl()).thenReturn("http://img");
        when(info.getId()).thenReturn("google-1");

        User result = ReflectionTestUtils.invokeMethod(service, "registerNewUser", googleUserRequest(), info);

        assertThat(result.getUsername()).isEqualTo("new1");
        assertThat(result.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(result.getRole()).isEqualTo(UserRole.USER);
        assertThat(result.getEnabled()).isTrue();
        assertThat(result.getEmailVerified()).isTrue();
    }

    @Test
    void updateExistingUserRefreshesNameAndImage() {
        User existing = new User();
        existing.setName("Old Name");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        com.kimanga.afyacheck.config.OAuth2UserInfo info = mock(com.kimanga.afyacheck.config.OAuth2UserInfo.class);
        when(info.getName()).thenReturn("New Name");
        when(info.getImageUrl()).thenReturn("http://newimg");

        User result = ReflectionTestUtils.invokeMethod(service, "updateExistingUser", existing, info);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getImageUrl()).isEqualTo("http://newimg");
    }

    @Test
    void processOAuth2UserThrowsWhenEmailMissing() {
        OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("sub", "google-sub-1", "name", "No Email"),
                "sub");

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "processOAuth2User",
                googleUserRequest(), oauth2User))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void processOAuth2UserThrowsWhenProviderMismatch() {
        User existing = new User();
        existing.setProvider(AuthProvider.GITHUB);
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "processOAuth2User",
                googleUserRequest(), googleOAuth2User("existing@example.com")))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void processOAuth2UserUpdatesExistingUserWithMatchingProvider() {
        User existing = new User();
        existing.setProvider(AuthProvider.GOOGLE);
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Object result = ReflectionTestUtils.invokeMethod(service, "processOAuth2User",
                googleUserRequest(), googleOAuth2User("existing@example.com"));

        assertThat(result).isNotNull();
        assertThat(existing.getName()).isEqualTo("Google User");
    }
}
