package com.kimanga.afyacheck.mail;

import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailSenderServiceTest {

    private final EmailService emailService = mock(EmailService.class);
    private final EmailSenderService service = new EmailSenderService(emailService);

    @Test
    void sendVerificationEmailDelegatesWithVerifyTemplate() {
        service.sendVerificationEmail("to@example.com", "User", "http://verify");
        verify(emailService).sendHtmlMail(eq("to@example.com"), any(), eq("email/verify-email.html"), any(Context.class));
    }

    @Test
    void sendPasswordResetEmailDelegatesWithResetTemplate() {
        service.sendPasswordResetEmail("to@example.com", "User", "http://reset");
        verify(emailService).sendHtmlMail(eq("to@example.com"), any(), eq("email/reset-password.html"), any(Context.class));
    }
}
