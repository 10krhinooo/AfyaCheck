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

    // Model-ops analytics (AdminService.getModelOpsStats)
    @Query("SELECT ra.riskLevel, COUNT(ra) FROM RiskAssessment ra GROUP BY ra.riskLevel")
    List<Object[]> countGroupedByRiskLevel();

    @Query("SELECT ra.modelVersion, COUNT(ra), AVG(ra.riskScore) FROM RiskAssessment ra GROUP BY ra.modelVersion ORDER BY COUNT(ra) DESC")
    List<Object[]> countGroupedByModelVersion();

    @Query("SELECT CAST(ra.createdAt AS date), COUNT(ra) FROM RiskAssessment ra WHERE ra.createdAt >= :since GROUP BY CAST(ra.createdAt AS date) ORDER BY CAST(ra.createdAt AS date)")
    List<Object[]> countByDaySince(@Param("since") java.util.Date since);

    // Bulk retention purge (DataRetentionService) — must run before the Session bulk delete.
    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM RiskAssessment ra WHERE ra.session IN (SELECT s FROM Session s WHERE s.createdAt < :cutoff)")
    int deleteBySessionCreatedAtBefore(@Param("cutoff") java.util.Date cutoff);
}