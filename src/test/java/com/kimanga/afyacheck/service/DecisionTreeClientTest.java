package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.DTO.QuestionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DecisionTreeClientTest {

    private RestTemplate restTemplate;
    private DecisionTreeClient client;

    @BeforeEach
    void setUp() {
        client = new DecisionTreeClient();
        restTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(client, "decisionTreeServiceUrl", "http://localhost:8001");
    }

    @Test
    void getNextQuestionReturnsNextQuestionOnSuccess() {
        QuestionResponse body = new QuestionResponse();
        body.setNextQuestion("age");
        body.setConfidence(0.9);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(QuestionResponse.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        String result = client.getNextQuestion("sid", Map.of("q1", "yes"), List.of("q1", "age"));

        assertThat(result).isEqualTo("age");
    }

    @Test
    void getNextQuestionReturnsNullWhenEnoughInformation() {
        QuestionResponse body = new QuestionResponse();
        body.setNextQuestion(null);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(QuestionResponse.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        String result = client.getNextQuestion("sid", Map.of("q1", "yes"), List.of("q1"));

        assertThat(result).isNull();
    }

    @Test
    void getNextQuestionThrowsOnNonOkStatus() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(QuestionResponse.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.ACCEPTED));

        assertThatThrownBy(() -> client.getNextQuestion("sid", Map.of(), List.of()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getNextQuestionWrapsHttpClientErrorException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(QuestionResponse.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", null, null, null));

        assertThatThrownBy(() -> client.getNextQuestion("sid", Map.of(), List.of()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Decision tree HTTP error");
    }

    @Test
    void getNextQuestionWrapsResourceAccessException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(QuestionResponse.class)))
                .thenThrow(new ResourceAccessException("connection refused"));

        assertThatThrownBy(() -> client.getNextQuestion("sid", Map.of(), List.of()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("unavailable");
    }

    @Test
    void getNextQuestionWrapsGenericException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(QuestionResponse.class)))
                .thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> client.getNextQuestion("sid", Map.of(), List.of()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unexpected error");
    }

    @Test
    void convertAnswersParsesKnownNumericFieldsAndDefaultsOnBadFormat() {
        Map<String, String> answers = Map.of(
                "age", "34",
                "recent_partners", "not-a-number",
                "other", "text-value"
        );

        Map<String, Object> converted = ReflectionTestUtils.invokeMethod(client, "convertAnswers", answers);

        assertThat(converted).containsEntry("age", 34);
        assertThat(converted).containsEntry("recent_partners", 0);
        assertThat(converted).containsEntry("other", "text-value");
    }

    @Test
    void getAvailableQuestionsExcludesAnsweredOnes() {
        List<String> available = ReflectionTestUtils.invokeMethod(client, "getAvailableQuestions",
                Map.of("q1", "yes"), List.of("q1", "q2", "q3"));

        assertThat(available).containsExactlyInAnyOrder("q2", "q3");
    }

    @Test
    void isServiceHealthyTrueWhenOk() {
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(Map.of(), HttpStatus.OK));
        assertThat(client.isServiceHealthy()).isTrue();
    }

    @Test
    void isServiceHealthyFalseOnException() {
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenThrow(new ResourceAccessException("down"));
        assertThat(client.isServiceHealthy()).isFalse();
    }
}
