package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.mail.EmailService;
import com.kimanga.afyacheck.model.RiskAssessment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
public class NotificationService {

    private final EmailService emailService;
    private final String appBaseUrl;

    public NotificationService(EmailService emailService, @Value("${app.base-url}") String appBaseUrl) {
        this.emailService = emailService;
        this.appBaseUrl = appBaseUrl;
    }

    /** Emails a copy of a completed risk assessment to a user who opted in on the results page. */
    public void sendRiskResultEmail(String toEmail, RiskAssessment assessment) {
        Context context = new Context();
        context.setVariable("riskLevel", assessment.getRiskLevel());
        context.setVariable("riskScore", assessment.getRiskScore());
        context.setVariable("recommendations", assessment.getRecommendations());
        context.setVariable("appHomeUrl", appBaseUrl);

        emailService.sendHtmlMail(toEmail, "Your AfyaCheck results", "email/risk-result.html", context);
    }
}
