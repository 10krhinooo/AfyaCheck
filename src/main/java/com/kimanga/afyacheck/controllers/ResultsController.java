package com.kimanga.afyacheck.controllers;

import com.kimanga.afyacheck.model.RiskAssessment;
import com.kimanga.afyacheck.model.Session;
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

    public ResultsController(SessionService sessionService) {
        this.sessionService = sessionService;
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
}
