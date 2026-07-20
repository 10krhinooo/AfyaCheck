package com.kimanga.afyacheck.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

/**
 * Opt-in "remind me to retest" record. Holds only the email and the send date — no link to
 * a session or assessment — and is deleted after the send attempt (see ReminderService).
 */
@Data
@Entity
@Table(name = "retest_reminder")
public class RetestReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "due_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date dueAt;

    @Column(name = "created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();
}
