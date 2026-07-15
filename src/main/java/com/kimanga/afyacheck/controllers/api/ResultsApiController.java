package com.kimanga.afyacheck.controllers.api;

import com.kimanga.afyacheck.model.RiskAssessment;
import com.kimanga.afyacheck.model.Session;
import com.kimanga.afyacheck.model.User;
import com.kimanga.afyacheck.service.SessionService;
import com.kimanga.afyacheck.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ResultsApiController {

    private static final Logger logger = LoggerFactory.getLogger(ResultsApiController.class);

    private final SessionService sessionService;
    private final UserService userService;

    public ResultsApiController(SessionService sessionService, UserService userService) {
        this.sessionService = sessionService;
        this.userService = userService;
    }

    @GetMapping("/results/{sessionId}")
    public ResponseEntity<?> viewResults(@PathVariable String sessionId, Authentication authentication) {
        Session session;
        RiskAssessment assessment;
        try {
            session = sessionService.getSessionWithDetailsOrThrow(sessionId);
            assessment = sessionService.getLatestRiskAssessmentOrThrow(sessionId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Results not found"));
        }

        if (!isOwnedByCurrentUser(session, authentication)) {
            logger.warn("Denied access to session {} for a user who does not own it", sessionId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not have permission to view these results"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getSessionId());
        response.put("riskScore", assessment.getRiskScore());
        response.put("riskLevel", assessment.getRiskLevel());
        response.put("recommendations", assessment.getRecommendations());
        response.put("createdAt", session.getCreatedAt());
        response.put("answers", sessionService.getCurrentAnswers(sessionId));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/results/latest")
    public ResponseEntity<?> latestResults(Authentication authentication) {
        Optional<User> currentUser = userService.resolveCurrentUser(authentication);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        return sessionService.getLatestSessionForUser(currentUser.get().getId())
                .map(session -> ResponseEntity.ok(Map.of("sessionId", session.getSessionId())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "No assessments yet")));
    }

    @GetMapping("/sessions")
    public ResponseEntity<?> mySessions(Authentication authentication) {
        Optional<User> currentUser = userService.resolveCurrentUser(authentication);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        List<Map<String, Object>> sessions = new ArrayList<>();
        for (Session session : sessionService.getSessionsForUser(currentUser.get().getId())) {
            Map<String, Object> summary = new HashMap<>();
            summary.put("sessionId", session.getSessionId());
            summary.put("status", session.getStatus());
            summary.put("riskScore", session.getRiskScore());
            summary.put("createdAt", session.getCreatedAt());
            summary.put("updatedAt", session.getUpdatedAt());
            sessions.add(summary);
        }

        return ResponseEntity.ok(sessions);
    }

    private boolean isOwnedByCurrentUser(Session session, Authentication authentication) {
        User sessionOwner = session.getUser();
        if (sessionOwner == null) {
            return false;
        }
        return userService.resolveCurrentUser(authentication)
                .map(currentUser -> currentUser.getId().equals(sessionOwner.getId()))
                .orElse(false);
    }
}
