package com.kimanga.afyacheck.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.kimanga.afyacheck.model.User;
import com.kimanga.afyacheck.service.DecisionService;
import com.kimanga.afyacheck.service.SessionService;
import com.kimanga.afyacheck.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/questionnaire")
public class QuestionController {

    private static final Logger logger = LoggerFactory.getLogger(QuestionController.class);

    private final DecisionService decisionService;
    private final SessionService sessionService;
    private final UserService userService;

    public QuestionController(DecisionService decisionService, SessionService sessionService, UserService userService) {
        this.decisionService = decisionService;
        this.sessionService = sessionService;
        this.userService = userService;
    }

    @GetMapping("/start")
    public String startQuestionnaire(HttpSession httpSession, Model model, Authentication authentication) {
        try {
            String sessionId = httpSession.getId();
            String createdSessionId = sessionService.createOrGetSession(sessionId);

            // /questionnaire/** requires authentication (see SecurityConfig), so
            // the current user is always resolvable here.
            userService.resolveCurrentUser(authentication)
                    .ifPresent(user -> sessionService.assignUser(createdSessionId, user));

            logger.info("Starting questionnaire for session: {} (created: {})", sessionId, createdSessionId);

            // Initialize with empty answers map - will start with consent
            Map<String, String> initialAnswers = new HashMap<>();
            initialAnswers.put("_sessionId", createdSessionId);

            Map<String, Object> firstQuestion = decisionService.getNextQuestion(initialAnswers);

            if (firstQuestion.containsKey("options") && firstQuestion.get("options") instanceof String) {
                String optionsString = (String) firstQuestion.get("options");
                List<String> optionsList = convertOptionsStringToList(optionsString);
                firstQuestion.put("options", optionsList);
            }

            logger.info("Received question data - keys: {}, has error: {}",
                    firstQuestion.keySet(), firstQuestion.containsKey("error"));

            // Check for error before ensuring structure
            if (firstQuestion.containsKey("error")) {
                logger.error("Error in first question: {}", firstQuestion.get("error"));
                model.addAttribute("error", firstQuestion.get("error"));
                model.addAttribute("sessionId", createdSessionId);
                return "error-page";
            }

            // Ensure answers is never null
            Map<String, String> safeAnswers = sessionService.getCurrentAnswers(createdSessionId);
            if (safeAnswers == null) {
                safeAnswers = new HashMap<>();
                logger.info("Initialized empty answers for new session");
            }

            model.addAttribute("question", firstQuestion);
            model.addAttribute("sessionId", createdSessionId);
            model.addAttribute("answers", safeAnswers);
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
            String cleanSessionId = cleanSessionId(sessionId);

            Map<String, String> answers = sessionService.getCurrentAnswers(cleanSessionId);
            if (answers == null) {
                answers = new HashMap<>();
            }

            sessionService.saveAnswers(cleanSessionId, formParams);

            Map<String, String> updatedAnswers = new HashMap<>(answers);
            for (Map.Entry<String, String> entry : formParams.entrySet()) {
                if (!"sessionId".equals(entry.getKey())) {
                    updatedAnswers.put(entry.getKey(), entry.getValue());
                }
            }

            updatedAnswers.put("_sessionId", cleanSessionId);

            Map<String, Object> nextStep = decisionService.getNextQuestion(updatedAnswers);

            if (nextStep.containsKey("options") && nextStep.get("options") instanceof String) {
                String optionsString = (String) nextStep.get("options");
                List<String> optionsList = convertOptionsStringToList(optionsString);
                nextStep.put("options", optionsList);
            }

            if (nextStep.containsKey("end") && Boolean.TRUE.equals(nextStep.get("end"))) {
                logger.info("Survey ended for session: {} with {} questions answered",
                        cleanSessionId, updatedAnswers.size());

                // FIXED: Handle consent denied properly
                if (Boolean.TRUE.equals(nextStep.get("consentDenied"))) {
                    // Save consent denied as a completed session
                    sessionService.completeSession(cleanSessionId);

                    // Add consent denied message to model
                    model.addAttribute("endMessage", nextStep.get("text"));
                    model.addAttribute("consentDenied", true);
                    model.addAttribute("sessionId", cleanSessionId);
                    model.addAttribute("session", sessionService.getSessionWithDetails(cleanSessionId));

                    // Return results page with consent denied message
                    return "results";
                }

                // Convert recommendations String to List for the template
                List<String> recommendationsList = new ArrayList<>();
                if (nextStep.containsKey("recommendations")) {
                    String recommendationsStr = (String) nextStep.get("recommendations");
                    if (recommendationsStr != null && !recommendationsStr.isEmpty()) {
                        // Split by semicolon (as used in DecisionService)
                        recommendationsList = Arrays.asList(recommendationsStr.split("; "));
                    }
                }

                if (nextStep.containsKey("riskScore")) {
                    // For SessionService - keep as String or convert to List based on what it expects
                    String recommendationsStr = (String) nextStep.get("recommendations");

                    sessionService.saveRiskAssessment(
                            cleanSessionId,
                            (String) nextStep.get("riskLevel"),
                            (Integer) nextStep.get("riskScore"),
                            recommendationsStr // Pass as String to SessionService
                    );

                    model.addAttribute("riskScore", nextStep.get("riskScore"));
                    model.addAttribute("riskLevel", nextStep.get("riskLevel"));
                    model.addAttribute("recommendations", recommendationsList); // Pass as List to template
                    model.addAttribute("endMessage", nextStep.get("text"));
                    model.addAttribute("confidence", nextStep.get("confidence"));
                    model.addAttribute("modelUsed", nextStep.get("modelUsed"));
                    model.addAttribute("questionsAnswered", updatedAnswers.size());

                } else {
                    Map<String, Object> riskAssessment = decisionService.calculateRiskScore(updatedAnswers);

                    // Convert for template
                    List<String> fallbackRecommendations = new ArrayList<>();
                    if (riskAssessment.containsKey("recommendations")) {
                        String recsStr = (String) riskAssessment.get("recommendations");
                        if (recsStr != null && !recsStr.isEmpty()) {
                            fallbackRecommendations = Arrays.asList(recsStr.split("; "));
                        }
                    }

                    sessionService.saveRiskAssessment(
                            cleanSessionId,
                            (String) riskAssessment.get("riskLevel"),
                            (Integer) riskAssessment.get("riskScore"),
                            (String) riskAssessment.get("recommendations") // Pass as String to SessionService
                    );

                    model.addAttribute("riskScore", riskAssessment.get("riskScore"));
                    model.addAttribute("riskLevel", riskAssessment.get("riskLevel"));
                    model.addAttribute("recommendations", fallbackRecommendations); // Pass as List to template
                    model.addAttribute("endMessage", "Thank you for completing the STI risk assessment!");
                    model.addAttribute("confidence", riskAssessment.get("confidence"));
                    model.addAttribute("modelUsed", riskAssessment.get("modelUsed"));
                    model.addAttribute("questionsAnswered", updatedAnswers.size());
                }

                model.addAttribute("session", sessionService.getSessionWithDetails(cleanSessionId));
                model.addAttribute("answers", updatedAnswers);

                return "results";
            } else {
                model.addAttribute("question", nextStep);
                model.addAttribute("sessionId", cleanSessionId);
                model.addAttribute("answers", updatedAnswers);
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

            // Add session ID for decision service
            previousAnswers.put("_sessionId", cleanSessionId);

            // Get the previous question based on remaining answers
            Map<String, Object> previousQuestion = decisionService.getNextQuestion(previousAnswers);

            if (previousQuestion.containsKey("options") && previousQuestion.get("options") instanceof String) {
                String optionsString = (String) previousQuestion.get("options");
                List<String> optionsList = convertOptionsStringToList(optionsString);
                previousQuestion.put("options", optionsList);
            }

            model.addAttribute("question", previousQuestion);
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
    private List<String> convertOptionsStringToList(String optionsString) {
        if (optionsString == null || optionsString.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(optionsString.split(","));
    }

    @GetMapping("/debug/database-status")
    @ResponseBody
    public Map<String, Object> debugDatabaseStatus() {
        return decisionService.debugDatabaseStatus();
    }

    @GetMapping("/debug/questions")
    @ResponseBody
    public Map<String, Object> debugQuestions() {
        return decisionService.debugQuestionDatabase();
    }

    // REST API endpoints, consumed by the React questionnaire page. Reuses
    // DecisionService/SessionService exactly as the MVC controller above
    // does; only the request/response shape differs (JSON instead of a
    // Thymeleaf Model).
    @RestController
    @RequestMapping("/api/questions")
    public static class QuestionApiController {

        private static final Logger logger = LoggerFactory.getLogger(QuestionApiController.class);

        private final DecisionService decisionService;
        private final SessionService sessionService;
        private final UserService userService;

        public QuestionApiController(DecisionService decisionService, SessionService sessionService, UserService userService) {
            this.decisionService = decisionService;
            this.sessionService = sessionService;
            this.userService = userService;
        }

        @PostMapping("/start")
        public ResponseEntity<Map<String, Object>> start(Authentication authentication) {
            String sessionId = UUID.randomUUID().toString();
            String createdSessionId = sessionService.createOrGetSession(sessionId);

            userService.resolveCurrentUser(authentication)
                    .ifPresent(user -> sessionService.assignUser(createdSessionId, user));

            Map<String, String> initialAnswers = new HashMap<>();
            initialAnswers.put("_sessionId", createdSessionId);

            Map<String, Object> firstQuestion = decisionService.getNextQuestion(initialAnswers);
            normalizeOptions(firstQuestion);

            if (firstQuestion.containsKey("error")) {
                return ResponseEntity.badRequest().body(firstQuestion);
            }

            // The decision service can signal completion on the very first
            // call (e.g. its Python backends are unavailable and it falls
            // back to an immediate assessment), same as it can mid-survey;
            // handle it the same way next() does rather than returning a
            // malformed "question" response.
            if (Boolean.TRUE.equals(firstQuestion.get("end"))) {
                return ResponseEntity.ok(buildResultResponse(createdSessionId, firstQuestion, initialAnswers));
            }

            Map<String, String> safeAnswers = sessionService.getCurrentAnswers(createdSessionId);
            return ResponseEntity.ok(buildQuestionResponse(createdSessionId, firstQuestion,
                    safeAnswers != null ? safeAnswers : new HashMap<>(), false));
        }

        @PostMapping("/next")
        public ResponseEntity<Map<String, Object>> next(@RequestBody AnswerSubmission submission) {
            String cleanSessionId = cleanSessionId(submission.getSessionId());

            Map<String, String> answers = sessionService.getCurrentAnswers(cleanSessionId);
            if (answers == null) {
                answers = new HashMap<>();
            }

            Map<String, String> formParams = submission.toFormParams();
            sessionService.saveAnswers(cleanSessionId, formParams);

            Map<String, String> updatedAnswers = new HashMap<>(answers);
            updatedAnswers.putAll(formParams);
            updatedAnswers.put("_sessionId", cleanSessionId);

            Map<String, Object> nextStep = decisionService.getNextQuestion(updatedAnswers);
            normalizeOptions(nextStep);

            if (Boolean.TRUE.equals(nextStep.get("end"))) {
                return ResponseEntity.ok(buildResultResponse(cleanSessionId, nextStep, updatedAnswers));
            }

            return ResponseEntity.ok(buildQuestionResponse(cleanSessionId, nextStep, updatedAnswers, true));
        }

        @PostMapping("/back")
        public ResponseEntity<Map<String, Object>> back(@RequestBody Map<String, String> body) {
            String cleanSessionId = cleanSessionId(body.get("sessionId"));

            Map<String, String> previousAnswers = sessionService.goBack(cleanSessionId);
            if (previousAnswers == null) {
                previousAnswers = new HashMap<>();
            }
            previousAnswers.put("_sessionId", cleanSessionId);

            Map<String, Object> previousQuestion = decisionService.getNextQuestion(previousAnswers);
            normalizeOptions(previousQuestion);

            return ResponseEntity.ok(buildQuestionResponse(cleanSessionId, previousQuestion,
                    previousAnswers, !previousAnswers.isEmpty()));
        }

        @GetMapping("/status")
        public Map<String, Object> status() {
            return decisionService.getDecisionTreeStatus();
        }

        private Map<String, Object> buildQuestionResponse(String sessionId, Map<String, Object> question,
                                                            Map<String, String> answers, boolean canGoBack) {
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("question", question);
            response.put("answers", answers);
            response.put("canGoBack", canGoBack);
            response.put("ended", false);
            return response;
        }

        private Map<String, Object> buildResultResponse(String sessionId, Map<String, Object> nextStep,
                                                          Map<String, String> updatedAnswers) {
            logger.info("Survey ended for session: {} with {} questions answered",
                    sessionId, updatedAnswers.size());

            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("ended", true);
            response.put("answers", updatedAnswers);
            response.put("questionsAnswered", updatedAnswers.size());

            if (Boolean.TRUE.equals(nextStep.get("consentDenied"))) {
                sessionService.completeSession(sessionId);
                response.put("consentDenied", true);
                response.put("endMessage", nextStep.get("text"));
                return response;
            }

            Map<String, Object> riskAssessment;
            if (nextStep.containsKey("riskScore")) {
                riskAssessment = nextStep;
            } else {
                riskAssessment = decisionService.calculateRiskScore(updatedAnswers);
            }

            String recommendationsStr = (String) riskAssessment.get("recommendations");
            List<String> recommendationsList = recommendationsStr != null && !recommendationsStr.isEmpty()
                    ? Arrays.asList(recommendationsStr.split("; "))
                    : new ArrayList<>();

            sessionService.saveRiskAssessment(
                    sessionId,
                    (String) riskAssessment.get("riskLevel"),
                    (Integer) riskAssessment.get("riskScore"),
                    recommendationsStr
            );

            response.put("consentDenied", false);
            response.put("riskScore", riskAssessment.get("riskScore"));
            response.put("riskLevel", riskAssessment.get("riskLevel"));
            response.put("recommendations", recommendationsList);
            response.put("endMessage", nextStep.containsKey("riskScore")
                    ? nextStep.get("text") : "Thank you for completing the STI risk assessment!");
            response.put("confidence", riskAssessment.get("confidence"));
            response.put("modelUsed", riskAssessment.get("modelUsed"));
            return response;
        }

        @SuppressWarnings("unchecked")
        private void normalizeOptions(Map<String, Object> question) {
            if (question.get("options") instanceof String optionsString) {
                question.put("options", convertOptionsStringToList(optionsString));
            }
        }

        private List<String> convertOptionsStringToList(String optionsString) {
            if (optionsString == null || optionsString.trim().isEmpty()) {
                return new ArrayList<>();
            }
            return Arrays.asList(optionsString.split(","));
        }

        /**
         * Mirrors QuestionController.cleanSessionId; duplicated because this
         * is a static nested class and can't call the outer instance's
         * private method.
         */
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
                        return cleanPart;
                    }
                }
                return parts[0].trim();
            }
            return trimmed;
        }
    }

    /**
     * multiple_choice answers arrive as a String array; every other answer
     * type is a single String. Flattened to form-param style (comma-joined)
     * before being handed to SessionService/DecisionService, which is what
     * the Python decision-tree service already expects (matching the old
     * form-encoded submission's shape).
     */
    public static class AnswerSubmission {
        private String sessionId;
        private Map<String, Object> answers = new HashMap<>();

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public Map<String, Object> getAnswers() {
            return answers;
        }

        public void setAnswers(Map<String, Object> answers) {
            this.answers = answers;
        }

        @SuppressWarnings("unchecked")
        public Map<String, String> toFormParams() {
            Map<String, String> formParams = new HashMap<>();
            for (Map.Entry<String, Object> entry : answers.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof List<?> list) {
                    formParams.put(entry.getKey(), String.join(",", (List<String>) list));
                } else if (value != null) {
                    formParams.put(entry.getKey(), value.toString());
                }
            }
            return formParams;
        }
    }
}