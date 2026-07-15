package com.kimanga.afyacheck.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MLServiceTest {

    private RestTemplate restTemplate;
    private MLService mlService;

    @BeforeEach
    void setUp() {
        mlService = new MLService();
        restTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(mlService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(mlService, "mlRiskServiceUrl", "http://localhost:8000");
    }

    @Test
    void predictRiskReturnsMlResultOnSuccess() {
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("riskScore", 42);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        Map<String, Object> result = mlService.predictRisk(Map.of("q1", "yes"));

        assertThat(result.get("riskScore")).isEqualTo(42);
    }

    @Test
    void predictRiskFallsBackWhenMlReportsFailure() {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        Map<String, Object> result = mlService.predictRisk(Map.of());

        assertThat(result.get("modelUsed")).isEqualTo(false);
    }

    @Test
    void predictRiskFallsBackOnNonOkStatus() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        Map<String, Object> result = mlService.predictRisk(Map.of());

        assertThat(result.get("modelUsed")).isEqualTo(false);
    }

    @Test
    void predictRiskFallsBackOnConnectionError() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new ResourceAccessException("down"));

        Map<String, Object> result = mlService.predictRisk(Map.of());

        assertThat(result.get("success")).isEqualTo(true);
        assertThat(result.get("modelUsed")).isEqualTo(false);
    }

    @Test
    void predictRiskFallsBackOnUnexpectedException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new IllegalStateException("boom"));

        Map<String, Object> result = mlService.predictRisk(Map.of());

        assertThat(result.get("modelUsed")).isEqualTo(false);
    }

    @Test
    void fallbackScoreComputesHighRiskAndRecommendations() {
        // restTemplate.exchange is unstubbed here, so it returns null and predictRisk
        // falls into its generic catch block, exercising the rule-based fallback path.
        Map<String, String> answers = new HashMap<>();
        answers.put("condom_use", "Never");
        answers.put("recent_partners", "3");
        answers.put("high_risk_partner", "Yes");
        answers.put("sti_symptoms", "Yes");
        answers.put("previous_sti", "Yes");
        answers.put("transactional_sex", "Yes");
        answers.put("hiv_tested", "no");

        Map<String, Object> result = mlService.predictRisk(answers);

        assertThat(result.get("riskScore")).isEqualTo(100);
        assertThat(result.get("riskLevel")).isEqualTo("High");
        assertThat((java.util.List<Object>) result.get("recommendations"))
                .contains("High risk detected: Consider immediate STI testing");
    }

    @Test
    void fallbackScoreHandlesLowRiskAndInvalidPartnerCount() {
        Map<String, String> answers = new HashMap<>();
        answers.put("recent_partners", "not-a-number");
        answers.put("hiv_tested", "yes");

        Map<String, Object> result = mlService.predictRisk(answers);

        assertThat(result.get("riskLevel")).isEqualTo("Low");
    }

    @Test
    void fallbackScoreHandlesModerateRiskFromTwoPartners() {
        Map<String, String> answers = new HashMap<>();
        answers.put("recent_partners", "2");
        answers.put("high_risk_partner", "Yes");

        Map<String, Object> result = mlService.predictRisk(answers);

        assertThat(result.get("riskLevel")).isEqualTo("Medium");
    }

    @Test
    void isServiceHealthyTrueWhenHealthyAndModelLoaded() {
        Map<String, Object> body = Map.of("status", "healthy", "model_loaded", true);
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        assertThat(mlService.isServiceHealthy()).isTrue();
    }

    @Test
    void isServiceHealthyFalseWhenStatusNotHealthy() {
        Map<String, Object> body = Map.of("status", "degraded", "model_loaded", true);
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        assertThat(mlService.isServiceHealthy()).isFalse();
    }

    @Test
    void isServiceHealthyDefaultsModelLoadedTrueWhenMissing() {
        Map<String, Object> body = Map.of("status", "healthy");
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        assertThat(mlService.isServiceHealthy()).isTrue();
    }

    @Test
    void isServiceHealthyFalseOnException() {
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenThrow(new ResourceAccessException("down"));
        assertThat(mlService.isServiceHealthy()).isFalse();
    }

    @Test
    void preprocessAnswersNormalizesKnownFields() {
        Map<String, String> raw = new HashMap<>();
        raw.put("marital_status", "I am married now");
        raw.put("education", "went to secondary school");
        raw.put("wealth_index", "middle income household");
        raw.put("condom_use", "always use protection");
        raw.put("hiv_tested", "yes I was tested");
        raw.put("other_field", "raw-value");
        raw.put("null_field", null);

        Map<String, String> processed = mlService.preprocessAnswers(raw);

        assertThat(processed.get("marital_status")).isEqualTo("married");
        assertThat(processed.get("education")).isEqualTo("secondary school");
        assertThat(processed.get("wealth_index")).isEqualTo("middle income");
        assertThat(processed.get("condom_use")).isEqualTo("always");
        assertThat(processed.get("hiv_tested")).isEqualTo("Yes");
        assertThat(processed.get("other_field")).isEqualTo("raw-value");
        assertThat(processed.get("null_field")).isEqualTo("");
    }

    @Test
    void preprocessAnswersUsesDefaultsWhenNoOptionMatches() {
        Map<String, String> raw = new HashMap<>();
        raw.put("marital_status", "unknown-value-xyz");
        // "education" default check must avoid any substring of "no formal education",
        // "primary school", etc. — "unknown-value-xyz" contains "no" (from "uNknOwn").
        raw.put("education", "qqqqqq");
        raw.put("wealth_index", "unknown-value-xyz");
        raw.put("condom_use", "unknown-value-xyz");
        raw.put("hiv_tested", "no info");

        Map<String, String> processed = mlService.preprocessAnswers(raw);

        assertThat(processed.get("marital_status")).isEqualTo("married");
        assertThat(processed.get("education")).isEqualTo("secondary school");
        assertThat(processed.get("wealth_index")).isEqualTo("middle income");
        assertThat(processed.get("condom_use")).isEqualTo("sometimes");
        assertThat(processed.get("hiv_tested")).isEqualTo("No");
    }
}
