package com.kimanga.afyacheck.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Computes the final risk score/level/recommendations for a completed
 * answer set, via the ML service when available and a rule-based fallback
 * otherwise. Split out of DecisionService, which is concerned with
 * sequencing questions rather than scoring a finished answer set.
 */
@Service
public class RiskScoringService {

    private static final Logger logger = LoggerFactory.getLogger(RiskScoringService.class);

    // --- ML REQUIRED FIELDS for Final Risk Assessment (to avoid warnings/failures on port 8002) ---
    // Note: The Python service relies on all these keys being present in the input dictionary.
    private static final List<String> ML_REQUIRED_FIELDS = List.of(
            "age", "gender", "sexual_activity", "marital_status", "education",
            "wealth_index", "hiv_tested", "sexual_partners", "condom_use",
            "recent_partners", "high_risk_partner", "sti_symptoms", "previous_sti", "transactional_sex"
    );

    private final MLService mlService;

    public RiskScoringService(MLService mlService) {
        this.mlService = mlService;
    }

    public Map<String, Object> calculateRiskScore(Map<String, String> answers) {
        return calculateRiskScoreWithML(answers);
    }

    public Map<String, Object> calculateRiskScoreWithML(Map<String, String> answers) {
        try {
            // Map only the required fields for the ML model (port 8002)
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
     * Filters all answers down to ONLY the fields required by the ML model.
     * This prevents the ML model from failing/warning about missing data.
     */
    private Map<String, String> mapToMLFields(Map<String, String> answers) {
        Map<String, String> mlAnswers = new HashMap<>();

        for (String field : ML_REQUIRED_FIELDS) {
            if (answers.containsKey(field)) {
                mlAnswers.put(field, answers.get(field));
            } else {
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

            case "sexual_partners":
            case "recent_partners":
                return "0"; // No partners

            case "condom_use":
                return "Always"; // Always use protection

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
        Double confidence = (Double) mlResult.get("confidence");

        double rawConfidence = confidence != null ? confidence : 0.85;
        double finalConfidence = Math.round(rawConfidence * 100.0) / 100.0;

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
                "recommendations", recommendationsString,
                "hivProbability", hivProbability != null ? hivProbability : 0.0,
                "confidence", finalConfidence,
                "modelUsed", modelUsed != null ? modelUsed : false
        );
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
}
