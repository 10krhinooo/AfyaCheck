package com.kimanga.afyacheck.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class QuestionProgress {

    // Getters and setters
    @JsonProperty("next_question")
    private String nextQuestion;

    private Double confidence;
    private Integer answered;
    private Integer total;
    private Integer remaining;

    public void setNextQuestion(String nextQuestion) { this.nextQuestion = nextQuestion; }

    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public void setAnswered(Integer answered) { this.answered = answered; }

    public void setTotal(Integer total) { this.total = total; }

    public void setRemaining(Integer remaining) { this.remaining = remaining; }
}