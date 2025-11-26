package com.kimanga.afyacheck.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.Map;
import java.util.List;

@Getter
public class SessionState {

    // Getters and setters
    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("current_answers")
    private Map<String, Object> currentAnswers;

    @JsonProperty("answered_questions")
    private List<String> answeredQuestions;

    @JsonProperty("remaining_questions")
    private List<String> remainingQuestions;

    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public void setCurrentAnswers(Map<String, Object> currentAnswers) { this.currentAnswers = currentAnswers; }

    public void setAnsweredQuestions(List<String> answeredQuestions) { this.answeredQuestions = answeredQuestions; }

    public void setRemainingQuestions(List<String> remainingQuestions) { this.remainingQuestions = remainingQuestions; }
}