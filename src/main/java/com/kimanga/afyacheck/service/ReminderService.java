package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.model.RetestReminder;
import com.kimanga.afyacheck.repository.RetestReminderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * Opt-in "remind me to retest" flow. Privacy contract (see V9 migration): the stored row is
 * email + due date only — no session/assessment linkage, generic reminder content — and the
 * row is deleted after the send attempt (success or failure; a permanently-failing address
 * must not be retried forever).
 */
@Service
public class ReminderService {

    private static final Logger logger = LoggerFactory.getLogger(ReminderService.class);

    private final RetestReminderRepository reminderRepository;
    private final NotificationService notificationService;
    private final int reminderDays;
    private final Clock clock;

    @Autowired
    public ReminderService(
            RetestReminderRepository reminderRepository,
            NotificationService notificationService,
            @Value("${retest.reminder.days:90}") int reminderDays) {
        this(reminderRepository, notificationService, reminderDays, Clock.systemUTC());
    }

    ReminderService(
            RetestReminderRepository reminderRepository,
            NotificationService notificationService,
            int reminderDays,
            Clock clock) {
        this.reminderRepository = reminderRepository;
        this.notificationService = notificationService;
        this.reminderDays = reminderDays;
        this.clock = clock;
    }

    public void scheduleReminder(String email) {
        RetestReminder reminder = new RetestReminder();
        reminder.setEmail(email);
        reminder.setDueAt(Date.from(clock.instant().plus(reminderDays, ChronoUnit.DAYS)));
        reminder.setCreatedAt(Date.from(clock.instant()));
        reminderRepository.save(reminder);
        logger.info("Scheduled retest reminder due in {} days", reminderDays);
    }

    @Scheduled(cron = "${retest.reminder.cron:0 0 8 * * *}")
    @Transactional
    public void sendDueReminders() {
        List<RetestReminder> due = reminderRepository.findByDueAtBefore(Date.from(clock.instant()));
        for (RetestReminder reminder : due) {
            try {
                notificationService.sendRetestReminderEmail(reminder.getEmail());
            } catch (Exception e) {
                logger.error("Failed to send retest reminder: {}", e.getMessage());
            }
            reminderRepository.delete(reminder);
        }
        if (!due.isEmpty()) {
            logger.info("Processed {} due retest reminders", due.size());
        }
    }
}
