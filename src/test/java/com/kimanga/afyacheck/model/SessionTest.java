package com.kimanga.afyacheck.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionTest {

    @Test
    void onCreateSetsTimestampsAndValidates() {
        Session session = new Session();
        session.setSessionId("sid-1");
        session.onCreate();
        assertThat(session.getCreatedAt()).isNotNull();
        assertThat(session.getUpdatedAt()).isNotNull();
    }

    @Test
    void onCreateDoesNotOverrideExistingTimestamps() {
        Session session = new Session();
        session.setSessionId("sid-1");
        java.util.Date fixed = new java.util.Date(0);
        session.setCreatedAt(fixed);
        session.setUpdatedAt(fixed);
        session.onCreate();
        assertThat(session.getCreatedAt()).isEqualTo(fixed);
        assertThat(session.getUpdatedAt()).isEqualTo(fixed);
    }

    @Test
    void onUpdateRefreshesUpdatedAtAndValidates() {
        Session session = new Session();
        session.setSessionId("sid-1");
        session.onUpdate();
        assertThat(session.getUpdatedAt()).isNotNull();
    }

    @Test
    void onUpdateThrowsWhenSessionIdMissing() {
        Session session = new Session();
        assertThatThrownBy(session::onUpdate).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validateRejectsNullSessionId() {
        Session session = new Session();
        assertThatThrownBy(session::validate).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validateRejectsBlankSessionId() {
        Session session = new Session();
        session.setSessionId("   ");
        assertThatThrownBy(session::validate).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validatePassesWithSessionId() {
        Session session = new Session();
        session.setSessionId("sid-1");
        session.validate();
    }

    @Test
    void defaultStatusIsActiveAndCollectionsAreEmpty() {
        Session session = new Session();
        assertThat(session.getStatus()).isEqualTo("active");
        assertThat(session.getAnswers()).isEmpty();
        assertThat(session.getRiskAssessments()).isEmpty();
    }
}
