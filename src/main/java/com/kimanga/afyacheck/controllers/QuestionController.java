package com.kimanga.afyacheck.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.kimanga.afyacheck.service.DecisionService;
import com.kimanga.afyacheck.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * JSON API backing the React questionnaire flow (see /app/questionnaire). Converted from
 * server-rendered MVC to REST as part of the Thymeleaf -> React migration (Phase 2).
 */
@RestController
@RequestMapping("/api/questionnaire")
public class QuestionController {

    private static final Logger logger = LoggerFactory.getLogger(QuestionController.class);

    private final DecisionService decisionService;
    private final SessionService sessionService;

    public QuestionController(DecisionService decisionService, SessionService sessionService) {
        this.decisionService = decisionService;
        this.sessionService = sessionService;
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start(HttpSession httpSession) {
        try {
            String sessionId = httpSession.getId();
            String createdSessionId = sessionService.createOrGetSession(sessionId);

            logger.info("Starting questionnaire for session: {} (created: {})", sessionId, createdSessionId);

            // Resume support: a reload keeps the same HttpSession, so pick up any answers
            // already saved instead of restarting the questionnaire from question one.
            Map<String, String> initialAnswers = new HashMap<>();
            Map<String, String> savedAnswers = sessionService.getCurrentAnswers(createdSessionId);
            if (savedAnswers != null) {
                initialAnswers.putAll(savedAnswers);
            }
            boolean resumed = !initialAnswers.isEmpty();
            initialAnswers.put("_sessionId", createdSessionId);

            Map<String, Object> firstQuestion = decisionService.getNextQuestion(initialAnswers);
            normalizeOptions(firstQuestion);

            if (firstQuestion.containsKey("error")) {
                logger.error("Error in first question: {}", firstQuestion.get("error"));
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", firstQuestion.get("error"), "sessionId", createdSessionId));
            }

            // A resumed session may already have every question answered — finish it the
            // same way /next would instead of handing back a questionless payload.
            if (Boolean.TRUE.equals(firstQuestion.get("end"))) {
                return ResponseEntity.ok(buildEndOfSurveyResponse(createdSessionId, initialAnswers, firstQuestion));
            }

            Map<String, Object> body = new HashMap<>();
            body.put("sessionId", createdSessionId);
            body.put("question", firstQuestion);
            body.put("canGoBack", resumed);
            body.put("resumed", resumed);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            logger.error("Error starting questionnaire", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to start questionnaire: " + e.getMessage()));
        }
    }

    public record NextRequest(String sessionId, Map<String, String> answers) {}

    @PostMapping("/next")
    public ResponseEntity<Map<String, Object>> next(@RequestBody NextRequest request) {
        try {
            String cleanSessionId = cleanSessionId(request.sessionId());
            logger.info("Processing next question for session: {}", cleanSessionId);

            Map<String, String> answers = sessionService.getCurrentAnswers(cleanSessionId);
            if (answers == null) {
                answers = new HashMap<>();
            }

            Map<String, String> submitted = request.answers() == null ? Map.of() : request.answers();
            sessionService.saveAnswers(cleanSessionId, submitted);

            Map<String, String> updatedAnswers = new HashMap<>(answers);
            updatedAnswers.putAll(submitted);
            updatedAnswers.put("_sessionId", cleanSessionId);

            Map<String, Object> nextStep = decisionService.getNextQuestion(updatedAnswers);
            normalizeOptions(nextStep);

            if (Boolean.TRUE.equals(nextStep.get("end"))) {
                logger.info("Survey ended for session: {} with {} questions answered",
                        cleanSessionId, updatedAnswers.size());
                return ResponseEntity.ok(buildEndOfSurveyResponse(cleanSessionId, updatedAnswers, nextStep));
            }

            Map<String, Object> body = new HashMap<>();
            body.put("sessionId", cleanSessionId);
            body.put("question", nextStep);
            body.put("canGoBack", true);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            logger.error("Error in next", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    public record BackRequest(String sessionId) {}

    @PostMapping("/back")
    public ResponseEntity<Map<String, Object>> back(@RequestBody BackRequest request) {
        try {
            String cleanSessionId = cleanSessionId(request.sessionId());
            logger.info("Going back for session: {}", cleanSessionId);

            Map<String, String> previousAnswers = sessionService.goBack(cleanSessionId);
            if (previousAnswers == null) {
                previousAnswers = new HashMap<>();
            }
            previousAnswers.put("_sessionId", cleanSessionId);

            Map<String, Object> previousQuestion = decisionService.getNextQuestion(previousAnswers);
            normalizeOptions(previousQuestion);

            Map<String, Object> body = new HashMap<>();
            body.put("sessionId", cleanSessionId);
            body.put("question", previousQuestion);
            body.put("canGoBack", !previousAnswers.isEmpty());
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            logger.error("Error in back for session: {}", request.sessionId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred while going back."));
        }
    }

    private Map<String, Object> buildEndOfSurveyResponse(
            String sessionId, Map<String, String> updatedAnswers, Map<String, Object> nextStep) {
        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", sessionId);
        body.put("end", true);

        if (Boolean.TRUE.equals(nextStep.get("consentDenied"))) {
            sessionService.completeSession(sessionId);
            body.put("consentDenied", true);
            body.put("endMessage", nextStep.get("text"));
            return body;
        }

        List<String> recommendationsList = splitRecommendations((String) nextStep.get("recommendations"));

        if (nextStep.containsKey("riskScore")) {
            sessionService.saveRiskAssessment(
                    sessionId,
                    (String) nextStep.get("riskLevel"),
                    (Integer) nextStep.get("riskScore"),
                    (String) nextStep.get("recommendations"),
                    (String) nextStep.get("modelVersion"));

            body.put("riskScore", nextStep.get("riskScore"));
            body.put("riskLevel", nextStep.get("riskLevel"));
            body.put("recommendations", recommendationsList);
            body.put("endMessage", nextStep.get("text"));
            body.put("confidence", nextStep.get("confidence"));
            body.put("modelUsed", nextStep.get("modelUsed"));
        } else {
            Map<String, Object> riskAssessment = decisionService.calculateRiskScore(updatedAnswers);
            List<String> fallbackRecommendations =
                    splitRecommendations((String) riskAssessment.get("recommendations"));

            sessionService.saveRiskAssessment(
                    sessionId,
                    (String) riskAssessment.get("riskLevel"),
                    (Integer) riskAssessment.get("riskScore"),
                    (String) riskAssessment.get("recommendations"),
                    (String) riskAssessment.get("modelVersion"));

            body.put("riskScore", riskAssessment.get("riskScore"));
            body.put("riskLevel", riskAssessment.get("riskLevel"));
            body.put("recommendations", fallbackRecommendations);
            body.put("endMessage", "Thank you for completing the STI risk assessment!");
            body.put("confidence", riskAssessment.get("confidence"));
            body.put("modelUsed", riskAssessment.get("modelUsed"));
        }

        body.put("questionsAnswered", updatedAnswers.size());
        return body;
    }

    private List<String> splitRecommendations(String recommendationsStr) {
        if (recommendationsStr == null || recommendationsStr.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(recommendationsStr.split("; "));
    }

    private void normalizeOptions(Map<String, Object> question) {
        if (question.get("options") instanceof String optionsString) {
            question.put("options", convertOptionsStringToList(optionsString));
        }
    }

    private String cleanSessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return sessionId;
        }

        String trimmed = sessionId.trim();

        if (trimmed.contains(",")) {
            String[] parts = trimmed.split(",");
            for (String part : parts) {
                String cleanPart = part.trim();
                if (sessionService.sessionExists(cleanPart)) {
                    logger.info("Using valid session ID part: {}", cleanPart);
                    return cleanPart;
                }
            }
            String cleanId = parts[0].trim();
            logger.warn("No valid session ID found, using first part: {}", cleanId);
            return cleanId;
        }

        return trimmed;
    }

    private List<String> convertOptionsStringToList(String optionsString) {
        if (optionsString == null || optionsString.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(optionsString.split(","));
    }

    // Separate REST surface for the decision-tree JSON contract used directly by other
    // clients (independent of the /api/questionnaire/* session-tracked flow above).
    @RestController
    @RequestMapping("/api/questions")
    public static class QuestionApiController {

        private final DecisionService decisionService;

        public QuestionApiController(DecisionService decisionService) {
            this.decisionService = decisionService;
        }

        @PostMapping("/next")
        public Map<String, Object> next(@RequestBody Map<String, String> answers) {
            return decisionService.getNextQuestion(answers);
        }

        @GetMapping("/status")
        public Map<String, Object> status() {
            return decisionService.getDecisionTreeStatus();
        }
    }
}
