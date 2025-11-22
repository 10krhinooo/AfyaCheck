package com.kimanga.afyacheck.controllers;

import com.kimanga.afyacheck.model.RiskAssessment;
import com.kimanga.afyacheck.model.Session;
import com.kimanga.afyacheck.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/results")
public class ResultsController {

    private static final Logger logger = LoggerFactory.getLogger(ResultsController.class);
    private final SessionService sessionService;

    public ResultsController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/view")
    public String viewResults(@RequestParam String sessionId, Model model) {
        try {
            RiskAssessment assessment = sessionService.getLatestRiskAssessmentOrThrow(sessionId);
            Session session = sessionService.getSessionWithDetailsOrThrow(sessionId);

            model.addAttribute("riskScore", assessment.getRiskScore());
            model.addAttribute("riskLevel", assessment.getRiskLevel());
            model.addAttribute("recommendations", assessment.getRecommendations());
            model.addAttribute("session", session);
            model.addAttribute("answers", sessionService.getCurrentAnswers(sessionId));

            return "results";
        } catch (Exception e) {
            logger.error("Error viewing results for session: {}", sessionId, e);
            model.addAttribute("error", "Results not found: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/history")
    public String viewHistory(@RequestParam String sessionId, Model model) {
        try {
            Session session = sessionService.getSessionWithDetailsOrThrow(sessionId);
            List<RiskAssessment> assessments = session.getRiskAssessments();

            model.addAttribute("session", session);
            model.addAttribute("assessments", assessments);
            model.addAttribute("answers", sessionService.getCurrentAnswers(sessionId));

            return "history";
        } catch (Exception e) {
            logger.error("Error viewing history for session: {}", sessionId, e);
            model.addAttribute("error", "Session history not found: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/latest")
    public String viewLatestResults(Model model) {
        try {
            // This could be enhanced to get the latest session for the current user
            // For now, we'll redirect to an error page since we need a sessionId
            model.addAttribute("error", "Please provide a session ID to view results");
            return "error";
        } catch (Exception e) {
            logger.error("Error in latest results", e);
            model.addAttribute("error", "An error occurred: " + e.getMessage());
            return "error";
        }
    }
}