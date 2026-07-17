package com.kimanga.afyacheck.controllers;

import com.kimanga.afyacheck.service.DecisionService;
import com.kimanga.afyacheck.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class QuestionControllerTest {

    private final DecisionService decisionService = mock(DecisionService.class);
    private final SessionService sessionService = mock(SessionService.class);
    private final QuestionController controller = new QuestionController(decisionService, sessionService);

    private HttpSession httpSession(String id) {
        HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn(id);
        return session;
    }

    @Test
    void startReturnsFirstQuestion() {
        when(sessionService.createOrGetSession("http-sid")).thenReturn("app-sid");
        Map<String, Object> question = new HashMap<>();
        question.put("key", "consent");
        question.put("options", "Yes,No");
        when(decisionService.getNextQuestion(any())).thenReturn(question);

        ResponseEntity<Map<String, Object>> response = controller.start(httpSession("http-sid"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("sessionId", "app-sid").containsEntry("canGoBack", false);
        @SuppressWarnings("unchecked")
        Map<String, Object> q = (Map<String, Object>) response.getBody().get("question");
        assertThat(q.get("options")).isEqualTo(List.of("Yes", "No"));
    }

    @Test
    void startReturns500WhenDecisionServiceReportsError() {
        when(sessionService.createOrGetSession("http-sid2")).thenReturn("app-sid2");
        Map<String, Object> question = new HashMap<>();
        question.put("error", "something failed");
        when(decisionService.getNextQuestion(any())).thenReturn(question);

        ResponseEntity<Map<String, Object>> response = controller.start(httpSession("http-sid2"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("error", "something failed");
    }

    @Test
    void startReturns500OnException() {
        when(sessionService.createOrGetSession(anyString())).thenThrow(new RuntimeException("boom"));

        ResponseEntity<Map<String, Object>> response = controller.start(httpSession("http-sid4"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void nextReturnsQuestionWhenNotEnded() {
        when(sessionService.getCurrentAnswers("sid-1")).thenReturn(new HashMap<>());
        Map<String, Object> nextStep = new HashMap<>();
        nextStep.put("key", "q2");
        nextStep.put("options", "A,B");
        when(decisionService.getNextQuestion(any())).thenReturn(nextStep);

        ResponseEntity<Map<String, Object>> response =
                controller.next(new QuestionController.NextRequest("sid-1", Map.of("q1", "answer")));

        assertThat(response.getBody()).containsEntry("canGoBack", true);
    }

    @Test
    void nextHandlesNullAnswersFromSessionService() {
        when(sessionService.getCurrentAnswers("sid-1b")).thenReturn(null);
        Map<String, Object> nextStep = new HashMap<>();
        nextStep.put("key", "q2");
        when(decisionService.getNextQuestion(any())).thenReturn(nextStep);

        ResponseEntity<Map<String, Object>> response =
                controller.next(new QuestionController.NextRequest("sid-1b", Map.of("q1", "answer")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void nextHandlesConsentDenied() {
        when(sessionService.getCurrentAnswers("sid-2")).thenReturn(new HashMap<>());
        Map<String, Object> nextStep = new HashMap<>();
        nextStep.put("end", true);
        nextStep.put("consentDenied", true);
        nextStep.put("text", "denied");
        when(decisionService.getNextQuestion(any())).thenReturn(nextStep);

        ResponseEntity<Map<String, Object>> response =
                controller.next(new QuestionController.NextRequest("sid-2", Map.of()));

        assertThat(response.getBody()).containsEntry("consentDenied", true);
        verify(sessionService).completeSession("sid-2");
    }

    @Test
    void nextSavesRiskAssessmentWhenSurveyEndsWithScore() {
        when(sessionService.getCurrentAnswers("sid-3")).thenReturn(new HashMap<>());
        Map<String, Object> nextStep = new HashMap<>();
        nextStep.put("end", true);
        nextStep.put("riskScore", 80);
        nextStep.put("riskLevel", "High");
        nextStep.put("recommendations", "Rec1; Rec2");
        nextStep.put("text", "done");
        when(decisionService.getNextQuestion(any())).thenReturn(nextStep);

        ResponseEntity<Map<String, Object>> response =
                controller.next(new QuestionController.NextRequest("sid-3", Map.of()));

        assertThat(response.getBody()).containsEntry("riskScore", 80);
        verify(sessionService).saveRiskAssessment("sid-3", "High", 80, "Rec1; Rec2");
    }

    @Test
    void nextFallsBackToCalculateRiskScoreWhenNoRiskScoreInResult() {
        when(sessionService.getCurrentAnswers("sid-4")).thenReturn(new HashMap<>());
        Map<String, Object> nextStep = new HashMap<>();
        nextStep.put("end", true);
        when(decisionService.getNextQuestion(any())).thenReturn(nextStep);

        Map<String, Object> fallbackAssessment = new HashMap<>();
        fallbackAssessment.put("riskScore", 40);
        fallbackAssessment.put("riskLevel", "Medium");
        fallbackAssessment.put("recommendations", "RecA; RecB");
        when(decisionService.calculateRiskScore(any())).thenReturn(fallbackAssessment);

        ResponseEntity<Map<String, Object>> response =
                controller.next(new QuestionController.NextRequest("sid-4", Map.of()));

        assertThat(response.getBody()).containsEntry("riskScore", 40);
        verify(sessionService).saveRiskAssessment("sid-4", "Medium", 40, "RecA; RecB");
    }

    @Test
    void nextReturns500OnException() {
        when(sessionService.getCurrentAnswers(anyString())).thenThrow(new RuntimeException("boom"));

        ResponseEntity<Map<String, Object>> response =
                controller.next(new QuestionController.NextRequest("sid-5", Map.of()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void backReturnsPreviousQuestion() {
        Map<String, String> remaining = new HashMap<>();
        remaining.put("q1", "yes");
        when(sessionService.goBack("sid-6")).thenReturn(remaining);
        Map<String, Object> question = new HashMap<>();
        question.put("key", "q1");
        when(decisionService.getNextQuestion(any())).thenReturn(question);

        ResponseEntity<Map<String, Object>> response = controller.back(new QuestionController.BackRequest("sid-6"));

        assertThat(response.getBody()).containsEntry("canGoBack", true);
    }

    @Test
    void backConvertsOptionsStringToList() {
        when(sessionService.goBack("sid-6b")).thenReturn(new HashMap<>());
        Map<String, Object> question = new HashMap<>();
        question.put("key", "q1");
        question.put("options", "X,Y,Z");
        when(decisionService.getNextQuestion(any())).thenReturn(question);

        ResponseEntity<Map<String, Object>> response = controller.back(new QuestionController.BackRequest("sid-6b"));

        @SuppressWarnings("unchecked")
        Map<String, Object> q = (Map<String, Object>) response.getBody().get("question");
        assertThat(q.get("options")).isEqualTo(List.of("X", "Y", "Z"));
    }

    @Test
    void backHandlesNullAnswersFromGoBack() {
        when(sessionService.goBack("sid-7")).thenReturn(null);
        when(decisionService.getNextQuestion(any())).thenReturn(new HashMap<>(Map.of("key", "consent")));

        ResponseEntity<Map<String, Object>> response = controller.back(new QuestionController.BackRequest("sid-7"));

        // The controller unconditionally adds "_sessionId" to previousAnswers before
        // checking isEmpty(), so canGoBack is always true even with no real answers.
        assertThat(response.getBody()).containsEntry("canGoBack", true);
    }

    @Test
    void backReturns500OnException() {
        when(sessionService.goBack(anyString())).thenThrow(new RuntimeException("boom"));

        ResponseEntity<Map<String, Object>> response = controller.back(new QuestionController.BackRequest("sid-8"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void cleanSessionIdPicksValidPartFromCommaSeparatedList() {
        when(sessionService.sessionExists("valid")).thenReturn(true);
        when(sessionService.sessionExists("other")).thenReturn(false);
        when(sessionService.getCurrentAnswers("valid")).thenReturn(new HashMap<>());
        when(decisionService.getNextQuestion(any())).thenReturn(new HashMap<>(Map.of("key", "q1")));

        ResponseEntity<Map<String, Object>> response =
                controller.next(new QuestionController.NextRequest("other,valid", Map.of()));

        assertThat(response.getBody()).containsEntry("sessionId", "valid");
    }

    @Test
    void nextConvertsEmptyOptionsStringToEmptyList() {
        when(sessionService.getCurrentAnswers("sid-empty-opts")).thenReturn(new HashMap<>());
        Map<String, Object> nextStep = new HashMap<>();
        nextStep.put("key", "q1");
        nextStep.put("options", "");
        when(decisionService.getNextQuestion(any())).thenReturn(nextStep);

        ResponseEntity<Map<String, Object>> response =
                controller.next(new QuestionController.NextRequest("sid-empty-opts", Map.of()));

        @SuppressWarnings("unchecked")
        Map<String, Object> q = (Map<String, Object>) response.getBody().get("question");
        assertThat(q.get("options")).isEqualTo(List.of());
    }

    @Test
    void cleanSessionIdFallsBackToFirstPartWhenNoneValid() {
        when(sessionService.sessionExists(anyString())).thenReturn(false);
        when(sessionService.getCurrentAnswers("first")).thenReturn(new HashMap<>());
        when(decisionService.getNextQuestion(any())).thenReturn(new HashMap<>(Map.of("key", "q1")));

        ResponseEntity<Map<String, Object>> response =
                controller.next(new QuestionController.NextRequest("first,second", Map.of()));

        assertThat(response.getBody()).containsEntry("sessionId", "first");
    }

    @Test
    void cleanSessionIdReturnsBlankSessionIdUnchanged() {
        when(sessionService.getCurrentAnswers("")).thenReturn(new HashMap<>());
        when(decisionService.getNextQuestion(any())).thenReturn(new HashMap<>(Map.of("key", "q1")));

        ResponseEntity<Map<String, Object>> response = controller.next(new QuestionController.NextRequest("", Map.of()));

        assertThat(response.getBody()).containsEntry("sessionId", "");
    }

    @Test
    void questionApiControllerNextDelegatesToDecisionService() {
        QuestionController.QuestionApiController api = new QuestionController.QuestionApiController(decisionService);
        when(decisionService.getNextQuestion(any())).thenReturn(Map.of("key", "q1"));

        Map<String, Object> result = api.next(Map.of("consent", "Yes"));

        assertThat(result).containsEntry("key", "q1");
    }

    @Test
    void questionApiControllerStatusDelegatesToDecisionService() {
        QuestionController.QuestionApiController api = new QuestionController.QuestionApiController(decisionService);
        when(decisionService.getDecisionTreeStatus()).thenReturn(Map.of("status", "HEALTHY"));

        assertThat(api.status()).containsEntry("status", "HEALTHY");
    }
}
