package com.kimanga.afyacheck.repository;

import com.kimanga.afyacheck.model.Session;
import com.kimanga.afyacheck.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findBySessionId(String sessionId);

    // Separate queries to avoid MultipleBagFetchException
    @Query("SELECT s FROM Session s LEFT JOIN FETCH s.answers WHERE s.sessionId = :sessionId")
    Optional<Session> findBySessionIdWithAnswers(@Param("sessionId") String sessionId);

    @Query("SELECT s FROM Session s LEFT JOIN FETCH s.riskAssessments WHERE s.sessionId = :sessionId")
    Optional<Session> findBySessionIdWithRiskAssessments(@Param("sessionId") String sessionId);

    // Find old sessions for cleanup
    @Query("SELECT s FROM Session s WHERE s.createdAt < :cutoff AND s.status = :status")
    List<Session> findByCreatedAtBeforeAndStatus(@Param("cutoff") Date cutoff, @Param("status") String status);

    // Find sessions by status
    List<Session> findByStatus(String status);

    // Check if session exists
    boolean existsBySessionId(String sessionId);

    // Count sessions by status
    long countByStatus(String status);

    // A user's past assessment sessions, most recent first
    List<Session> findByUserIdOrderByCreatedAtDesc(Long userId);

    // A user's most recent session
    Optional<Session> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    // One row per user: their latest completed session, only when that
    // session is older than the cutoff (i.e. the user is overdue for
    // reassessment).
    @Query("SELECT s FROM Session s WHERE s.status = 'completed' AND s.user IS NOT NULL " +
            "AND s.createdAt = (SELECT MAX(s2.createdAt) FROM Session s2 WHERE s2.user = s.user AND s2.status = 'completed') " +
            "AND s.createdAt < :cutoff")
    List<Session> findLatestCompletedSessionsOlderThan(@Param("cutoff") Date cutoff);

}