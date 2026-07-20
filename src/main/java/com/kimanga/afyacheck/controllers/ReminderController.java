package com.kimanga.afyacheck.controllers;

import com.kimanga.afyacheck.service.ReminderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public opt-in retest reminder. Rate-limited to the same strict email budget as
 * /api/results/notify (see RateLimitFilter) since both trigger outbound email.
 */
@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    public record ReminderRequest(String email) {}

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ReminderRequest request) {
        String email = request.email() == null ? "" : request.email().trim();
        if (email.isEmpty() || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please enter a valid email address"));
        }
        reminderService.scheduleReminder(email);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("status", "scheduled"));
    }
}
