package com.kimanga.afyacheck.repository;

import com.kimanga.afyacheck.model.User;
import com.kimanga.afyacheck.model.AuthProvider;
import com.kimanga.afyacheck.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);
    Optional<User> findByKeycloakId(String keycloakId);

    // Count users active after a certain date (using updatedAt as last active)
    @Query("SELECT COUNT(u) FROM User u WHERE u.updatedAt > :date")
    Long countByLastActiveAfter(@Param("date") LocalDateTime date);

    // Count users created after a certain date
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt > :date")
    Long countByCreatedAtAfter(@Param("date") LocalDateTime date);

    List<User> findTop10ByOrderByCreatedAtDesc();

    // Check if email exists
    boolean existsByEmail(String email);

    // Check if username exists
    boolean existsByUsername(String username);

    // NEW METHODS FOR ADMIN DASHBOARD:

    // Count enabled users
    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = true")
    Long countByEnabledTrue();

    // Find users by role
    @Query("SELECT u FROM User u WHERE u.role = :role")
    List<User> findByRole(@Param("role") UserRole role);

    // Count all enabled and verified users
    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = true AND u.emailVerified = true")
    Long countActiveUsers();

    // Add this method to your existing UserRepository
    @Query("SELECT CONCAT(TO_CHAR(u.createdAt, 'Mon'), ' ', TO_CHAR(u.createdAt, 'YYYY')) as month, COUNT(u) " +
            "FROM User u " +
            "WHERE u.createdAt >= :startDate " +
            "GROUP BY TO_CHAR(u.createdAt, 'Mon YYYY'), EXTRACT(YEAR FROM u.createdAt), EXTRACT(MONTH FROM u.createdAt) " +
            "ORDER BY EXTRACT(YEAR FROM u.createdAt), EXTRACT(MONTH FROM u.createdAt)")
    List<Object[]> findUserRegistrationsByMonth(@Param("startDate") LocalDateTime startDate);


}