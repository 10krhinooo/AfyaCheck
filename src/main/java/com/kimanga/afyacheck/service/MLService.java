package com.kimanga.afyacheck.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;

@Service
public class MLService {

    private static final Logger logger = LoggerFactory.getLogger(MLService.class);

    private final RestTemplate restTemplate;

    // Assuming the ML risk predictor endpoint is separate from the Decision Tree sequencer.
    @Value("${ml.risk.service.url:http://localhost:8000}")
    private String mlRiskServiceUrl;

    // --- MAPPINGS (Used only for normalization, not for integer encoding) ---
    // The Python service handles all float encoding based on these normalized string values.
    private static final List<String> MARITAL_STATUS_OPTIONS = List.of("single", "married", "divorced", "widowed", "separated", "living with partner");
    private static final List<String> EDUCATION_OPTIONS = List.of("no formal education", "primary school", "secondary school", "high school", "college/university", "postgraduate");
    private static final List<String> WEALTH_INDEX_OPTIONS = List.of("low income", "lower middle income", "middle income", "upper middle income", "high income");
    private static final List<String> CONDOM_USE_OPTIONS = List.of("never", "sometimes", "always");

    public MLService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Sends the complete map of raw answers to the ML prediction endpoint
     * for a final risk assessment.
     */
    public Map<String, Object> predictRisk(Map<String, String> answers) {
        logger.info("Calling ML risk prediction service with mapped answers: {}", answers.keySet());

        try {
            // FIX: Endpoint change to /predict (assuming a separate, standalone risk service)
            String url = mlRiskServiceUrl + "/predict";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Send raw answers. Python side handles feature selection/encoding.
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("answers", answers);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> result = response.getBody();

                if (Boolean.TRUE.equals(result.get("success"))) {
                    logger.info("ML prediction successful - Score: {}, Confidence: {}",
                            result.get("riskScore"), result.get("confidence"));
                    return result;
                } else {
                    logger.warn("ML service returned error: {}", result.get("error"));
                    return fallbackRiskAssessment(answers);
                }
            } else {
                logger.error("ML service returned unexpected status: {}", response.getStatusCode());
                return fallbackRiskAssessment(answers);
            }

        } catch (HttpClientErrorException | ResourceAccessException e) {
            logger.error("ML service call failed (Connection/HTTP Error): {}", e.getMessage());
            return fallbackRiskAssessment(answers);
        } catch (Exception e) {
            logger.error("Unexpected error calling ML service: {}", e.getMessage());
            return fallbackRiskAssessment(answers);
        }
    }

    public boolean isServiceHealthy() {
        try {
            // FIX: Use the specific risk service URL for health check
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    mlRiskServiceUrl + "/health",
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> healthResponse = response.getBody();
                Object status = healthResponse.get("status");
                Object modelLoaded = healthResponse.getOrDefault("model_loaded", true); // Default to true if not specified

                // Assuming "model_loaded" is available in the health check response
                boolean healthy = "healthy".equals(status) && Boolean.TRUE.equals(modelLoaded);
                logger.info("ML risk service health check: {}", healthy ? "HEALTHY" : "UNHEALTHY");
                return healthy;
            }

            return false;

        } catch (Exception e) {
            logger.warn("ML risk service health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Fallback logic when ML prediction fails (consolidated from DecisionService and this file).
     */
    private Map<String, Object> fallbackRiskAssessment(Map<String, String> answers) {
        logger.warn("Using consolidated rule-based risk assessment fallback");

        int riskScore = calculateFallbackScore(answers);
        String riskLevel = getRiskLevel(riskScore);
        List<String> recommendations = generateFallbackRecommendations(riskScore, answers);

        double hivProbability = riskScore / 100.0;

        return Map.of(
                "success", true,
                "hivProbability", hivProbability,
                "riskScore", riskScore,
                "riskLevel", riskLevel,
                "recommendations", recommendations,
                "confidence", 0.75,
                "modelUsed", false,
                "timestamp", java.time.LocalDateTime.now().toString()
        );
    }

    private int calculateFallbackScore(Map<String, String> answers) {
        int score = 0;

        // Scoring logic from the previous DecisionService fallback
        if ("No".equals(answers.get("condom_use")) || "Never".equals(answers.get("condom_use"))) score += 30;

        if (answers.containsKey("recent_partners")) {
            try {
                // Use Integer.valueOf for robust parsing
                int partners = Integer.valueOf(answers.get("recent_partners"));
                if (partners >= 3) score += 25;
                else if (partners == 2) score += 15;
            } catch (NumberFormatException ignored) {}
        }

        // Include other critical factors from the combined logic
        if ("Yes".equals(answers.get("high_risk_partner"))) score += 20;
        if ("Yes".equals(answers.get("sti_symptoms"))) score += 15;
        if ("Yes".equals(answers.get("previous_sti"))) score += 10;
        if ("Yes".equals(answers.get("transactional_sex"))) score += 25;

        // Check for lack of testing history as a risk factor
        String hivTested = answers.getOrDefault("hiv_tested", "no").toLowerCase();
        if ("no".equals(hivTested)) {
            score += 10;
        }

        return Math.min(score, 100);
    }

    private String getRiskLevel(int score) {
        // Using the 50/25 thresholds from DecisionService
        if (score >= 50) return "High";
        if (score >= 25) return "Medium";
        return "Low";
    }

    private List<String> generateFallbackRecommendations(int riskScore, Map<String, String> answers) {
        List<String> recommendations = new ArrayList<>();

        if (riskScore >= 50) {
            recommendations.addAll(Arrays.asList(
                    "High risk detected: Consider immediate STI testing",
                    "Consult healthcare provider for comprehensive screening",
                    "Practice consistent condom use",
                    "Discuss PrEP options with your doctor"
            ));
        } else if (riskScore >= 25) {
            recommendations.addAll(Arrays.asList(
                    "Moderate risk: Schedule STI testing soon",
                    "Consider regular screening every 6 months",
                    "Review safer sex practices"
            ));
        } else {
            recommendations.addAll(Arrays.asList(
                    "Low risk: Maintain current protective behaviors",
                    "Consider baseline testing for peace of mind",
                    "Regular health check-ups are recommended"
            ));
        }

        // Context-specific enhancements (similar to DecisionService's enhancedRecommendations)
        String condomUse = answers.getOrDefault("condom_use", "").toLowerCase();
        if ("never".equals(condomUse)) {
            recommendations.add("Consistent condom use can significantly reduce HIV transmission risk");
        }

        String hivTested = answers.getOrDefault("hiv_tested", "").toLowerCase();
        if ("no".equals(hivTested)) {
            recommendations.add("Getting tested provides important health information and peace of mind");
        }

        // Remove duplicates and return
        return new ArrayList<>(new LinkedHashSet<>(recommendations));
    }


    /**
     * Transform and validate answers before sending to ML service (Simplified for ML service consumption)
     * NOTE: This method is now primarily used by DecisionService to prepare the data packet.
     */
    public Map<String, String> preprocessAnswers(Map<String, String> rawAnswers) {
        // Since the Decision Tree/Sequencer in Python handles complex encoding,
        // this service should only ensure keys exist and common values are normalized.
        Map<String, String> processed = new HashMap<>();

        for (Map.Entry<String, String> entry : rawAnswers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (value == null) {
                processed.put(key, "");
                continue;
            }

            // Normalization for specific fields (e.g., marital_status, condom_use)
            String normalizedValue = normalizeValue(key, value);
            processed.put(key, normalizedValue);
        }

        logger.debug("Preprocessed answers keys: {}", processed.keySet());
        return processed;
    }

    // --- Helper Normalization Methods (Based on Python expectations) ---

    private String normalizeValue(String key, String value) {
        String lowerValue = value.toLowerCase();

        switch (key) {
            case "marital_status":
                return MARITAL_STATUS_OPTIONS.stream()
                        .filter(opt -> lowerValue.contains(opt.split(" ")[0]))
                        .findFirst()
                        .orElse(MARITAL_STATUS_OPTIONS.get(1)); // default 'married'

            case "education":
                return EDUCATION_OPTIONS.stream()
                        .filter(opt -> lowerValue.contains(opt.split(" ")[0]))
                        .findFirst()
                        .orElse(EDUCATION_OPTIONS.get(2)); // default 'secondary school'

            case "wealth_index":
                return WEALTH_INDEX_OPTIONS.stream()
                        .filter(opt -> lowerValue.contains(opt.split(" ")[0]))
                        .findFirst()
                        .orElse(WEALTH_INDEX_OPTIONS.get(2)); // default 'middle income'

            case "condom_use":
                return CONDOM_USE_OPTIONS.stream()
                        .filter(opt -> lowerValue.contains(opt))
                        .findFirst()
                        .orElse(CONDOM_USE_OPTIONS.get(1)); // default 'sometimes'

            case "hiv_tested":
                return lowerValue.contains("yes") || lowerValue.contains("1") ? "Yes" : "No";

            default:
                return value; // Return raw value for numbers/other strings
        }
    }

    // NOTE: Removed other validation/utility methods not directly used in the main sequencing path for brevity.
}