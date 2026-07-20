package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.DTO.admin.DashboardStats;
import com.kimanga.afyacheck.DTO.admin.UserDTO;
import com.kimanga.afyacheck.DTO.ServiceResult;
import com.kimanga.afyacheck.model.AdminAuditLog;
import com.kimanga.afyacheck.model.Answer;
import com.kimanga.afyacheck.model.HealthCenter;
import com.kimanga.afyacheck.model.Question;
import com.kimanga.afyacheck.model.User;
import com.kimanga.afyacheck.model.UserRole;
import com.kimanga.afyacheck.repository.AdminAuditLogRepository;
import com.kimanga.afyacheck.repository.AnswerRepository;
import com.kimanga.afyacheck.repository.HealthCenterRepository;
import com.kimanga.afyacheck.repository.QuestionRepository;
import com.kimanga.afyacheck.repository.RiskAssessmentRepository;
import com.kimanga.afyacheck.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class AdminServiceTest {

    private UserRepository userRepository;
    private QuestionRepository questionRepository;
    private AnswerRepository answerRepository;
    private AdminAuditLogRepository adminAuditLogRepository;
    private HealthCenterRepository healthCenterRepository;
    private RiskAssessmentRepository riskAssessmentRepository;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        questionRepository = mock(QuestionRepository.class);
        answerRepository = mock(AnswerRepository.class);
        adminAuditLogRepository = mock(AdminAuditLogRepository.class);
        healthCenterRepository = mock(HealthCenterRepository.class);
        riskAssessmentRepository = mock(RiskAssessmentRepository.class);
        adminService = new AdminService(userRepository, questionRepository, answerRepository, adminAuditLogRepository, healthCenterRepository, riskAssessmentRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String email) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", email)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @Test
    void getDashboardStatsAggregatesCounts() {
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByEnabledTrue()).thenReturn(80L);
        when(answerRepository.count()).thenReturn(500L);
        when(questionRepository.countByIsActiveTrue()).thenReturn(20L);
        when(userRepository.countByCreatedAtAfter(any())).thenReturn(10L);

        DashboardStats stats = adminService.getDashboardStats();

        assertThat(stats.getTotalUsers()).isEqualTo(100L);
        assertThat(stats.getActiveUsers()).isEqualTo(80L);
        assertThat(stats.getTotalQuestionnaires()).isEqualTo(500L);
        assertThat(stats.getTotalQuestions()).isEqualTo(20L);
        assertThat(stats.getNewUsersThisMonth()).isEqualTo(10L);
    }

    @Test
    void getDashboardStatsFallsBackToZerosOnException() {
        when(userRepository.count()).thenThrow(new RuntimeException("db down"));

        DashboardStats stats = adminService.getDashboardStats();

        assertThat(stats.getTotalUsers()).isZero();
        assertThat(stats.getActiveUsers()).isZero();
    }

    @Test
    void getUserGrowthDataFallsBackOnException() {
        when(userRepository.findUserRegistrationsByMonth(any())).thenThrow(new RuntimeException("boom"));

        var chartData = adminService.getUserGrowthData();

        assertThat(chartData).containsKeys("labels", "data");
    }

    @Test
    void getUserGrowthDataBuildsSixMonthLabels() {
        when(userRepository.findUserRegistrationsByMonth(any())).thenReturn(List.of());

        var chartData = adminService.getUserGrowthData();

        assertThat((List<?>) chartData.get("labels")).hasSize(6);
        assertThat((List<?>) chartData.get("data")).hasSize(6);
    }

    @Test
    void getUserGrowthDataMatchesCurrentMonthLabel() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String currentMonthLabel = now.getMonth().toString().substring(0, 3) + " " + now.getYear();
        Object[] row = new Object[]{currentMonthLabel, 42L};
        when(userRepository.findUserRegistrationsByMonth(any())).thenReturn(List.<Object[]>of(row));

        var chartData = adminService.getUserGrowthData();

        assertThat((List<Object>) chartData.get("data")).contains(42L);
    }

    @Test
    void getAnswerCompletionsDataGroupsByWeek() {
        Answer recent = new Answer();
        recent.setCreatedAt(new Date());
        when(answerRepository.findAll()).thenReturn(List.of(recent));

        var chartData = adminService.getAnswerCompletionsData();

        assertThat((List<?>) chartData.get("labels")).hasSize(4);
    }

    @Test
    void getAnswerCompletionsDataIgnoresAnswersWithNullCreatedAt() {
        Answer noDate = new Answer();
        when(answerRepository.findAll()).thenReturn(List.of(noDate));

        var chartData = adminService.getAnswerCompletionsData();

        assertThat(chartData).containsKey("labels");
    }

    @Test
    void getAnswerCompletionsDataFallsBackOnException() {
        when(answerRepository.findAll()).thenThrow(new RuntimeException("boom"));
        var chartData = adminService.getAnswerCompletionsData();
        assertThat(chartData).containsKeys("labels", "data");
    }

    @Test
    void getQuestionTypeDistributionDataGroupsByType() {
        Question q1 = new Question();
        q1.setQuestionType("yes_no");
        Question q2 = new Question();
        q2.setQuestionType("yes_no");
        Question q3 = new Question();
        q3.setQuestionType("text");
        when(questionRepository.findByIsActiveTrue()).thenReturn(List.of(q1, q2, q3));

        var chartData = adminService.getQuestionTypeDistributionData();

        assertThat((List<?>) chartData.get("labels")).hasSize(2);
    }

    @Test
    void getQuestionTypeDistributionDataFallsBackOnException() {
        when(questionRepository.findByIsActiveTrue()).thenThrow(new RuntimeException("boom"));
        var chartData = adminService.getQuestionTypeDistributionData();
        assertThat(chartData).containsKeys("labels", "data", "backgroundColors");
    }

    @Test
    void getSectionDistributionDataUsesUncategorizedForMissingSection() {
        Question q1 = new Question();
        q1.setSectionTitle(null);
        when(questionRepository.findByIsActiveTrue()).thenReturn(List.of(q1));

        var chartData = adminService.getSectionDistributionData();

        assertThat((List<Object>) chartData.get("labels")).contains("Uncategorized");
    }

    @Test
    void getSectionDistributionDataFallsBackOnException() {
        when(questionRepository.findByIsActiveTrue()).thenThrow(new RuntimeException("boom"));
        var chartData = adminService.getSectionDistributionData();
        assertThat(chartData).containsKeys("labels", "data");
    }

    private User sampleUser() {
        User user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setEnabled(true);
        user.setRole(UserRole.USER);
        return user;
    }

    @Test
    void getRecentUsersConvertsToDTOs() {
        when(userRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(List.of(sampleUser()));
        List<UserDTO> result = adminService.getRecentUsers(10);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void getRecentUsersReturnsEmptyOnException() {
        when(userRepository.findTop10ByOrderByCreatedAtDesc()).thenThrow(new RuntimeException("boom"));
        assertThat(adminService.getRecentUsers(10)).isEmpty();
    }

    @Test
    void getAllQuestionsReturnsEmptyOnException() {
        when(questionRepository.findByIsActiveTrueOrderByDisplayOrderAsc()).thenThrow(new RuntimeException("boom"));
        assertThat(adminService.getAllQuestions()).isEmpty();
    }

    @Test
    void addQuestionSetsDefaultsWhenMissing() {
        when(questionRepository.findByIsActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of());
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));

        Question question = new Question();
        ServiceResult<Question> result = adminService.addQuestion(question);

        assertThat(result.isSuccess()).isTrue();
        assertThat(question.getIsActive()).isTrue();
        assertThat(question.getDisplayOrder()).isEqualTo(1);
        verify(adminAuditLogRepository).save(any(AdminAuditLog.class));
    }

    @Test
    void addQuestionUsesMaxDisplayOrderPlusOne() {
        Question existing = new Question();
        existing.setDisplayOrder(5);
        when(questionRepository.findByIsActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(existing));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));

        Question question = new Question();
        adminService.addQuestion(question);

        assertThat(question.getDisplayOrder()).isEqualTo(6);
    }

    @Test
    void addQuestionFailsOnException() {
        when(questionRepository.save(any())).thenThrow(new RuntimeException("boom"));
        ServiceResult<Question> result = adminService.addQuestion(new Question());
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void deleteQuestionFailsWhenNotFound() {
        when(questionRepository.findById(1L)).thenReturn(Optional.empty());
        ServiceResult<Void> result = adminService.deleteQuestion(1L);
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void deleteQuestionSoftDeletesQuestion() {
        Question question = new Question();
        question.setIsActive(true);
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));

        ServiceResult<Void> result = adminService.deleteQuestion(1L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(question.getIsActive()).isFalse();
        verify(adminAuditLogRepository).save(any(AdminAuditLog.class));
    }

    @Test
    void updateQuestionFailsWhenNotFound() {
        when(questionRepository.findById(1L)).thenReturn(Optional.empty());
        ServiceResult<Question> result = adminService.updateQuestion(1L, new Question());
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void updateQuestionAppliesNewFields() {
        Question existing = new Question();
        when(questionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));

        Question updated = new Question();
        updated.setQuestionText("New text");
        updated.setQuestionType("number");

        ServiceResult<Question> result = adminService.updateQuestion(1L, updated);

        assertThat(result.isSuccess()).isTrue();
        assertThat(existing.getQuestionText()).isEqualTo("New text");
        verify(adminAuditLogRepository).save(any(AdminAuditLog.class));
    }

    @Test
    void getAllHealthCentersReturnsRepositoryResults() {
        when(healthCenterRepository.findAll()).thenReturn(List.of(new HealthCenter()));
        assertThat(adminService.getAllHealthCenters()).hasSize(1);
    }

    @Test
    void getAllHealthCentersFallsBackToEmptyListOnException() {
        when(healthCenterRepository.findAll()).thenThrow(new RuntimeException("boom"));
        assertThat(adminService.getAllHealthCenters()).isEmpty();
    }

    @Test
    void addHealthCenterSetsDefaultActiveFlag() {
        HealthCenter center = new HealthCenter();
        when(healthCenterRepository.save(any(HealthCenter.class))).thenAnswer(inv -> inv.getArgument(0));

        ServiceResult<HealthCenter> result = adminService.addHealthCenter(center);

        assertThat(result.isSuccess()).isTrue();
        assertThat(center.getIsActive()).isTrue();
        verify(adminAuditLogRepository).save(any(AdminAuditLog.class));
    }

    @Test
    void addHealthCenterFailsOnException() {
        when(healthCenterRepository.save(any())).thenThrow(new RuntimeException("boom"));
        ServiceResult<HealthCenter> result = adminService.addHealthCenter(new HealthCenter());
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void deleteHealthCenterFailsWhenNotFound() {
        when(healthCenterRepository.findById(1L)).thenReturn(Optional.empty());
        ServiceResult<Void> result = adminService.deleteHealthCenter(1L);
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void deleteHealthCenterSoftDeletesCenter() {
        HealthCenter center = new HealthCenter();
        center.setIsActive(true);
        when(healthCenterRepository.findById(1L)).thenReturn(Optional.of(center));

        ServiceResult<Void> result = adminService.deleteHealthCenter(1L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(center.getIsActive()).isFalse();
        verify(adminAuditLogRepository).save(any(AdminAuditLog.class));
    }

    @Test
    void updateHealthCenterFailsWhenNotFound() {
        when(healthCenterRepository.findById(1L)).thenReturn(Optional.empty());
        ServiceResult<HealthCenter> result = adminService.updateHealthCenter(1L, new HealthCenter());
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void updateHealthCenterAppliesNewFields() {
        HealthCenter existing = new HealthCenter();
        when(healthCenterRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(healthCenterRepository.save(any(HealthCenter.class))).thenAnswer(inv -> inv.getArgument(0));

        HealthCenter updated = new HealthCenter();
        updated.setName("New Center");
        updated.setLatitude(-1.28);
        updated.setLongitude(36.82);
        updated.setStiTestingAvailable(true);
        updated.setIsActive(false);

        ServiceResult<HealthCenter> result = adminService.updateHealthCenter(1L, updated);

        assertThat(result.isSuccess()).isTrue();
        assertThat(existing.getName()).isEqualTo("New Center");
        assertThat(existing.getStiTestingAvailable()).isTrue();
        assertThat(existing.getIsActive()).isFalse();
        verify(adminAuditLogRepository).save(any(AdminAuditLog.class));
    }

    @Test
    void getAnswerStatisticsComputesAverageWhenQuestionsExist() {
        when(answerRepository.count()).thenReturn(10L);
        when(questionRepository.countByIsActiveTrue()).thenReturn(5L);

        var stats = adminService.getAnswerStatistics();

        assertThat(stats.get("avgAnswersPerQuestion")).isEqualTo("2.00");
    }

    @Test
    void getAnswerStatisticsHandlesZeroQuestions() {
        when(answerRepository.count()).thenReturn(0L);
        when(questionRepository.countByIsActiveTrue()).thenReturn(0L);

        var stats = adminService.getAnswerStatistics();

        assertThat(stats.get("avgAnswersPerQuestion")).isEqualTo("0.00");
    }

    @Test
    void getAnswerStatisticsFallsBackOnException() {
        when(answerRepository.count()).thenThrow(new RuntimeException("boom"));
        var stats = adminService.getAnswerStatistics();
        assertThat(stats.get("totalAnswers")).isEqualTo(0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getModelOpsStatsAggregatesGroupedCounts() {
        when(riskAssessmentRepository.count()).thenReturn(12L);
        when(riskAssessmentRepository.countGroupedByRiskLevel())
                .thenReturn(List.<Object[]>of(new Object[]{"Low", 7L}, new Object[]{"High", 5L}, new Object[]{null, 0L}));
        when(riskAssessmentRepository.countGroupedByModelVersion())
                .thenReturn(List.<Object[]>of(new Object[]{"ml-2026-07-17", 10L, 42.36}, new Object[]{null, 2L, null}));
        when(riskAssessmentRepository.countByDaySince(any(Date.class)))
                .thenReturn(List.<Object[]>of(new Object[]{java.sql.Date.valueOf("2026-07-19"), 3L}));

        var stats = adminService.getModelOpsStats();

        assertThat(stats.get("totalAssessments")).isEqualTo(12L);
        var riskLevels = (java.util.Map<String, Long>) stats.get("riskLevelCounts");
        assertThat(riskLevels).containsEntry("Low", 7L).containsEntry("High", 5L).containsEntry("Unknown", 0L);
        var versions = (List<java.util.Map<String, Object>>) stats.get("modelVersions");
        assertThat(versions).hasSize(2);
        assertThat(versions.get(0).get("modelVersion")).isEqualTo("ml-2026-07-17");
        assertThat(versions.get(0).get("avgRiskScore")).isEqualTo(42.4);
        assertThat(versions.get(1).get("modelVersion")).isEqualTo("unknown");
        assertThat(versions.get(1).get("avgRiskScore")).isNull();
        var perDay = (List<java.util.Map<String, Object>>) stats.get("assessmentsPerDay");
        assertThat(perDay).hasSize(1);
        assertThat(perDay.get(0).get("date")).isEqualTo("2026-07-19");
        assertThat(perDay.get(0).get("count")).isEqualTo(3L);
    }

    @Test
    void getModelOpsStatsFallsBackOnException() {
        when(riskAssessmentRepository.count()).thenThrow(new RuntimeException("boom"));

        var stats = adminService.getModelOpsStats();

        assertThat(stats.get("totalAssessments")).isEqualTo(0L);
        assertThat((java.util.Map<?, ?>) stats.get("riskLevelCounts")).isEmpty();
        assertThat((List<?>) stats.get("modelVersions")).isEmpty();
        assertThat((List<?>) stats.get("assessmentsPerDay")).isEmpty();
    }
}
