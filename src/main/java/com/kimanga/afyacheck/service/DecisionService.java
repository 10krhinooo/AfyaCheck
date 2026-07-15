package com.kimanga.afyacheck.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.kimanga.afyacheck.model.Question;
import com.kimanga.afyacheck.repository.QuestionRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DecisionService {

    private static final Logger logger = LoggerFactory.getLogger(DecisionService.class);

    // --- CRITICAL FALLBACK LIST ---
    private static final List<String> CRITICAL_FALLBACK_QUESTIONS = List.of(
            "consent", "age", "gender", "sexual_activity", "hiv_tested", "previous_sti"
    );

    // --- ML REQUIRED FIELDS for Final Risk Assessment (to avoid warnings/failures on port 8002) ---
    // Note: The Python service relies on all these keys being present in the input dictionary.
    private static final List<String> ML_REQUIRED_FIELDS = List.of(
            "age", "gender", "sexual_activity", "marital_status", "education",
            "wealth_index", "hiv_tested", "sexual_partners", "condom_use",
            "recent_partners", "high_risk_partner", "sti_symptoms", "previous_sti", "transactional_sex"
    );

    private final DecisionTreeClient decisionTreeClient;
    private final SessionService sessionService;
    private final MLService mlService;
    private final QuestionRepository questionRepository;

    public DecisionService(DecisionTreeClient decisionTreeClient, SessionService sessionService,
                           MLService mlService, QuestionRepository questionRepository) {
        this.decisionTreeClient = decisionTreeClient;
        this.sessionService = sessionService;
        this.mlService = mlService;
        this.questionRepository = questionRepository;
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
                List<String> allQuestionKeys = getAllQuestionKeys();
                if (allQuestionKeys.contains("consent")) {
                    logger.info("Forcing 'consent' as the very first question from Java service.");
                    return createQuestionResponse("consent", cleanAnswers, sessionId);
                }
            }
            // *****************************************

            if (cleanAnswers.containsKey("consent")) {
                String consentAnswer = cleanAnswers.get("consent");
                if ("No".equals(consentAnswer) || "No, I do not consent".equals(consentAnswer)) {
                    return createConsentDeniedResponse(sessionId);
                }
            }

            List<String> allQuestionKeys = getAllQuestionKeys();

            // Pass the list of all questions to the client.
            String nextQuestionKey = decisionTreeClient.getNextQuestion(sessionId, cleanAnswers, allQuestionKeys);

            // DECISION TREE HAS ENOUGH INFORMATION - SHOW RESULTS IMMEDIATELY
            if (nextQuestionKey == null) {
                logger.info("Decision tree determined enough information gathered for session: {}, showing results immediately", sessionId);
                logger.info("Total questions answered: {}", cleanAnswers.size());
                return createEndSurveyResponse(cleanAnswers, sessionId);
            }

            return createQuestionResponse(nextQuestionKey, cleanAnswers, sessionId);

        } catch (Exception e) {
            logger.error("Error getting next question from decision tree", e);
            return getNextQuestionFallback(currentAnswers);
        }
    }

    // --- Private Utility Methods for Risk Calculation and Recommendations (Placed early for compilation) ---

    private int calculateFallbackRiskScore(Map<String, String> answers) {
        int score = 0;

        if ("No".equals(answers.get("condom_use")) || "Never".equals(answers.get("condom_use"))) score += 30;

        if (answers.containsKey("recent_partners")) {
            try {
                int partners = Integer.valueOf(answers.get("recent_partners"));
                if (partners >= 3) score += 25;
                else if (partners == 2) score += 15;
            } catch (NumberFormatException ignored) {}
        }

        if ("Yes".equals(answers.get("high_risk_partner"))) score += 20;
        if ("Yes".equals(answers.get("sti_symptoms"))) score += 15;
        if ("Yes".equals(answers.get("previous_sti"))) score += 10;
        if ("Yes".equals(answers.get("transactional_sex"))) score += 25;

        return Math.min(score, 100);
    }

    private String determineRiskLevel(int score) {
        if (score >= 50) return "High";
        if (score >= 25) return "Medium";
        return "Low";
    }

    private List<String> enhancedRecommendations(List<String> recommendations, Map<String, String> answers) {
        List<String> enhanced = new ArrayList<>(recommendations);

        if ("Never".equals(answers.get("condom_use"))) {
            enhanced.add("Consistent condom use can significantly reduce STI transmission risk");
        }

        if ("Yes".equals(answers.get("sti_symptoms"))) {
            enhanced.add("Consult a healthcare provider about your symptoms as soon as possible");
        }

        if ("Yes".equals(answers.get("high_risk_partner"))) {
            enhanced.add("Consider discussing mutual testing with your partner(s)");
        }

        return new ArrayList<>(new LinkedHashSet<>(enhanced));
    }

    private List<String> generateFallbackRecommendations(int score, Map<String, String> answers) {
        List<String> recommendations = new ArrayList<>();

        if (score >= 50) {
            recommendations.add("High risk detected: Consider immediate STI testing");
            recommendations.add("Consult healthcare provider for comprehensive screening");
            recommendations.add("Practice consistent condom use");
            recommendations.add("Discuss PrEP options with your doctor");
        } else if (score >= 25) {
            recommendations.add("Moderate risk: Schedule STI testing soon");
            recommendations.add("Consider regular screening every 6 months");
            recommendations.add("Review safer sex practices");
        } else {
            recommendations.add("Low risk: Maintain current protective behaviors");
            recommendations.add("Consider baseline testing for peace of mind");
            recommendations.add("Regular health check-ups are recommended");
        }

        return enhancedRecommendations(recommendations, answers);
    }

    private Map<String, Object> calculateFallbackRiskAssessment(Map<String, String> answers) {
        int riskScore = calculateFallbackRiskScore(answers);
        String riskLevel = determineRiskLevel(riskScore);
        List<String> recommendations = generateFallbackRecommendations(riskScore, answers);

        String recommendationsString = String.join("; ", recommendations);

        return Map.of(
                "riskScore", riskScore,
                "riskLevel", riskLevel,
                "recommendations", recommendationsString,
                "hivProbability", riskScore / 100.0,
                "confidence", 0.75, // Heuristic confidence for fallback
                "modelUsed", false
        );
    }

    // --- Core Question/Response Formatting Methods ---

    private Map<String, Object> createQuestionResponse(String questionKey,
                                                       Map<String, String> currentAnswers,
                                                       String sessionId) {

        Optional<Question> questionOpt = questionRepository.findByQuestionKeyAndIsActiveTrue(questionKey);

        if (questionOpt.isEmpty()) {
            logger.error("Question not found in database: {}", questionKey);
            return createDefaultQuestionResponse(sessionId);
        }

        Question question = questionOpt.get();
        Map<String, Object> response = convertQuestionToMap(question);

        response.put("sessionId", sessionId);

        int totalQuestions = getTotalActiveQuestionsCount();
        int answeredCount = currentAnswers.size();
        int progress = (int) ((answeredCount / (double) totalQuestions) * 100);

        response.put("progress", progress);
        response.put("questionIndex", answeredCount + 1);
        response.put("totalQuestions", totalQuestions);

        logger.info("Generated question: {} (progress: {}%)", questionKey, progress);

        return response;
    }

    private Map<String, Object> convertQuestionToMap(Question question) {
        Map<String, Object> questionMap = new HashMap<>();

        questionMap.put("key", question.getQuestionKey());
        questionMap.put("text", question.getQuestionText());
        questionMap.put("description", question.getDescription());
        questionMap.put("type", mapQuestionType(question.getQuestionType()));
        questionMap.put("sectionTitle", question.getSectionTitle());
        questionMap.put("displayOrder", question.getDisplayOrder());

        String optionsString = parseOptionsToString(question.getOptions(), question.getQuestionType());
        questionMap.put("options", optionsString);

        logger.debug("Question: {}, Options String: {}", question.getQuestionKey(), optionsString);

        if ("number".equals(mapQuestionType(question.getQuestionType()))) {
            Map<String, Object> validation = new HashMap<>();
            validation.put("min", question.getMinValue() != null ? question.getMinValue() : 0);
            validation.put("max", question.getMaxValue() != null ? question.getMaxValue() : 100);
            questionMap.put("validation", validation);
            questionMap.put("min", validation.get("min"));
            questionMap.put("max", validation.get("max"));
        }

        return questionMap;
    }

    private String parseOptionsToString(List<String> optionsList, String questionType) {
        if (optionsList == null || optionsList.isEmpty()) {
            if ("yes_no".equalsIgnoreCase(questionType)) {
                return "Yes,No";
            }
            return "";
        }

        try {
            return String.join(",", optionsList);
        } catch (Exception e) {
            logger.warn("Error converting options list to string: {}. Using fallback.", optionsList);
            if ("yes_no".equalsIgnoreCase(questionType)) {
                return "Yes,No";
            }
            return "";
        }
    }

    private String mapQuestionType(String dbQuestionType) {
        if (dbQuestionType == null) return "radio";

        switch (dbQuestionType.toLowerCase()) {
            case "yes_no":
            case "multiple_choice":
                return "radio";
            case "number":
                return "number";
            case "text":
                return "text";
            default:
                return "radio";
        }
    }

    private int getTotalActiveQuestionsCount() {
        try {
            Long count = questionRepository.countByIsActiveTrue();
            return count != null ? count.intValue() : 40;
        } catch (Exception e) {
            logger.error("Error getting question count from database", e);
            return 40;
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

        Map<String, Object> riskAssessment = calculateRiskScoreWithML(currentAnswers);
        response.putAll(riskAssessment);

        logger.info("Survey completed for session: {} with {} questions answered", sessionId, currentAnswers.size());

        return response;
    }

    private Map<String, Object> createDefaultQuestionResponse(String sessionId) {
        Map<String, Object> response = new HashMap<>();
        response.put("key", "default");
        response.put("text", "Please answer the following question:");
        response.put("description", "Please answer the following question:");
        response.put("type", "radio");
        response.put("sectionTitle", "Health Assessment");
        response.put("options", "Yes,No");
        response.put("sessionId", sessionId);
        response.put("progress", 0);
        response.put("questionIndex", 1);
        return response;
    }

    // --- Fallback and Utility Methods ---

    private Map<String, Object> getNextQuestionFallback(Map<String, String> currentAnswers) {
        logger.warn("Using fallback question logic (Database sequential order).");

        Map<String, String> cleanAnswers = new HashMap<>(currentAnswers);
        String sessionId = cleanAnswers.remove("_sessionId");
        if (sessionId == null) sessionId = "fallback-session";

        List<Question> activeQuestions = questionRepository.findByIsActiveTrueOrderByDisplayOrderAsc();

        if (hasEnoughInformationFallback(cleanAnswers)) {
            logger.info("Fallback logic determined enough information after {} questions", cleanAnswers.size());
            return createEndSurveyResponse(cleanAnswers, sessionId);
        }

        for (Question question : activeQuestions) {
            if (!cleanAnswers.containsKey(question.getQuestionKey())) {
                Map<String, Object> response = convertQuestionToMap(question);
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

    /**
     * Fetches all active question keys from the database. Used by DecisionTreeClient.
     */
    public List<String> getAllQuestionKeys() {
        try {
            List<Question> activeQuestions = questionRepository.findByIsActiveTrue();
            return activeQuestions.stream()
                    .map(Question::getQuestionKey)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting question keys from database, using critical fallback list.", e);
            return CRITICAL_FALLBACK_QUESTIONS;
        }
    }

    public Map<String, Object> calculateRiskScoreWithML(Map<String, String> answers) {
        try {
            // FIX: Map only the required fields for the ML model (port 8002)
            Map<String, String> mlAnswers = mapToMLFields(answers);
            logger.info("Calling ML service with mapped answers: {}", mlAnswers.keySet());

            Map<String, Object> mlResult = mlService.predictRisk(mlAnswers);

            if (mlResult != null && Boolean.TRUE.equals(mlResult.get("success"))) {
                logger.info("ML service returned successful prediction");
                return extractMLResults(mlResult, answers);
            } else {
                logger.warn("ML service returned error or unsuccessful, using fallback");
                return calculateFallbackRiskAssessment(answers);
            }

        } catch (Exception e) {
            logger.error("Error calling ML service, using fallback assessment", e);
            return calculateFallbackRiskAssessment(answers);
        }
    }

    /**
     * FIX 2: Filters all answers down to ONLY the fields required by the ML model.
     * This prevents the ML model from failing/warning about missing data.
     */
    private Map<String, String> mapToMLFields(Map<String, String> answers) {
        Map<String, String> mlAnswers = new HashMap<>();

        // Use the defined list of required ML fields
        for (String field : ML_REQUIRED_FIELDS) {
            if (answers.containsKey(field)) {
                mlAnswers.put(field, answers.get(field));
            } else {
                // Use intelligent defaults instead of N/A_ML
                String defaultValue = getDefaultValueForField(field);
                mlAnswers.put(field, defaultValue);
                logger.debug("Field {} not provided, using default: {}", field, defaultValue);
            }
        }
        return mlAnswers;
    }

    /**
     * Returns appropriate default values for ML model fields based on field type and context
     */
    private String getDefaultValueForField(String field) {
        switch (field) {
            // Demographic fields
            case "age":
                return "30"; // Slightly older default

            case "gender":
                return "Male";

            case "sexual_activity":
                return "No"; // Critical - if unknown, assume no activity (lowest risk)

            case "marital_status":
                return "Single";

            case "education":
                return "College/University"; // Higher education often correlates with lower risk

            case "wealth_index":
                return "Middle income";

            // Sexual behavior fields - default to ABSOLUTE LOWEST RISK when unknown
            case "sexual_partners":
            case "recent_partners":
                return "0"; // No partners

            case "condom_use":
                return "Always"; // Always use protection

            // All risk factors default to NO
            case "high_risk_partner":
            case "sti_symptoms":
            case "previous_sti":
            case "transactional_sex":
            case "hiv_tested": // If never tested, assume low risk
                return "No";

            default:
                return "No";
        }
    }
    private Map<String, Object> extractMLResults(Map<String, Object> mlResult, Map<String, String> answers) {
        // Retrieve confidence directly from the ML service output (mlResult)
        Double confidence = (Double) mlResult.get("confidence");

        // --- FIX: Round the confidence to 2 decimal places ---
        double rawConfidence = confidence != null ? confidence : 0.85;
        double FINAL_CONFIDENCE = Math.round(rawConfidence * 100.0) / 100.0;
        // -----------------------------------------------------

        Double hivProbability = (Double) mlResult.get("hivProbability");
        Integer riskScore = (Integer) mlResult.get("riskScore");
        String riskLevel = (String) mlResult.get("riskLevel");
        Boolean modelUsed = (Boolean) mlResult.get("modelUsed");

        @SuppressWarnings("unchecked")
        List<String> mlRecommendations = (List<String>) mlResult.get("recommendations");

        List<String> enhancedRecommendations = enhancedRecommendations(
                mlRecommendations != null ? mlRecommendations : new ArrayList<>(),
                answers
        );

        String recommendationsString = String.join("; ", enhancedRecommendations);

        return Map.of(
                "riskScore", riskScore != null ? riskScore : calculateFallbackRiskScore(answers),
                "riskLevel", riskLevel != null ? riskLevel : determineRiskLevel(
                        riskScore != null ? riskScore : calculateFallbackRiskScore(answers)
                ),
                "recommendations", recommendationsString, // This holds the final string output
                "hivProbability", hivProbability != null ? hivProbability : 0.0,
                "confidence", FINAL_CONFIDENCE,
                "modelUsed", modelUsed != null ? modelUsed : false
        );
    }

    public Map<String, Object> calculateRiskScore(Map<String, String> answers) {
        return calculateRiskScoreWithML(answers);
    }

    public Map<String, Object> debugQuestionDatabase() {
        Map<String, Object> debugInfo = new HashMap<>();

        try {
            List<Question> allQuestions = questionRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
            debugInfo.put("totalActiveQuestions", allQuestions.size());

            List<Map<String, Object>> questionsList = new ArrayList<>();
            for (Question q : allQuestions) {
                Map<String, Object> qInfo = new HashMap<>();
                qInfo.put("key", q.getQuestionKey());
                qInfo.put("text", q.getQuestionText());
                qInfo.put("type", q.getQuestionType());
                qInfo.put("section", q.getSectionTitle());
                qInfo.put("order", q.getDisplayOrder());
                qInfo.put("options", parseOptionsToString(q.getOptions(), q.getQuestionType()));
                questionsList.add(qInfo);
            }
            debugInfo.put("questions", questionsList);

        } catch (Exception e) {
            debugInfo.put("error", "Failed to fetch questions: " + e.getMessage());
        }

        return debugInfo;
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
                "totalQuestionsInDatabase", getTotalActiveQuestionsCount(),
                "timestamp", new Date().toString()
        );
    }
}