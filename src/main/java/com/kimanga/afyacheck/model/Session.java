package com.kimanga.afyacheck.model;

import jakarta.persistence.*;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "session")
public class Session {
    private static final Logger logger = LoggerFactory.getLogger(Session.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "session_id", unique = true, nullable = false, columnDefinition = "TEXT")
    private String sessionId;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "status", length = 50)
    private String status = "active";

    @OneToMany(mappedBy = "session", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, fetch = FetchType.LAZY)
    private List<Answer> answers = new ArrayList<>();

    @OneToMany(mappedBy = "session", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, fetch = FetchType.LAZY)
    private List<RiskAssessment> riskAssessments = new ArrayList<>();

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
        validate();
    }

    protected void validate() {
        if (this.sessionId == null || this.sessionId.trim().isEmpty()) {
            throw new IllegalStateException("sessionId cannot be null or empty");
        }
        // No need to truncate with TEXT column type
    }
}