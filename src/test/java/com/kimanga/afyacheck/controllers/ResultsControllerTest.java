package com.kimanga.afyacheck.controllers;

import com.kimanga.afyacheck.model.RiskAssessment;
import com.kimanga.afyacheck.model.Session;
import com.kimanga.afyacheck.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResultsControllerTest {

    private final SessionService sessionService = mock(SessionService.class);
    private final ResultsController controller = new ResultsController(sessionService);

    @Test
    void viewResultsPopulatesModelOnSuccess() {
        RiskAssessment assessment = new RiskAssessment();
        assessment.setRiskScore(50);
        assessment.setRiskLevel("Medium");
        when(sessionService.getLatestRiskAssessmentOrThrow("sid-1")).thenReturn(assessment);
        when(sessionService.getSessionWithDetailsOrThrow("sid-1")).thenReturn(new Session());
        when(sessionService.getCurrentAnswers("sid-1")).thenReturn(Map.of());

        Model model = new ExtendedModelMap();
        String view = controller.viewResults("sid-1", model);

        assertThat(view).isEqualTo("results");
        assertThat(model.getAttribute("riskScore")).isEqualTo(50);
    }

    @Test
    void viewResultsReturnsErrorViewOnException() {
        when(sessionService.getLatestRiskAssessmentOrThrow("sid-2")).thenThrow(new RuntimeException("not found"));

        Model model = new ExtendedModelMap();
        String view = controller.viewResults("sid-2", model);

        assertThat(view).isEqualTo("error");
        assertThat(model.getAttribute("error")).isNotNull();
    }

    @Test
    void viewHistoryPopulatesModelOnSuccess() {
        Session session = new Session();
        session.setRiskAssessments(List.of(new RiskAssessment()));
        when(sessionService.getSessionWithDetailsOrThrow("sid-3")).thenReturn(session);
        when(sessionService.getCurrentAnswers("sid-3")).thenReturn(Map.of());

        Model model = new ExtendedModelMap();
        String view = controller.viewHistory("sid-3", model);

        assertThat(view).isEqualTo("history");
        assertThat((List<?>) model.getAttribute("assessments")).hasSize(1);
    }

    @Test
    void viewHistoryReturnsErrorViewOnException() {
        when(sessionService.getSessionWithDetailsOrThrow("sid-4")).thenThrow(new RuntimeException("boom"));

        Model model = new ExtendedModelMap();
        String view = controller.viewHistory("sid-4", model);

        assertThat(view).isEqualTo("error");
    }

    @Test
    void viewLatestResultsAlwaysReturnsErrorAskingForSessionId() {
        Model model = new ExtendedModelMap();
        String view = controller.viewLatestResults(model);

        assertThat(view).isEqualTo("error");
        assertThat(model.getAttribute("error")).isEqualTo("Please provide a session ID to view results");
    }

    @Test
    void viewLatestResultsReturnsErrorViewWhenModelThrows() {
        Model model = mock(Model.class);
        // Throw only on the first call (inside the try block); the catch block's
        // own model.addAttribute call must succeed, or the exception it would
        // otherwise raise propagates out of the method uncaught.
        when(model.addAttribute(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new RuntimeException("model boom"))
                .thenReturn(model);

        String view = controller.viewLatestResults(model);

        assertThat(view).isEqualTo("error");
    }
}
