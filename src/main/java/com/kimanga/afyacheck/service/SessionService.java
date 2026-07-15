package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.model.RiskAssessment;
import com.kimanga.afyacheck.model.Session;
import com.kimanga.afyacheck.model.User;
import com.kimanga.afyacheck.repository.AnswerRepository;
import com.kimanga.afyacheck.repository.RiskAssessmentRepository;
import com.kimanga.afyacheck.repository.SessionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Owns session lifecycle (create/find/complete/cleanup) and user
 * association. Answer persistence and risk-assessment persistence are
 * split out into AnswerService and RiskAssessmentPersistenceService;
 * this class delegates to them so the public API callers already depend
 * on (controllers) doesn't need to change.
 */
@Service
public class SessionService {

    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);

    private final SessionRepository sessionRepository;
    private final AnswerRepository answerRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final SessionIdSanitizer sessionIdSanitizer;
    private final AnswerService answerService;
    private final RiskAssessmentPersistenceService riskAssessmentPersistenceService;

    @PersistenceContext
    private EntityManager entityManager;

    public SessionService(SessionRepository sessionRepository,
                          AnswerRepository answerRepository,
                          RiskAssessmentRepository riskAssessmentRepository,
                          SessionIdSanitizer sessionIdSanitizer,
                          AnswerService answerService,
                          RiskAssessmentPersistenceService riskAssessmentPersistenceService) {
        this.sessionRepository = sessionRepository;
        this.answerRepository = answerRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.sessionIdSanitizer = sessionIdSanitizer;
        this.answerService = answerService;
        this.riskAssessmentPersistenceService = riskAssessmentPersistenceService;
    }

    private String createSafeSessionId(String originalSessionId) {
        return sessionIdSanitizer.createSafeSessionId(originalSessionId, this::sessionExists);
    }

    public String cleanSessionId(String sessionId) {
        return sessionIdSanitizer.clean(sessionId, this::sessionExists);
    }

    @Transactional
    public String createOrGetSession(String httpSessionId) {
        try {
            String safeSessionId = createSafeSessionId(httpSessionId);
            logger.info("Creating or getting session for: {} (original: {})",
                    safeSessionId, httpSessionId);

            Optional<Session> existingSession = sessionRepository.findBySessionId(safeSessionId);
            if (existingSession.isPresent()) {
                Session session = existingSession.get();
                if (!isValidSessionState(session)) {
                    logger.warn("Found invalid session state, recreating: {}", safeSessionId);
                    return createNewSession(safeSessionId).getSessionId();
                }
                logger.info("Found existing session: {}", safeSessionId);
                return session.getSessionId();
            } else {
                Session newSession = createNewSession(safeSessionId);
                return newSession.getSessionId();
            }
        } catch (Exception e) {
            logger.error("Error creating/getting session for: {}", httpSessionId, e);
            clearPersistenceContext();
            return UUID.randomUUID().toString();
        }
    }

    @Transactional
    public Session createNewSession(String sessionId) {
        String safeSessionId = createSafeSessionId(sessionId);

        try {
            Session newSession = new Session();
            newSession.setSessionId(safeSessionId);
            newSession.setStatus("active");

            Session savedSession = sessionRepository.saveAndFlush(newSession);
            logger.info("Created new session: {} with ID: {}", safeSessionId, savedSession.getId());
            return savedSession;
        } catch (Exception e) {
            logger.error("Error creating new session: {}", safeSessionId, e);
            clearPersistenceContext();

            try {
                String fallbackId = UUID.randomUUID().toString();
                Session fallbackSession = new Session();
                fallbackSession.setSessionId(fallbackId);
                fallbackSession.setStatus("active");

                Session savedSession = sessionRepository.saveAndFlush(fallbackSession);
                logger.info("Created fallback UUID session: {} for original: {}", fallbackId, sessionId);
                return savedSession;
            } catch (Exception ex) {
                logger.error("CRITICAL: Failed to create fallback session", ex);
                throw new RuntimeException("Failed to create session after multiple attempts", ex);
            }
        }
    }

    public void saveAnswers(String sessionId, Map<String, String> formParams) {
        answerService.saveAnswers(sessionId, formParams);
    }

    public Map<String, String> getCurrentAnswers(String sessionId) {
        return answerService.getCurrentAnswers(sessionId);
    }

    public Map<String, String> goBack(String sessionId) {
        return answerService.goBack(sessionId);
    }

    public void saveRiskAssessment(String sessionId, String riskLevel, Integer riskScore, String recommendations) {
        riskAssessmentPersistenceService.saveRiskAssessment(sessionId, riskLevel, riskScore, recommendations);
    }

    public Optional<RiskAssessment> getLatestRiskAssessment(String sessionId) {
        return riskAssessmentPersistenceService.getLatestRiskAssessment(sessionId);
    }

    public Optional<Session> getSessionWithDetails(String sessionId) {
        return riskAssessmentPersistenceService.getSessionWithDetails(sessionId);
    }

    public RiskAssessment getLatestRiskAssessmentOrThrow(String sessionId) {
        return riskAssessmentPersistenceService.getLatestRiskAssessmentOrThrow(sessionId);
    }

    public Session getSessionWithDetailsOrThrow(String sessionId) {
        return riskAssessmentPersistenceService.getSessionWithDetailsOrThrow(sessionId);
    }

    @Transactional
    public void completeSession(String sessionId) {
        try {
            String safeSessionId = createSafeSessionId(sessionId);
            Optional<Session> sessionOpt = sessionRepository.findBySessionId(safeSessionId);
            if (sessionOpt.isPresent()) {
                Session session = sessionOpt.get();
                if (isValidSessionState(session)) {
                    session.setStatus("completed");
                    sessionRepository.save(session);
                    logger.info("Marked session as completed: {}", safeSessionId);
                } else {
                    logger.error("Cannot complete session with invalid state: {}", safeSessionId);
                }
            } else {
                logger.warn("Session not found when completing: {}", safeSessionId);
            }
        } catch (Exception e) {
            logger.error("Error completing session: {}", sessionId, e);
            clearPersistenceContext();
        }
    }

    @Transactional
    public void assignUser(String sessionId, User user) {
        try {
            String safeSessionId = createSafeSessionId(sessionId);
            Optional<Session> sessionOpt = sessionRepository.findBySessionId(safeSessionId);
            if (sessionOpt.isPresent()) {
                Session session = sessionOpt.get();
                session.setUser(user);
                sessionRepository.save(session);
                logger.info("Assigned user {} to session: {}", user.getId(), safeSessionId);
            } else {
                logger.warn("Session not found when assigning user: {}", safeSessionId);
            }
        } catch (Exception e) {
            logger.error("Error assigning user to session: {}", sessionId, e);
            clearPersistenceContext();
        }
    }

    public List<Session> getSessionsForUser(Long userId) {
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Optional<Session> getLatestSessionForUser(Long userId) {
        return sessionRepository.findTopByUserIdOrderByCreatedAtDesc(userId);
    }

    @Scheduled(cron = "0 0 3 * * *") // nightly at 3am
    @Transactional
    public void cleanupOldSessions() {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR, -24);
            Date cutoff = calendar.getTime();

            List<Session> oldSessions = sessionRepository.findByCreatedAtBeforeAndStatus(cutoff, "active");
            logger.info("Found {} old sessions to cleanup", oldSessions.size());

            int deletedCount = 0;
            for (Session session : oldSessions) {
                if (isValidSessionState(session)) {
                    try {
                        answerRepository.deleteBySession(session);
                        riskAssessmentRepository.deleteBySession_SessionId(session.getSessionId());

                        sessionRepository.delete(session);
                        deletedCount++;
                        logger.debug("Cleaned up old session: {}", session.getSessionId());
                    } catch (Exception e) {
                        logger.error("Error cleaning up session: {}", session.getSessionId(), e);
                    }
                }
            }

            logger.info("Successfully cleaned up {} old sessions", deletedCount);
        } catch (Exception e) {
            logger.error("Error cleaning up old sessions", e);
            clearPersistenceContext();
        }
    }

    public boolean sessionExists(String sessionId) {
        try {
            String safeSessionId = createSafeSessionId(sessionId);
            Optional<Session> sessionOpt = sessionRepository.findBySessionId(safeSessionId);
            return sessionOpt.map(this::isValidSessionState).orElse(false);
        } catch (Exception e) {
            logger.error("Error checking if session exists: {}", sessionId, e);
            return false;
        }
    }

    public Optional<Session> getSession(String sessionId) {
        try {
            String safeSessionId = createSafeSessionId(sessionId);
            return sessionRepository.findBySessionId(safeSessionId)
                    .filter(this::isValidSessionState);
        } catch (Exception e) {
            logger.error("Error getting session: {}", sessionId, e);
            return Optional.empty();
        }
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
