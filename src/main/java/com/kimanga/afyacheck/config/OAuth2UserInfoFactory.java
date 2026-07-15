package com.kimanga.afyacheck.config;



import java.util.Map;

public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if (registrationId.equalsIgnoreCase("google")) {
            return new GoogleOAuth2UserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase("github")) {
            return new GithubOAuth2UserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase("apple")) {
            return new AppleOAuth2UserInfo(attributes);
        } else {
            throw new RuntimeException("Login with " + registrationId + " is not supported yet");
        }
    }
}

// Google OAuth2 User Info
class GoogleOAuth2UserInfo implements OAuth2UserInfo {
    private Map<String, Object> attributes;

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getImageUrl() {
        return (String) attributes.get("picture");
    }
}

// GitHub OAuth2 User Info
class GithubOAuth2UserInfo implements OAuth2UserInfo {
    private Map<String, Object> attributes;

    public GithubOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getId() {
        return ((Integer) attributes.get("id")).toString();
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getImageUrl() {
        return (String) attributes.get("avatar_url");
    }
}

// Apple OAuth2 User Info
class AppleOAuth2UserInfo implements OAuth2UserInfo {
    private Map<String, Object> attributes;

    public AppleOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getName() {
        // Apple doesn't always provide name
        Map<String, Object> name = (Map<String, Object>) attributes.get("name");
        if (name != null) {
            String firstName = (String) name.get("firstName");
            String lastName = (String) name.get("lastName");
            return firstName + " " + lastName;
        }
        return (String) attributes.get("email"); // Fallback to email
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getImageUrl() {
        // Apple doesn't provide profile pictures
        return null;
    }
}