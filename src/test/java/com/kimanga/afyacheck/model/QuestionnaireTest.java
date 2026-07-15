package com.kimanga.afyacheck.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionnaireTest {

    @Test
    void onCreateSetsCreatedAtAndCompletedAtWhenMissing() {
        Questionnaire q = new Questionnaire();
        q.onCreate();
        assertThat(q.getCreatedAt()).isNotNull();
        assertThat(q.getCompletedAt()).isNotNull();
    }

    @Test
    void onCreateDoesNotOverrideExistingCompletedAt() {
        Questionnaire q = new Questionnaire();
        java.time.LocalDateTime fixed = java.time.LocalDateTime.of(2020, 1, 1, 0, 0);
        q.setCompletedAt(fixed);
        q.onCreate();
        assertThat(q.getCompletedAt()).isEqualTo(fixed);
        assertThat(q.getCreatedAt()).isNotNull();
    }
}
