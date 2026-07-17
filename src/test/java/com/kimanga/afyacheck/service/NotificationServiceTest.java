package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.mail.EmailService;
import com.kimanga.afyacheck.model.RiskAssessment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceTest {

    private EmailService emailService;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        emailService = mock(EmailService.class);
        notificationService = new NotificationService(emailService, "https://afyacheck.example.com");
    }

    @Test
    void sendRiskResultEmailUsesRiskResultTemplateWithAssessmentDetails() {
        RiskAssessment assessment = new RiskAssessment();
        assessment.setRiskLevel("High");
        assessment.setRiskScore(80);
        assessment.setRecommendations(List.of("Schedule STI testing soon"));

        notificationService.sendRiskResultEmail("user@example.com", assessment);

        verify(emailService).sendHtmlMail(
                eq("user@example.com"),
                eq("Your AfyaCheck results"),
                eq("email/risk-result.html"),
                any(Context.class));
    }
}
