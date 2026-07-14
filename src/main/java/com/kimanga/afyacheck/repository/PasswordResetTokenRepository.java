package com.kimanga.afyacheck.repository;

import com.kimanga.afyacheck.model.PasswordResetToken;
import com.kimanga.afyacheck.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUser(User user);
}
