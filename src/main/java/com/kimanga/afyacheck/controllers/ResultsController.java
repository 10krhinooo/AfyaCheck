package com.kimanga.afyacheck.controllers;

import com.kimanga.afyacheck.model.RiskAssessment;
import com.kimanga.afyacheck.model.Session;
import com.kimanga.afyacheck.model.User;
import com.kimanga.afyacheck.service.SessionService;
import com.kimanga.afyacheck.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
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
    private final UserService userService;

    public ResultsController(SessionService sessionService, UserService userService) {
        this.sessionService = sessionService;
        this.userService = userService;
    }

    @GetMapping("/view")
    public String viewResults(@RequestParam String sessionId, Model model, Authentication authentication) {
        try {
            RiskAssessment assessment = sessionService.getLatestRiskAssessmentOrThrow(sessionId);
            Session session = sessionService.getSessionWithDetailsOrThrow(sessionId);

            if (!isOwnedByCurrentUser(session, authentication)) {
                logger.warn("Denied access to session {} for a user who does not own it", sessionId);
                model.addAttribute("error", "You do not have permission to view these results.");
                return "error";
            }

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

    /**
     * A session with no owner (created before authentication, e.g. abandoned
     * on the login page) is treated as inaccessible rather than public, since
     * there's no way to tell who it belongs to.
     */
    private boolean isOwnedByCurrentUser(Session session, Authentication authentication) {
        User sessionOwner = session.getUser();
        if (sessionOwner == null) {
            return false;
        }
        return userService.resolveCurrentUser(authentication)
                .map(currentUser -> currentUser.getId().equals(sessionOwner.getId()))
                .orElse(false);
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
    public String viewLatestResults(Model model, Authentication authentication) {
        try {
            Optional<User> currentUser = userService.resolveCurrentUser(authentication);
            if (currentUser.isEmpty()) {
                model.addAttribute("error", "Please log in to view your results.");
                return "error";
            }

            Optional<Session> latestSession = sessionService.getLatestSessionForUser(currentUser.get().getId());
            if (latestSession.isEmpty()) {
                model.addAttribute("error", "You haven't completed an assessment yet.");
                return "error";
            }

            return "redirect:/results/view?sessionId=" + latestSession.get().getSessionId();
        } catch (Exception e) {
            logger.error("Error in latest results", e);
            model.addAttribute("error", "An error occurred: " + e.getMessage());
            return "error";
        }
    }
}