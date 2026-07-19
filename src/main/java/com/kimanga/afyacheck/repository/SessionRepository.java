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

    // Bulk retention purge (DataRetentionService). Children are deleted first via the
    // Answer/RiskAssessment repos' bulk deletes — JPQL bulk deletes bypass entity-level
    // cascades, so ordering is the caller's responsibility.
    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM Session s WHERE s.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") Date cutoff);

    // Check if session exists
    boolean existsBySessionId(String sessionId);

    // Count sessions by status
    long countByStatus(String status);



}