package com.kimanga.afyacheck.controllers;

import com.kimanga.afyacheck.model.Answer;
import com.kimanga.afyacheck.model.RiskAssessment;
import com.kimanga.afyacheck.model.Session;
import com.kimanga.afyacheck.service.NotificationService;
import com.kimanga.afyacheck.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResultsControllerTest {

    private final SessionService sessionService = mock(SessionService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final ResultsController controller = new ResultsController(sessionService, notificationService);

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

    @Test
    void notifyReturnsBadRequestWhenEmailBlank() {
        ResponseEntity<?> response = controller.notify(new ResultsController.NotifyRequest("sid-5", " "));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void notifySendsEmailOnSuccess() {
        RiskAssessment assessment = new RiskAssessment();
        when(sessionService.getLatestRiskAssessmentOrThrow("sid-6")).thenReturn(assessment);

        ResponseEntity<?> response = controller.notify(new ResultsController.NotifyRequest("sid-6", "user@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificationService).sendRiskResultEmail("user@example.com", assessment);
    }

    @Test
    void notifyReturnsBadRequestOnInvalidEmail() {
        RiskAssessment assessment = new RiskAssessment();
        when(sessionService.getLatestRiskAssessmentOrThrow("sid-7")).thenReturn(assessment);
        doThrow(new IllegalArgumentException("Invalid email format"))
                .when(notificationService).sendRiskResultEmail(anyString(), any());

        ResponseEntity<?> response = controller.notify(new ResultsController.NotifyRequest("sid-7", "not-an-email"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void notifyReturns500WhenSessionNotFound() {
        when(sessionService.getLatestRiskAssessmentOrThrow("sid-8")).thenThrow(new RuntimeException("not found"));

        ResponseEntity<?> response = controller.notify(new ResultsController.NotifyRequest("sid-8", "user@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void exportReturnsAnswersAndAssessmentsOnSuccess() {
        Session session = new Session();
        session.setCreatedAt(new java.util.Date());
        session.setStatus("completed");
        Answer answer = new Answer();
        answer.setQuestionKey("age");
        answer.setAnswerValue("30");
        session.setAnswers(List.of(answer));
        session.setRiskAssessments(List.of(new RiskAssessment()));
        when(sessionService.getSessionWithDetailsOrThrow("sid-9")).thenReturn(session);

        ResponseEntity<?> response = controller.export("sid-9");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = (ResultsController.DataExportResponse) response.getBody();
        assertThat(body.answers()).hasSize(1);
        assertThat(body.answers().get(0).questionKey()).isEqualTo("age");
        assertThat(body.riskAssessments()).hasSize(1);
    }

    @Test
    void exportReturns404WhenSessionNotFound() {
        when(sessionService.getSessionWithDetailsOrThrow("sid-10")).thenThrow(new RuntimeException("not found"));

        ResponseEntity<?> response = controller.export("sid-10");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteReturnsOkWhenSessionDeleted() {
        when(sessionService.deleteSessionData("sid-11")).thenReturn(true);

        ResponseEntity<?> response = controller.delete("sid-11");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteReturns404WhenSessionNotFound() {
        when(sessionService.deleteSessionData("sid-12")).thenReturn(false);

        ResponseEntity<?> response = controller.delete("sid-12");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteReturns500OnException() {
        when(sessionService.deleteSessionData("sid-13")).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.delete("sid-13");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
