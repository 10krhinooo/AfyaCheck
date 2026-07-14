package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.model.Answer;
import com.kimanga.afyacheck.model.Session;
import com.kimanga.afyacheck.model.RiskAssessment;
import com.kimanga.afyacheck.repository.AnswerRepository;
import com.kimanga.afyacheck.repository.SessionRepository;
import com.kimanga.afyacheck.repository.RiskAssessmentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SessionService {

    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);

    // Use a shorter session ID if the original is too problematic
    private static final int MAX_SESSION_ID_LENGTH = 500;

    private final SessionRepository sessionRepository;
    private final AnswerRepository answerRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public SessionService(SessionRepository sessionRepository,
                          AnswerRepository answerRepository,
                          RiskAssessmentRepository riskAssessmentRepository) {
        this.sessionRepository = sessionRepository;
        this.answerRepository = answerRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
    }

    /**
     * Creates a safe session ID by hashing long session IDs
     */
    private String createSafeSessionId(String originalSessionId) {
        if (originalSessionId == null) {
            return UUID.randomUUID().toString();
        }

        // Clean the session ID first - remove duplicates
        String cleanSessionId = cleanSessionId(originalSessionId);

        // If session ID is reasonable length, use it as-is
        if (cleanSessionId.length() <= MAX_SESSION_ID_LENGTH) {
            return cleanSessionId;
        }

        // For very long session IDs, create a hash-based ID
        String hashBasedId = "sess_" + Integer.toHexString(cleanSessionId.hashCode()) +
                "_" + System.currentTimeMillis();
        logger.warn("Session ID too long ({} chars), using hash-based ID: {}",
                cleanSessionId.length(), hashBasedId);
        return hashBasedId;
    }

    /**
     * Clean session ID by removing duplicates and extra commas
     */
    public String cleanSessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return sessionId;
        }

        String trimmed = sessionId.trim();

        // If the session ID contains commas, take the first part
        if (trimmed.contains(",")) {
            String[] parts = trimmed.split(",");
            for (String part : parts) {
                String cleanPart = part.trim();
                // Check if this part exists in the database
                if (sessionExists(cleanPart)) {
                    logger.info("Using valid session ID part: {}", cleanPart);
                    return cleanPart;
                }
            }
            // If no valid part found, use the first one
            String cleanId = parts[0].trim();
            logger.warn("No valid session ID found, using first part: {}", cleanId);
            return cleanId;
        }

        return trimmed;
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
            // Return a guaranteed safe session ID
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

            // Final fallback - use UUID
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

    @Transactional
    public void saveAnswers(String sessionId, Map<String, String> formParams) {
        Session session = null;
        try {
            String safeSessionId = createSafeSessionId(sessionId);
            logger.info("Saving answers for session: {}, answers: {}", safeSessionId, formParams.keySet());

            Optional<Session> sessionOpt = sessionRepository.findBySessionId(safeSessionId);
            if (sessionOpt.isEmpty()) {
                logger.warn("Session not found when saving answers: {}, creating new session", safeSessionId);
                session = createNewSession(safeSessionId);
            } else {
                session = sessionOpt.get();
                if (!isValidSessionState(session)) {
                    logger.error("Invalid session state for: {}, recreating session", safeSessionId);
                    clearPersistenceContext();
                    session = createNewSession(safeSessionId);
                }
            }

            // Verify session is properly managed and has an ID
            if (session.getId() == null) {
                logger.error("Session still has null ID after creation: {}", safeSessionId);
                session = sessionRepository.saveAndFlush(session);
            }

            int savedCount = 0;
            for (Map.Entry<String, String> entry : formParams.entrySet()) {
                String questionKey = entry.getKey();
                String answerValue = entry.getValue();

                if (isValidAnswerParameter(questionKey, answerValue)) {
                    if (saveOrUpdateAnswer(session, questionKey, answerValue)) {
                        savedCount++;
                    }
                }
            }

            logger.info("Successfully saved {} answers for session: {}", savedCount, safeSessionId);

        } catch (Exception e) {
            logger.error("Error saving answers for session: {}", sessionId, e);
            clearPersistenceContext();
            throw new RuntimeException("Failed to save answers: " + e.getMessage(), e);
        }
    }

    /**
     * Enhanced method to ensure never returning null answers
     */
    public Map<String, String> getCurrentAnswers(String sessionId) {
        try {
            String safeSessionId = createSafeSessionId(sessionId);
            logger.debug("Getting current answers for session: {}", safeSessionId);

            Optional<Session> sessionOpt = sessionRepository.findBySessionId(safeSessionId);
            if (sessionOpt.isEmpty()) {
                logger.warn("Session not found when getting answers: {}, returning empty answers", safeSessionId);
                return new HashMap<>();
            }

            Session session = sessionOpt.get();
            if (!isValidSessionState(session)) {
                logger.error("Invalid session state when getting answers: {}", safeSessionId);
                return new HashMap<>();
            }

            List<Answer> answers = answerRepository.findBySessionOrderByCreatedAtAsc(session);

            Map<String, String> answerMap = answers.stream()
                    .collect(Collectors.toMap(
                            Answer::getQuestionKey,
                            Answer::getAnswerValue,
                            (oldValue, newValue) -> newValue
                    ));

            logger.debug("Found {} answers for session: {}", answerMap.size(), safeSessionId);
            return answerMap;

        } catch (Exception e) {
            logger.error("Error getting current answers for session: {}", sessionId, e);
            // Always return empty map instead of null
            return new HashMap<>();
        }
    }

    /**
     * Enhanced method to retrieve answers from storage with null safety
     */
    private Map<String, String> retrieveAnswersFromStorage(String sessionId) {
        try {
            // Your existing logic to retrieve answers from database or session storage
            // This should return either a Map of answers or empty map if none exist
            Map<String, String> answers = getCurrentAnswers(sessionId);
            return answers != null ? answers : new HashMap<>();
        } catch (Exception e) {
            logger.error("Error retrieving answers from storage for session: {}", sessionId, e);
            return new HashMap<>();
        }
    }

    @Transactional
    public Map<String, String> goBack(String sessionId) {
        try {
            String safeSessionId = createSafeSessionId(sessionId);
            logger.info("Going back for session: {}", safeSessionId);

            Optional<Session> sessionOpt = sessionRepository.findBySessionId(safeSessionId);
            if (sessionOpt.isEmpty()) {
                logger.warn("Session not found when going back: {}", safeSessionId);
                return new HashMap<>();
            }

            Session session = sessionOpt.get();
            if (!isValidSessionState(session)) {
                logger.error("Invalid session state when going back: {}", safeSessionId);
                return new HashMap<>();
            }

            Optional<Answer> lastAnswer = answerRepository.findTopBySessionOrderByCreatedAtDesc(session);

            if (lastAnswer.isPresent()) {
                Answer answerToRemove = lastAnswer.get();
                logger.info("Removing last answer: {} = {}", answerToRemove.getQuestionKey(), answerToRemove.getAnswerValue());

                answerRepository.delete(answerToRemove);
                answerRepository.flush(); // Force flush to ensure delete is processed

                List<Answer> remainingAnswers = answerRepository.findBySessionOrderByCreatedAtAsc(session);
                Map<String, String> remainingAnswerMap = remainingAnswers.stream()
                        .collect(Collectors.toMap(
                                Answer::getQuestionKey,
                                Answer::getAnswerValue
                        ));

                logger.info("After going back, {} answers remaining for session: {}", remainingAnswerMap.size(), safeSessionId);
                return remainingAnswerMap;
            }

            logger.info("No answers to remove for session: {}", safeSessionId);
            return new HashMap<>();

        } catch (Exception e) {
            logger.error("Error going back for session: {}", sessionId, e);
            clearPersistenceContext();
            return new HashMap<>();
        }
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

            // Convert String to List<String> for JSON storage
            List<String> recommendationsList = new ArrayList<>();
            if (recommendations != null && !recommendations.trim().isEmpty()) {
                // Split by semicolon (as used in DecisionService)
                recommendationsList = Arrays.asList(recommendations.split("; "));
            }
            assessment.setRecommendations(recommendationsList);

            riskAssessmentRepository.save(assessment);

            // Update session with risk score and status
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

    public Optional<Session> getSessionWithDetails(String sessionId) {
        try {
            String safeSessionId = createSafeSessionId(sessionId);
            // Get session with answers first
            Optional<Session> sessionWithAnswers = sessionRepository.findBySessionIdWithAnswers(safeSessionId);
            if (sessionWithAnswers.isPresent()) {
                Session session = sessionWithAnswers.get();
                // If you need risk assessments, fetch them separately
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

    public Optional<Session> getSessionWithAnswers(String sessionId) {
        try {
            String safeSessionId = createSafeSessionId(sessionId);
            return sessionRepository.findBySessionIdWithAnswers(safeSessionId);
        } catch (Exception e) {
            logger.error("Error getting session with answers: {}", sessionId, e);
            return Optional.empty();
        }
    }

    // Helper methods that throw exceptions for the ResultsController
    public RiskAssessment getLatestRiskAssessmentOrThrow(String sessionId) {
        String safeSessionId = createSafeSessionId(sessionId);
        return riskAssessmentRepository.findLatestBySessionId(safeSessionId)
                .orElseThrow(() -> new RuntimeException("No risk assessment found for session: " + safeSessionId));
    }

    public Session getSessionWithDetailsOrThrow(String sessionId) {
        String safeSessionId = createSafeSessionId(sessionId);
        return getSessionWithDetails(safeSessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + safeSessionId));
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

    public List<Answer> getSessionAnswers(String sessionId) {
        try {
            String safeSessionId = createSafeSessionId(sessionId);
            Optional<Session> sessionOpt = sessionRepository.findBySessionId(safeSessionId);
            if (sessionOpt.isPresent() && isValidSessionState(sessionOpt.get())) {
                return answerRepository.findBySessionOrderByCreatedAtAsc(sessionOpt.get());
            }
            return List.of();
        } catch (Exception e) {
            logger.error("Error getting session answers: {}", sessionId, e);
            return List.of();
        }
    }

    public boolean hasCompletedAssessment(String sessionId) {
        try {
            String safeSessionId = createSafeSessionId(sessionId);
            Optional<Session> sessionOpt = sessionRepository.findBySessionId(safeSessionId);
            return sessionOpt.map(session ->
                    "completed".equals(session.getStatus()) && isValidSessionState(session)
            ).orElse(false);
        } catch (Exception e) {
            logger.error("Error checking if session completed: {}", sessionId, e);
            return false;
        }
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
                        // Delete associated answers and risk assessments first
                        answerRepository.deleteBySession(session);
                        riskAssessmentRepository.deleteBySession_SessionId(session.getSessionId());

                        // Then delete the session
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

    // Validation helper methods
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

    private boolean saveOrUpdateAnswer(Session session, String questionKey, String answerValue) {
        try {
            Optional<Answer> existingAnswer = answerRepository.findBySessionAndQuestionKey(session, questionKey);

            if (existingAnswer.isPresent()) {
                Answer answer = existingAnswer.get();
                answer.setAnswerValue(answerValue);
                answerRepository.save(answer);
                logger.debug("Updated answer for question: {}", questionKey);
            } else {
                Answer answer = new Answer();
                answer.setQuestionKey(questionKey);
                answer.setAnswerValue(answerValue);
                answer.setSession(session);
                answerRepository.save(answer);
                logger.debug("Created new answer for question: {}", questionKey);
            }
            return true;
        } catch (Exception e) {
            logger.error("Error saving answer for question: {}", questionKey, e);
            return false;
        }
    }

    private boolean isValidAnswerParameter(String questionKey, String answerValue) {
        return !"sessionId".equals(questionKey) &&
                answerValue != null &&
                !answerValue.trim().isEmpty();
    }

    // Debug method to log session state
    public void logSessionState(String sessionId) {
        try {
            String safeSessionId = createSafeSessionId(sessionId);
            Optional<Session> sessionOpt = sessionRepository.findBySessionId(safeSessionId);
            if (sessionOpt.isPresent()) {
                Session session = sessionOpt.get();
                logger.info("Session State - SessionID: {}, Entity ID: {}, Status: {}, Answers: {}, RiskAssessments: {}",
                        session.getSessionId(),
                        session.getId(),
                        session.getStatus(),
                        session.getAnswers() != null ? session.getAnswers().size() : 0,
                        session.getRiskAssessments() != null ? session.getRiskAssessments().size() : 0);
            } else {
                logger.info("Session not found: {}", safeSessionId);
            }
        } catch (Exception e) {
            logger.error("Error logging session state: {}", sessionId, e);
        }
    }

    public void debugSessionState(String sessionId) {
        try {
            String cleanSessionId = cleanSessionId(sessionId);
            Optional<Session> sessionOpt = sessionRepository.findBySessionId(cleanSessionId);

            if (sessionOpt.isPresent()) {
                Session session = sessionOpt.get();
                List<Answer> answers = answerRepository.findBySessionOrderByCreatedAtAsc(session);

                logger.info("=== SESSION DEBUG ===");
                logger.info("Session ID: {}", session.getSessionId());
                logger.info("Entity ID: {}", session.getId());
                logger.info("Status: {}", session.getStatus());
                logger.info("Answer Count: {}", answers.size());
                logger.info("Answers: {}", answers.stream()
                        .map(a -> a.getQuestionKey() + "=" + a.getAnswerValue())
                        .collect(Collectors.joining(", ")));
            } else {
                logger.warn("Session not found: {}", cleanSessionId);
            }
        } catch (Exception e) {
            logger.error("Error debugging session: {}", sessionId, e);
        }
    }

    // Statistics methods
    public Map<String, Object> getSessionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        try {
            stats.put("totalSessions", sessionRepository.count());
            stats.put("activeSessions", sessionRepository.countByStatus("active"));
            stats.put("completedSessions", sessionRepository.countByStatus("completed"));
            stats.put("totalRiskAssessments", riskAssessmentRepository.count());
        } catch (Exception e) {
            logger.error("Error getting session statistics", e);
        }
        return stats;
    }
}