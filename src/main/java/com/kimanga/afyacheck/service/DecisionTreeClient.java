package com.kimanga.afyacheck.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import com.kimanga.afyacheck.DTO.*;

import java.time.Duration;
import java.util.*;

@Service
public class DecisionTreeClient {

    private static final Logger logger = LoggerFactory.getLogger(DecisionTreeClient.class);

    @Value("${decision.tree.service.url:http://localhost:8001}")
    private String decisionTreeServiceUrl;

    private final RestTemplate restTemplate;

    public DecisionTreeClient(RestTemplateBuilder restTemplateBuilder,
                               @Value("${service.connection.timeout:5000}") long connectTimeoutMs,
                               @Value("${service.read.timeout:10000}") long readTimeoutMs) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }

    /**
     * Get the next optimal question using the decision tree.
     * * @param allQuestionKeys The complete master list of question keys from the database.
     */
    public String getNextQuestion(String sessionId, Map<String, String> currentAnswers, List<String> allQuestionKeys) {
        try {
            String url = decisionTreeServiceUrl + "/question/next";

            // Prepare request
            QuestionRequest request = new QuestionRequest();
            request.setCurrentAnswers(convertAnswers(currentAnswers));
            request.setAvailableQuestions(getAvailableQuestions(currentAnswers, allQuestionKeys));

            logger.info("Sending request to decision tree service - Session: {}, Answered: {}",
                    sessionId, currentAnswers.size());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<QuestionRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<QuestionResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    QuestionResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                QuestionResponse questionResponse = response.getBody();

                // DECISION TREE RETURNS NULL WHEN IT HAS ENOUGH INFORMATION
                if (questionResponse.getNextQuestion() == null) {
                    logger.info("Decision tree service indicates enough information gathered after {} questions",
                            currentAnswers.size());
                    return null;
                }

                logger.info("Next question: {} (confidence: {}, answered so far: {})",
                        questionResponse.getNextQuestion(),
                        questionResponse.getConfidence(),
                        currentAnswers.size());
                return questionResponse.getNextQuestion();
            } else {
                logger.error("Unexpected response status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to get next question: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException e) {
            logger.error("HTTP error getting next question: {}", e.getStatusCode());
            logger.error("Response body: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Decision tree HTTP error: " + e.getStatusCode());
        } catch (ResourceAccessException e) {
            logger.warn("Decision tree service unavailable, reporting failure");
            throw new RuntimeException("Decision tree service unavailable: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error getting next question", e);
            throw new RuntimeException("Unexpected error communicating with Decision Tree service: " + e.getMessage());
        }
    }

    /**
     * Convert Spring answers to format expected by Python service (map numerics to Integers)
     */
    private Map<String, Object> convertAnswers(Map<String, String> currentAnswers) {
        Map<String, Object> converted = new HashMap<>();

        for (Map.Entry<String, String> entry : currentAnswers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // Explicitly convert known numeric types (matching Python's needs)
            if (key.equals("age") || key.equals("recent_partners") || key.equals("sexual_partners")) {
                try {
                    converted.put(key, Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    converted.put(key, 0); // Default to 0 on bad number format
                }
            } else {
                converted.put(key, value);
            }
        }

        return converted;
    }

    /**
     * Get list of questions that haven't been answered yet (Passed to Python service)
     * * @param allQuestions The complete master list of question keys.
     */
    private List<String> getAvailableQuestions(Map<String, String> currentAnswers, List<String> allQuestions) {
        List<String> available = new ArrayList<>();
        for (String question : allQuestions) {
            if (!currentAnswers.containsKey(question)) {
                available.add(question);
            }
        }
        return available;
    }

    /**
     * Health check for decision tree service
     */
    public boolean isServiceHealthy() {
        try {
            String url = decisionTreeServiceUrl + "/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            logger.warn("Decision tree service health check failed");
            return false;
        }
    }
}