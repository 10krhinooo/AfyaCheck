package com.kimanga.afyacheck.controllers;

import com.kimanga.afyacheck.model.RiskAssessment;
import com.kimanga.afyacheck.model.Session;
import com.kimanga.afyacheck.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResultsControllerTest {

    private final SessionService sessionService = mock(SessionService.class);
    private final ResultsController controller = new ResultsController(sessionService);

    @Test
    void latestReturnsAssessmentOnSuccess() {
        RiskAssessment assessment = new RiskAssessment();
        assessment.setRiskScore(50);
        assessment.setRiskLevel("Medium");
        when(sessionService.getLatestRiskAssessmentOrThrow("sid-1")).thenReturn(assessment);

        ResponseEntity<?> response = controller.latest("sid-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = (ResultsController.LatestResultResponse) response.getBody();
        assertThat(body.assessment().riskScore()).isEqualTo(50);
        assertThat(body.assessment().riskLevel()).isEqualTo("Medium");
    }

    @Test
    void latestReturns404OnException() {
        when(sessionService.getLatestRiskAssessmentOrThrow("sid-2")).thenThrow(new RuntimeException("not found"));

        ResponseEntity<?> response = controller.latest("sid-2");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void historyReturnsAssessmentsOnSuccess() {
        Session session = new Session();
        session.setRiskAssessments(List.of(new RiskAssessment()));
        when(sessionService.getSessionWithDetailsOrThrow("sid-3")).thenReturn(session);

        ResponseEntity<?> response = controller.history("sid-3");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = (ResultsController.HistoryResponse) response.getBody();
        assertThat(body.assessments()).hasSize(1);
    }

    @Test
    void historyReturns404OnException() {
        when(sessionService.getSessionWithDetailsOrThrow("sid-4")).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.history("sid-4");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
