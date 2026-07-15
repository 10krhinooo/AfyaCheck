package com.kimanga.afyacheck.config;

import com.kimanga.afyacheck.model.User;
import com.kimanga.afyacheck.model.UserRole;
import com.kimanga.afyacheck.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        // Make your email admin (replace with your actual email)
        String adminEmail = "victor.moruri@strathmore.edu";

        Optional<User> userOptional = userRepository.findByEmail(adminEmail);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getRole() != UserRole.ADMIN) {
                user.setRole(UserRole.ADMIN);
                userRepository.save(user);
                System.out.println("✅ Made " + adminEmail + " an admin");
            } else {
                System.out.println("✅ " + adminEmail + " is already an admin");
            }
        } else {
            System.out.println("⚠️  User with email " + adminEmail + " not found. Please register first.");
        }
    }
}