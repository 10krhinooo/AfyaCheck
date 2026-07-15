package com.kimanga.afyacheck.mail;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.beans.factory.annotation.Value;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailConfig emailConfig; // Add configuration class

    /**
     * Send an HTML email using a Thymeleaf template
     *
     * @param toEmail      Recipient email
     * @param subject      Email subject
     * @param templateName Thymeleaf template path (e.g., "email/verify-email.html")
     * @param context      Thymeleaf context with variables
     */
    public void sendHtmlMail(String toEmail, String subject, String templateName, Context context) {
        validateParameters(toEmail, subject, templateName, context);

        try {
            String htmlContent = templateEngine.process(templateName, context);
            MimeMessage message = createMimeMessage(toEmail, subject, htmlContent);
            mailSender.send(message);
            log.info("Email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send email to: {}", toEmail, e);
            throw new EmailException("Failed to send email to " + toEmail, e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to: {}", toEmail, e);
            throw new EmailException("Unexpected error sending email", e);
        }
    }

    private MimeMessage createMimeMessage(String toEmail, String subject, String htmlContent)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        helper.setFrom(emailConfig.getFromAddress()); // Configurable
        return message;
    }

    private void validateParameters(String toEmail, String subject, String templateName, Context context) {
        Objects.requireNonNull(toEmail, "Recipient email cannot be null");
        Objects.requireNonNull(subject, "Email subject cannot be null");
        Objects.requireNonNull(templateName, "Template name cannot be null");
        Objects.requireNonNull(context, "Thymeleaf context cannot be null");

        if (toEmail.isBlank()) {
            throw new IllegalArgumentException("Recipient email cannot be empty");
        }
        if (!isValidEmail(toEmail)) {
            throw new IllegalArgumentException("Invalid email format: " + toEmail);
        }
    }

    private boolean isValidEmail(String email) {
        // Simple email validation - consider using Apache Commons Validator or regex
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
}

// Custom exception
class EmailException extends RuntimeException {
    public EmailException(String message) {
        super(message);
    }

    public EmailException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Configuration class
@Component
class EmailConfig {

    @Value("${spring.mail.from:noreply.afyacheck@gmail.com}")
    private String fromAddress;

    public String getFromAddress() {
        return fromAddress;
    }
}