package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.model.Answer;
import com.kimanga.afyacheck.model.Session;
import com.kimanga.afyacheck.repository.AnswerRepository;
import com.kimanga.afyacheck.repository.SessionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Persists and retrieves questionnaire answers for a session. Split out of
 * SessionService, which now delegates saveAnswers/getCurrentAnswers/goBack
 * here so every existing caller keeps working unchanged.
 */
@Service
public class AnswerService {

    private static final Logger logger = LoggerFactory.getLogger(AnswerService.class);

    private final SessionRepository sessionRepository;
    private final AnswerRepository answerRepository;
    private final SessionIdSanitizer sessionIdSanitizer;

    @PersistenceContext
    private EntityManager entityManager;

    public AnswerService(SessionRepository sessionRepository,
                          AnswerRepository answerRepository,
                          SessionIdSanitizer sessionIdSanitizer) {
        this.sessionRepository = sessionRepository;
        this.answerRepository = answerRepository;
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
    public void saveAnswers(String sessionId, Map<String, String> formParams) {
        Session session = null;
        try {
            String safeSessionId = createSafeSessionId(sessionId);
            logger.info("Saving answers for session: {}, answers: {}", safeSessionId, formParams.keySet());

            session = getOrCreateValidSession(safeSessionId);

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
     * Finds the session, recreating it if missing or in an invalid state,
     * matching the auto-create behavior saveAnswers has always had.
     */
    private Session getOrCreateValidSession(String safeSessionId) {
        Optional<Session> sessionOpt = sessionRepository.findBySessionId(safeSessionId);
        Session session;
        if (sessionOpt.isEmpty()) {
            logger.warn("Session not found when saving answers: {}, creating new session", safeSessionId);
            session = createFallbackSession(safeSessionId);
        } else {
            session = sessionOpt.get();
            if (!isValidSessionState(session)) {
                logger.error("Invalid session state for: {}, recreating session", safeSessionId);
                clearPersistenceContext();
                session = createFallbackSession(safeSessionId);
            }
        }

        if (session.getId() == null) {
            logger.error("Session still has null ID after creation: {}", safeSessionId);
            session = sessionRepository.saveAndFlush(session);
        }
        return session;
    }

    private Session createFallbackSession(String sessionId) {
        try {
            Session newSession = new Session();
            newSession.setSessionId(sessionId);
            newSession.setStatus("active");
            return sessionRepository.saveAndFlush(newSession);
        } catch (Exception e) {
            logger.error("Error creating new session: {}", sessionId, e);
            clearPersistenceContext();

            String fallbackId = java.util.UUID.randomUUID().toString();
            Session fallbackSession = new Session();
            fallbackSession.setSessionId(fallbackId);
            fallbackSession.setStatus("active");

            Session savedSession = sessionRepository.saveAndFlush(fallbackSession);
            logger.info("Created fallback UUID session: {} for original: {}", fallbackId, sessionId);
            return savedSession;
        }
    }

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

            java.util.List<Answer> answers = answerRepository.findBySessionOrderByCreatedAtAsc(session);

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
                answerRepository.flush();

                java.util.List<Answer> remainingAnswers = answerRepository.findBySessionOrderByCreatedAtAsc(session);
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
