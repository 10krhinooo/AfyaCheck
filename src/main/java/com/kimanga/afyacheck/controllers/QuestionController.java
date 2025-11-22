package com.kimanga.afyacheck.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kimanga.afyacheck.service.DecisionService;
import com.kimanga.afyacheck.service.SessionService;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/questionnaire")
public class QuestionController {

    private static final Logger logger = LoggerFactory.getLogger(QuestionController.class);

    private final DecisionService decisionService;
    private final SessionService sessionService;

    public QuestionController(DecisionService decisionService, SessionService sessionService) {
        this.decisionService = decisionService;
        this.sessionService = sessionService;
    }

    @GetMapping("/start")
    public String startQuestionnaire(HttpSession httpSession, Model model) {
        try {
            String sessionId = httpSession.getId();
            String createdSessionId = sessionService.createOrGetSession(sessionId);

            logger.info("Starting questionnaire for session: {} (created: {})", sessionId, createdSessionId);

            // Initialize with empty answers map
            Map<String, String> initialAnswers = new HashMap<>();
            Map<String, Object> firstQuestion = new HashMap<>(decisionService.getNextQuestion(initialAnswers));

            logger.info("Received question data - keys: {}, has error: {}",
                    firstQuestion.keySet(), firstQuestion.containsKey("error"));

            // Check for error before ensuring structure
            if (firstQuestion.containsKey("error")) {
                logger.error("Error in first question: {}", firstQuestion.get("error"));
                model.addAttribute("error", firstQuestion.get("error"));
                model.addAttribute("sessionId", createdSessionId);
                return "error-page";
            }

            Map<String, Object> ensuredQuestion = ensureQuestionStructure(firstQuestion, "Start Question");

            // Ensure answers is never null
            Map<String, String> safeAnswers = sessionService.getCurrentAnswers(createdSessionId);
            if (safeAnswers == null) {
                safeAnswers = new HashMap<>();
                logger.info("Initialized empty answers for new session");
            }

            model.addAttribute("question", ensuredQuestion);
            model.addAttribute("sessionId", createdSessionId);
            model.addAttribute("answers", safeAnswers); // Use safe, non-null answers
            model.addAttribute("canGoBack", false);

            return "questionnaire";
        } catch (Exception e) {
            logger.error("Error starting questionnaire", e);
            model.addAttribute("error", "Failed to start questionnaire: " + e.getMessage());
            return "error-page";
        }
    }

    @PostMapping("/next")
    public String nextQuestion(
            @RequestParam Map<String, String> formParams,
            @RequestParam String sessionId,
            HttpSession httpSession,
            Model model) {

        try {
            logger.info("Processing next question for session: {}", sessionId);

            // Clean the session ID
            String cleanSessionId = cleanSessionId(sessionId);

            // Get current answers - ensure non-null
            Map<String, String> answers = sessionService.getCurrentAnswers(cleanSessionId);
            if (answers == null) {
                answers = new HashMap<>();
                logger.info("Initialized empty answers for session: {}", cleanSessionId);
            }

            // Save the new answers
            sessionService.saveAnswers(cleanSessionId, formParams);

            // Update answers with new form params (excluding sessionId)
            Map<String, String> updatedAnswers = new HashMap<>(answers);
            for (Map.Entry<String, String> entry : formParams.entrySet()) {
                if (!"sessionId".equals(entry.getKey())) {
                    updatedAnswers.put(entry.getKey(), entry.getValue());
                }
            }

            Map<String, Object> nextStep = decisionService.getNextQuestion(updatedAnswers);

            Map<String, Object> ensuredQuestion = ensureQuestionStructure(nextStep, "Next Question");

            if (ensuredQuestion.containsKey("end") && Boolean.TRUE.equals(ensuredQuestion.get("end"))) {
                logger.info("Survey ended for session: {}", cleanSessionId);

                // Check if we have risk assessment data
                if (ensuredQuestion.containsKey("riskScore")) {
                    // Save risk assessment to database
                    sessionService.saveRiskAssessment(
                            cleanSessionId,
                            (String) ensuredQuestion.get("riskLevel"),
                            (Integer) ensuredQuestion.get("riskScore"),
                            (java.util.List<String>) ensuredQuestion.get("recommendations")
                    );

                    // Add data to model for results page
                    model.addAttribute("riskScore", ensuredQuestion.get("riskScore"));
                    model.addAttribute("riskLevel", ensuredQuestion.get("riskLevel"));
                    model.addAttribute("recommendations", ensuredQuestion.get("recommendations"));
                    model.addAttribute("endMessage", ensuredQuestion.get("text"));

                } else {
                    // No risk assessment data - calculate it
                    Map<String, Object> riskAssessment = decisionService.calculateRiskScore(updatedAnswers);
                    sessionService.saveRiskAssessment(
                            cleanSessionId,
                            (String) riskAssessment.get("riskLevel"),
                            (Integer) riskAssessment.get("riskScore"),
                            (java.util.List<String>) riskAssessment.get("recommendations")
                    );

                    model.addAttribute("riskScore", riskAssessment.get("riskScore"));
                    model.addAttribute("riskLevel", riskAssessment.get("riskLevel"));
                    model.addAttribute("recommendations", riskAssessment.get("recommendations"));
                    model.addAttribute("endMessage", "Thank you for completing the STI risk assessment!");
                }

                model.addAttribute("session", sessionService.getSessionWithDetails(cleanSessionId));
                model.addAttribute("answers", updatedAnswers);

                return "results";
            } else {
                model.addAttribute("question", ensuredQuestion);
                model.addAttribute("sessionId", cleanSessionId);
                model.addAttribute("answers", updatedAnswers); // This should never be null now
                model.addAttribute("canGoBack", true);

                return "questionnaire";
            }
        } catch (Exception e) {
            logger.error("Error in nextQuestion", e);
            model.addAttribute("error", "An error occurred: " + e.getMessage());
            return "error-page";
        }
    }

    @PostMapping("/back")
    public String previousQuestion(
            @RequestParam String sessionId,
            @RequestParam Map<String, String> allParams,
            HttpSession httpSession,
            Model model) {

        try {
            // Clean the session ID properly
            String cleanSessionId = cleanSessionId(sessionId);

            logger.info("Going back for session: {}", cleanSessionId);

            // Remove the last answer using the service
            Map<String, String> previousAnswers = sessionService.goBack(cleanSessionId);

            // Ensure previousAnswers is never null
            if (previousAnswers == null) {
                previousAnswers = new HashMap<>();
                logger.info("Initialized empty answers for back navigation");
            }

            // Get the previous question based on remaining answers
            Map<String, Object> previousQuestion = decisionService.getNextQuestion(previousAnswers);

            // Ensure the question structure is proper
            Map<String, Object> ensuredQuestion = ensureQuestionStructure(previousQuestion, "Back Navigation");

            model.addAttribute("question", ensuredQuestion);
            model.addAttribute("sessionId", cleanSessionId);
            model.addAttribute("answers", previousAnswers);
            model.addAttribute("canGoBack", !previousAnswers.isEmpty());

            return "questionnaire";

        } catch (Exception e) {
            logger.error("Error in goBack for session: {}", sessionId, e);
            model.addAttribute("error", "An error occurred while going back.");
            return "error-page";
        }
    }

    /**
     * Enhanced session ID cleaning
     */
    private String cleanSessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return sessionId;
        }

        String trimmed = sessionId.trim();

        // If the session ID contains commas, take the first part
        if (trimmed.contains(",")) {
            String[] parts = trimmed.split(",");
            for (String part : parts) {
                String cleanPart = part.trim();
                // Check if this part exists in the database
                if (sessionService.sessionExists(cleanPart)) {
                    logger.info("Using valid session ID part: {}", cleanPart);
                    return cleanPart;
                }
            }
            // If no valid part found, use the first one
            String cleanId = parts[0].trim();
            logger.warn("No valid session ID found, using first part: {}", cleanId);
            return cleanId;
        }

        return trimmed;
    }

    /**
     * Fixed version - returns a new mutable map instead of modifying the original
     */
    private Map<String, Object> ensureQuestionStructure(Map<String, Object> question, String context) {
        if (question == null) {
            logger.warn("{} is null", context);
            return createDefaultQuestion();
        }

        // Always create a new mutable HashMap to avoid UnsupportedOperationException
        Map<String, Object> mutableQuestion = new HashMap<>(question);

        // Handle text/question field mapping
        if (!mutableQuestion.containsKey("text") && mutableQuestion.containsKey("question")) {
            mutableQuestion.put("text", mutableQuestion.get("question"));
        }

        // Ensure required fields with defaults
        mutableQuestion.putIfAbsent("sectionTitle", "Health Assessment");
        mutableQuestion.putIfAbsent("progress", 0);
        mutableQuestion.putIfAbsent("questionIndex", 1);
        mutableQuestion.putIfAbsent("totalQuestions", 10);
        mutableQuestion.putIfAbsent("end", false);
        mutableQuestion.putIfAbsent("type", "radio");

        // Ensure key field exists
        if (!mutableQuestion.containsKey("key") && mutableQuestion.containsKey("id")) {
            mutableQuestion.put("key", mutableQuestion.get("id"));
        }

        logger.info("{} structure ensured - keys: {}", context, mutableQuestion.keySet());

        return mutableQuestion;
    }

    /**
     * Create a default question structure for error cases
     */
    private Map<String, Object> createDefaultQuestion() {
        Map<String, Object> defaultQuestion = new HashMap<>();
        defaultQuestion.put("text", "Please answer the following question:");
        defaultQuestion.put("sectionTitle", "Health Assessment");
        defaultQuestion.put("progress", 0);
        defaultQuestion.put("questionIndex", 1);
        defaultQuestion.put("totalQuestions", 10);
        defaultQuestion.put("end", false);
        defaultQuestion.put("type", "radio");
        defaultQuestion.put("key", "default");
        return defaultQuestion;
    }

    @GetMapping("/debug/database-status")
    @ResponseBody
    public Map<String, Object> debugDatabaseStatus() {
        return decisionService.debugDatabaseStatus();
    }

    // REST API endpoints
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

        @PostMapping("/debug/consent")
        public Map<String, Object> debugConsent(@RequestBody Map<String, String> answers) {
            return decisionService.debugConsentFlow(answers);
        }

        @GetMapping("/status")
        public Map<String, Object> status() {
            return decisionService.getDecisionTreeStatus();
        }
    }
}