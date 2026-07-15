package com.kimanga.afyacheck.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.kimanga.afyacheck.model.Question;

import java.util.*;

/**
 * Orchestrates the questionnaire flow: decides what to ask next (via the
 * decision-tree service, falling back to a database-order sequence), and
 * triggers final risk scoring once enough information is gathered. Question
 * formatting is delegated to QuestionFormattingService and risk scoring to
 * RiskScoringService.
 */
@Service
public class DecisionService {

    private static final Logger logger = LoggerFactory.getLogger(DecisionService.class);

    private final DecisionTreeClient decisionTreeClient;
    private final RiskScoringService riskScoringService;
    private final QuestionFormattingService questionFormattingService;

    public DecisionService(DecisionTreeClient decisionTreeClient,
                           RiskScoringService riskScoringService,
                           QuestionFormattingService questionFormattingService) {
        this.decisionTreeClient = decisionTreeClient;
        this.riskScoringService = riskScoringService;
        this.questionFormattingService = questionFormattingService;
    }

    public Map<String, Object> getNextQuestion(Map<String, String> currentAnswers) {
        try {
            String sessionId = currentAnswers.get("_sessionId");
            if (sessionId == null) {
                logger.warn("No session ID provided, using fallback logic");
                return getNextQuestionFallback(currentAnswers);
            }

            Map<String, String> cleanAnswers = new HashMap<>(currentAnswers);
            cleanAnswers.remove("_sessionId");

            // *** FIX 1: FORCE CONSENT ON FIRST CALL ***
            if (cleanAnswers.isEmpty()) {
                List<String> allQuestionKeys = questionFormattingService.getAllQuestionKeys();
                if (allQuestionKeys.contains("consent")) {
                    logger.info("Forcing 'consent' as the very first question from Java service.");
                    return questionFormattingService.createQuestionResponse("consent", cleanAnswers, sessionId);
                }
            }
            // *****************************************

            if (cleanAnswers.containsKey("consent")) {
                String consentAnswer = cleanAnswers.get("consent");
                if ("No".equals(consentAnswer) || "No, I do not consent".equals(consentAnswer)) {
                    return createConsentDeniedResponse(sessionId);
                }
            }

            List<String> allQuestionKeys = questionFormattingService.getAllQuestionKeys();

            // Pass the list of all questions to the client.
            String nextQuestionKey = decisionTreeClient.getNextQuestion(sessionId, cleanAnswers, allQuestionKeys);

            // DECISION TREE HAS ENOUGH INFORMATION - SHOW RESULTS IMMEDIATELY
            if (nextQuestionKey == null) {
                logger.info("Decision tree determined enough information gathered for session: {}, showing results immediately", sessionId);
                logger.info("Total questions answered: {}", cleanAnswers.size());
                return createEndSurveyResponse(cleanAnswers, sessionId);
            }

            return questionFormattingService.createQuestionResponse(nextQuestionKey, cleanAnswers, sessionId);

        } catch (Exception e) {
            logger.error("Error getting next question from decision tree", e);
            return getNextQuestionFallback(currentAnswers);
        }
    }

    private Map<String, Object> createConsentDeniedResponse(String sessionId) {
        Map<String, Object> response = new HashMap<>();
        response.put("end", true);
        response.put("text", "Thank you for your consideration. The assessment has been ended as you did not provide consent.");
        response.put("sessionId", sessionId);
        response.put("progress", 100);
        response.put("consentDenied", true);

        return response;
    }

    private Map<String, Object> createEndSurveyResponse(Map<String, String> currentAnswers, String sessionId) {
        Map<String, Object> response = new HashMap<>();
        response.put("end", true);
        response.put("text", "Thank you for completing the assessment! Calculating your risk level...");
        response.put("sessionId", sessionId);
        response.put("progress", 100);
        response.put("questionsAnswered", currentAnswers.size());

        Map<String, Object> riskAssessment = riskScoringService.calculateRiskScoreWithML(currentAnswers);
        response.putAll(riskAssessment);

        logger.info("Survey completed for session: {} with {} questions answered", sessionId, currentAnswers.size());

        return response;
    }

    // --- Fallback sequencing (used when the decision-tree call itself fails) ---

    private Map<String, Object> getNextQuestionFallback(Map<String, String> currentAnswers) {
        logger.warn("Using fallback question logic (Database sequential order).");

        Map<String, String> cleanAnswers = new HashMap<>(currentAnswers);
        String sessionId = cleanAnswers.remove("_sessionId");
        if (sessionId == null) sessionId = "fallback-session";

        List<Question> activeQuestions = questionFormattingService.getActiveQuestionsInOrder();

        if (hasEnoughInformationFallback(cleanAnswers)) {
            logger.info("Fallback logic determined enough information after {} questions", cleanAnswers.size());
            return createEndSurveyResponse(cleanAnswers, sessionId);
        }

        for (Question question : activeQuestions) {
            if (!cleanAnswers.containsKey(question.getQuestionKey())) {
                Map<String, Object> response = questionFormattingService.convertQuestionToMap(question);
                response.put("sessionId", sessionId);

                int totalQuestions = activeQuestions.size();
                int answeredCount = cleanAnswers.size();
                int progress = (int) ((answeredCount / (double) totalQuestions) * 100);

                response.put("progress", progress);
                response.put("questionIndex", answeredCount + 1);
                response.put("totalQuestions", totalQuestions);

                logger.info("Fallback question: {} (progress: {}%)", question.getQuestionKey(), progress);
                return response;
            }
        }

        return createEndSurveyResponse(cleanAnswers, sessionId);
    }

    private boolean hasEnoughInformationFallback(Map<String, String> answers) {
        int answered = answers.size();

        if (!"Yes".equals(answers.get("consent"))) {
            return false;
        }

        boolean hasDemographics = answers.containsKey("age") && answers.containsKey("gender");
        boolean hasSexualActivityInfo = answers.containsKey("sexual_activity");

        if (hasDemographics && hasSexualActivityInfo) {
            long riskFactors = answers.keySet().stream()
                    .filter(k -> k.matches("recent_partners|condom_use|high_risk_partner|sti_symptoms|previous_sti"))
                    .count();

            return riskFactors >= 3 || answered >= 10;
        }

        return answered >= 15;
    }

    public Map<String, Object> calculateRiskScore(Map<String, String> answers) {
        return riskScoringService.calculateRiskScore(answers);
    }

    public Map<String, Object> debugQuestionDatabase() {
        return questionFormattingService.debugQuestionDatabase();
    }

    public Map<String, Object> getDecisionTreeStatus() {
        boolean healthy = decisionTreeClient.isServiceHealthy();
        return Map.of(
                "status", healthy ? "HEALTHY" : "DEGRADED",
                "serviceUrl", healthy ? "Connected" : "Disconnected",
                "timestamp", new Date().toString()
        );
    }

    public Map<String, Object> debugDatabaseStatus() {
        return Map.of(
                "decisionTreeServiceAvailable", decisionTreeClient.isServiceHealthy(),
                "totalQuestionsInDatabase", questionFormattingService.getTotalActiveQuestionsCount(),
                "timestamp", new Date().toString()
        );
    }
}