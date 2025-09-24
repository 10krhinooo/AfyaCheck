package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        com.kimanga.afyacheck.model.User u = repository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password."));

        // Return the user but don't throw exception here - let Spring Security handle disabled state
        return org.springframework.security.core.userdetails.User.builder()
                .username(u.getEmail())
                .password(u.getPassword())
                .roles("USER")
                .disabled(!u.isEnabled()) // This tells Spring Security the account is disabled
                .build();
    }
}