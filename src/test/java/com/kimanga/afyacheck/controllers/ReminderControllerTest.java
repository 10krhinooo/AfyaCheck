package com.kimanga.afyacheck.controllers;

import com.kimanga.afyacheck.service.ReminderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ReminderControllerTest {

    @Mock
    private ReminderService reminderService;

    @Test
    void schedulesReminderForValidEmail() {
        ReminderController controller = new ReminderController(reminderService);
        ResponseEntity<?> response =
                controller.create(new ReminderController.ReminderRequest("  someone@example.com "));

        assertThat(response.getStatusCode().value()).isEqualTo(202);
        verify(reminderService).scheduleReminder("someone@example.com");
    }

    @Test
    void rejectsInvalidEmails() {
        ReminderController controller = new ReminderController(reminderService);

        assertThat(controller.create(new ReminderController.ReminderRequest(null)).getStatusCode().value())
                .isEqualTo(400);
        assertThat(controller.create(new ReminderController.ReminderRequest("   ")).getStatusCode().value())
                .isEqualTo(400);
        assertThat(controller.create(new ReminderController.ReminderRequest("not-an-email")).getStatusCode().value())
                .isEqualTo(400);
        verifyNoInteractions(reminderService);
    }
}
