package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.model.RetestReminder;
import com.kimanga.afyacheck.repository.RetestReminderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-20T08:00:00Z");

    @Mock
    private RetestReminderRepository reminderRepository;
    @Mock
    private NotificationService notificationService;

    private ReminderService service() {
        return new ReminderService(reminderRepository, notificationService, 90,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private RetestReminder reminder(String email) {
        RetestReminder r = new RetestReminder();
        r.setEmail(email);
        r.setDueAt(Date.from(NOW.minusSeconds(60)));
        return r;
    }

    @Test
    void scheduleReminderStoresEmailWithDueDate() {
        service().scheduleReminder("someone@example.com");

        ArgumentCaptor<RetestReminder> saved = ArgumentCaptor.forClass(RetestReminder.class);
        verify(reminderRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("someone@example.com");
        assertThat(saved.getValue().getDueAt())
                .isEqualTo(Date.from(Instant.parse("2026-10-18T08:00:00Z")));
    }

    @Test
    void sendDueRemindersSendsAndDeletes() {
        RetestReminder due = reminder("due@example.com");
        when(reminderRepository.findByDueAtBefore(any())).thenReturn(List.of(due));

        service().sendDueReminders();

        verify(notificationService).sendRetestReminderEmail("due@example.com");
        verify(reminderRepository).delete(due);
    }

    @Test
    void sendDueRemindersDeletesEvenWhenSendFails() {
        RetestReminder due = reminder("broken@example.com");
        when(reminderRepository.findByDueAtBefore(any())).thenReturn(List.of(due));
        doThrow(new RuntimeException("smtp down")).when(notificationService)
                .sendRetestReminderEmail("broken@example.com");

        service().sendDueReminders();

        verify(reminderRepository).delete(due);
    }

    @Test
    void sendDueRemindersNoopsWhenNothingDue() {
        when(reminderRepository.findByDueAtBefore(any())).thenReturn(List.of());
        service().sendDueReminders();
    }
}
