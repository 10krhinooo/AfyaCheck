package com.kimanga.afyacheck.mail;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    private JavaMailSender mailSender;
    private TemplateEngine templateEngine;
    private EmailConfig emailConfig;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        templateEngine = mock(TemplateEngine.class);
        emailConfig = new EmailConfig();
        ReflectionTestUtils.setField(emailConfig, "fromAddress", "noreply@example.com");
        emailService = new EmailService(mailSender, templateEngine, emailConfig);
    }

    @Test
    void sendHtmlMailRejectsNullToEmail() {
        assertThatThrownBy(() -> emailService.sendHtmlMail(null, "subj", "tpl", new Context()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void sendHtmlMailRejectsBlankToEmail() {
        assertThatThrownBy(() -> emailService.sendHtmlMail("  ", "subj", "tpl", new Context()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sendHtmlMailRejectsInvalidEmailFormat() {
        assertThatThrownBy(() -> emailService.sendHtmlMail("not-an-email", "subj", "tpl", new Context()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email format");
    }

    @Test
    void sendHtmlMailSendsSuccessfully() throws Exception {
        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>body</html>");

        emailService.sendHtmlMail("to@example.com", "Subject", "email/verify-email.html", new Context());

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendHtmlMailWrapsUnexpectedExceptionsFromTemplateEngine() {
        when(templateEngine.process(anyString(), any(Context.class))).thenThrow(new RuntimeException("template error"));

        assertThatThrownBy(() -> emailService.sendHtmlMail("to@example.com", "Subject", "tpl", new Context()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void sendHtmlMailWrapsMessagingExceptionFromMalformedFromAddress() {
        // An invalid From address makes MimeMessageHelper.setFrom() throw a real
        // jakarta.mail.MessagingException (AddressException), exercising the
        // MessagingException-specific catch clause (as opposed to the generic one).
        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>body</html>");
        ReflectionTestUtils.setField(emailConfig, "fromAddress", "not @@ a valid address");

        assertThatThrownBy(() -> emailService.sendHtmlMail("to@example.com", "Subject", "tpl", new Context()))
                .isInstanceOf(EmailException.class)
                .hasMessageContaining("Failed to send email to");
    }

    @Test
    void emailConfigReturnsConfiguredFromAddress() {
        org.assertj.core.api.Assertions.assertThat(emailConfig.getFromAddress()).isEqualTo("noreply@example.com");
    }

    @Test
    void emailExceptionSingleArgConstructorSetsMessage() {
        EmailException ex = new EmailException("just a message");
        org.assertj.core.api.Assertions.assertThat(ex.getMessage()).isEqualTo("just a message");
    }
}
