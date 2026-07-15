package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.model.RiskAssessment;
import com.kimanga.afyacheck.model.Session;
import com.kimanga.afyacheck.repository.RiskAssessmentRepository;
import com.kimanga.afyacheck.repository.SessionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Persists and retrieves risk assessments and the session views built on top
 * of them. Split out of SessionService, which delegates these methods here
 * so every existing caller keeps working unchanged.
 */
@Service
public class RiskAssessmentPersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(RiskAssessmentPersistenceService.class);

    private final SessionRepository sessionRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final SessionIdSanitizer sessionIdSanitizer;

    @PersistenceContext
    private EntityManager entityManager;

    public RiskAssessmentPersistenceService(SessionRepository sessionRepository,
                                             RiskAssessmentRepository riskAssessmentRepository,
                                             SessionIdSanitizer sessionIdSanitizer) {
        this.sessionRepository = sessionRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.sessionIdSanitizer = sessionIdSanitizer;
    }

    private String createSafeSessionId(String originalSessionId) {
        return sessionIdSanitizer.createSafeSessionId(originalSessionId, this::sessionExists);
    }

    private boolean sessionExists(String sessionId) {
        return sessionRepository.findBySessionId(sessionId)
                .map(this::isValidSessionState)
                .orElse(false);
    }

    @Transactional
    public void saveRiskAssessment(String sessionId, String riskLevel, Integer riskScore, String recommendations) {
        try {
            String safeSessionId = createSafeSessionId(sessionId);
            logger.info("Saving risk assessment for session: {}, risk level: {}, score: {}",
                    safeSessionId, riskLevel, riskScore);

            Optional<Session> sessionOpt = sessionRepository.findBySessionId(safeSessionId);
            if (sessionOpt.isEmpty()) {
                logger.warn("Session not found when saving risk assessment: {}", safeSessionId);
                throw new RuntimeException("Session not found: " + safeSessionId);
            }

            Session session = sessionOpt.get();
            if (!isValidSessionState(session)) {
                logger.error("Invalid session state when saving risk assessment: {}", safeSessionId);
                throw new RuntimeException("Invalid session state: " + safeSessionId);
            }

            RiskAssessment assessment = new RiskAssessment();
            assessment.setSession(session);
            assessment.setRiskLevel(riskLevel);
            assessment.setRiskScore(riskScore);

            List<String> recommendationsList = new ArrayList<>();
            if (recommendations != null && !recommendations.trim().isEmpty()) {
                recommendationsList = Arrays.asList(recommendations.split("; "));
            }
            assessment.setRecommendations(recommendationsList);

            riskAssessmentRepository.save(assessment);

            session.setRiskScore(riskScore);
            session.setStatus("completed");
            sessionRepository.save(session);

            logger.info("Successfully saved risk assessment for session: {}", safeSessionId);

        } catch (Exception e) {
            logger.error("Error saving risk assessment for session: {}", sessionId, e);
            clearPersistenceContext();
            throw new RuntimeException("Failed to save risk assessment: " + e.getMessage(), e);
        }
    }

    public Optional<RiskAssessment> getLatestRiskAssessment(String sessionId) {
        try {
            String safeSessionId = createSafeSessionId(sessionId);
            return riskAssessmentRepository.findLatestBySessionId(safeSessionId);
        } catch (Exception e) {
            logger.error("Error getting latest risk assessment for session: {}", sessionId, e);
            return Optional.empty();
        }
    }

    public RiskAssessment getLatestRiskAssessmentOrThrow(String sessionId) {
        String safeSessionId = createSafeSessionId(sessionId);
        return riskAssessmentRepository.findLatestBySessionId(safeSessionId)
                .orElseThrow(() -> new RuntimeException("No risk assessment found for session: " + safeSessionId));
    }

    public Optional<Session> getSessionWithDetails(String sessionId) {
        try {
            String safeSessionId = createSafeSessionId(sessionId);
            Optional<Session> sessionWithAnswers = sessionRepository.findBySessionIdWithAnswers(safeSessionId);
            if (sessionWithAnswers.isPresent()) {
                Session session = sessionWithAnswers.get();
                List<RiskAssessment> riskAssessments = riskAssessmentRepository.findBySessionIdOrderByCreatedAtDesc(safeSessionId);
                session.setRiskAssessments(riskAssessments);
                return Optional.of(session);
            }
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Error getting session with details: {}", sessionId, e);
            return Optional.empty();
        }
    }

    public Session getSessionWithDetailsOrThrow(String sessionId) {
        String safeSessionId = createSafeSessionId(sessionId);
        return getSessionWithDetails(safeSessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + safeSessionId));
    }

    private boolean isValidSessionState(Session session) {
        return session != null &&
                session.getId() != null &&
                session.getSessionId() != null &&
                !session.getSessionId().trim().isEmpty();
    }

    private void clearPersistenceContext() {
        try {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.clear();
                logger.debug("Persistence context cleared");
            }
        } catch (Exception e) {
            logger.warn("Error clearing persistence context", e);
        }
    }
}
