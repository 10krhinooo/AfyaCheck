package com.kimanga.afyacheck.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.Map;
import java.util.List;

@Getter
public class QuestionRequest {

    // Getters and setters
    @JsonProperty("current_answers")
    private Map<String, Object> currentAnswers;

    @JsonProperty("available_questions")
    private List<String> availableQuestions;

    public void setCurrentAnswers(Map<String, Object> currentAnswers) { this.currentAnswers = currentAnswers; }

    public void setAvailableQuestions(List<String> availableQuestions) { this.availableQuestions = availableQuestions; }
}