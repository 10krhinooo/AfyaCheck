package com.kimanga.afyacheck.controllers;

import com.kimanga.afyacheck.model.Answer;
import com.kimanga.afyacheck.model.RiskAssessment;
import com.kimanga.afyacheck.model.Session;
import com.kimanga.afyacheck.service.NotificationService;
import com.kimanga.afyacheck.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * JSON API backing the React results flow (see /app/results). Converted from server-rendered
 * MVC to REST as part of the Thymeleaf -> React migration (Phase 3). The old "/results/view"
 * and "/results/latest" (previously broken — no sessionId parameter, always 404'd on a
 * nonexistent "error" view) are consolidated into a single "/latest" lookup by sessionId.
 */
@RestController
@RequestMapping("/api/results")
public class ResultsController {

    private static final Logger logger = LoggerFactory.getLogger(ResultsController.class);
    private final SessionService sessionService;
    private final NotificationService notificationService;

    public ResultsController(SessionService sessionService, NotificationService notificationService) {
        this.sessionService = sessionService;
        this.notificationService = notificationService;
    }

    public record RiskAssessmentDto(Integer riskScore, String riskLevel, List<String> recommendations, Date createdAt) {
        static RiskAssessmentDto from(RiskAssessment assessment) {
            return new RiskAssessmentDto(
                    assessment.getRiskScore(),
                    assessment.getRiskLevel(),
                    assessment.getRecommendations(),
                    assessment.getCreatedAt());
        }
    }

    public record LatestResultResponse(String sessionId, RiskAssessmentDto assessment) {}

    @GetMapping("/latest")
    public ResponseEntity<?> latest(@RequestParam String sessionId) {
        try {
            RiskAssessment assessment = sessionService.getLatestRiskAssessmentOrThrow(sessionId);
            return ResponseEntity.ok(new LatestResultResponse(sessionId, RiskAssessmentDto.from(assessment)));
        } catch (Exception e) {
            logger.error("Error fetching latest result for session: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Results not found: " + e.getMessage()));
        }
    }

    public record NotifyRequest(String sessionId, String email) {}

    // Opt-in only, triggered by the "email me these results" prompt on the results page --
    // the assessment itself stays anonymous; the email address is used once to send this
    // message and is never persisted.
    @PostMapping("/notify")
    public ResponseEntity<?> notify(@RequestBody NotifyRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }

        try {
            RiskAssessment assessment = sessionService.getLatestRiskAssessmentOrThrow(request.sessionId());
            notificationService.sendRiskResultEmail(request.email(), assessment);
            return ResponseEntity.ok(Map.of("message", "Results sent"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid email address"));
        } catch (Exception e) {
            logger.error("Error sending result email for session: {}", request.sessionId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Could not send results email"));
        }
    }

    public record HistoryResponse(String sessionId, List<RiskAssessmentDto> assessments) {}

    @GetMapping("/history")
    public ResponseEntity<?> history(@RequestParam String sessionId) {
        try {
            Session session = sessionService.getSessionWithDetailsOrThrow(sessionId);
            List<RiskAssessmentDto> assessments =
                    session.getRiskAssessments().stream().map(RiskAssessmentDto::from).toList();
            return ResponseEntity.ok(new HistoryResponse(sessionId, assessments));
        } catch (Exception e) {
            logger.error("Error fetching history for session: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Session history not found: " + e.getMessage()));
        }
    }

    public record AnswerDto(String questionKey, String answerValue, Date createdAt) {
        static AnswerDto from(Answer answer) {
            return new AnswerDto(answer.getQuestionKey(), answer.getAnswerValue(), answer.getCreatedAt());
        }
    }

    public record DataExportResponse(
            String sessionId, Date createdAt, String status, List<AnswerDto> answers, List<RiskAssessmentDto> riskAssessments) {}

    // Self-service export for a user who knows their own sessionId -- no authentication
    // required, matching the rest of the anonymous questionnaire flow (see NotifyRequest).
    @GetMapping("/export")
    public ResponseEntity<?> export(@RequestParam String sessionId) {
        try {
            Session session = sessionService.getSessionWithDetailsOrThrow(sessionId);
            List<AnswerDto> answers = session.getAnswers().stream().map(AnswerDto::from).toList();
            List<RiskAssessmentDto> assessments =
                    session.getRiskAssessments().stream().map(RiskAssessmentDto::from).toList();
            return ResponseEntity.ok(new DataExportResponse(
                    sessionId, session.getCreatedAt(), session.getStatus(), answers, assessments));
        } catch (Exception e) {
            logger.error("Error exporting data for session: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Session not found: " + e.getMessage()));
        }
    }

    // Self-service deletion, same access model as export above: knowledge of the sessionId
    // is the access control, no authentication required.
    @DeleteMapping
    public ResponseEntity<?> delete(@RequestParam String sessionId) {
        try {
            boolean deleted = sessionService.deleteSessionData(sessionId);
            return deleted
                    ? ResponseEntity.ok(Map.of("message", "Your data has been deleted"))
                    : ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Session not found"));
        } catch (Exception e) {
            logger.error("Error deleting data for session: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Could not delete session data"));
        }
    }
}
