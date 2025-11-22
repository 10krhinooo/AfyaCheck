package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.config.OAuth2UserInfo;
import com.kimanga.afyacheck.config.OAuth2UserInfoFactory;
import com.kimanga.afyacheck.model.CustomOAuth2User;
import com.kimanga.afyacheck.model.User;
import com.kimanga.afyacheck.model.AuthProvider;
import com.kimanga.afyacheck.model.UserRole;
import com.kimanga.afyacheck.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oauth2User);
        } catch (Exception ex) {
            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo oauth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oauth2User.getAttributes());

        if (oauth2UserInfo.getEmail() == null || oauth2UserInfo.getEmail().isEmpty()) {
            throw new RuntimeException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(oauth2UserInfo.getEmail());
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            if (!user.getProvider().name().equals(registrationId.toUpperCase())) {
                throw new RuntimeException("Looks like you're signed up with " +
                        user.getProvider() + " account. Please use your " +
                        user.getProvider() + " account to login.");
            }
            user = updateExistingUser(user, oauth2UserInfo);
        } else {
            user = registerNewUser(userRequest, oauth2UserInfo);
        }

        return new CustomOAuth2User(oauth2User, user);
    }

    private User registerNewUser(OAuth2UserRequest userRequest, OAuth2UserInfo oauth2UserInfo) {
        User user = new User();

        user.setEmail(oauth2UserInfo.getEmail());
        user.setName(oauth2UserInfo.getName());

        // Generate username from email
        String username = oauth2UserInfo.getEmail().split("@")[0];

        // Ensure username is unique
        String uniqueUsername = username;
        int counter = 1;
        while (userRepository.findByUsername(uniqueUsername).isPresent()) {
            uniqueUsername = username + counter;
            counter++;
        }

        user.setUsername(uniqueUsername);
        user.setImageUrl(oauth2UserInfo.getImageUrl());
        user.setProvider(AuthProvider.valueOf(userRequest.getClientRegistration().getRegistrationId().toUpperCase()));
        user.setProviderId(oauth2UserInfo.getId());
        user.setEmailVerified(true);
        user.setEnabled(true);

        // Set default role - you can modify this logic to make specific users admin
        user.setRole(UserRole.USER);

        return userRepository.save(user);
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo oauth2UserInfo) {
        existingUser.setName(oauth2UserInfo.getName());
        existingUser.setImageUrl(oauth2UserInfo.getImageUrl());
        return userRepository.save(existingUser);
    }
}