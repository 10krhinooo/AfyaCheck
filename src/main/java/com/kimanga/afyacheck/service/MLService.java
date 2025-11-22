package com.kimanga.afyacheck.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

@Service
public class MLService {

    private static final Logger logger = LoggerFactory.getLogger(MLService.class);

    private final RestTemplate restTemplate;

    @Value("${ml.service.url:http://localhost:8000}")
    private String mlServiceUrl;

    @Value("${ml.service.timeout:5000}")
    private int timeout;

    // Categorical value mappings from Flask app
    private final Map<String, Integer> maritalStatusMapping = Map.of(
            "never_married", 0,
            "married", 1,
            "divorced", 2,
            "widowed", 3,
            "separated", 4
    );

    private final Map<String, Integer> educationMapping = Map.of(
            "no_education", 0,
            "primary", 1,
            "secondary", 2,
            "higher", 3
    );

    private final Map<String, Integer> wealthIndexMapping = Map.of(
            "poorest", 0,
            "poorer", 1,
            "middle", 2,
            "richer", 3,
            "richest", 4
    );

    private final Map<String, Integer> hivTestedMapping = Map.of(
            "no", 0,
            "yes", 1
    );

    private final Map<String, Integer> sexualPartnersMapping = Map.of(
            "0", 0,
            "1", 1,
            "2", 2,
            "3+", 3
    );

    private final Map<String, Integer> condomUseMapping = Map.of(
            "never", 0,
            "sometimes", 1,
            "always", 2
    );

    public MLService() {
        this.restTemplate = new RestTemplate();
    }

    public Map<String, Object> predictRisk(Map<String, String> answers) {
        logger.info("Calling ML service with {} answers", answers.size());

        try {
            // Prepare request matching Flask API structure
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("answers", answers);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Make API call to Flask/FastAPI endpoint
            ResponseEntity<Map> response = restTemplate.exchange(
                    mlServiceUrl + "/predict",
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
                logger.error("ML service returned status: {}", response.getStatusCode());
                return fallbackRiskAssessment(answers);
            }

        } catch (Exception e) {
            logger.error("Error calling ML service: {}", e.getMessage());
            return fallbackRiskAssessment(answers);
        }
    }

    public boolean isServiceHealthy() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    mlServiceUrl + "/health",
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> healthResponse = response.getBody();
                Object status = healthResponse.get("status");
                Object modelLoaded = healthResponse.get("model_loaded");

                boolean healthy = "healthy".equals(status) && Boolean.TRUE.equals(modelLoaded);
                logger.info("ML service health check: {}", healthy ? "HEALTHY" : "UNHEALTHY");
                return healthy;
            }

            return false;

        } catch (Exception e) {
            logger.warn("ML service health check failed: {}", e.getMessage());
            return false;
        }
    }

    private Map<String, Object> fallbackRiskAssessment(Map<String, String> answers) {
        logger.info("Using fallback rule-based risk assessment");

        // Updated rule-based scoring matching Flask app logic
        int riskScore = 0;

        // Age (numeric)
        String ageStr = answers.getOrDefault("age", "30");
        int age;
        try {
            age = Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            age = 30;
        }
        if (age >= 35) {
            riskScore += 20;
        }

        // Number of Sexual Partners (categorical)
        String sexualPartners = answers.getOrDefault("sexual_partners", "1").toLowerCase();
        if ("3+".equals(sexualPartners)) {
            riskScore += 30;
        } else if ("2".equals(sexualPartners)) {
            riskScore += 15;
        }

        // Condom Use Frequency (categorical)
        String condomUse = answers.getOrDefault("condom_use", "sometimes").toLowerCase();
        if ("never".equals(condomUse)) {
            riskScore += 25;
        }

        // HIV Testing History (categorical)
        String hivTested = answers.getOrDefault("hiv_tested", "no").toLowerCase();
        if ("no".equals(hivTested)) {
            riskScore += 10;
        }

        riskScore = Math.min(riskScore, 100);

        // Determine risk level matching Flask app
        String riskLevel = getRiskLevel(riskScore);

        // Generate recommendations matching Flask app
        List<String> recommendations = generateRecommendations(riskScore, answers);

        double hivProbability = riskScore / 100.0;

        return Map.of(
                "success", true,
                "hivProbability", hivProbability,
                "riskScore", riskScore,
                "riskLevel", riskLevel,
                "recommendations", recommendations,
                "confidence", 0.75,
                "modelUsed", false,
                "featuresUsed", 0,
                "featureValues", answers,
                "timestamp", java.time.LocalDateTime.now().toString()
        );
    }

    private String getRiskLevel(int score) {
        if (score >= 50) {
            return "High";
        } else if (score >= 20) {
            return "Medium";
        } else {
            return "Low";
        }
    }

    private List<String> generateRecommendations(int riskScore, Map<String, String> answers) {
        List<String> recommendations = new ArrayList<>();

        // Base recommendations based on risk score
        if (riskScore >= 50) {
            recommendations.addAll(Arrays.asList(
                    "High risk detected: Consider immediate HIV testing and counseling",
                    "Consult healthcare provider for comprehensive STI screening",
                    "Discuss PrEP (Pre-Exposure Prophylaxis) options with your doctor",
                    "Regular testing every 3-6 months strongly recommended"
            ));
        } else if (riskScore >= 20) {
            recommendations.addAll(Arrays.asList(
                    "Moderate risk: Schedule HIV testing at your earliest convenience",
                    "Consider routine STI screening during next healthcare visit",
                    "Practice consistent condom use to reduce transmission risk",
                    "Annual HIV testing recommended while sexually active"
            ));
        } else {
            recommendations.addAll(Arrays.asList(
                    "Low risk: Maintain current protective behaviors",
                    "Consider baseline HIV testing for peace of mind",
                    "Regular health check-ups support overall wellness",
                    "Open communication with partners about sexual health"
            ));
        }

        // Context-specific recommendations based on individual factors
        String condomUse = answers.getOrDefault("condom_use", "").toLowerCase();
        if ("never".equals(condomUse)) {
            recommendations.add("Consistent condom use can significantly reduce HIV transmission risk");
        }

        String sexualPartners = answers.getOrDefault("sexual_partners", "").toLowerCase();
        if ("3+".equals(sexualPartners)) {
            recommendations.add("Multiple partners increase risk; consider more frequent testing");
        }

        String hivTested = answers.getOrDefault("hiv_tested", "").toLowerCase();
        if ("no".equals(hivTested)) {
            recommendations.add("Getting tested provides important health information and peace of mind");
        }

        String ageStr = answers.getOrDefault("age", "30");
        try {
            int age = Integer.parseInt(ageStr);
            if (age >= 35) {
                recommendations.add("Older age groups may benefit from regular screening as part of routine healthcare");
            }
        } catch (NumberFormatException e) {
            // Use default age
        }

        return recommendations;
    }

    /**
     * Get feature mappings for frontend reference
     */
    public Map<String, Object> getFeatureMappings() {
        return Map.of(
                "required_features", Arrays.asList(
                        "age", "marital_status", "education", "wealth_index",
                        "hiv_tested", "sexual_partners", "condom_use"
                ),
                "value_mappings", Map.of(
                        "marital_status", maritalStatusMapping,
                        "education", educationMapping,
                        "wealth_index", wealthIndexMapping,
                        "hiv_tested", hivTestedMapping,
                        "sexual_partners", sexualPartnersMapping,
                        "condom_use", condomUseMapping
                ),
                "age_range", "15-65 (typical survey range)"
        );
    }

    /**
     * Test the ML service connection with a sample request
     */
    public Map<String, Object> testConnection() {
        try {
            Map<String, String> testAnswers = Map.of(
                    "age", "35",
                    "marital_status", "married",
                    "education", "secondary",
                    "wealth_index", "middle",
                    "hiv_tested", "no",
                    "sexual_partners", "2",
                    "condom_use", "sometimes"
            );

            logger.info("Testing ML service connection with sample data");
            Map<String, Object> result = predictRisk(testAnswers);

            Map<String, Object> testResult = new HashMap<>();
            testResult.put("serviceAvailable", true);
            testResult.put("testRequest", testAnswers);
            testResult.put("testResponse", result);
            testResult.put("modelUsed", result.get("modelUsed"));
            testResult.put("timestamp", java.time.LocalDateTime.now().toString());

            return testResult;

        } catch (Exception e) {
            logger.error("ML service connection test failed", e);
            return Map.of(
                    "serviceAvailable", false,
                    "error", e.getMessage(),
                    "timestamp", java.time.LocalDateTime.now().toString()
            );
        }
    }

    /**
     * Get detailed service status including health and capabilities
     */
    public Map<String, Object> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();

        boolean healthy = isServiceHealthy();
        status.put("healthy", healthy);
        status.put("serviceUrl", mlServiceUrl);
        status.put("timestamp", java.time.LocalDateTime.now().toString());

        if (healthy) {
            try {
                // Get additional info from health endpoint
                ResponseEntity<Map> response = restTemplate.getForEntity(
                        mlServiceUrl + "/health",
                        Map.class
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    Map<String, Object> healthInfo = response.getBody();
                    status.put("modelLoaded", healthInfo.get("model_loaded"));
                    status.put("modelName", healthInfo.get("model_name"));
                    status.put("features", healthInfo.get("features"));
                }

                // Test with a prediction to verify full functionality
                Map<String, Object> testResult = testConnection();
                status.put("predictionTest", testResult.get("modelUsed"));

            } catch (Exception e) {
                logger.warn("Could not get detailed service status", e);
                status.put("detailedStatus", "Unable to retrieve detailed status");
            }
        }

        return status;
    }

    /**
     * Transform and validate answers before sending to ML service
     */
    public Map<String, String> preprocessAnswers(Map<String, String> rawAnswers) {
        Map<String, String> processed = new HashMap<>();

        // Age validation and normalization
        String age = rawAnswers.getOrDefault("age", "30");
        try {
            int ageValue = Integer.parseInt(age);
            if (ageValue < 15 || ageValue > 65) {
                processed.put("age", "30"); // Default to 30 if out of range
                logger.warn("Age {} out of range, using default 30", ageValue);
            } else {
                processed.put("age", age);
            }
        } catch (NumberFormatException e) {
            processed.put("age", "30"); // Default to 30 if invalid
            logger.warn("Invalid age format: {}, using default 30", age);
        }

        // Marital status normalization
        String maritalStatus = rawAnswers.getOrDefault("marital_status", "married");
        processed.put("marital_status", normalizeMaritalStatus(maritalStatus));

        // Education level normalization
        String education = rawAnswers.getOrDefault("education", "secondary");
        processed.put("education", normalizeEducation(education));

        // Wealth index normalization
        String wealthIndex = rawAnswers.getOrDefault("wealth_index", "middle");
        processed.put("wealth_index", normalizeWealthIndex(wealthIndex));

        // HIV tested normalization
        String hivTested = rawAnswers.getOrDefault("hiv_tested", "no");
        processed.put("hiv_tested", normalizeHIVTested(hivTested));

        // Sexual partners normalization
        String sexualPartners = rawAnswers.getOrDefault("sexual_partners", "1");
        processed.put("sexual_partners", normalizeSexualPartners(sexualPartners));

        // Condom use normalization
        String condomUse = rawAnswers.getOrDefault("condom_use", "sometimes");
        processed.put("condom_use", normalizeCondomUse(condomUse));

        logger.debug("Preprocessed answers: {}", processed);
        return processed;
    }

    // Normalization helper methods
    private String normalizeMaritalStatus(String status) {
        if (status == null) return "married";

        String lowerStatus = status.toLowerCase();
        if (maritalStatusMapping.containsKey(lowerStatus)) {
            return lowerStatus;
        }

        // Handle variations
        if (lowerStatus.contains("single") || lowerStatus.contains("never")) {
            return "never_married";
        } else if (lowerStatus.contains("divorce")) {
            return "divorced";
        } else if (lowerStatus.contains("widow")) {
            return "widowed";
        } else if (lowerStatus.contains("separat")) {
            return "separated";
        } else {
            return "married"; // default
        }
    }

    private String normalizeEducation(String education) {
        if (education == null) return "secondary";

        String lowerEducation = education.toLowerCase();
        if (educationMapping.containsKey(lowerEducation)) {
            return lowerEducation;
        }

        // Handle variations
        if (lowerEducation.contains("none") || lowerEducation.contains("no education")) {
            return "no_education";
        } else if (lowerEducation.contains("primary") || lowerEducation.contains("elementary")) {
            return "primary";
        } else if (lowerEducation.contains("secondary") || lowerEducation.contains("high school")) {
            return "secondary";
        } else if (lowerEducation.contains("higher") || lowerEducation.contains("college") || lowerEducation.contains("university")) {
            return "higher";
        } else {
            return "secondary"; // default
        }
    }

    private String normalizeWealthIndex(String wealth) {
        if (wealth == null) return "middle";

        String lowerWealth = wealth.toLowerCase();
        if (wealthIndexMapping.containsKey(lowerWealth)) {
            return lowerWealth;
        }

        // Handle variations
        if (lowerWealth.contains("poorest") || lowerWealth.contains("very poor")) {
            return "poorest";
        } else if (lowerWealth.contains("poorer") || lowerWealth.contains("poor")) {
            return "poorer";
        } else if (lowerWealth.contains("middle") || lowerWealth.contains("average")) {
            return "middle";
        } else if (lowerWealth.contains("richer") || lowerWealth.contains("rich")) {
            return "richer";
        } else if (lowerWealth.contains("richest") || lowerWealth.contains("very rich")) {
            return "richest";
        } else {
            return "middle"; // default
        }
    }

    private String normalizeHIVTested(String tested) {
        if (tested == null) return "no";

        String lowerTested = tested.toLowerCase();
        if (hivTestedMapping.containsKey(lowerTested)) {
            return lowerTested;
        }

        // Handle variations
        if (lowerTested.contains("yes") || lowerTested.contains("true") || lowerTested.contains("1")) {
            return "yes";
        } else {
            return "no"; // default
        }
    }

    private String normalizeSexualPartners(String partners) {
        if (partners == null) return "1";

        String lowerPartners = partners.toLowerCase();
        if (sexualPartnersMapping.containsKey(lowerPartners)) {
            return lowerPartners;
        }

        // Handle numeric values and variations
        try {
            int partnerCount = Integer.parseInt(partners);
            if (partnerCount >= 3) {
                return "3+";
            } else if (partnerCount == 2) {
                return "2";
            } else if (partnerCount == 1) {
                return "1";
            } else {
                return "0";
            }
        } catch (NumberFormatException e) {
            // Handle text variations
            if (lowerPartners.contains("none") || lowerPartners.contains("zero") || lowerPartners.contains("0")) {
                return "0";
            } else if (lowerPartners.contains("one") || lowerPartners.contains("single") || lowerPartners.contains("1")) {
                return "1";
            } else if (lowerPartners.contains("two") || lowerPartners.contains("couple") || lowerPartners.contains("2")) {
                return "2";
            } else if (lowerPartners.contains("three") || lowerPartners.contains("multiple") || lowerPartners.contains("many") || lowerPartners.contains("3")) {
                return "3+";
            } else {
                return "1"; // default
            }
        }
    }

    private String normalizeCondomUse(String condomUse) {
        if (condomUse == null) return "sometimes";

        String lowerCondomUse = condomUse.toLowerCase();
        if (condomUseMapping.containsKey(lowerCondomUse)) {
            return lowerCondomUse;
        }

        // Handle variations
        if (lowerCondomUse.contains("never") || lowerCondomUse.contains("none") || lowerCondomUse.contains("no")) {
            return "never";
        } else if (lowerCondomUse.contains("always") || lowerCondomUse.contains("every time") || lowerCondomUse.contains("consistent")) {
            return "always";
        } else if (lowerCondomUse.contains("sometimes") || lowerCondomUse.contains("occasional") || lowerCondomUse.contains("inconsistent")) {
            return "sometimes";
        } else {
            return "sometimes"; // default
        }
    }

    /**
     * Batch prediction for multiple sets of answers
     */
    public List<Map<String, Object>> predictBatch(List<Map<String, String>> batchAnswers) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (int i = 0; i < batchAnswers.size(); i++) {
            try {
                Map<String, String> answers = batchAnswers.get(i);
                Map<String, Object> result = predictRisk(answers);
                results.add(result);

                logger.debug("Processed batch item {}/{}", i + 1, batchAnswers.size());

            } catch (Exception e) {
                logger.error("Error processing batch item {}: {}", i, e.getMessage());
                // Add error result for this item
                results.add(Map.of(
                        "success", false,
                        "error", "Failed to process: " + e.getMessage(),
                        "timestamp", java.time.LocalDateTime.now().toString()
                ));
            }
        }

        logger.info("Batch prediction completed: {}/{} successful",
                results.stream().filter(r -> Boolean.TRUE.equals(r.get("success"))).count(),
                batchAnswers.size());

        return results;
    }

    /**
     * Validate if answers contain all required fields for ML prediction
     */
    public Map<String, Object> validateAnswers(Map<String, String> answers) {
        List<String> requiredFields = Arrays.asList(
                "age", "marital_status", "education", "wealth_index",
                "hiv_tested", "sexual_partners", "condom_use"
        );

        List<String> missingFields = new ArrayList<>();
        List<String> invalidFields = new ArrayList<>();
        Map<String, String> suggestions = new HashMap<>();

        for (String field : requiredFields) {
            if (!answers.containsKey(field) || answers.get(field) == null || answers.get(field).trim().isEmpty()) {
                missingFields.add(field);
            } else {
                // Validate field values
                String value = answers.get(field);
                boolean valid = validateField(field, value);
                if (!valid) {
                    invalidFields.add(field);
                    suggestions.put(field, getFieldSuggestion(field));
                }
            }
        }

        boolean valid = missingFields.isEmpty() && invalidFields.isEmpty();

        return Map.of(
                "valid", valid,
                "missingFields", missingFields,
                "invalidFields", invalidFields,
                "suggestions", suggestions,
                "requiredFields", requiredFields
        );
    }

    private boolean validateField(String field, String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        switch (field) {
            case "age":
                try {
                    int age = Integer.parseInt(value);
                    return age >= 15 && age <= 65;
                } catch (NumberFormatException e) {
                    return false;
                }

            case "marital_status":
                return maritalStatusMapping.containsKey(value.toLowerCase());

            case "education":
                return educationMapping.containsKey(value.toLowerCase());

            case "wealth_index":
                return wealthIndexMapping.containsKey(value.toLowerCase());

            case "hiv_tested":
                return hivTestedMapping.containsKey(value.toLowerCase());

            case "sexual_partners":
                return sexualPartnersMapping.containsKey(value.toLowerCase());

            case "condom_use":
                return condomUseMapping.containsKey(value.toLowerCase());

            default:
                return true;
        }
    }

    private String getFieldSuggestion(String field) {
        switch (field) {
            case "age":
                return "Must be a number between 15 and 65";
            case "marital_status":
                return "Accepted values: " + String.join(", ", maritalStatusMapping.keySet());
            case "education":
                return "Accepted values: " + String.join(", ", educationMapping.keySet());
            case "wealth_index":
                return "Accepted values: " + String.join(", ", wealthIndexMapping.keySet());
            case "hiv_tested":
                return "Accepted values: " + String.join(", ", hivTestedMapping.keySet());
            case "sexual_partners":
                return "Accepted values: " + String.join(", ", sexualPartnersMapping.keySet());
            case "condom_use":
                return "Accepted values: " + String.join(", ", condomUseMapping.keySet());
            default:
                return "Please provide a valid value";
        }
    }

    /**
     * Get performance metrics and statistics
     */
    public Map<String, Object> getPerformanceMetrics() {
        // This would typically query a metrics database or cache
        // For now, return basic metrics
        return Map.of(
                "totalPredictions", "Not tracked", // Would be from metrics storage
                "averageResponseTime", "Not tracked",
                "successRate", "Not tracked",
                "fallbackUsage", "Not tracked",
                "lastUpdated", java.time.LocalDateTime.now().toString()
        );
    }
}