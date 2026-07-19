package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.repository.AnswerRepository;
import com.kimanga.afyacheck.repository.RiskAssessmentRepository;
import com.kimanga.afyacheck.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Completes the anonymous-by-design privacy story with data minimization: questionnaire
 * sessions (and their answers/assessments) are purged after a retention window instead of
 * accumulating forever. Users can still delete their own data immediately via
 * DELETE /api/results (SessionService.deleteSessionData); this is the backstop for everyone
 * who never does.
 *
 * Children are bulk-deleted before sessions because JPQL bulk deletes bypass the entity-level
 * REMOVE cascades on Session.
 */
@Service
public class DataRetentionService {

    private static final Logger logger = LoggerFactory.getLogger(DataRetentionService.class);

    private final SessionRepository sessionRepository;
    private final AnswerRepository answerRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final boolean enabled;
    private final int retentionDays;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public DataRetentionService(
            SessionRepository sessionRepository,
            AnswerRepository answerRepository,
            RiskAssessmentRepository riskAssessmentRepository,
            @Value("${retention.enabled:true}") boolean enabled,
            @Value("${retention.days:90}") int retentionDays) {
        this(sessionRepository, answerRepository, riskAssessmentRepository, enabled, retentionDays,
                Clock.systemUTC());
    }

    DataRetentionService(
            SessionRepository sessionRepository,
            AnswerRepository answerRepository,
            RiskAssessmentRepository riskAssessmentRepository,
            boolean enabled,
            int retentionDays,
            Clock clock) {
        this.sessionRepository = sessionRepository;
        this.answerRepository = answerRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.enabled = enabled;
        this.retentionDays = retentionDays;
        this.clock = clock;
    }

    @Scheduled(cron = "${retention.cron:0 0 3 * * *}")
    @Transactional
    public void purgeExpiredSessions() {
        if (!enabled) {
            return;
        }
        Date cutoff = Date.from(clock.instant().minus(retentionDays, ChronoUnit.DAYS));
        int answers = answerRepository.deleteBySessionCreatedAtBefore(cutoff);
        int assessments = riskAssessmentRepository.deleteBySessionCreatedAtBefore(cutoff);
        int sessions = sessionRepository.deleteByCreatedAtBefore(cutoff);
        if (sessions > 0 || answers > 0 || assessments > 0) {
            logger.info("Retention purge: removed {} sessions, {} answers, {} assessments older than {} days",
                    sessions, answers, assessments, retentionDays);
        }
    }
}
