package com.kimanga.afyacheck.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailSenderService {

    private final EmailService emailService;

    public void sendVerificationEmail(String to, String userName, String verificationUrl) {
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("userEmail", to);
        context.setVariable("verificationUrl", verificationUrl);

        // Call EmailService and pass template name + context
        emailService.sendHtmlMail(
                to,
                "Verify Your AfyaCheck Account",
                "email/verify-email.html",
                context
        );
    }

    public void sendPasswordResetEmail(String to, String userName, String resetUrl) {
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("userEmail", to);
        context.setVariable("resetUrl", resetUrl);

        emailService.sendHtmlMail(
                to,
                "Reset Your AfyaCheck Password",
                "email/reset-password.html",
                context
        );
    }
}
