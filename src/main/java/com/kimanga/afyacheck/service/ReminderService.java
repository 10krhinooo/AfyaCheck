package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.mail.EmailService;
import com.kimanga.afyacheck.model.Session;
import com.kimanga.afyacheck.model.User;
import com.kimanga.afyacheck.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class ReminderService {

    private static final Logger logger = LoggerFactory.getLogger(ReminderService.class);

    private final SessionRepository sessionRepository;
    private final EmailService emailService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.reassessment.reminder-days}")
    private int reminderDays;

    public ReminderService(SessionRepository sessionRepository, EmailService emailService) {
        this.sessionRepository = sessionRepository;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 0 8 * * *") // daily at 8am
    public void sendReassessmentReminders() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -reminderDays);
        Date cutoff = calendar.getTime();

        List<Session> overdueSessions = sessionRepository.findLatestCompletedSessionsOlderThan(cutoff);
        logger.info("Found {} users overdue for reassessment (older than {} days)", overdueSessions.size(), reminderDays);

        for (Session session : overdueSessions) {
            User user = session.getUser();
            if (user == null || user.getEmail() == null) {
                continue;
            }
            try {
                sendReminderEmail(user);
            } catch (Exception e) {
                logger.error("Failed to send reassessment reminder to: {}", user.getEmail(), e);
            }
        }
    }

    private void sendReminderEmail(User user) {
        String assessmentUrl = baseUrl + "/questionnaire/start";

        Context context = new Context();
        context.setVariable("userName", user.getUsername());
        context.setVariable("userEmail", user.getEmail());
        context.setVariable("assessmentUrl", assessmentUrl);

        emailService.sendHtmlMail(
                user.getEmail(),
                "Time for a Reassessment - AfyaCheck",
                "email/reassessment-reminder.html",
                context
        );
    }
}
