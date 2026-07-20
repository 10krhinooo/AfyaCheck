package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.repository.AnswerRepository;
import com.kimanga.afyacheck.repository.RiskAssessmentRepository;
import com.kimanga.afyacheck.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataRetentionServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-20T03:00:00Z");

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private AnswerRepository answerRepository;
    @Mock
    private RiskAssessmentRepository riskAssessmentRepository;

    @Test
    void purgesEverythingOlderThanTheRetentionCutoff() {
        when(answerRepository.deleteBySessionCreatedAtBefore(any())).thenReturn(3);
        when(riskAssessmentRepository.deleteBySessionCreatedAtBefore(any())).thenReturn(2);
        when(sessionRepository.deleteByCreatedAtBefore(any())).thenReturn(1);

        DataRetentionService service = new DataRetentionService(
                sessionRepository, answerRepository, riskAssessmentRepository,
                true, 90, Clock.fixed(NOW, ZoneOffset.UTC));
        service.purgeExpiredSessions();

        ArgumentCaptor<Date> cutoff = ArgumentCaptor.forClass(Date.class);
        verify(sessionRepository).deleteByCreatedAtBefore(cutoff.capture());
        verify(answerRepository).deleteBySessionCreatedAtBefore(cutoff.getValue());
        verify(riskAssessmentRepository).deleteBySessionCreatedAtBefore(cutoff.getValue());
        assertThat(cutoff.getValue()).isEqualTo(Date.from(Instant.parse("2026-04-21T03:00:00Z")));
    }

    @Test
    void purgeHandlesZeroDeletions() {
        when(answerRepository.deleteBySessionCreatedAtBefore(any())).thenReturn(0);
        when(riskAssessmentRepository.deleteBySessionCreatedAtBefore(any())).thenReturn(0);
        when(sessionRepository.deleteByCreatedAtBefore(any())).thenReturn(0);

        DataRetentionService service = new DataRetentionService(
                sessionRepository, answerRepository, riskAssessmentRepository,
                true, 90, Clock.fixed(NOW, ZoneOffset.UTC));
        service.purgeExpiredSessions();

        verify(sessionRepository).deleteByCreatedAtBefore(any());
    }

    @Test
    void doesNothingWhenDisabled() {
        DataRetentionService service = new DataRetentionService(
                sessionRepository, answerRepository, riskAssessmentRepository,
                false, 90, Clock.fixed(NOW, ZoneOffset.UTC));
        service.purgeExpiredSessions();

        verifyNoInteractions(sessionRepository, answerRepository, riskAssessmentRepository);
    }
}
