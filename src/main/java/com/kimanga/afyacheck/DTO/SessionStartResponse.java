
package com.kimanga.afyacheck.DTO;

import java.util.List;

public class SessionStartResponse {
    private String sessionId;
    private String initialQuestion;
    private List<String> allQuestions;
    private String timestamp;

    // Constructors
    public SessionStartResponse() {}

    public SessionStartResponse(String sessionId, String initialQuestion,
                                List<String> allQuestions, String timestamp) {
        this.sessionId = sessionId;
        this.initialQuestion = initialQuestion;
        this.allQuestions = allQuestions;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getInitialQuestion() {
        return initialQuestion;
    }

    public void setInitialQuestion(String initialQuestion) {
        this.initialQuestion = initialQuestion;
    }

    public List<String> getAllQuestions() {
        return allQuestions;
    }

    public void setAllQuestions(List<String> allQuestions) {
        this.allQuestions = allQuestions;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}