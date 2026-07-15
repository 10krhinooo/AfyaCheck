package com.kimanga.afyacheck.controllers;

import com.kimanga.afyacheck.model.Session;
import com.kimanga.afyacheck.service.DecisionService;
import com.kimanga.afyacheck.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    void startQuestionnaireRendersFirstQuestion() {
        when(sessionService.createOrGetSession("http-sid")).thenReturn("app-sid");
        Map<String, Object> question = new HashMap<>();
        question.put("key", "consent");
        question.put("options", "Yes,No");
        when(decisionService.getNextQuestion(any())).thenReturn(question);
        when(sessionService.getCurrentAnswers("app-sid")).thenReturn(Map.of());

        Model model = new ExtendedModelMap();
        String view = controller.startQuestionnaire(httpSession("http-sid"), model);

        assertThat(view).isEqualTo("questionnaire");
        assertThat(model.getAttribute("sessionId")).isEqualTo("app-sid");
        assertThat(model.getAttribute("question")).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> q = (Map<String, Object>) model.getAttribute("question");
        assertThat(q.get("options")).isEqualTo(List.of("Yes", "No"));
    }

    @Test
    void startQuestionnaireReturnsErrorPageWhenDecisionServiceReportsError() {
        when(sessionService.createOrGetSession("http-sid2")).thenReturn("app-sid2");
        Map<String, Object> question = new HashMap<>();
        question.put("error", "something failed");
        when(decisionService.getNextQuestion(any())).thenReturn(question);

        Model model = new ExtendedModelMap();
        String view = controller.startQuestionnaire(httpSession("http-sid2"), model);

        assertThat(view).isEqualTo("error-page");
        assertThat(model.getAttribute("error")).isEqualTo("something failed");
    }

    @Test
    void startQuestionnaireHandlesNullAnswersFromSessionService() {
        when(sessionService.createOrGetSession("http-sid3")).thenReturn("app-sid3");
        when(decisionService.getNextQuestion(any())).thenReturn(new HashMap<>(Map.of("key", "q1")));
        when(sessionService.getCurrentAnswers("app-sid3")).thenReturn(null);

        Model model = new ExtendedModelMap();
        String view = controller.startQuestionnaire(httpSession("http-sid3"), model);

        assertThat(view).isEqualTo("questionnaire");
        assertThat(model.getAttribute("answers")).isEqualTo(new HashMap<>());
    }

    @Test
    void startQuestionnaireReturnsErrorPageOnException() {
        when(sessionService.createOrGetSession(anyString())).thenThrow(new RuntimeException("boom"));

        Model model = new ExtendedModelMap();
        String view = controller.startQuestionnaire(httpSession("http-sid4"), model);

        assertThat(view).isEqualTo("error-page");
    }

    @Test
    void nextQuestionReturnsQuestionnaireViewWhenNotEnded() {
        when(sessionService.getCurrentAnswers("sid-1")).thenReturn(new HashMap<>());
        Map<String, Object> nextStep = new HashMap<>();
        nextStep.put("key", "q2");
        nextStep.put("options", "A,B");
        when(decisionService.getNextQuestion(any())).thenReturn(nextStep);

        Map<String, String> formParams = new HashMap<>();
        formParams.put("q1", "answer");

        Model model = new ExtendedModelMap();
        String view = controller.nextQuestion(formParams, "sid-1", httpSession("http"), model);

        assertThat(view).isEqualTo("questionnaire");
        assertThat(model.getAttribute("canGoBack")).isEqualTo(true);
    }

    @Test
    void nextQuestionHandlesNullAnswersFromSessionService() {
        when(sessionService.getCurrentAnswers("sid-1b")).thenReturn(null);
        Map<String, Object> nextStep = new HashMap<>();
        nextStep.put("key", "q2");
        when(decisionService.getNextQuestion(any())).thenReturn(nextStep);

        Model model = new ExtendedModelMap();
        String view = controller.nextQuestion(Map.of("q1", "answer"), "sid-1b", httpSession("http"), model);

        assertThat(view).isEqualTo("questionnaire");
    }

    @Test
    void nextQuestionHandlesConsentDenied() {
        when(sessionService.getCurrentAnswers("sid-2")).thenReturn(new HashMap<>());
        Map<String, Object> nextStep = new HashMap<>();
        nextStep.put("end", true);
        nextStep.put("consentDenied", true);
        nextStep.put("text", "denied");
        when(decisionService.getNextQuestion(any())).thenReturn(nextStep);
        when(sessionService.getSessionWithDetails("sid-2")).thenReturn(Optional.of(new Session()));

        Model model = new ExtendedModelMap();
        String view = controller.nextQuestion(Map.of(), "sid-2", httpSession("http"), model);

        assertThat(view).isEqualTo("results");
        assertThat(model.getAttribute("consentDenied")).isEqualTo(true);
        verify(sessionService).completeSession("sid-2");
    }

    @Test
    void nextQuestionSavesRiskAssessmentWhenSurveyEndsWithScore() {
        when(sessionService.getCurrentAnswers("sid-3")).thenReturn(new HashMap<>());
        Map<String, Object> nextStep = new HashMap<>();
        nextStep.put("end", true);
        nextStep.put("riskScore", 80);
        nextStep.put("riskLevel", "High");
        nextStep.put("recommendations", "Rec1; Rec2");
        nextStep.put("text", "done");
        when(decisionService.getNextQuestion(any())).thenReturn(nextStep);
        when(sessionService.getSessionWithDetails("sid-3")).thenReturn(Optional.of(new Session()));

        Model model = new ExtendedModelMap();
        String view = controller.nextQuestion(Map.of(), "sid-3", httpSession("http"), model);

        assertThat(view).isEqualTo("results");
        assertThat(model.getAttribute("riskScore")).isEqualTo(80);
        verify(sessionService).saveRiskAssessment("sid-3", "High", 80, "Rec1; Rec2");
    }

    @Test
    void nextQuestionFallsBackToCalculateRiskScoreWhenNoRiskScoreInResult() {
        when(sessionService.getCurrentAnswers("sid-4")).thenReturn(new HashMap<>());
        Map<String, Object> nextStep = new HashMap<>();
        nextStep.put("end", true);
        when(decisionService.getNextQuestion(any())).thenReturn(nextStep);

        Map<String, Object> fallbackAssessment = new HashMap<>();
        fallbackAssessment.put("riskScore", 40);
        fallbackAssessment.put("riskLevel", "Medium");
        fallbackAssessment.put("recommendations", "RecA; RecB");
        when(decisionService.calculateRiskScore(any())).thenReturn(fallbackAssessment);
        when(sessionService.getSessionWithDetails("sid-4")).thenReturn(Optional.of(new Session()));

        Model model = new ExtendedModelMap();
        String view = controller.nextQuestion(Map.of(), "sid-4", httpSession("http"), model);

        assertThat(view).isEqualTo("results");
        assertThat(model.getAttribute("riskScore")).isEqualTo(40);
        verify(sessionService).saveRiskAssessment("sid-4", "Medium", 40, "RecA; RecB");
    }

    @Test
    void nextQuestionReturnsErrorPageOnException() {
        when(sessionService.getCurrentAnswers(anyString())).thenThrow(new RuntimeException("boom"));

        Model model = new ExtendedModelMap();
        String view = controller.nextQuestion(Map.of(), "sid-5", httpSession("http"), model);

        assertThat(view).isEqualTo("error-page");
    }

    @Test
    void previousQuestionReturnsQuestionnaireView() {
        Map<String, String> remaining = new HashMap<>();
        remaining.put("q1", "yes");
        when(sessionService.goBack("sid-6")).thenReturn(remaining);
        Map<String, Object> question = new HashMap<>();
        question.put("key", "q1");
        when(decisionService.getNextQuestion(any())).thenReturn(question);

        Model model = new ExtendedModelMap();
        String view = controller.previousQuestion("sid-6", Map.of(), httpSession("http"), model);

        assertThat(view).isEqualTo("questionnaire");
        assertThat(model.getAttribute("canGoBack")).isEqualTo(true);
    }

    @Test
    void previousQuestionConvertsOptionsStringToList() {
        when(sessionService.goBack("sid-6b")).thenReturn(new HashMap<>());
        Map<String, Object> question = new HashMap<>();
        question.put("key", "q1");
        question.put("options", "X,Y,Z");
        when(decisionService.getNextQuestion(any())).thenReturn(question);

        Model model = new ExtendedModelMap();
        controller.previousQuestion("sid-6b", Map.of(), httpSession("http"), model);

        @SuppressWarnings("unchecked")
        Map<String, Object> q = (Map<String, Object>) model.getAttribute("question");
        assertThat(q.get("options")).isEqualTo(List.of("X", "Y", "Z"));
    }

    @Test
    void previousQuestionHandlesNullAnswersFromGoBack() {
        when(sessionService.goBack("sid-7")).thenReturn(null);
        when(decisionService.getNextQuestion(any())).thenReturn(new HashMap<>(Map.of("key", "consent")));

        Model model = new ExtendedModelMap();
        String view = controller.previousQuestion("sid-7", Map.of(), httpSession("http"), model);

        assertThat(view).isEqualTo("questionnaire");
        // The controller unconditionally adds "_sessionId" to previousAnswers before
        // checking isEmpty(), so canGoBack is always true even with no real answers.
        assertThat(model.getAttribute("canGoBack")).isEqualTo(true);
    }

    @Test
    void previousQuestionReturnsErrorPageOnException() {
        when(sessionService.goBack(anyString())).thenThrow(new RuntimeException("boom"));

        Model model = new ExtendedModelMap();
        String view = controller.previousQuestion("sid-8", Map.of(), httpSession("http"), model);

        assertThat(view).isEqualTo("error-page");
    }

    @Test
    void cleanSessionIdPicksValidPartFromCommaSeparatedList() {
        when(sessionService.sessionExists("valid")).thenReturn(true);
        when(sessionService.sessionExists("other")).thenReturn(false);
        when(sessionService.getCurrentAnswers("valid")).thenReturn(new HashMap<>());
        when(decisionService.getNextQuestion(any())).thenReturn(new HashMap<>(Map.of("key", "q1")));

        Model model = new ExtendedModelMap();
        controller.nextQuestion(Map.of(), "other,valid", httpSession("http"), model);

        assertThat(model.getAttribute("sessionId")).isEqualTo("valid");
    }

    @Test
    void nextQuestionConvertsEmptyOptionsStringToEmptyList() {
        when(sessionService.getCurrentAnswers("sid-empty-opts")).thenReturn(new HashMap<>());
        Map<String, Object> nextStep = new HashMap<>();
        nextStep.put("key", "q1");
        nextStep.put("options", "");
        when(decisionService.getNextQuestion(any())).thenReturn(nextStep);

        Model model = new ExtendedModelMap();
        controller.nextQuestion(Map.of(), "sid-empty-opts", httpSession("http"), model);

        @SuppressWarnings("unchecked")
        Map<String, Object> q = (Map<String, Object>) model.getAttribute("question");
        assertThat(q.get("options")).isEqualTo(List.of());
    }

    @Test
    void cleanSessionIdFallsBackToFirstPartWhenNoneValid() {
        when(sessionService.sessionExists(anyString())).thenReturn(false);
        when(sessionService.getCurrentAnswers("first")).thenReturn(new HashMap<>());
        when(decisionService.getNextQuestion(any())).thenReturn(new HashMap<>(Map.of("key", "q1")));

        Model model = new ExtendedModelMap();
        controller.nextQuestion(Map.of(), "first,second", httpSession("http"), model);

        assertThat(model.getAttribute("sessionId")).isEqualTo("first");
    }

    @Test
    void cleanSessionIdReturnsBlankSessionIdUnchanged() {
        when(sessionService.getCurrentAnswers("")).thenReturn(new HashMap<>());
        when(decisionService.getNextQuestion(any())).thenReturn(new HashMap<>(Map.of("key", "q1")));

        Model model = new ExtendedModelMap();
        controller.nextQuestion(Map.of(), "", httpSession("http"), model);

        assertThat(model.getAttribute("sessionId")).isEqualTo("");
    }

    @Test
    void debugDatabaseStatusDelegatesToDecisionService() {
        when(decisionService.debugDatabaseStatus()).thenReturn(Map.of("ok", true));
        assertThat(controller.debugDatabaseStatus()).containsEntry("ok", true);
    }

    @Test
    void debugQuestionsDelegatesToDecisionService() {
        when(decisionService.debugQuestionDatabase()).thenReturn(Map.of("count", 5));
        assertThat(controller.debugQuestions()).containsEntry("count", 5);
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
