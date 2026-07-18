package com.kimanga.afyacheck.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "risk_assessment")
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(name = "risk_level", nullable = false)
    private String riskLevel;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> recommendations;

    // Identifies which ML model version produced this assessment (ml-service's
    // HIVRiskPredictor.model_version, or a rule-based-fallback marker) -- important for a
    // health-risk product's auditability. Nullable since assessments saved before this field
    // existed have no recorded version.
    @Column(name = "model_version")
    private String modelVersion;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = new Date();
        }
        validate();
    }

    protected void validate() {
        if (this.session == null) {
            throw new IllegalStateException("session cannot be null");
        }
        if (this.riskLevel == null || this.riskLevel.trim().isEmpty()) {
            throw new IllegalStateException("riskLevel cannot be null or empty");
        }
        if (this.riskScore == null) {
            throw new IllegalStateException("riskScore cannot be null");
        }
    }
}