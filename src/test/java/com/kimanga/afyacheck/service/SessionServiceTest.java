package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.model.Answer;
import com.kimanga.afyacheck.model.RiskAssessment;
import com.kimanga.afyacheck.model.Session;
import com.kimanga.afyacheck.repository.AnswerRepository;
import com.kimanga.afyacheck.repository.RiskAssessmentRepository;
import com.kimanga.afyacheck.repository.SessionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SessionServiceTest {

    private SessionRepository sessionRepository;
    private AnswerRepository answerRepository;
    private RiskAssessmentRepository riskAssessmentRepository;
    private EntityManager entityManager;
    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(SessionRepository.class);
        answerRepository = mock(AnswerRepository.class);
        riskAssessmentRepository = mock(RiskAssessmentRepository.class);
        entityManager = mock(EntityManager.class);
        when(entityManager.isOpen()).thenReturn(true);

        sessionService = new SessionService(sessionRepository, answerRepository, riskAssessmentRepository);
        ReflectionTestUtils.setField(sessionService, "entityManager", entityManager);
    }

    private Session validSession(String sessionId) {
        Session session = new Session();
        session.setId(1L);
        session.setSessionId(sessionId);
        session.setStatus("active");
        return session;
    }

    @Test
    void cleanSessionIdReturnsNullOrBlankUnchanged() {
        assertThat(sessionService.cleanSessionId(null)).isNull();
        assertThat(sessionService.cleanSessionId("  ")).isEqualTo("  ");
    }

    @Test
    void cleanSessionIdTrimsSimpleId() {
        assertThat(sessionService.cleanSessionId(" abc ")).isEqualTo("abc");
    }

    @Test
    void cleanSessionIdPicksValidCommaSeparatedPart() {
        when(sessionRepository.findBySessionId("valid")).thenReturn(Optional.of(validSession("valid")));
        when(sessionRepository.findBySessionId("other")).thenReturn(Optional.empty());

        String result = sessionService.cleanSessionId("other,valid");

        assertThat(result).isEqualTo("valid");
    }

    @Test
    void cleanSessionIdFallsBackToFirstPartWhenNoneValid() {
        when(sessionRepository.findBySessionId(anyString())).thenReturn(Optional.empty());
        String result = sessionService.cleanSessionId("first,second");
        assertThat(result).isEqualTo("first");
    }

    @Test
    void createOrGetSessionReturnsExistingSessionId() {
        Session existing = validSession("sid-1");
        when(sessionRepository.findBySessionId("sid-1")).thenReturn(Optional.of(existing));

        String result = sessionService.createOrGetSession("sid-1");

        assertThat(result).isEqualTo("sid-1");
    }

    @Test
    void createOrGetSessionCreatesNewWhenMissing() {
        when(sessionRepository.findBySessionId("sid-2")).thenReturn(Optional.empty());
        Session saved = validSession("sid-2");
        when(sessionRepository.saveAndFlush(any(Session.class))).thenReturn(saved);

        String result = sessionService.createOrGetSession("sid-2");

        assertThat(result).isEqualTo("sid-2");
        verify(sessionRepository).saveAndFlush(any(Session.class));
    }

    @Test
    void createOrGetSessionRecoversFromException() {
        when(sessionRepository.findBySessionId(anyString())).thenThrow(new RuntimeException("db down"));

        String result = sessionService.createOrGetSession("sid-3");

        assertThat(result).isNotNull();
        verify(entityManager).clear();
    }

    @Test
    void createNewSessionFallsBackToUuidOnFailure() {
        when(sessionRepository.saveAndFlush(any(Session.class)))
                .thenThrow(new RuntimeException("fail once"))
                .thenReturn(validSession("fallback-id"));

        Session result = sessionService.createNewSession("sid-4");

        assertThat(result.getSessionId()).isEqualTo("fallback-id");
        verify(sessionRepository, times(2)).saveAndFlush(any(Session.class));
    }

    @Test
    void createNewSessionThrowsWhenFallbackAlsoFails() {
        when(sessionRepository.saveAndFlush(any(Session.class)))
                .thenThrow(new RuntimeException("fail 1"))
                .thenThrow(new RuntimeException("fail 2"));

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> sessionService.createNewSession("sid-5"));
    }

    @Test
    void saveAnswersCreatesSessionWhenMissingAndSavesValidAnswers() {
        when(sessionRepository.findBySessionId("sid-6")).thenReturn(Optional.empty());
        Session created = validSession("sid-6");
        when(sessionRepository.saveAndFlush(any(Session.class))).thenReturn(created);
        when(answerRepository.findBySessionAndQuestionKey(any(), anyString())).thenReturn(Optional.empty());

        Map<String, String> params = new LinkedHashMap<>();
        params.put("q1", "yes");
        params.put("sessionId", "sid-6"); // should be skipped
        params.put("q2", "");             // blank, should be skipped

        sessionService.saveAnswers("sid-6", params);

        verify(answerRepository, times(1)).save(any(Answer.class));
    }

    @Test
    void saveAnswersUpdatesExistingAnswer() {
        Session session = validSession("sid-7");
        when(sessionRepository.findBySessionId("sid-7")).thenReturn(Optional.of(session));
        Answer existing = new Answer();
        existing.setQuestionKey("q1");
        existing.setSession(session);
        when(answerRepository.findBySessionAndQuestionKey(session, "q1")).thenReturn(Optional.of(existing));

        sessionService.saveAnswers("sid-7", Map.of("q1", "updated"));

        assertThat(existing.getAnswerValue()).isEqualTo("updated");
        verify(answerRepository).save(existing);
    }

    @Test
    void saveAnswersThrowsWrappedExceptionOnFailure() {
        when(sessionRepository.findBySessionId(anyString())).thenThrow(new RuntimeException("boom"));

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> sessionService.saveAnswers("sid-8", Map.of("q1", "a")));
        verify(entityManager, atLeastOnce()).clear();
    }

    @Test
    void getCurrentAnswersReturnsEmptyWhenSessionMissing() {
        when(sessionRepository.findBySessionId("sid-9")).thenReturn(Optional.empty());
        assertThat(sessionService.getCurrentAnswers("sid-9")).isEmpty();
    }

    @Test
    void getCurrentAnswersReturnsMappedAnswers() {
        Session session = validSession("sid-10");
        when(sessionRepository.findBySessionId("sid-10")).thenReturn(Optional.of(session));
        Answer a1 = new Answer();
        a1.setQuestionKey("q1");
        a1.setAnswerValue("yes");
        when(answerRepository.findBySessionOrderByCreatedAtAsc(session)).thenReturn(List.of(a1));

        Map<String, String> result = sessionService.getCurrentAnswers("sid-10");

        assertThat(result).containsEntry("q1", "yes");
    }

    @Test
    void getCurrentAnswersReturnsEmptyOnException() {
        when(sessionRepository.findBySessionId(anyString())).thenThrow(new RuntimeException("boom"));
        assertThat(sessionService.getCurrentAnswers("sid-11")).isEmpty();
    }

    @Test
    void goBackReturnsEmptyWhenSessionMissing() {
        when(sessionRepository.findBySessionId("sid-12")).thenReturn(Optional.empty());
        assertThat(sessionService.goBack("sid-12")).isEmpty();
    }

    @Test
    void goBackRemovesLastAnswerAndReturnsRemaining() {
        Session session = validSession("sid-13");
        when(sessionRepository.findBySessionId("sid-13")).thenReturn(Optional.of(session));
        Answer last = new Answer();
        last.setQuestionKey("q2");
        last.setAnswerValue("no");
        when(answerRepository.findTopBySessionOrderByCreatedAtDesc(session)).thenReturn(Optional.of(last));
        Answer remaining = new Answer();
        remaining.setQuestionKey("q1");
        remaining.setAnswerValue("yes");
        when(answerRepository.findBySessionOrderByCreatedAtAsc(session)).thenReturn(List.of(remaining));

        Map<String, String> result = sessionService.goBack("sid-13");

        verify(answerRepository).delete(last);
        assertThat(result).containsEntry("q1", "yes");
    }

    @Test
    void goBackReturnsEmptyWhenNoAnswersToRemove() {
        Session session = validSession("sid-14");
        when(sessionRepository.findBySessionId("sid-14")).thenReturn(Optional.of(session));
        when(answerRepository.findTopBySessionOrderByCreatedAtDesc(session)).thenReturn(Optional.empty());

        assertThat(sessionService.goBack("sid-14")).isEmpty();
    }

    @Test
    void saveRiskAssessmentThrowsWhenSessionMissing() {
        when(sessionRepository.findBySessionId("sid-15")).thenReturn(Optional.empty());
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> sessionService.saveRiskAssessment("sid-15", "LOW", 1, "rec1; rec2", "test-model-v1"));
    }

    @Test
    void saveRiskAssessmentPersistsAssessmentAndUpdatesSession() {
        Session session = validSession("sid-16");
        when(sessionRepository.findBySessionId("sid-16")).thenReturn(Optional.of(session));

        sessionService.saveRiskAssessment("sid-16", "HIGH", 9, "rec1; rec2", "test-model-v1");

        ArgumentCaptor<RiskAssessment> captor = ArgumentCaptor.forClass(RiskAssessment.class);
        verify(riskAssessmentRepository).save(captor.capture());
        assertThat(captor.getValue().getRecommendations()).containsExactly("rec1", "rec2");
        assertThat(captor.getValue().getModelVersion()).isEqualTo("test-model-v1");
        assertThat(session.getStatus()).isEqualTo("completed");
        assertThat(session.getRiskScore()).isEqualTo(9);
        verify(sessionRepository).save(session);
    }

    @Test
    void deleteSessionDataReturnsFalseWhenSessionNotFound() {
        when(sessionRepository.findBySessionId("sid-missing")).thenReturn(Optional.empty());

        boolean result = sessionService.deleteSessionData("sid-missing");

        assertThat(result).isFalse();
        verify(answerRepository, never()).deleteBySession(any());
    }

    @Test
    void deleteSessionDataDeletesAnswersAssessmentsAndSession() {
        Session session = validSession("sid-18");
        when(sessionRepository.findBySessionId("sid-18")).thenReturn(Optional.of(session));

        boolean result = sessionService.deleteSessionData("sid-18");

        assertThat(result).isTrue();
        verify(answerRepository).deleteBySession(session);
        verify(riskAssessmentRepository).deleteBySession_SessionId("sid-18");
        verify(sessionRepository).delete(session);
    }

    @Test
    void getLatestRiskAssessmentDelegatesToRepository() {
        RiskAssessment ra = new RiskAssessment();
        when(riskAssessmentRepository.findLatestBySessionId("sid-17")).thenReturn(Optional.of(ra));
        assertThat(sessionService.getLatestRiskAssessment("sid-17")).contains(ra);
    }

    @Test
    void getLatestRiskAssessmentOrThrowThrowsWhenMissing() {
        when(riskAssessmentRepository.findLatestBySessionId("sid-18")).thenReturn(Optional.empty());
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> sessionService.getLatestRiskAssessmentOrThrow("sid-18"));
    }

    @Test
    void getSessionWithDetailsReturnsSessionWithRiskAssessments() {
        Session session = validSession("sid-19");
        when(sessionRepository.findBySessionIdWithAnswers("sid-19")).thenReturn(Optional.of(session));
        when(riskAssessmentRepository.findBySessionIdOrderByCreatedAtDesc("sid-19")).thenReturn(List.of(new RiskAssessment()));

        Optional<Session> result = sessionService.getSessionWithDetails("sid-19");

        assertThat(result).isPresent();
        assertThat(result.get().getRiskAssessments()).hasSize(1);
    }

    @Test
    void getSessionWithDetailsOrThrowThrowsWhenMissing() {
        when(sessionRepository.findBySessionIdWithAnswers("sid-20")).thenReturn(Optional.empty());
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> sessionService.getSessionWithDetailsOrThrow("sid-20"));
    }

    @Test
    void completeSessionMarksStatusCompleted() {
        Session session = validSession("sid-21");
        when(sessionRepository.findBySessionId("sid-21")).thenReturn(Optional.of(session));

        sessionService.completeSession("sid-21");

        assertThat(session.getStatus()).isEqualTo("completed");
        verify(sessionRepository).save(session);
    }

    @Test
    void completeSessionNoOpWhenSessionMissing() {
        when(sessionRepository.findBySessionId("sid-22")).thenReturn(Optional.empty());
        sessionService.completeSession("sid-22");
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void getSessionAnswersReturnsEmptyWhenSessionMissing() {
        when(sessionRepository.findBySessionId("sid-23")).thenReturn(Optional.empty());
        assertThat(sessionService.getSessionAnswers("sid-23")).isEmpty();
    }

    @Test
    void getSessionAnswersReturnsAnswersWhenSessionValid() {
        Session session = validSession("sid-24");
        when(sessionRepository.findBySessionId("sid-24")).thenReturn(Optional.of(session));
        when(answerRepository.findBySessionOrderByCreatedAtAsc(session)).thenReturn(List.of(new Answer()));

        assertThat(sessionService.getSessionAnswers("sid-24")).hasSize(1);
    }

    @Test
    void hasCompletedAssessmentTrueWhenStatusCompleted() {
        Session session = validSession("sid-25");
        session.setStatus("completed");
        when(sessionRepository.findBySessionId("sid-25")).thenReturn(Optional.of(session));

        assertThat(sessionService.hasCompletedAssessment("sid-25")).isTrue();
    }

    @Test
    void hasCompletedAssessmentFalseWhenSessionMissing() {
        when(sessionRepository.findBySessionId("sid-26")).thenReturn(Optional.empty());
        assertThat(sessionService.hasCompletedAssessment("sid-26")).isFalse();
    }

    @Test
    void cleanupOldSessionsDeletesValidStaleSessions() {
        Session stale = validSession("sid-27");
        when(sessionRepository.findByCreatedAtBeforeAndStatus(any(), eq("active"))).thenReturn(List.of(stale));

        sessionService.cleanupOldSessions();

        verify(answerRepository).deleteBySession(stale);
        verify(riskAssessmentRepository).deleteBySession_SessionId("sid-27");
        verify(sessionRepository).delete(stale);
    }

    @Test
    void cleanupOldSessionsSwallowsExceptionOnPerSessionFailure() {
        Session stale = validSession("sid-28");
        when(sessionRepository.findByCreatedAtBeforeAndStatus(any(), eq("active"))).thenReturn(List.of(stale));
        doThrow(new RuntimeException("delete failed")).when(answerRepository).deleteBySession(stale);

        sessionService.cleanupOldSessions();

        verify(sessionRepository, never()).delete(stale);
    }

    @Test
    void sessionExistsTrueForValidSession() {
        when(sessionRepository.findBySessionId("sid-29")).thenReturn(Optional.of(validSession("sid-29")));
        assertThat(sessionService.sessionExists("sid-29")).isTrue();
    }

    @Test
    void sessionExistsFalseWhenMissing() {
        when(sessionRepository.findBySessionId("sid-30")).thenReturn(Optional.empty());
        assertThat(sessionService.sessionExists("sid-30")).isFalse();
    }

    @Test
    void getSessionReturnsEmptyWhenInvalidState() {
        Session invalid = new Session(); // no id, no sessionId set properly -> invalid state
        when(sessionRepository.findBySessionId("sid-31")).thenReturn(Optional.of(invalid));
        assertThat(sessionService.getSession("sid-31")).isEmpty();
    }

    @Test
    void getSessionReturnsSessionWhenValid() {
        Session session = validSession("sid-32");
        when(sessionRepository.findBySessionId("sid-32")).thenReturn(Optional.of(session));
        assertThat(sessionService.getSession("sid-32")).contains(session);
    }

    @Test
    void logSessionStateHandlesPresentAndMissingSessions() {
        when(sessionRepository.findBySessionId("sid-33")).thenReturn(Optional.of(validSession("sid-33")));
        sessionService.logSessionState("sid-33");

        when(sessionRepository.findBySessionId("sid-34")).thenReturn(Optional.empty());
        sessionService.logSessionState("sid-34");
    }

    @Test
    void debugSessionStateHandlesPresentSession() {
        Session session = validSession("sid-35");
        when(sessionRepository.findBySessionId("sid-35")).thenReturn(Optional.of(session));
        when(answerRepository.findBySessionOrderByCreatedAtAsc(session)).thenReturn(List.of());

        sessionService.debugSessionState("sid-35");
    }

    @Test
    void getSessionStatisticsAggregatesCounts() {
        when(sessionRepository.count()).thenReturn(10L);
        when(sessionRepository.countByStatus("active")).thenReturn(4L);
        when(sessionRepository.countByStatus("completed")).thenReturn(6L);
        when(riskAssessmentRepository.count()).thenReturn(6L);

        Map<String, Object> stats = sessionService.getSessionStatistics();

        assertThat(stats).containsEntry("totalSessions", 10L);
        assertThat(stats).containsEntry("activeSessions", 4L);
        assertThat(stats).containsEntry("completedSessions", 6L);
        assertThat(stats).containsEntry("totalRiskAssessments", 6L);
    }

    @Test
    void getSessionStatisticsReturnsPartialMapOnException() {
        when(sessionRepository.count()).thenThrow(new RuntimeException("boom"));
        Map<String, Object> stats = sessionService.getSessionStatistics();
        assertThat(stats).isEmpty();
    }

    // --- Additional branch coverage ---

    @Test
    void createSafeSessionIdReturnsUuidWhenInputNull() {
        String result = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                sessionService, "createSafeSessionId", (Object) null);
        assertThat(result).isNotBlank();
    }

    @Test
    void createSafeSessionIdHashesVeryLongSessionId() {
        String longId = "x".repeat(600);
        String result = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                sessionService, "createSafeSessionId", longId);
        assertThat(result).startsWith("sess_");
    }

    @Test
    void createOrGetSessionRecreatesWhenExistingSessionInvalid() {
        Session invalid = new Session();
        invalid.setSessionId("sid-x1");
        // no id set -> isValidSessionState() is false
        when(sessionRepository.findBySessionId("sid-x1")).thenReturn(Optional.of(invalid));
        when(sessionRepository.saveAndFlush(any(Session.class))).thenReturn(validSession("sid-x1"));

        String result = sessionService.createOrGetSession("sid-x1");

        assertThat(result).isEqualTo("sid-x1");
        verify(sessionRepository).saveAndFlush(any(Session.class));
    }

    @Test
    void saveAnswersRecreatesSessionWhenExistingStateInvalid() {
        Session invalid = new Session();
        invalid.setSessionId("sid-x2");
        when(sessionRepository.findBySessionId("sid-x2")).thenReturn(Optional.of(invalid));
        when(sessionRepository.saveAndFlush(any(Session.class))).thenReturn(validSession("sid-x2"));

        sessionService.saveAnswers("sid-x2", Map.of());

        verify(sessionRepository).saveAndFlush(any(Session.class));
    }

    @Test
    void saveAnswersReflushesSessionWhenIdStillNullAfterCreation() {
        when(sessionRepository.findBySessionId("sid-x3")).thenReturn(Optional.empty());
        Session stillNoId = new Session();
        stillNoId.setSessionId("sid-x3");
        Session withId = validSession("sid-x3");
        when(sessionRepository.saveAndFlush(any(Session.class)))
                .thenReturn(stillNoId)
                .thenReturn(withId);

        sessionService.saveAnswers("sid-x3", Map.of());

        verify(sessionRepository, times(2)).saveAndFlush(any(Session.class));
    }

    @Test
    void getCurrentAnswersReturnsEmptyWhenSessionStateInvalid() {
        Session invalid = new Session();
        invalid.setSessionId("sid-x4");
        when(sessionRepository.findBySessionId("sid-x4")).thenReturn(Optional.of(invalid));

        assertThat(sessionService.getCurrentAnswers("sid-x4")).isEmpty();
    }

    @Test
    void retrieveAnswersFromStorageDelegatesToGetCurrentAnswers() {
        when(sessionRepository.findBySessionId("sid-x5")).thenReturn(Optional.empty());

        Map<String, String> result = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                sessionService, "retrieveAnswersFromStorage", "sid-x5");

        assertThat(result).isEmpty();
    }

    @Test
    void goBackReturnsEmptyWhenSessionStateInvalid() {
        Session invalid = new Session();
        invalid.setSessionId("sid-x6");
        when(sessionRepository.findBySessionId("sid-x6")).thenReturn(Optional.of(invalid));

        assertThat(sessionService.goBack("sid-x6")).isEmpty();
    }

    @Test
    void goBackReturnsEmptyOnException() {
        when(sessionRepository.findBySessionId("sid-x7")).thenThrow(new RuntimeException("boom"));
        assertThat(sessionService.goBack("sid-x7")).isEmpty();
        verify(entityManager, atLeastOnce()).clear();
    }

    @Test
    void saveRiskAssessmentThrowsWhenSessionStateInvalid() {
        Session invalid = new Session();
        invalid.setSessionId("sid-x8");
        when(sessionRepository.findBySessionId("sid-x8")).thenReturn(Optional.of(invalid));

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> sessionService.saveRiskAssessment("sid-x8", "LOW", 1, null, null));
    }

    @Test
    void getLatestRiskAssessmentReturnsEmptyOnException() {
        when(riskAssessmentRepository.findLatestBySessionId(anyString())).thenThrow(new RuntimeException("boom"));
        assertThat(sessionService.getLatestRiskAssessment("sid-x9")).isEmpty();
    }

    @Test
    void getSessionWithDetailsReturnsEmptyOnException() {
        when(sessionRepository.findBySessionIdWithAnswers(anyString())).thenThrow(new RuntimeException("boom"));
        assertThat(sessionService.getSessionWithDetails("sid-x10")).isEmpty();
    }

    @Test
    void getSessionWithAnswersReturnsEmptyOnException() {
        when(sessionRepository.findBySessionIdWithAnswers(anyString())).thenThrow(new RuntimeException("boom"));
        assertThat(sessionService.getSessionWithAnswers("sid-x11")).isEmpty();
    }

    @Test
    void completeSessionLogsErrorWhenStateInvalid() {
        Session invalid = new Session();
        invalid.setSessionId("sid-x12");
        when(sessionRepository.findBySessionId("sid-x12")).thenReturn(Optional.of(invalid));

        sessionService.completeSession("sid-x12");

        verify(sessionRepository, never()).save(any());
    }

    @Test
    void completeSessionCatchesException() {
        when(sessionRepository.findBySessionId("sid-x13")).thenThrow(new RuntimeException("boom"));
        sessionService.completeSession("sid-x13");
        verify(entityManager, atLeastOnce()).clear();
    }

    @Test
    void getSessionAnswersReturnsEmptyOnException() {
        when(sessionRepository.findBySessionId("sid-x14")).thenThrow(new RuntimeException("boom"));
        assertThat(sessionService.getSessionAnswers("sid-x14")).isEmpty();
    }

    @Test
    void hasCompletedAssessmentReturnsFalseOnException() {
        when(sessionRepository.findBySessionId("sid-x15")).thenThrow(new RuntimeException("boom"));
        assertThat(sessionService.hasCompletedAssessment("sid-x15")).isFalse();
    }

    @Test
    void cleanupOldSessionsCatchesOuterException() {
        when(sessionRepository.findByCreatedAtBeforeAndStatus(any(), any())).thenThrow(new RuntimeException("boom"));
        sessionService.cleanupOldSessions();
        verify(entityManager, atLeastOnce()).clear();
    }

    @Test
    void sessionExistsReturnsFalseOnException() {
        when(sessionRepository.findBySessionId("sid-x16")).thenThrow(new RuntimeException("boom"));
        assertThat(sessionService.sessionExists("sid-x16")).isFalse();
    }

    @Test
    void getSessionReturnsEmptyOnException() {
        when(sessionRepository.findBySessionId("sid-x17")).thenThrow(new RuntimeException("boom"));
        assertThat(sessionService.getSession("sid-x17")).isEmpty();
    }

    @Test
    void clearPersistenceContextSwallowsExceptionFromEntityManager() {
        doThrow(new RuntimeException("clear failed")).when(entityManager).clear();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(sessionService, "clearPersistenceContext");
    }

    @Test
    void saveOrUpdateAnswerReturnsFalseOnException() {
        Session session = validSession("sid-x18");
        when(answerRepository.findBySessionAndQuestionKey(any(), anyString())).thenThrow(new RuntimeException("boom"));

        boolean result = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                sessionService, "saveOrUpdateAnswer", session, "q1", "a1");

        assertThat(result).isFalse();
    }

    @Test
    void logSessionStateCatchesException() {
        when(sessionRepository.findBySessionId("sid-x19")).thenThrow(new RuntimeException("boom"));
        sessionService.logSessionState("sid-x19");
    }

    @Test
    void debugSessionStateLogsNotFound() {
        when(sessionRepository.findBySessionId("sid-x20")).thenReturn(Optional.empty());
        sessionService.debugSessionState("sid-x20");
    }

    @Test
    void debugSessionStateCatchesException() {
        when(sessionRepository.findBySessionId("sid-x21")).thenThrow(new RuntimeException("boom"));
        sessionService.debugSessionState("sid-x21");
    }
}
