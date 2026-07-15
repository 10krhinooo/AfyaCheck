package com.kimanga.afyacheck.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiskAssessmentTest {

    private RiskAssessment valid() {
        RiskAssessment ra = new RiskAssessment();
        ra.setSession(new Session());
        ra.setRiskLevel("LOW");
        ra.setRiskScore(1);
        return ra;
    }

    @Test
    void onCreateSetsCreatedAtAndValidates() {
        RiskAssessment ra = valid();
        ra.onCreate();
        assertThat(ra.getCreatedAt()).isNotNull();
    }

    @Test
    void onCreateDoesNotOverrideExistingCreatedAt() {
        RiskAssessment ra = valid();
        java.util.Date fixed = new java.util.Date(0);
        ra.setCreatedAt(fixed);
        ra.onCreate();
        assertThat(ra.getCreatedAt()).isEqualTo(fixed);
    }

    @Test
    void validateRejectsNullSession() {
        RiskAssessment ra = valid();
        ra.setSession(null);
        assertThatThrownBy(ra::validate).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validateRejectsNullRiskLevel() {
        RiskAssessment ra = valid();
        ra.setRiskLevel(null);
        assertThatThrownBy(ra::validate).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validateRejectsBlankRiskLevel() {
        RiskAssessment ra = valid();
        ra.setRiskLevel("  ");
        assertThatThrownBy(ra::validate).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validateRejectsNullRiskScore() {
        RiskAssessment ra = valid();
        ra.setRiskScore(null);
        assertThatThrownBy(ra::validate).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validatePassesWithAllFields() {
        valid().validate();
    }
}
