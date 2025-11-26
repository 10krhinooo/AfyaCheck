package com.kimanga.afyacheck.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class QuestionResponse {

    // Getters and setters
    @JsonProperty("next_question")
    private String nextQuestion;

    private Double confidence;

    public void setNextQuestion(String nextQuestion) { this.nextQuestion = nextQuestion; }

    public void setConfidence(Double confidence) { this.confidence = confidence; }
}