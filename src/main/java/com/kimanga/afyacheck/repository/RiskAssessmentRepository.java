package com.kimanga.afyacheck.repository;

import com.kimanga.afyacheck.model.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> {

    // Find latest risk assessment for a session
    @Query("SELECT ra FROM RiskAssessment ra WHERE ra.session.sessionId = :sessionId ORDER BY ra.createdAt DESC LIMIT 1")
    Optional<RiskAssessment> findLatestBySessionId(@Param("sessionId") String sessionId);

    // Find all risk assessments for a session ordered by creation date
    @Query("SELECT ra FROM RiskAssessment ra WHERE ra.session.sessionId = :sessionId ORDER BY ra.createdAt DESC")
    List<RiskAssessment> findBySessionIdOrderByCreatedAtDesc(@Param("sessionId") String sessionId);

    // Find risk assessments by risk level
    List<RiskAssessment> findByRiskLevel(String riskLevel);

    // Count risk assessments by risk level
    long countByRiskLevel(String riskLevel);

    // Find risk assessments with score greater than
    List<RiskAssessment> findByRiskScoreGreaterThan(Integer score);

    // Find risk assessments with score between
    List<RiskAssessment> findByRiskScoreBetween(Integer minScore, Integer maxScore);

    // Delete all risk assessments for a session
    void deleteBySession_SessionId(String sessionId);

    // For the admin CSV export: fetches session and user eagerly so the
    // controller can read them outside a transaction without triggering
    // LazyInitializationException.
    @Query("SELECT ra FROM RiskAssessment ra JOIN FETCH ra.session s LEFT JOIN FETCH s.user ORDER BY ra.createdAt DESC")
    List<RiskAssessment> findAllForExport();
}