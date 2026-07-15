package com.kimanga.afyacheck.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnswerTest {

    private Answer validAnswer() {
        Answer answer = new Answer();
        answer.setQuestionKey("q1");
        answer.setSession(new Session());
        return answer;
    }

    @Test
    void onCreateSetsTimestampsAndValidates() {
        Answer answer = validAnswer();
        answer.onCreate();
        assertThat(answer.getCreatedAt()).isNotNull();
        assertThat(answer.getUpdatedAt()).isNotNull();
    }

    @Test
    void onCreateDoesNotOverrideExistingTimestamps() {
        Answer answer = validAnswer();
        java.util.Date fixed = new java.util.Date(0);
        answer.setCreatedAt(fixed);
        answer.setUpdatedAt(fixed);
        answer.onCreate();
        assertThat(answer.getCreatedAt()).isEqualTo(fixed);
        assertThat(answer.getUpdatedAt()).isEqualTo(fixed);
    }

    @Test
    void onUpdateRefreshesUpdatedAt() {
        Answer answer = validAnswer();
        answer.onUpdate();
        assertThat(answer.getUpdatedAt()).isNotNull();
    }

    @Test
    void validateRejectsNullQuestionKey() {
        Answer answer = new Answer();
        answer.setSession(new Session());
        assertThatThrownBy(answer::validate).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validateRejectsBlankQuestionKey() {
        Answer answer = new Answer();
        answer.setQuestionKey("   ");
        answer.setSession(new Session());
        assertThatThrownBy(answer::validate).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validateRejectsNullSession() {
        Answer answer = new Answer();
        answer.setQuestionKey("q1");
        assertThatThrownBy(answer::validate).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validatePassesWithQuestionKeyAndSession() {
        Answer answer = validAnswer();
        answer.validate();
    }
}
