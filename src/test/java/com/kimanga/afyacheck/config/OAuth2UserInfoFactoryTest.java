package com.kimanga.afyacheck.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuth2UserInfoFactoryTest {

    @Test
    void googleAttributesAreMappedCorrectly() {
        Map<String, Object> attrs = Map.of(
                "sub", "g-1",
                "name", "Google User",
                "email", "google@example.com",
                "picture", "http://img/google"
        );
        OAuth2UserInfo info = OAuth2UserInfoFactory.getOAuth2UserInfo("google", attrs);
        assertThat(info.getId()).isEqualTo("g-1");
        assertThat(info.getName()).isEqualTo("Google User");
        assertThat(info.getEmail()).isEqualTo("google@example.com");
        assertThat(info.getImageUrl()).isEqualTo("http://img/google");
    }

    @Test
    void googleIsCaseInsensitive() {
        Map<String, Object> attrs = Map.of("sub", "g-1");
        OAuth2UserInfo info = OAuth2UserInfoFactory.getOAuth2UserInfo("GOOGLE", attrs);
        assertThat(info).isNotNull();
    }

    @Test
    void githubAttributesAreMappedCorrectly() {
        Map<String, Object> attrs = Map.of(
                "id", 42,
                "name", "GitHub User",
                "email", "github@example.com",
                "avatar_url", "http://img/github"
        );
        OAuth2UserInfo info = OAuth2UserInfoFactory.getOAuth2UserInfo("github", attrs);
        assertThat(info.getId()).isEqualTo("42");
        assertThat(info.getName()).isEqualTo("GitHub User");
        assertThat(info.getEmail()).isEqualTo("github@example.com");
        assertThat(info.getImageUrl()).isEqualTo("http://img/github");
    }

    @Test
    void appleUsesFirstAndLastNameWhenPresent() {
        Map<String, Object> attrs = Map.of(
                "sub", "a-1",
                "name", Map.of("firstName", "Ada", "lastName", "Lovelace"),
                "email", "apple@example.com"
        );
        OAuth2UserInfo info = OAuth2UserInfoFactory.getOAuth2UserInfo("apple", attrs);
        assertThat(info.getId()).isEqualTo("a-1");
        assertThat(info.getName()).isEqualTo("Ada Lovelace");
        assertThat(info.getEmail()).isEqualTo("apple@example.com");
        assertThat(info.getImageUrl()).isNull();
    }

    @Test
    void appleFallsBackToEmailWhenNameMissing() {
        Map<String, Object> attrs = Map.of(
                "sub", "a-1",
                "email", "apple@example.com"
        );
        OAuth2UserInfo info = OAuth2UserInfoFactory.getOAuth2UserInfo("apple", attrs);
        assertThat(info.getName()).isEqualTo("apple@example.com");
    }

    @Test
    void unsupportedProviderThrows() {
        assertThatThrownBy(() -> OAuth2UserInfoFactory.getOAuth2UserInfo("facebook", Map.of()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("facebook");
    }
}
