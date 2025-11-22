package com.kimanga.afyacheck.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "answer")
public class Answer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_key", nullable = false)
    private String questionKey;

    @Column(name = "answer_value", columnDefinition = "TEXT")
    private String answerValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = new Date();
        }
        if (updatedAt == null) {
            updatedAt = new Date();
        }
        validate();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }

    protected void validate() {
        if (this.questionKey == null || this.questionKey.trim().isEmpty()) {
            throw new IllegalStateException("questionKey cannot be null or empty");
        }
        if (this.session == null) {
            throw new IllegalStateException("session cannot be null");
        }
    }
}